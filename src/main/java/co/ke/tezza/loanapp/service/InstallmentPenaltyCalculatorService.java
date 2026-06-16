package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.entity.MPayments;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.PenaltyBaseEnum;
import co.ke.tezza.loanapp.enums.PenaltyCalculationBaseEnum;
import co.ke.tezza.loanapp.repository.PaymentRepository;

@Service
public class InstallmentPenaltyCalculatorService {

	private static final Logger logger = LoggerFactory.getLogger(InstallmentPenaltyCalculatorService.class);

	@Autowired
	private PaymentRepository paymentRepository;

	/**
	 * Check if installment penalty should be calculated Returns true only if: 1.
	 * config.isInstallmentDueChargePenalty = true AND 2. Either: a.
	 * config.isLoanOverDueChargePenaltyInstallmentDue = false (installments don't
	 * override loan) b. OR config.isLoanOverDueChargePenaltyInstallmentDue = true
	 * (installments override loan)
	 */
	private boolean shouldCalculateInstallmentPenalty(MLoanApplication loan) {
		if (loan == null || loan.getLoanProductConfiguration() == null) {
			return false;
		}

		MLoanProductConfiguration config = loan.getLoanProductConfiguration();

		// Check if installment penalties are enabled at all
		if (!config.isInstallmentDueChargePenalty()) {
			return false;
		}

		// If installment penalties are enabled, check the override flag
		// We calculate if either:
		// 1. Installments don't override loan penalties (false) - we calculate
		// installment penalties separately
		// 2. Installments do override loan penalties (true) - we calculate installment
		// penalties instead of loan penalties
		return config.isLoanOverDueChargePenaltyInstallmentDue() || !config.isLoanOverDueChargePenaltyInstallmentDue();
	}

	/**
	 * Get dates with payments within the period (INCLUSIVE of both dates)
	 */
	private Set<LocalDate> getPaymentDatesWithinPeriod(LocalDate startDate, LocalDate endDate, MLoanApplication loan) {
		Set<LocalDate> paymentDates = new HashSet<>();

		logger.debug("Getting payments for loan ID: {}, Date range: {} to {}", loan.getLoanApplicationId(), startDate,
				endDate);

		List<MPayments> payments = paymentRepository.findByIsActiveAndAdOrgIDAndLoanAndDocStatusOrderByPaymentIdDesc(
				true, loan.getAdOrgID(), loan, DocStatus.COMPLETED);

		logger.debug("Found {} completed payments for loan ID: {}", payments.size(), loan.getLoanApplicationId());

		if (!payments.isEmpty()) {
			for (MPayments pay : payments) {
				LocalDateTime paymentDateTime = pay.getPaymentDateTime();
				if (paymentDateTime == null || pay.getAmount() == null
						|| pay.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}

				LocalDate paymentDate = paymentDateTime.toLocalDate();

				// Include payments on or between dates (inclusive)
				if (!paymentDate.isBefore(startDate) && !paymentDate.isAfter(endDate)) {
					paymentDates.add(paymentDate);
					logger.debug("Payment included: ID={}, Date={}, Amount={}", pay.getPaymentId(), paymentDate,
							pay.getAmount());
				}
			}
		}

		logger.debug("Total payment dates in range {} to {}: {}", startDate, endDate, paymentDates.size());
		return paymentDates;
	}

	/**
	 * Consolidated logging method for penalty calculation summary
	 */
	private void logPenaltySummary(MInstallments installment, MLoanApplication loan, MLoanProductConfiguration config,
			Date penaltyStartDate, int effectiveDaysOverdue, BigDecimal penaltyBaseAmount, BigDecimal penaltyAmount,
			String calculationType) {

		BigDecimal principal = loan.getApprovedAmount() != null ? loan.getApprovedAmount() : loan.getAppliedAmount();
		BigDecimal loanBalance = loan.getBalance();
		BigDecimal installmentBalance = installment.getBalance();

		String summary = String.format(
				"PENALTY_CHARGED | Installment: %d | Loan: %d | Period: %s to %s | "
						+ "Grace: %d days | Effective Days: %d | Config: PPSP=%s, PROOD=%s | "
						+ "Principal: %s | Loan Bal: %s | Installment Bal: %s | "
						+ "Penalty Base: %s | Base Amount: %s | Rate: %s%% | " + "Calculation: %s | Penalty: %s",
				installment.getInstallmentId(), loan.getLoanApplicationId(),
				penaltyStartDate != null ? penaltyStartDate.toString() : "N/A", new Date(),
				config.getPenaltyGracePeriodDays() != null ? config.getPenaltyGracePeriodDays() : 0,
				effectiveDaysOverdue, config.isPeriodPaymentStopPenalty(), config.isPaymentReliefOnOverdueDebt(),
				principal.setScale(2, RoundingMode.HALF_UP), loanBalance.setScale(2, RoundingMode.HALF_UP),
				installmentBalance.setScale(2, RoundingMode.HALF_UP), config.getPenaltyAppliesTo(),
				penaltyBaseAmount.setScale(2, RoundingMode.HALF_UP),
				config.getPenaltyRatePercent() != null
						? config.getPenaltyRatePercent().setScale(2, RoundingMode.HALF_UP)
						: "0.00",
				calculationType, penaltyAmount.setScale(2, RoundingMode.HALF_UP));

		logger.info(summary);
	}

