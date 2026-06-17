package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;

@Service
@Transactional
public class ChargeMonitoringService {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final int DEFAULT_SCALE = 2;
	private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
	private static final int HIGH_PRECISION_SCALE = 6;

	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	@Autowired
	private LoanStatementService loanStatementService;
	@Autowired
	private InstallmentRepository installmentRepository;

	/**
	 * Calculate total charges (interest + penalty) with null safety
	 */
	public BigDecimal getTotalCharges(MLoanApplication loan) {
		if (loan == null) {
			return BigDecimal.ZERO;
		}

		BigDecimal total = BigDecimal.ZERO;

		if (loan.getInterestsEarned() != null) {
			total = total.add(loan.getInterestsEarned());
		}

		if (loan.getPenaltyEarned() != null) {
			total = total.add(loan.getPenaltyEarned());
		}

		return total;
	}

	/**
	 * Calculate percentage of charges relative to principal amount Formula: (total
	 * charges / principal) * 100
	 */
	public BigDecimal getTotalPercentageCharged(MLoanApplication loan) {
		if (loan == null) {
			return BigDecimal.ZERO;
		}

		BigDecimal principal = loan.getApprovedAmount();
		if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}

		BigDecimal totalCharges = getTotalCharges(loan);

		try {
			// Calculate with high precision first, then round
			BigDecimal precisePercentage = totalCharges.multiply(ONE_HUNDRED)
					.divide(principal, HIGH_PRECISION_SCALE, DEFAULT_ROUNDING);
			return precisePercentage.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
		} catch (ArithmeticException e) {
			e.printStackTrace();
			return BigDecimal.ZERO;
		}
	}

	/**
	 * Calculate the proportion of each charge type in the total charges Returns an
	 * array where: - result[0] = interest proportion (0-1) - result[1] = penalty
	 * proportion (0-1)
	 */
	public BigDecimal[] calculateChargeProportions(MLoanApplication loan) {
		BigDecimal[] proportions = new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };

		if (loan == null) {
			return proportions;
		}

		BigDecimal interestEarned = loan.getInterestsEarned() != null ? loan.getInterestsEarned() : BigDecimal.ZERO;
		BigDecimal penaltyEarned = loan.getPenaltyEarned() != null ? loan.getPenaltyEarned() : BigDecimal.ZERO;
		BigDecimal totalCharges = interestEarned.add(penaltyEarned);
		
		if (totalCharges.compareTo(BigDecimal.ZERO) <= 0) {
			return proportions;
		}

		// Calculate proportions with high precision
		proportions[0] = interestEarned.divide(totalCharges, HIGH_PRECISION_SCALE, DEFAULT_ROUNDING);
		proportions[1] = penaltyEarned.divide(totalCharges, HIGH_PRECISION_SCALE, DEFAULT_ROUNDING);

		return proportions;
	}

	/**
	 * Check if we should continue charging based on caps Returns true if: 1. Caps
	 * are disabled OR 2. Current percentage is below allowed cap
	 */
	public boolean continueCharging(MLoanApplication loan) {
		if (loan == null || loan.getLoanProductConfiguration() == null) {
			return false;
		}

		if (!loan.getLoanProductConfiguration().isAllowOveralChargesCap()) {
			return true;
		}

		BigDecimal allowedPercentage = loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage();
		if (allowedPercentage == null || allowedPercentage.compareTo(BigDecimal.ZERO) <= 0) {
			return true;
		}

		BigDecimal currentPercentage = getTotalPercentageCharged(loan);
		return currentPercentage.compareTo(allowedPercentage) < 0;
	}

	/**
	 * Check if current charges exceed allowed percentage cap Returns true only when
	 * caps are enabled AND current percentage exceeds cap
	 */
	public boolean chargesExceedAllowedPercentageCap(MLoanApplication loan) {
		if (loan == null || loan.getLoanProductConfiguration() == null) {
			return false;
		}

		// If caps are disabled, we're never over the cap
		if (!loan.getLoanProductConfiguration().isAllowOveralChargesCap()) {
			return false;
		}

		BigDecimal allowedPercentage = loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage();
		if (allowedPercentage == null || allowedPercentage.compareTo(BigDecimal.ZERO) <= 0) {
			return false;
		}

		BigDecimal currentPercentage = getTotalPercentageCharged(loan);
		return currentPercentage.compareTo(allowedPercentage) > 0;
	}

	/**
	 * Calculate excess amount over the allowed cap
	 */
	public BigDecimal calculateExcessAmount(MLoanApplication loan) {
		if (loan == null || loan.getLoanProductConfiguration() == null) {
			return BigDecimal.ZERO;
		}

		BigDecimal principal = loan.getApprovedAmount();
		if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}

		BigDecimal allowedPercentage = loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage();
		BigDecimal currentPercentage = getTotalPercentageCharged(loan);

		if (allowedPercentage == null || currentPercentage.compareTo(allowedPercentage) <= 0) {
			return BigDecimal.ZERO;
		}

		// Calculate with high precision
		BigDecimal allowedCharges = principal.multiply(allowedPercentage)
				.divide(ONE_HUNDRED, HIGH_PRECISION_SCALE, DEFAULT_ROUNDING);
		
		BigDecimal currentCharges = getTotalCharges(loan);
		
		// Excess = current charges - allowed charges
		BigDecimal excess = currentCharges.subtract(allowedCharges);
		
		if (excess.compareTo(BigDecimal.ZERO) > 0) {
			return excess.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
		}
		
		return BigDecimal.ZERO;
	}

	/**
	 * Calculate the exact amount needed to bring charges down to allowed percentage
	 */
	public BigDecimal calculateRequiredExemption(MLoanApplication loan) {
		if (loan == null || loan.getLoanProductConfiguration() == null) {
			return BigDecimal.ZERO;
		}

		BigDecimal principal = loan.getApprovedAmount();
		if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}

		BigDecimal allowedPercentage = loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage();
		if (allowedPercentage == null) {
			return BigDecimal.ZERO;
		}

		BigDecimal currentCharges = getTotalCharges(loan);
		
		// Calculate maximum allowed charges
		BigDecimal allowedCharges = principal.multiply(allowedPercentage)
				.divide(ONE_HUNDRED, HIGH_PRECISION_SCALE, DEFAULT_ROUNDING)
				.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
		
		// Required exemption = current charges - allowed charges
		BigDecimal requiredExemption = currentCharges.subtract(allowedCharges);
		
		if (requiredExemption.compareTo(BigDecimal.ZERO) > 0) {
			return requiredExemption;
		}
		
		return BigDecimal.ZERO;
	}

	/**
	 * Exempt charges that exceed the allowed percentage cap This adjusts the loan
	 * balance and tracks exempted amounts proportionally BOTH at loan level AND
	 * installment level (only current installment)
	 */
	@Transactional
	public void exemptAmountOverChargesAboveAllowedPercentageCap(MLoanApplication loan) {
		if (loan == null || loan.getLoanProductConfiguration() == null) {
			return;
		}

		if (!chargesExceedAllowedPercentageCap(loan)) {
			return;
		}

		BigDecimal principal = loan.getApprovedAmount();
		if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}

		// Store percentage BEFORE exemption for logging
		BigDecimal percentageBeforeExemption = getTotalPercentageCharged(loan);
		
		// Calculate required exemption (more accurate than excess amount)
		BigDecimal requiredExemption = calculateRequiredExemption(loan);
		if (requiredExemption.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}

		BigDecimal currentBalance = loan.getBalance() != null ? loan.getBalance() : BigDecimal.ZERO;
		
		// We can only exempt up to the current balance
		BigDecimal amountToExempt = requiredExemption.min(currentBalance);

		if (amountToExempt.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}

		// Debug log before exemption
		debugLog(loan, "BEFORE EXEMPTION");
		
		// Get current earned amounts
		BigDecimal currentInterestEarned = loan.getInterestsEarned() != null ? loan.getInterestsEarned() : BigDecimal.ZERO;
		BigDecimal currentPenaltyEarned = loan.getPenaltyEarned() != null ? loan.getPenaltyEarned() : BigDecimal.ZERO;
		BigDecimal totalCurrentCharges = currentInterestEarned.add(currentPenaltyEarned);
		
		// Calculate proportions based on current charges
		BigDecimal interestProportion = BigDecimal.ZERO;
		BigDecimal penaltyProportion = BigDecimal.ZERO;
		
		if (totalCurrentCharges.compareTo(BigDecimal.ZERO) > 0) {
			interestProportion = currentInterestEarned.divide(totalCurrentCharges, HIGH_PRECISION_SCALE, DEFAULT_ROUNDING);
			penaltyProportion = currentPenaltyEarned.divide(totalCurrentCharges, HIGH_PRECISION_SCALE, DEFAULT_ROUNDING);
		}

		// Calculate exemptions
		BigDecimal interestExemption = amountToExempt.multiply(interestProportion).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
		BigDecimal penaltyExemption = amountToExempt.multiply(penaltyProportion).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);

		// Adjust for rounding differences
		BigDecimal totalAllocated = interestExemption.add(penaltyExemption);
		BigDecimal allocationDifference = amountToExempt.subtract(totalAllocated);
		
		if (allocationDifference.abs().compareTo(BigDecimal.ZERO) != 0) {
			// Apply rounding difference
			if (interestExemption.compareTo(penaltyExemption) >= 0) {
				interestExemption = interestExemption.add(allocationDifference);
			} else {
				penaltyExemption = penaltyExemption.add(allocationDifference);
			}
		}

		// Ensure exemptions don't exceed earned amounts
		if (interestExemption.compareTo(currentInterestEarned) > 0) {
			BigDecimal excess = interestExemption.subtract(currentInterestEarned);
			interestExemption = currentInterestEarned;
			penaltyExemption = penaltyExemption.add(excess);
		}
		
		if (penaltyExemption.compareTo(currentPenaltyEarned) > 0) {
			BigDecimal excess = penaltyExemption.subtract(currentPenaltyEarned);
			penaltyExemption = currentPenaltyEarned;
			interestExemption = interestExemption.add(excess);
		}
		
		// Final recalculation
		BigDecimal finalInterestExemption = interestExemption.compareTo(currentInterestEarned) > 0 
				? currentInterestEarned : interestExemption;
		BigDecimal finalPenaltyExemption = penaltyExemption.compareTo(currentPenaltyEarned) > 0 
				? currentPenaltyEarned : penaltyExemption;
		
		BigDecimal finalTotalExempted = finalInterestExemption.add(finalPenaltyExemption);
		
		// Update loan exemption totals - ADD to existing exemptions
		BigDecimal currentExemptedInterest = loan.getExemptedInterests() != null ? loan.getExemptedInterests() : BigDecimal.ZERO;
		BigDecimal newExemptedInterest = currentExemptedInterest.add(finalInterestExemption);

		BigDecimal currentExemptedPenalty = loan.getExemptedPenalties() != null ? loan.getExemptedPenalties() : BigDecimal.ZERO;
		BigDecimal newExemptedPenalty = currentExemptedPenalty.add(finalPenaltyExemption);

		BigDecimal currentTotalExempted = loan.getExemptedAmount() != null ? loan.getExemptedAmount() : BigDecimal.ZERO;
		BigDecimal newTotalExempted = currentTotalExempted.add(finalTotalExempted);

		// Reduce loan balance by the exempted amount
		BigDecimal newBalance = currentBalance.subtract(finalTotalExempted);
		if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
			newBalance = BigDecimal.ZERO;
		}

		// Apply changes to LOAN - update earned amounts
		BigDecimal newInterestEarned = currentInterestEarned.subtract(finalInterestExemption);
		if (newInterestEarned.compareTo(BigDecimal.ZERO) < 0) {
			newInterestEarned = BigDecimal.ZERO;
		}
		loan.setInterestsEarned(newInterestEarned);

		BigDecimal newPenaltyEarned = currentPenaltyEarned.subtract(finalPenaltyExemption);
		if (newPenaltyEarned.compareTo(BigDecimal.ZERO) < 0) {
			newPenaltyEarned = BigDecimal.ZERO;
		}
		loan.setPenaltyEarned(newPenaltyEarned);

		loan.setExemptedInterests(newExemptedInterest);
		loan.setExemptedPenalties(newExemptedPenalty);
		loan.setExemptedAmount(newTotalExempted);
		loan.setBalance(newBalance);
		loan.setExempted(true);

		loanApplicationRepository.save(loan);

		// Debug log after loan update
		debugLog(loan, "AFTER LOAN UPDATE");
		
		// Record waivers only if we actually exempted something
		if (finalInterestExemption.compareTo(BigDecimal.ZERO) > 0) {
			String defaultInterestReason = "Charge cap exemption - interest portion ("
					+ loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage() + "% cap exceeded)";
			loanStatementService.recordInterestWaiver(loan.getLoanApplicationId(), null, finalInterestExemption,
					defaultInterestReason, "CAP-EXEMPT-" + loan.getDocumentNo());
		}

		if (finalPenaltyExemption.compareTo(BigDecimal.ZERO) > 0) {
			String defaultPenaltyReason = "Charge cap exemption - penalty portion ("
					+ loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage() + "% cap exceeded)";
			loanStatementService.recordPenaltyWaiver(loan.getLoanApplicationId(), null, finalPenaltyExemption,
					defaultPenaltyReason, "CAP-EXEMPT-" + loan.getDocumentNo());
		}

		// Apply to current installment if exists
		MInstallments currentInstallment = installmentRepository
				.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(true, BigDecimal.ZERO, loan);

		if (currentInstallment != null) {
			BigDecimal installmentBalance = currentInstallment.getBalance() != null ? currentInstallment.getBalance()
					: BigDecimal.ZERO;

			// Only exempt from installment if it has balance
			if (installmentBalance.compareTo(BigDecimal.ZERO) > 0) {
				// Calculate how much of the total exemption should come from this installment
				// Based on installment's share of the total loan balance
				BigDecimal installmentShare = BigDecimal.ZERO;
				if (currentBalance.compareTo(BigDecimal.ZERO) > 0) {
					installmentShare = installmentBalance.divide(currentBalance, HIGH_PRECISION_SCALE, DEFAULT_ROUNDING);
				}
				
				BigDecimal instExemption = finalTotalExempted.multiply(installmentShare)
						.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
				BigDecimal instInterestExemption = finalInterestExemption.multiply(installmentShare)
						.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
				BigDecimal instPenaltyExemption = finalPenaltyExemption.multiply(installmentShare)
						.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
				
				// Adjust to not exceed installment balance
				if (instExemption.compareTo(installmentBalance) > 0) {
					instExemption = installmentBalance;
					// Recalculate proportions
					if (finalTotalExempted.compareTo(BigDecimal.ZERO) > 0) {
						instInterestExemption = instExemption.multiply(finalInterestExemption)
								.divide(finalTotalExempted, DEFAULT_SCALE, DEFAULT_ROUNDING);
						instPenaltyExemption = instExemption.subtract(instInterestExemption);
					}
				}

				// Update installment exemption totals
				BigDecimal currentInstExemptedInterest = currentInstallment.getExemptedInterests() != null
						? currentInstallment.getExemptedInterests() : BigDecimal.ZERO;
				BigDecimal newInstExemptedInterest = currentInstExemptedInterest.add(instInterestExemption);

				BigDecimal currentInstExemptedPenalty = currentInstallment.getExemptedPenalties() != null
						? currentInstallment.getExemptedPenalties() : BigDecimal.ZERO;
				BigDecimal newInstExemptedPenalty = currentInstExemptedPenalty.add(instPenaltyExemption);

				BigDecimal currentInstTotalExempted = currentInstallment.getExemptedAmount() != null
						? currentInstallment.getExemptedAmount() : BigDecimal.ZERO;
				BigDecimal newInstTotalExempted = currentInstTotalExempted.add(instExemption);

				BigDecimal newInstBalance = installmentBalance.subtract(instExemption);
				if (newInstBalance.compareTo(BigDecimal.ZERO) < 0) {
					newInstBalance = BigDecimal.ZERO;
				}

				// Update installment earned amounts
				BigDecimal currentInstInterestEarned = currentInstallment.getInterestEarned() != null 
						? currentInstallment.getInterestEarned() : BigDecimal.ZERO;
				BigDecimal currentInstPenaltyEarned = currentInstallment.getPenaltyEarned() != null 
						? currentInstallment.getPenaltyEarned() : BigDecimal.ZERO;
				
				BigDecimal newInstInterestEarned = currentInstInterestEarned.subtract(instInterestExemption);
				if (newInstInterestEarned.compareTo(BigDecimal.ZERO) < 0) {
					newInstInterestEarned = BigDecimal.ZERO;
				}
				currentInstallment.setInterestEarned(newInstInterestEarned);
				
				BigDecimal newInstPenaltyEarned = currentInstPenaltyEarned.subtract(instPenaltyExemption);
				if (newInstPenaltyEarned.compareTo(BigDecimal.ZERO) < 0) {
					newInstPenaltyEarned = BigDecimal.ZERO;
				}
				currentInstallment.setPenaltyEarned(newInstPenaltyEarned);

				currentInstallment.setExemptedInterests(newInstExemptedInterest);
				currentInstallment.setExemptedPenalties(newInstExemptedPenalty);
				currentInstallment.setExemptedAmount(newInstTotalExempted);
				currentInstallment.setBalance(newInstBalance);
				currentInstallment.setExempted(true);

				installmentRepository.save(currentInstallment);

				// Record installment waivers
				if (instInterestExemption.compareTo(BigDecimal.ZERO) > 0) {
					String instInterestReason = "Installment charge cap exemption - interest portion ("
							+ loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage() + "% cap exceeded)";
					loanStatementService.recordInterestWaiver(null, currentInstallment.getInstallmentId(),
							instInterestExemption, instInterestReason,
							"CAP-EXEMPT-" + loan.getDocumentNo() + "-INST-" + currentInstallment.getDocumentNo());
				}

				if (instPenaltyExemption.compareTo(BigDecimal.ZERO) > 0) {
					String instPenaltyReason = "Installment charge cap exemption - penalty portion ("
							+ loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage() + "% cap exceeded)";
					loanStatementService.recordPenaltyWaiver(null, currentInstallment.getInstallmentId(),
							instPenaltyExemption, instPenaltyReason,
							"CAP-EXEMPT-" + loan.getDocumentNo() + "-INST-" + currentInstallment.getDocumentNo());
				}
			}
		}

		// Final debug log
		debugLog(loan, "AFTER COMPLETE EXEMPTION");
		
		// Log exemption details
		logExemption(loan, percentageBeforeExemption, requiredExemption, finalTotalExempted, 
				finalInterestExemption, finalPenaltyExemption);
	}

	/**
	 * Helper method for debugging
	 */
	private void debugLog(MLoanApplication loan, String stage) {
		if (loan == null) return;
		
		System.out.printf("[DEBUG %s] Loan ID: %d%n", stage, loan.getLoanApplicationId());
		System.out.printf("  - Balance: %s%n", loan.getBalance());
		System.out.printf("  - Interest Earned: %s%n", loan.getInterestsEarned());
		System.out.printf("  - Penalty Earned: %s%n", loan.getPenaltyEarned());
		System.out.printf("  - Total Charges: %s%n", getTotalCharges(loan));
		System.out.printf("  - Percentage Charged: %s%%%n", getTotalPercentageCharged(loan));
		System.out.printf("  - Principal: %s%n", loan.getApprovedAmount());
		if (loan.getExemptedInterests() != null || loan.getExemptedPenalties() != null) {
			System.out.printf("  - Previously Exempted Interest: %s%n", loan.getExemptedInterests());
			System.out.printf("  - Previously Exempted Penalty: %s%n", loan.getExemptedPenalties());
		}
		System.out.println();
	}

	/**
	 * Helper method to log exemption details
	 */
	private void logExemption(MLoanApplication loan, BigDecimal percentageBeforeExemption, BigDecimal requiredExemption,
			BigDecimal totalExempted, BigDecimal interestExemption, BigDecimal penaltyExemption) {

		BigDecimal allowedPercentage = loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage();
		BigDecimal percentageAfterExemption = getTotalPercentageCharged(loan);
		
		// Calculate what percentage SHOULD be after exemption
		BigDecimal principal = loan.getApprovedAmount();
		BigDecimal allowedCharges = principal.multiply(allowedPercentage)
				.divide(ONE_HUNDRED, HIGH_PRECISION_SCALE, DEFAULT_ROUNDING)
				.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
		BigDecimal remainingCharges = getTotalCharges(loan);
		BigDecimal expectedPercentage = remainingCharges.multiply(ONE_HUNDRED)
				.divide(principal, HIGH_PRECISION_SCALE, DEFAULT_ROUNDING)
				.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);

		System.out.printf("=== CHARGE EXEMPTION DETAILS ===%n");
		System.out.printf("Loan ID: %d%n", loan.getLoanApplicationId());
		System.out.printf("Principal: %s%n", principal);
		System.out.printf("Allowed Percentage: %s%%%n", allowedPercentage);
		System.out.printf("Maximum Allowed Charges: %s%n", allowedCharges);
		System.out.printf("Percentage BEFORE Exemption: %s%%%n", percentageBeforeExemption);
		System.out.printf("Percentage AFTER Exemption: %s%%%n", percentageAfterExemption);
		System.out.printf("Expected Percentage (calculated): %s%%%n", expectedPercentage);
		System.out.printf("Required Exemption (to reach cap): %s%n", requiredExemption);
		System.out.printf("Total Exempted (this run): %s%n", totalExempted);
		System.out.printf("  - Interest Exemption: %s%n", interestExemption);
		System.out.printf("  - Penalty Exemption: %s%n", penaltyExemption);
		System.out.printf("Total Exempted Interest (cumulative): %s%n",
				loan.getExemptedInterests() != null ? loan.getExemptedInterests() : BigDecimal.ZERO);
		System.out.printf("Total Exempted Penalty (cumulative): %s%n",
				loan.getExemptedPenalties() != null ? loan.getExemptedPenalties() : BigDecimal.ZERO);
		System.out.printf("Remaining Interest Earned: %s%n", loan.getInterestsEarned());
		System.out.printf("Remaining Penalty Earned: %s%n", loan.getPenaltyEarned());
		System.out.printf("Remaining Total Charges: %s%n", getTotalCharges(loan));
		System.out.printf("New Balance: %s%n", loan.getBalance());
		System.out.printf("================================%n%n");
	}

	/**
	 * Utility method to check if loan qualifies for charge monitoring
	 */
	public boolean isLoanEligibleForChargeMonitoring(MLoanApplication loan) {
		if (loan == null || loan.getLoanProductConfiguration() == null) {
			return false;
		}

		// Check if loan has principal amount
		BigDecimal principal = loan.getApprovedAmount();
		if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
			return false;
		}

		// Check if loan has charges cap configuration
		return loan.getLoanProductConfiguration().isAllowOveralChargesCap()
				&& loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage() != null
				&& loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage()
						.compareTo(BigDecimal.ZERO) > 0;
	}

	/**
	 * Get detailed breakdown of charge proportions Useful for reporting and
	 * debugging
	 */
	public ChargeBreakdown getChargeBreakdown(MLoanApplication loan) {
		ChargeBreakdown breakdown = new ChargeBreakdown();

		if (loan == null) {
			return breakdown;
		}

		breakdown.setPrincipal(loan.getApprovedAmount());
		breakdown.setInterestEarned(loan.getInterestsEarned());
		breakdown.setPenaltyEarned(loan.getPenaltyEarned());
		breakdown.setTotalCharges(getTotalCharges(loan));
		breakdown.setPercentageCharged(getTotalPercentageCharged(loan));

		BigDecimal[] proportions = calculateChargeProportions(loan);
		breakdown.setInterestProportion(proportions[0]);
		breakdown.setPenaltyProportion(proportions[1]);

		if (loan.getLoanProductConfiguration() != null) {
			breakdown.setAllowedPercentage(loan.getLoanProductConfiguration().getAllowedOveralChargesCapPercentage());
			breakdown.setCapsEnabled(loan.getLoanProductConfiguration().isAllowOveralChargesCap());
		}

		return breakdown;
	}

	/**
	 * DTO for charge breakdown information
	 */
	public static class ChargeBreakdown {
		private BigDecimal principal;
		private BigDecimal interestEarned;
		private BigDecimal penaltyEarned;
		private BigDecimal totalCharges;
		private BigDecimal percentageCharged;
		private BigDecimal interestProportion;
		private BigDecimal penaltyProportion;
		private BigDecimal allowedPercentage;
		private boolean capsEnabled;

		// Getters and setters
		public BigDecimal getPrincipal() {
			return principal;
		}

		public void setPrincipal(BigDecimal principal) {
			this.principal = principal;
		}

		public BigDecimal getInterestEarned() {
			return interestEarned;
		}

		public void setInterestEarned(BigDecimal interestEarned) {
			this.interestEarned = interestEarned;
		}

		public BigDecimal getPenaltyEarned() {
			return penaltyEarned;
		}

		public void setPenaltyEarned(BigDecimal penaltyEarned) {
			this.penaltyEarned = penaltyEarned;
		}

		public BigDecimal getTotalCharges() {
			return totalCharges;
		}

		public void setTotalCharges(BigDecimal totalCharges) {
			this.totalCharges = totalCharges;
		}

		public BigDecimal getPercentageCharged() {
			return percentageCharged;
		}

		public void setPercentageCharged(BigDecimal percentageCharged) {
			this.percentageCharged = percentageCharged;
		}

		public BigDecimal getInterestProportion() {
			return interestProportion;
		}

		public void setInterestProportion(BigDecimal interestProportion) {
			this.interestProportion = interestProportion;
		}

		public BigDecimal getPenaltyProportion() {
			return penaltyProportion;
		}

		public void setPenaltyProportion(BigDecimal penaltyProportion) {
			this.penaltyProportion = penaltyProportion;
		}

		public BigDecimal getAllowedPercentage() {
			return allowedPercentage;
		}

		public void setAllowedPercentage(BigDecimal allowedPercentage) {
			this.allowedPercentage = allowedPercentage;
		}

		public boolean isCapsEnabled() {
			return capsEnabled;
		}

		public void setCapsEnabled(boolean capsEnabled) {
			this.capsEnabled = capsEnabled;
		}

		@Override
		public String toString() {
			return String.format(
					"ChargeBreakdown{principal=%s, interestEarned=%s, penaltyEarned=%s, "
							+ "totalCharges=%s, percentageCharged=%s%%, interestProportion=%s, "
							+ "penaltyProportion=%s, allowedPercentage=%s%%, capsEnabled=%s}",
					principal, interestEarned, penaltyEarned, totalCharges, percentageCharged, interestProportion,
					penaltyProportion, allowedPercentage, capsEnabled);
		}
	}
}