	/**
	 * Get the base penalty start date for installment If lastPenaltyCalculationDate
	 * is null, use periodEnd + gracePeriod
	 */
	private Date getPenaltyCalculationStartDate(MInstallments installment, MLoanProductConfiguration config) {
		Date periodEnd = installment.getPeriodEnd();
		Integer graceDays = installment.getGracePeriod() != null ? installment.getGracePeriod()
				: (config.getPenaltyGracePeriodDays() != null ? config.getPenaltyGracePeriodDays() : 0);

		LocalDate periodEndLocal = periodEnd.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate penaltyStartLocal = periodEndLocal.plusDays(graceDays);

		return Date.from(penaltyStartLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Calculate the start date for current penalty calculation period for
	 * installment FIXED FOR ISSUE 1: Always use the maximum of
	 * (nextPenaltyCalculationDate, gracePeriodEndDate) to ensure penalty is charged
	 * immediately when grace period ends
	 */
	private Date getCurrentPeriodStartDate(MInstallments installment, MLoanProductConfiguration config,
			Date calculationDate) {
		logger.debug("Calculating current period start date for installment ID: {}", installment.getInstallmentId());
		logger.debug("Next Penalty Date: {}, Last Penalty Date: {}", installment.getNextPenaltyCalculationDate(),
				installment.getLastPenaltyCalculationDate());

		Integer graceDays = installment.getGracePeriod() != null ? installment.getGracePeriod()
				: (config.getPenaltyGracePeriodDays() != null ? config.getPenaltyGracePeriodDays() : 0);

		// Get the base grace period end date (when penalty eligibility starts)
		Date gracePeriodEndDate = getPenaltyCalculationStartDate(installment, config);

		// Convert calculation date to LocalDate for comparison
		LocalDate calculationLocal = calculationDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		// FIX FOR ISSUE 1: If we're already past the grace period end date,
		// we should start calculating from that date, not wait for next scheduled date
		if (calculationLocal.isAfter(gracePeriodEndDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())) {
			// We're already past grace period - check if we have a next penalty date
			if (installment.getNextPenaltyCalculationDate() != null) {
				// Use the LATER of next penalty date or grace period end
				LocalDate nextCalcLocal = installment.getNextPenaltyCalculationDate().toInstant()
						.atZone(ZoneId.systemDefault()).toLocalDate();
				LocalDate graceEndLocal = gracePeriodEndDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

				LocalDate periodStartLocal;
				if (nextCalcLocal.isAfter(graceEndLocal)) {
					periodStartLocal = nextCalcLocal;
				} else {
					periodStartLocal = graceEndLocal;
				}

				logger.debug(
						"Past grace period - using max of nextPenaltyDate({}) and graceEnd({}) = {} as period start",
						nextCalcLocal, graceEndLocal, periodStartLocal);

				return Date.from(periodStartLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
			} else {
				// No next penalty date, use grace period end
				logger.debug("Past grace period, no next penalty date - using grace period end {} as period start",
						gracePeriodEndDate);
				return gracePeriodEndDate;
			}
		}

		// RULE 1: If we have a scheduled next calculation date, use it
		if (installment.getNextPenaltyCalculationDate() != null) {
			LocalDate nextCalcLocal = installment.getNextPenaltyCalculationDate().toInstant()
					.atZone(ZoneId.systemDefault()).toLocalDate();

			logger.debug("Using next penalty date {} as period start", nextCalcLocal);

			return Date.from(nextCalcLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
		}

		// RULE 2: If last calculation exists, use last calculation + frequency
		if (installment.getLastPenaltyCalculationDate() != null) {
			Date frequencyBasedDate = calculateDateWithFrequency(installment.getLastPenaltyCalculationDate(), config,
					false);

			if (frequencyBasedDate != null) {
				logger.debug("Using last calculation date {} + frequency = {} as period start",
						installment.getLastPenaltyCalculationDate(), frequencyBasedDate);

				return frequencyBasedDate;
			}
		}

		// RULE 3: Default to grace period end
		logger.debug("Using default grace period end = {} as period start", gracePeriodEndDate);

		return gracePeriodEndDate;
	}

	/**
	 * Calculate date by adding frequency period
	 * 
	 * @param baseDate      The base date to add frequency to
	 * @param config        Loan product configuration
	 * @param forNextPeriod If true, calculate for next period; if false, calculate
	 *                      based on current period
	 * @return Date with frequency period added
	 */
	private Date calculateDateWithFrequency(Date baseDate, MLoanProductConfiguration config, boolean forNextPeriod) {
		if (baseDate == null || config == null) {
			return null;
		}

		LocalDate baseLocal = baseDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		PenaltyCalculationBaseEnum calculationBase = config.getDefaultPenaltyCalculationBase();

		if (calculationBase == null) {
			calculationBase = PenaltyCalculationBaseEnum.PER_DAY;
		}

		LocalDate resultLocal;

		switch (calculationBase) {
		case PER_DAY:
			Integer frequencyDays = config.getPenaltyFrequencyDays();
			if (frequencyDays != null && frequencyDays > 0) {
				resultLocal = baseLocal.plusDays(forNextPeriod ? frequencyDays : 0);
			} else {
				resultLocal = baseLocal.plusDays(forNextPeriod ? 1 : 0);
			}
			break;

		case PER_WEEK:
			resultLocal = baseLocal.plusWeeks(forNextPeriod ? 1 : 0);
			break;

		case PER_MONTH:
			resultLocal = baseLocal.plusMonths(forNextPeriod ? 1 : 0);
			break;

		case PER_CYCLE:
			Integer cycleDays = config.getPenaltyFrequencyDays();
			if (cycleDays == null || cycleDays <= 0) {
				cycleDays = 30;
			}
			resultLocal = baseLocal.plusDays(forNextPeriod ? cycleDays : 0);
			break;

		case ONCE:
			// For one-time penalty
			if (forNextPeriod) {
				return null; // No next date for one-time penalty
			} else {
				resultLocal = baseLocal; // For current period, use the same date
			}
			break;

		default:
			resultLocal = baseLocal.plusDays(forNextPeriod ? 1 : 0);
			break;
		}

		return Date.from(resultLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Calculate next penalty calculation date This should be: current calculation
	 * date + frequency period
	 */
	private Date calculateNextPenaltyCalculationDate(Date calculationDate, MLoanProductConfiguration config) {
		if (calculationDate == null || config == null) {
			return null;
		}

		PenaltyCalculationBaseEnum calculationBase = config.getDefaultPenaltyCalculationBase();

		// For one-time penalty, there is no next date
		if (calculationBase == PenaltyCalculationBaseEnum.ONCE) {
			return null;
		}

		// Calculate next date by adding frequency to the current calculation date
		return calculateDateWithFrequency(calculationDate, config, true);
	}

	/**
	 * This method applies penalty calculation logic with the corrected
	 * requirements: Only skip penalty for specific days when payments were made,
	 * not the entire period
	 */
	public PenaltyCalculationResult calculateInstallmentPenalty(MInstallments installment) {
		logger.debug("=== STARTING INSTALLMENT PENALTY CALCULATION FOR INSTALLMENT ID: {} ===",
				installment.getInstallmentId());

		if (installment == null || installment.getLoan() == null) {
			logger.error("Invalid installment or loan is null");
			return new PenaltyCalculationResult(BigDecimal.ZERO, 0, "Invalid installment", null, null);
		}

		MLoanApplication loan = installment.getLoan();
		MLoanProductConfiguration config = loan.getLoanProductConfiguration();
		Date calculationDate = new Date();
		LocalDate calculationLocal = calculationDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		// Log current state before calculation
		logger.debug("CURRENT INSTALLMENT STATE:");
		logger.debug("  - Installment Period End: {}", installment.getPeriodEnd());
		logger.debug("  - Last Penalty Calculation Date: {}", installment.getLastPenaltyCalculationDate());
		logger.debug("  - Next Penalty Calculation Date: {}", installment.getNextPenaltyCalculationDate());
		logger.debug("  - Installment Grace Period: {}", installment.getGracePeriod());
		logger.debug("  - Config Grace Period: {}", config.getPenaltyGracePeriodDays());

		// Check if we should calculate installment penalty
		if (!shouldCalculateInstallmentPenalty(loan)) {
			logger.debug("Skipping penalty calculation - installment penalties not enabled");
			Date nextPenaltyDate = calculateNextPenaltyCalculationDate(calculationDate, config);
			return new PenaltyCalculationResult(BigDecimal.ZERO, 0, "No penalty - installment penalties not enabled",
					nextPenaltyDate, calculationDate);
		}

		// Get penalty calculation start date (periodEnd + gracePeriod)
		Date penaltyStartDate = getPenaltyCalculationStartDate(installment, config);
		LocalDate penaltyStartLocal = penaltyStartDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		// Check if we're past the penalty eligibility date
		// FIX FOR ISSUE 1: Use inclusive check (isBefore) instead of exclusive
		if (calculationLocal.isBefore(penaltyStartLocal)) {
			logger.debug("Installment within grace period: {} < {}", calculationLocal, penaltyStartLocal);

			// Schedule next penalty calculation for when grace period ends
			Date nextPenaltyDate = penaltyStartDate;
			return new PenaltyCalculationResult(BigDecimal.ZERO, 0, "No penalty - installment within grace",
					nextPenaltyDate, calculationDate);
		}

		// Get current period start date based on last/next calculation dates
		// FIX FOR ISSUE 1: Pass calculationDate to handle immediate penalty charging
		Date currentPeriodStartDate = getCurrentPeriodStartDate(installment, config, calculationDate);
		LocalDate currentPeriodStartLocal = currentPeriodStartDate.toInstant().atZone(ZoneId.systemDefault())
				.toLocalDate();

		// FIX FOR ISSUE 1: If current period start is after calculation date,
		// adjust it to be the penalty start date
		if (calculationLocal.isBefore(currentPeriodStartLocal)) {
			// We should have started calculating from penalty start date
			currentPeriodStartDate = penaltyStartDate;
			currentPeriodStartLocal = penaltyStartLocal;
			logger.debug("Adjusted period start to penalty start date: {}", currentPeriodStartDate);
		}

		// Determine if we should use inclusive counting
		// Use inclusive counting when nextPenaltyCalculationDate is not null OR when
		// we're at grace period end
		boolean useInclusiveCounting = installment.getNextPenaltyCalculationDate() != null
				|| calculationLocal.equals(penaltyStartLocal);
		logger.debug("Using inclusive counting: {} (nextPenaltyDate is {}null, at grace end: {})", useInclusiveCounting,
				installment.getNextPenaltyCalculationDate() == null ? "" : "not ",
				calculationLocal.equals(penaltyStartLocal));

		// Calculate effective days from current period start to calculation date
		int effectiveDaysOverdue = calculateEffectiveDaysOverdue(currentPeriodStartDate, calculationDate, config, loan,
				useInclusiveCounting);

		logger.debug("CALCULATION DETAILS:");
		logger.debug("  - Penalty Start Date: {}", penaltyStartDate);
		logger.debug("  - Current Period Start Date: {}", currentPeriodStartDate);
		logger.debug("  - Calculation Date: {}", calculationDate);
		logger.debug("  - Effective Days Overdue: {}", effectiveDaysOverdue);
		logger.debug("  - Calculation Base: {}", config.getDefaultPenaltyCalculationBase());
		logger.debug("  - PeriodPaymentStopPenalty: {}", config.isPeriodPaymentStopPenalty());
		logger.debug("  - PaymentReliefOnOverdueDebt: {}", config.isPaymentReliefOnOverdueDebt());

		if (effectiveDaysOverdue <= 0) {
			logger.debug("No effective days overdue for this period");
			Date nextPenaltyDate = calculateNextPenaltyCalculationDate(calculationDate, config);
			return new PenaltyCalculationResult(BigDecimal.ZERO, 0, "No effective days overdue for this period",
					nextPenaltyDate, calculationDate);
		}

		BigDecimal principal = loan.getApprovedAmount() != null ? loan.getApprovedAmount() : loan.getAppliedAmount();
		BigDecimal loanBalance = loan.getBalance();
		BigDecimal installmentBalance = installment.getBalance();
		BigDecimal overdueAmount = installmentBalance;

		PenaltyBaseEnum penaltyBase = config.getPenaltyAppliesTo();
		if (penaltyBase == null) {
			penaltyBase = PenaltyBaseEnum.PRINCIPAL;
		}

		BigDecimal outstandingBalanceToPass;
		switch (penaltyBase) {
		case PRINCIPAL:
			outstandingBalanceToPass = principal;
			break;
		case FULL_LOAN_BALANCE:
			outstandingBalanceToPass = loanBalance;
			break;
		case CURRENT_INSTALLMENT_OVERDUE:
			outstandingBalanceToPass = overdueAmount;
			break;
		default:
			outstandingBalanceToPass = principal;
			break;
		}

		// Calculate penalty with effective days
		PenaltyCalculationResult result = calculatePenaltyWithEffectiveDays(principal, outstandingBalanceToPass,
				overdueAmount, installment.getPeriodEnd(), calculationDate, installment.getLastPenaltyCalculationDate(),
				config, effectiveDaysOverdue);

		// Calculate next penalty date for NEXT calculation cycle
		Date nextPenaltyDate = calculateNextPenaltyCalculationDate(calculationDate, config);

		logger.debug("CALCULATION RESULT:");
		logger.debug("  - Penalty Amount: {}", result.getPenaltyAmount());
		logger.debug("  - Next Penalty Date: {}", nextPenaltyDate);

		// Create result with next penalty date
		result = new PenaltyCalculationResult(result.getPenaltyAmount(), result.getDaysOverdue(), result.getMessage(),
				nextPenaltyDate, calculationDate);

		// Only log when penalty is actually charged
		if (result.getPenaltyAmount().compareTo(BigDecimal.ZERO) > 0) {
			logPenaltySummary(installment, loan, config, currentPeriodStartDate, effectiveDaysOverdue,
					getPenaltyBaseAmount(principal, outstandingBalanceToPass, overdueAmount, penaltyBase),
					result.getPenaltyAmount(), getCalculationType(config));
		}

		logger.debug("=== INSTALLMENT PENALTY CALCULATION COMPLETED ===");
		return result;
	}

	/**
	 * Get calculation type for logging
	 */
	private String getCalculationType(MLoanProductConfiguration config) {
		if (config.getDefaultPenaltyCalculationBase() != null) {
			return config.getDefaultPenaltyCalculationBase().toString()
					+ (shouldCompoundPenalty(config.getPenaltyAppliesTo()) ? "_COMPOUNDED" : "_SIMPLE");
		}
		return "DAILY_SIMPLE";
	}

	/**
	 * Calculate effective days overdue based on payment exclusion rules FIXED: Use
	 * inclusive counting (+1) only when nextPenaltyCalculationDate is used Use
	 * exclusive counting when periodEnd + gracePeriod is used
	 */
	public int calculateEffectiveDaysOverdue(Date startDate, Date endDate, MLoanProductConfiguration config,
			MLoanApplication loan, boolean useInclusiveCounting) {
		logger.debug("=== CALCULATING EFFECTIVE DAYS OVERDUE ===");
		logger.debug("Start Date: {}, End Date: {}", startDate, endDate);
		logger.debug("Loan ID: {}, Loan Due Date: {}", loan.getLoanApplicationId(),
				loan.getDueDate() != null ? loan.getDueDate() : "null");
		logger.debug("Use inclusive counting: {}", useInclusiveCounting);

		if (startDate == null || endDate == null) {
			logger.error("startDate or endDate is null");
			return 0;
		}

		LocalDate startLocal = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate endLocal = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		logger.debug("Local Dates - Start: {}, End: {}", startLocal, endLocal);

		if (startLocal.isAfter(endLocal)) {
			logger.error("startLocal is after endLocal: {} > {}", startLocal, endLocal);
			return 0;
		}

		// Calculate total calendar days - conditional inclusive counting
		long totalDays;
		if (useInclusiveCounting) {
			// Use inclusive counting for nextPenaltyCalculationDate periods
			totalDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal) + 1;
			logger.debug("Total calendar days between dates (INCLUSIVE): {}", totalDays);
		} else {
			// Use exclusive counting for periodEnd + gracePeriod periods
			totalDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal);
			logger.debug("Total calendar days between dates (EXCLUSIVE): {}", totalDays);
		}

		// Check if payment exclusion is configured
		boolean periodPaymentStopPenalty = config.isPeriodPaymentStopPenalty();
		boolean paymentReliefOnOverdueDebt = config.isPaymentReliefOnOverdueDebt();

		// Determine if loan is overdue
		boolean isLoanOverdue = isLoanOverdue(loan, endLocal);

		// Detailed overdue check logging
		if (loan.getDueDate() != null) {
			LocalDate loanDueDate = loan.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			logger.debug("Loan Due Date: {}, Calculation Date: {}, Is Overdue: {}", loanDueDate, endLocal,
					isLoanOverdue);
		}

		logger.debug("=== CONFIGURATION CHECKS ===");
		logger.debug("PeriodPaymentStopPenalty: {}", periodPaymentStopPenalty);
		logger.debug("PaymentReliefOnOverdueDebt: {}", paymentReliefOnOverdueDebt);
		logger.debug("IsLoanOverdue: {}", isLoanOverdue);

		// Apply the new logic based on the configuration
		if (!periodPaymentStopPenalty) {
			logger.debug("PERIOD PAYMENT STOP PENALTY IS FALSE - returning all {} days", totalDays);
			return (int) totalDays;
		}

		logger.debug("PERIOD PAYMENT STOP PENALTY IS TRUE - checking payment relief logic");

		if (totalDays <= 0) {
			logger.debug("Total days <= 0: {}", totalDays);
			return (int) totalDays;
		}

		// Get all payment dates in the period
		Set<LocalDate> paymentDates = getPaymentDatesWithinPeriod(startLocal, endLocal, loan);

		logger.debug("Found {} payment dates in period {} to {}", paymentDates.size(), startLocal, endLocal);

		// Log each payment date found
		if (!paymentDates.isEmpty()) {
			logger.debug("Payment dates found:");
			for (LocalDate paymentDate : paymentDates) {
				logger.debug("  - {}", paymentDate);
			}
		}

		// CRITICAL FIX: Check if we should apply payment exclusion
		if (!paymentReliefOnOverdueDebt && isLoanOverdue) {
			// If relief is NOT allowed AND loan IS overdue → NO payment exclusion
			logger.debug("=== PAYMENT RELIEF LOGIC ===");
			logger.debug("PaymentReliefOnOverdueDebt=FALSE and loan IS overdue");
			logger.debug("RETURNING ALL {} DAYS (NO PAYMENT EXCLUSION)", totalDays);
			return (int) totalDays;
		}

		if (paymentDates.isEmpty()) {
			logger.debug("No payment dates found - returning all {} days", totalDays);
			return (int) totalDays;
		}

		// Apply the new logic for payment relief on overdue debt
		if (paymentReliefOnOverdueDebt) {
			logger.debug("PaymentReliefOnOverdueDebt=TRUE - applying overdue logic");
			return calculateDaysWithOverdueLogic(startLocal, endLocal, paymentDates, config, loan, isLoanOverdue,
					useInclusiveCounting);
		} else {
			// paymentReliefOnOverdueDebt = false AND loan is NOT overdue (handled above)
			logger.debug("=== PAYMENT RELIEF LOGIC ===");
			logger.debug("PaymentReliefOnOverdueDebt=FALSE and loan NOT overdue");
			logger.debug("APPLYING PAYMENT EXCLUSION LOGIC");
			return calculateDaysExcludingPayments(startLocal, endLocal, paymentDates, config, useInclusiveCounting);
		}
	}

	/**
	 * Check if loan is overdue at a given date
	 */
	private boolean isLoanOverdue(MLoanApplication loan, LocalDate calculationDate) {
		logger.debug("=== CHECKING IF LOAN IS OVERDUE ===");
		logger.debug("Loan ID: {}", loan.getLoanApplicationId());
		logger.debug("Calculation Date: {}", calculationDate);

		if (loan == null || loan.getDueDate() == null) {
			logger.debug("Loan or due date is null - returning false");
			return false;
		}

		LocalDate loanDueDate = loan.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		boolean isOverdue = calculationDate.isAfter(loanDueDate);

		logger.debug("Loan Due Date: {}, Calculation Date: {}", loanDueDate, calculationDate);
		logger.debug("Is calculation date after due date? {}", isOverdue);
		logger.debug("Loan is overdue: {}", isOverdue);

		return isOverdue;
	}

	/**
	 * Calculate days with the new overdue logic
	 */
	private int calculateDaysWithOverdueLogic(LocalDate startLocal, LocalDate endLocal, Set<LocalDate> paymentDates,
			MLoanProductConfiguration config, MLoanApplication loan, boolean isLoanOverdue,
			boolean useInclusiveCounting) {

		PenaltyCalculationBaseEnum calculationBase = config.getDefaultPenaltyCalculationBase();
		if (calculationBase == null) {
			calculationBase = PenaltyCalculationBaseEnum.PER_DAY;
		}

		logger.debug("{} calculation with overdue logic: Start={}, End={}, IsOverdue={}, Payment dates={}",
				calculationBase, startLocal, endLocal, isLoanOverdue, paymentDates.size());

		// If loan is overdue, use the original logic (skip payments)
		if (isLoanOverdue) {
			logger.debug("Loan is overdue - applying payment exclusion logic");
			return calculateDaysExcludingPayments(startLocal, endLocal, paymentDates, config, useInclusiveCounting);
		} else {
			// For non-overdue loans, check due date vs calculation date
			LocalDate loanDueDate = loan.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

			if (endLocal.isBefore(loanDueDate) || endLocal.isEqual(loanDueDate)) {
				// Calculation date is before or on due date - apply payment exclusion
				logger.debug("Loan not overdue yet (due date: {}) - applying payment exclusion", loanDueDate);
				return calculateDaysExcludingPayments(startLocal, endLocal, paymentDates, config, useInclusiveCounting);
			} else {
				// Split the period into pre-due and post-due portions
				return calculateDaysSplitPeriod(startLocal, endLocal, paymentDates, config, loanDueDate,
						useInclusiveCounting);
			}
		}
	}

	/**
	 * Calculate days splitting period into pre-due and post-due portions
	 */
	private int calculateDaysSplitPeriod(LocalDate startLocal, LocalDate endLocal, Set<LocalDate> paymentDates,
			MLoanProductConfiguration config, LocalDate loanDueDate, boolean useInclusiveCounting) {

		// Part 1: Before due date (inclusive) - apply payment exclusion
		LocalDate preDueEnd = endLocal.isAfter(loanDueDate) ? loanDueDate : endLocal;
		int preDueDays = 0;

		if (!startLocal.isAfter(preDueEnd)) {
			preDueDays = calculateDaysExcludingPayments(startLocal, preDueEnd, paymentDates, config,
					useInclusiveCounting);
			logger.debug("Pre-due period {} to {}: {} days", startLocal, preDueEnd, preDueDays);
		}

		// Part 2: After due date - no payment exclusion
		if (endLocal.isAfter(loanDueDate)) {
			LocalDate postDueStart = loanDueDate.plusDays(1);
			long postDueDaysCount;
			if (useInclusiveCounting) {
				postDueDaysCount = java.time.temporal.ChronoUnit.DAYS.between(postDueStart, endLocal) + 1;
			} else {
				postDueDaysCount = java.time.temporal.ChronoUnit.DAYS.between(postDueStart, endLocal);
			}
			logger.debug("Post-due period {} to {}: {} days (no payment exclusion)", postDueStart, endLocal,
					postDueDaysCount);
			return preDueDays + (int) postDueDaysCount;
		}

		return preDueDays;
	}

	/**
	 * Generic method to calculate days excluding payments based on calculation base
	 */
	private int calculateDaysExcludingPayments(LocalDate startLocal, LocalDate endLocal, Set<LocalDate> paymentDates,
			MLoanProductConfiguration config, boolean useInclusiveCounting) {

		logger.debug("=== CALCULATING DAYS EXCLUDING PAYMENTS ===");
		logger.debug("Period: {} to {}", startLocal, endLocal);
		logger.debug("Payment dates count: {}", paymentDates != null ? paymentDates.size() : 0);
		logger.debug("Use inclusive counting: {}", useInclusiveCounting);

		// Use frequency-specific calculation
		return calculateDaysExcludingPaymentsByFrequency(startLocal, endLocal, paymentDates, config,
				useInclusiveCounting);
	}

	/**
	 * Calculate days excluding only specific payment days (for DAILY frequency)
	 */
	private int calculateDaysExcludingPaymentDays(LocalDate startLocal, LocalDate endLocal, Set<LocalDate> paymentDates,
			boolean useInclusiveCounting) {

		logger.debug("=== PER_DAY EXCLUSION CALCULATION ===");

		long totalDays;
		if (useInclusiveCounting) {
			totalDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal) + 1;
			logger.debug("Period: {} to {} ({} days INCLUSIVE)", startLocal, endLocal, totalDays);
		} else {
			totalDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal);
			logger.debug("Period: {} to {} ({} days EXCLUSIVE)", startLocal, endLocal, totalDays);
		}

		if (paymentDates == null || paymentDates.isEmpty()) {
			logger.debug("No payment dates - returning all {} days", totalDays);
			return (int) totalDays;
		}

		logger.debug("Checking {} payment dates for exclusion:", paymentDates.size());
		for (LocalDate paymentDate : paymentDates) {
			logger.debug("  Payment on: {}", paymentDate);
		}

		int effectiveDays = 0;
		LocalDate currentDate = startLocal;
		int penaltyDays = 0;
		int excludedDays = 0;

		logger.debug("Day-by-day calculation:");

		// Handle both inclusive and exclusive counting
		if (useInclusiveCounting) {
			// Inclusive: include end date
			while (!currentDate.isAfter(endLocal)) {
				if (!paymentDates.contains(currentDate)) {
					effectiveDays++;
					penaltyDays++;
					logger.debug("  {}: PENALTY APPLIED", currentDate);
				} else {
					excludedDays++;
					logger.debug("  {}: EXCLUDED (payment made)", currentDate);
				}
				currentDate = currentDate.plusDays(1);
			}
		} else {
			// Exclusive: exclude end date
			while (currentDate.isBefore(endLocal)) {
				if (!paymentDates.contains(currentDate)) {
					effectiveDays++;
					penaltyDays++;
					logger.debug("  {}: PENALTY APPLIED", currentDate);
				} else {
					excludedDays++;
					logger.debug("  {}: EXCLUDED (payment made)", currentDate);
				}
				currentDate = currentDate.plusDays(1);
			}
		}

		// Calculate total days for summary
		long totalPossibleDays;
		if (useInclusiveCounting) {
			totalPossibleDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal) + 1;
		} else {
			totalPossibleDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal);
		}

		logger.debug("=== PER_DAY RESULT SUMMARY ===");
		logger.debug("Penalty days: {}", penaltyDays);
		logger.debug("Excluded days: {}", excludedDays);
		logger.debug("Total possible days: {}", totalPossibleDays);
		logger.debug("Effective days for penalty: {}", effectiveDays);

		return effectiveDays;
	}

	/**
	 * Calculate days excluding entire periods when payments were made
	 */
	private int calculateDaysExcludingPaymentPeriods(LocalDate startLocal, LocalDate endLocal,
	        Set<LocalDate> paymentDates, MLoanProductConfiguration config, boolean useInclusiveCounting) {

	    PenaltyCalculationBaseEnum calculationBase = config.getDefaultPenaltyCalculationBase();
	    if (calculationBase == null) {
	        calculationBase = PenaltyCalculationBaseEnum.PER_DAY;
	    }

	    logger.debug("{} calculation: Start={}, End={}, Payment dates={}", calculationBase, startLocal, endLocal,
	            paymentDates.size());

	    int effectiveDays = 0;
	    LocalDate currentPeriodStart = startLocal;
	    int periodCounter = 0;
	    int penaltyPeriods = 0;
	    int excludedPeriods = 0;

	    while (!currentPeriodStart.isAfter(endLocal)) {
	        periodCounter++;
	        LocalDate currentPeriodEnd = getPeriodEndDate(currentPeriodStart, calculationBase, config, endLocal);

	        // Check if any payment was made in this period
	        boolean paymentInPeriod = hasPaymentInPeriod(paymentDates, currentPeriodStart, currentPeriodEnd);

	        if (!paymentInPeriod) {
	            // Calculate period days based on inclusive/exclusive counting
	            long periodDays;
	            if (useInclusiveCounting) {
	                periodDays = java.time.temporal.ChronoUnit.DAYS.between(currentPeriodStart, currentPeriodEnd) + 1;
	            } else {
	                periodDays = java.time.temporal.ChronoUnit.DAYS.between(currentPeriodStart, currentPeriodEnd);
	            }
	            effectiveDays += periodDays;
	            penaltyPeriods++;
	            logger.debug("Period {}-{}: Penalty applied ({} days)", currentPeriodStart, currentPeriodEnd,
	                    periodDays);
	        } else {
	            excludedPeriods++;
	            logger.debug("Period {}-{}: Excluded (payment made)", currentPeriodStart, currentPeriodEnd);
	        }

	        // Move to next period (start AFTER the current period)
	        // For period-based frequencies, we should move to the next period start date
	        currentPeriodStart = getNextPeriodStartDate(currentPeriodEnd, calculationBase, config);
	    }

	    // Calculate total days for logging
	    long totalDays;
	    if (useInclusiveCounting) {
	        totalDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal) + 1;
	    } else {
	        totalDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal);
	    }

	    logger.debug(
	            "{} result: {} periods processed, {} penalty periods, {} excluded periods, {} effective days, {} total days",
	            calculationBase, periodCounter, penaltyPeriods, excludedPeriods, effectiveDays, totalDays);

	    return effectiveDays;
	}

	/**
	 * Get the start date of the next penalty period based on frequency
	 */
	private LocalDate getNextPeriodStartDate(LocalDate periodEnd, PenaltyCalculationBaseEnum calculationBase,
	        MLoanProductConfiguration config) {
	    // Start the next period on the day AFTER the current period ends
	    return periodEnd.plusDays(1);
	}

	/**
	 * Get the end date of a penalty period based on frequency
	 */
	private LocalDate getPeriodEndDate(LocalDate periodStart, PenaltyCalculationBaseEnum calculationBase,
			MLoanProductConfiguration config, LocalDate maxEndDate) {
		LocalDate periodEnd;

		switch (calculationBase) {
		case PER_DAY:
			periodEnd = periodStart; // Daily - period is just one day
			break;

		case PER_WEEK:
			periodEnd = periodStart.plusDays(6);
			break;

		case PER_MONTH:
			periodEnd = periodStart.plusMonths(1).minusDays(1);
			break;

		case PER_CYCLE:
			Integer cycleDays = config.getPenaltyFrequencyDays();
			if (cycleDays == null || cycleDays <= 0) {
				cycleDays = 30;
			}
			periodEnd = periodStart.plusDays(cycleDays - 1);
			break;

		case ONCE:
			periodEnd = maxEndDate;
			break;

		default:
			periodEnd = periodStart;
			break;
		}

		// Ensure period end doesn't exceed max end date
		return periodEnd.isAfter(maxEndDate) ? maxEndDate : periodEnd;
	}

	/**
	 * Check if any payment was made in the given period (inclusive)
	 */
	private boolean hasPaymentInPeriod(Set<LocalDate> paymentDates, LocalDate periodStart, LocalDate periodEnd) {
		for (LocalDate paymentDate : paymentDates) {
			if (!paymentDate.isBefore(periodStart) && !paymentDate.isAfter(periodEnd)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Calculate penalty with effective days
	 */
	private PenaltyCalculationResult calculatePenaltyWithEffectiveDays(BigDecimal principalAmount,
			BigDecimal outstandingBalance, BigDecimal overdueAmount, Date dueDate, Date calculationDate,
			Date lastPenaltyCalculationDate, MLoanProductConfiguration config, int effectiveDaysOverdue) {

		if (dueDate == null || calculationDate == null || config == null || effectiveDaysOverdue <= 0) {
			return new PenaltyCalculationResult(BigDecimal.ZERO, 0, "Invalid data for penalty calculation", null, null);
		}

		BigDecimal penaltyBaseAmount = getPenaltyBaseAmount(principalAmount, outstandingBalance, overdueAmount,
				config.getPenaltyAppliesTo());

		BigDecimal penaltyAmount = calculatePenaltyAmountWithEffectiveDays(penaltyBaseAmount, effectiveDaysOverdue,
				config, lastPenaltyCalculationDate, config.getPenaltyAppliesTo());

		penaltyAmount = applyPenaltyCap(penaltyAmount, principalAmount, config.getMaxPenaltyCapPercentOfPrincipal(),
				config.isAllowMaxPenaltyCap());

		return new PenaltyCalculationResult(penaltyAmount, effectiveDaysOverdue,
				"Penalty calculated with effective days", null, calculationDate);
	}

	/**
	 * Calculate penalty amount using effective days - UPDATED TO USE Math.ceil()
	 */
	private BigDecimal calculatePenaltyAmountWithEffectiveDays(BigDecimal penaltyBaseAmount, int effectiveDaysOverdue,
	        MLoanProductConfiguration config, Date lastPenaltyCalculationDate, PenaltyBaseEnum penaltyBase) {

	    if (config.getPenaltyFlatRateAmount() != null
	            && config.getPenaltyFlatRateAmount().compareTo(BigDecimal.ZERO) > 0) {
	        logger.debug("Using flat rate penalty: {}", config.getPenaltyFlatRateAmount());
	        return config.getPenaltyFlatRateAmount();
	    }

	    if (config.getPenaltyRatePercent() == null || config.getPenaltyRatePercent().compareTo(BigDecimal.ZERO) <= 0) {
	        return BigDecimal.ZERO;
	    }

	    PenaltyCalculationBaseEnum calculationBase = config.getDefaultPenaltyCalculationBase();
	    if (calculationBase == null) {
	        calculationBase = PenaltyCalculationBaseEnum.PER_DAY;
	    }

	    BigDecimal penaltyRate = config.getPenaltyRatePercent().divide(BigDecimal.valueOf(100), 6,
	            RoundingMode.HALF_UP);

	    boolean shouldCompound = shouldCompoundPenalty(penaltyBase);

	    BigDecimal penaltyAmount;
	    switch (calculationBase) {
	    case PER_DAY:
	        // For daily frequency - use actual days
	        if (shouldCompound && effectiveDaysOverdue > 1) {
	            penaltyAmount = calculateCompoundedPenalty(penaltyBaseAmount, penaltyRate, effectiveDaysOverdue);
	            logger.debug("PER_DAY compounded penalty: {} days", effectiveDaysOverdue);
	        } else {
	            penaltyAmount = penaltyBaseAmount.multiply(penaltyRate)
	                    .multiply(BigDecimal.valueOf(effectiveDaysOverdue));
	            logger.debug("PER_DAY simple penalty: {} days", effectiveDaysOverdue);
	        }
	        break;

	    case PER_WEEK:
	        // UPDATED: Use Math.ceil() for cleaner ceiling division
	        int weeksOverdue;
	        if (lastPenaltyCalculationDate == null) {
	            // First penalty calculation - use CEILING to charge immediately
	            weeksOverdue = (int) Math.ceil(effectiveDaysOverdue / 7.0);
	            logger.debug("PER_WEEK (FIRST): {} days = {} weeks (CEILING using Math.ceil)", 
	                    effectiveDaysOverdue, weeksOverdue);
	        } else {
	            // Subsequent calculations - use FLOOR (complete weeks only)
	            weeksOverdue = effectiveDaysOverdue / 7; // Floor division
	            logger.debug("PER_WEEK (SUBSEQUENT): {} days = {} complete weeks (FLOOR)", 
	                    effectiveDaysOverdue, weeksOverdue);
	        }
	        
	        if (shouldCompound && weeksOverdue > 1) {
	            penaltyAmount = calculateCompoundedPenalty(penaltyBaseAmount, penaltyRate, weeksOverdue);
	        } else {
	            penaltyAmount = penaltyBaseAmount.multiply(penaltyRate).multiply(BigDecimal.valueOf(weeksOverdue));
	        }
	        break;

	    case PER_MONTH:
	        // UPDATED: Use Math.ceil() for cleaner ceiling division
	        int monthsOverdue;
	        if (lastPenaltyCalculationDate == null) {
	            // First penalty calculation - use CEILING to charge immediately
	            monthsOverdue = (int) Math.ceil(effectiveDaysOverdue / 30.0);
	            logger.debug("PER_MONTH (FIRST): {} days = {} months (CEILING using Math.ceil)", 
	                    effectiveDaysOverdue, monthsOverdue);
	        } else {
	            // Subsequent calculations - use FLOOR (complete months only)
	            monthsOverdue = effectiveDaysOverdue / 30; // Floor division
	            logger.debug("PER_MONTH (SUBSEQUENT): {} days = {} complete months (FLOOR)", 
	                    effectiveDaysOverdue, monthsOverdue);
	        }
	        
	        if (shouldCompound && monthsOverdue > 1) {
	            penaltyAmount = calculateCompoundedPenalty(penaltyBaseAmount, penaltyRate, monthsOverdue);
	        } else {
	            penaltyAmount = penaltyBaseAmount.multiply(penaltyRate).multiply(BigDecimal.valueOf(monthsOverdue));
	        }
	        break;

	    case PER_CYCLE:
	        Integer cycleDays = config.getPenaltyFrequencyDays();
	        if (cycleDays == null || cycleDays <= 0) {
	            cycleDays = 30;
	        }
	        
	        // UPDATED: Use Math.ceil() for cleaner ceiling division
	        int cyclesOverdue;
	        if (lastPenaltyCalculationDate == null) {
	            // First penalty calculation - use CEILING to charge immediately
	            cyclesOverdue = (int) Math.ceil(effectiveDaysOverdue / (double) cycleDays);
	            logger.debug("PER_CYCLE (FIRST): {} days = {} cycles of {} days (CEILING using Math.ceil)", 
	                    effectiveDaysOverdue, cyclesOverdue, cycleDays);
	        } else {
	            // Subsequent calculations - use FLOOR (complete cycles only)
	            cyclesOverdue = effectiveDaysOverdue / cycleDays; // Floor division
	            logger.debug("PER_CYCLE (SUBSEQUENT): {} days = {} complete cycles of {} days (FLOOR)", 
	                    effectiveDaysOverdue, cyclesOverdue, cycleDays);
	        }
	        
	        if (shouldCompound && cyclesOverdue > 1) {
	            penaltyAmount = calculateCompoundedPenalty(penaltyBaseAmount, penaltyRate, cyclesOverdue);
	        } else {
	            penaltyAmount = penaltyBaseAmount.multiply(penaltyRate).multiply(BigDecimal.valueOf(cyclesOverdue));
	        }
	        break;

	    case ONCE:
	        if (lastPenaltyCalculationDate == null) {
	            penaltyAmount = penaltyBaseAmount.multiply(penaltyRate);
	            logger.debug("ONCE penalty applied (first time)");
	        } else {
	            penaltyAmount = BigDecimal.ZERO;
	            logger.debug("ONCE penalty already applied");
	        }
	        break;

	    default:
	        if (shouldCompound && effectiveDaysOverdue > 1) {
	            penaltyAmount = calculateCompoundedPenalty(penaltyBaseAmount, penaltyRate, effectiveDaysOverdue);
	        } else {
	            penaltyAmount = penaltyBaseAmount.multiply(penaltyRate)
	                    .multiply(BigDecimal.valueOf(effectiveDaysOverdue));
	        }
	        break;
	    }

	    logger.debug("Final penalty amount: {}", penaltyAmount.setScale(2, RoundingMode.HALF_UP));
	    return penaltyAmount.setScale(2, RoundingMode.HALF_UP);
	}
	/**
	 * Calculate compounded penalty
	 */
	private BigDecimal calculateCompoundedPenalty(BigDecimal baseAmount, BigDecimal rate, int periods) {
		BigDecimal onePlusRate = BigDecimal.ONE.add(rate);
		BigDecimal compoundedFactor = onePlusRate.pow(periods);
		BigDecimal penaltyFactor = compoundedFactor.subtract(BigDecimal.ONE);
		return baseAmount.multiply(penaltyFactor).setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * Determine if penalty should be compounded
	 */
	private boolean shouldCompoundPenalty(PenaltyBaseEnum penaltyBase) {
		return penaltyBase == PenaltyBaseEnum.FULL_LOAN_BALANCE
				|| penaltyBase == PenaltyBaseEnum.CURRENT_INSTALLMENT_OVERDUE;
	}

	/**
	 * Get the base amount for penalty calculation
	 */
	private BigDecimal getPenaltyBaseAmount(BigDecimal principalAmount, BigDecimal outstandingBalance,
			BigDecimal overdueAmount, PenaltyBaseEnum penaltyAppliesTo) {

		if (penaltyAppliesTo == null) {
			return outstandingBalance != null ? outstandingBalance : BigDecimal.ZERO;
		}

		BigDecimal baseAmount;
		switch (penaltyAppliesTo) {
		case PRINCIPAL:
			baseAmount = principalAmount != null ? principalAmount : BigDecimal.ZERO;
			break;
		case FULL_LOAN_BALANCE:
			baseAmount = outstandingBalance != null ? outstandingBalance : BigDecimal.ZERO;
			break;
		case CURRENT_INSTALLMENT_OVERDUE:
			baseAmount = overdueAmount != null ? overdueAmount : BigDecimal.ZERO;
			break;
		default:
			baseAmount = principalAmount != null ? principalAmount : BigDecimal.ZERO;
			break;
		}

		return baseAmount;
	}

	/**
	 * Apply maximum penalty cap
	 */
	private BigDecimal applyPenaltyCap(BigDecimal penaltyAmount, BigDecimal principalAmount,
			BigDecimal maxPenaltyCapPercent, boolean allowMaxPenaltyCap) {

		if (!allowMaxPenaltyCap || maxPenaltyCapPercent == null || principalAmount == null) {
			return penaltyAmount;
		}

		BigDecimal maxAllowedPenalty = principalAmount.multiply(maxPenaltyCapPercent).divide(BigDecimal.valueOf(100), 2,
				RoundingMode.HALF_UP);

		BigDecimal finalPenalty = penaltyAmount.min(maxAllowedPenalty);

		if (finalPenalty.compareTo(penaltyAmount) != 0) {
			logger.info("PENALTY_CAPPED | Original: {} | Capped: {} | Max Allowed: {}", penaltyAmount, finalPenalty,
					maxAllowedPenalty);
		}

		return finalPenalty;
	}

	/**
	 * Result class for penalty calculation
	 */
	public static class PenaltyCalculationResult {
		private final BigDecimal penaltyAmount;
		private final int daysOverdue;
		private final String message;
		private final Date nextPenaltyDate;
		private final Date lastCalculationDate;

		public PenaltyCalculationResult(BigDecimal penaltyAmount, int daysOverdue, String message, Date nextPenaltyDate,
				Date lastCalculationDate) {
			this.penaltyAmount = penaltyAmount != null ? penaltyAmount : BigDecimal.ZERO;
			this.daysOverdue = daysOverdue;
			this.message = message;
			this.nextPenaltyDate = nextPenaltyDate;
			this.lastCalculationDate = lastCalculationDate;
		}

		public BigDecimal getPenaltyAmount() {
			return penaltyAmount;
		}

		public int getDaysOverdue() {
			return daysOverdue;
		}

		public String getMessage() {
			return message;
		}

		public Date getNextPenaltyDate() {
			return nextPenaltyDate;
		}

		public Date getLastCalculationDate() {
			return lastCalculationDate;
		}
	}

	/**
	 * FIX FOR ISSUE 2: Special logic for DAILY frequency with payment relief -
	 * RESETS after each payment This prevents over-calculation when multiple
	 * consecutive payments are made
	 */
	private int calculateDaysExcludingPaymentDaysWithReset(LocalDate startLocal, LocalDate endLocal,
			Set<LocalDate> paymentDates, boolean useInclusiveCounting) {

		logger.debug("=== DAILY FREQUENCY WITH PAYMENT RESET LOGIC ===");
		logger.debug("Period: {} to {}", startLocal, endLocal);
		logger.debug("Payment dates count: {}", paymentDates != null ? paymentDates.size() : 0);
		logger.debug("Use inclusive counting: {}", useInclusiveCounting);

		if (paymentDates == null || paymentDates.isEmpty()) {
			long totalDays;
			if (useInclusiveCounting) {
				totalDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal) + 1;
			} else {
				totalDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal);
			}
			logger.debug("No payment dates - returning all {} days", totalDays);
			return (int) totalDays;
		}

		// Sort payment dates chronologically
		List<LocalDate> sortedPayments = new ArrayList<>(paymentDates);
		Collections.sort(sortedPayments);

		logger.debug("Sorted payment dates ({}):", sortedPayments.size());
		for (LocalDate date : sortedPayments) {
			logger.debug("  - {}", date);
		}

		int effectiveDays = 0;
		LocalDate currentStart = startLocal;
		int segmentCount = 0;

		// Process each payment date to reset the counter
		for (LocalDate paymentDate : sortedPayments) {
			// Skip payments before current start or after end
			if (paymentDate.isBefore(currentStart)) {
				logger.debug("Segment {}: Skipping payment on {} (before current start {})", ++segmentCount,
						paymentDate, currentStart);
				continue;
			}

			if (paymentDate.isAfter(endLocal)) {
				logger.debug("Segment {}: Skipping payment on {} (after end {})", ++segmentCount, paymentDate,
						endLocal);
				break;
			}

			segmentCount++;

			// Calculate days from currentStart to day BEFORE payment (if any)
			if (paymentDate.isAfter(currentStart)) {
				LocalDate segmentEnd;
				if (useInclusiveCounting) {
					// For inclusive counting, include up to day before payment
					segmentEnd = paymentDate.minusDays(1);
				} else {
					// For exclusive counting, include up to day before payment
					segmentEnd = paymentDate.minusDays(1);
				}

				if (!segmentEnd.isBefore(currentStart)) {
					long daysInSegment;
					if (useInclusiveCounting) {
						daysInSegment = java.time.temporal.ChronoUnit.DAYS.between(currentStart, segmentEnd) + 1;
					} else {
						daysInSegment = java.time.temporal.ChronoUnit.DAYS.between(currentStart, segmentEnd);
					}

					if (daysInSegment > 0) {
						effectiveDays += daysInSegment;
						logger.debug("Segment {}: Penalty days from {} to {} = {} days", segmentCount, currentStart,
								segmentEnd, daysInSegment);
					}
				}
			}

			// Exclude the payment day itself
			logger.debug("Segment {}: EXCLUDED - Payment on {}", segmentCount, paymentDate);

			// FIX FOR ISSUE 2: RESET - Start counting from day AFTER payment
			currentStart = paymentDate.plusDays(1);
			logger.debug("Segment {}: Reset calculation start to: {}", segmentCount, currentStart);

			// If we've passed the end date, stop
			if (currentStart.isAfter(endLocal)) {
				logger.debug("Segment {}: New start {} is after end {}", segmentCount, currentStart, endLocal);
				break;
			}
		}

		// Add remaining days after last payment
		if (!currentStart.isAfter(endLocal)) {
			long remainingDays;
			if (useInclusiveCounting) {
				remainingDays = java.time.temporal.ChronoUnit.DAYS.between(currentStart, endLocal) + 1;
			} else {
				remainingDays = java.time.temporal.ChronoUnit.DAYS.between(currentStart, endLocal);
			}

			if (remainingDays > 0) {
				effectiveDays += remainingDays;
				logger.debug("Final segment: Penalty days from {} to {} = {} days", currentStart, endLocal,
						remainingDays);
			}
		}

		// Calculate total possible days for comparison
		long totalPossibleDays;
		if (useInclusiveCounting) {
			totalPossibleDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal) + 1;
		} else {
			totalPossibleDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal);
		}

		logger.debug("=== DAILY WITH RESET RESULT ===");
		logger.debug("Total possible days: {}", totalPossibleDays);
		logger.debug("Effective penalty days: {}", effectiveDays);
		logger.debug("Excluded days: {}", totalPossibleDays - effectiveDays);

		return effectiveDays;
	}

	/**
	 * Frequency-specific days calculation
	 */
	private int calculateDaysExcludingPaymentsByFrequency(LocalDate startLocal, LocalDate endLocal,
			Set<LocalDate> paymentDates, MLoanProductConfiguration config, boolean useInclusiveCounting) {

		PenaltyCalculationBaseEnum calculationBase = config.getDefaultPenaltyCalculationBase();
		if (calculationBase == null) {
			calculationBase = PenaltyCalculationBaseEnum.PER_DAY;
		}

		logger.debug("Using {} calculation for payment exclusion", calculationBase);

		switch (calculationBase) {
		case PER_DAY:
			// DAILY: Use new reset logic for payment relief (FIX FOR ISSUE 2)
			return calculateDaysExcludingPaymentDaysWithReset(startLocal, endLocal, paymentDates, useInclusiveCounting);

		case PER_WEEK:
		case PER_MONTH:
		case PER_CYCLE:
			// PERIOD-BASED: Keep existing working logic
			return calculateDaysExcludingPaymentPeriods(startLocal, endLocal, paymentDates, config,
					useInclusiveCounting);

		case ONCE:
			// ONE-TIME: Simple count
			long totalDays;
			if (useInclusiveCounting) {
				totalDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal) + 1;
			} else {
				totalDays = java.time.temporal.ChronoUnit.DAYS.between(startLocal, endLocal);
			}
			logger.debug("ONCE penalty: {} days", totalDays);
			return (int) totalDays;

		default:
			// Fallback to daily calculation
			return calculateDaysExcludingPaymentDaysWithReset(startLocal, endLocal, paymentDates, useInclusiveCounting);
		}
	}
}