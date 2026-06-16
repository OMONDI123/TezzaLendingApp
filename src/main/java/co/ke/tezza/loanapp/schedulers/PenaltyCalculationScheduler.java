package co.ke.tezza.loanapp.schedulers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.entity.MNextOfKin;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.PenaltyBaseEnum;
import co.ke.tezza.loanapp.enums.PenaltyCalculationBaseEnum;
import co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.service.ChargeMonitoringService;
import co.ke.tezza.loanapp.service.LoanStatementService;
import co.ke.tezza.loanapp.service.PenaltyCalculatorService;
import co.ke.tezza.loanapp.service.SmsHandlersService;
import co.ke.tezza.loanapp.service.PenaltyCalculatorService.PenaltyCalculationResult;
import co.ke.tezza.loanapp.util.Utils;

@Component
public class PenaltyCalculationScheduler {

	@Autowired
	private PenaltyCalculatorService calculatorService;

	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	@Autowired
	private InstallmentRepository installmentRepository;

	@Autowired
	private SmsHandlersService remindersScheduler;

	@Autowired
	private LoanStatementService loanStatementService;
	@Autowired
	private ChargeMonitoringService chargeMonitoringService;

	@Autowired
	private Utils utils;

	@Scheduled(cron = "0/30 35-49 23 * * *")
	 //@Scheduled(cron = "0/30 * * * * *")
	@Transactional
	public void updatePenalties() {
		List<MLoanApplication> activeLoans = loanApplicationRepository
				.findByBalanceGreaterThanAndApprovalStageAndIsActive(BigDecimal.ZERO, ApprovalStage.APPROVED,true);

		if (activeLoans.isEmpty()) {
			return;
		}

		Date currentDate = new Date();
		LocalDate localCurrentDate = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		for (MLoanApplication loan : activeLoans) {
			try {
				if (shouldCalculatePenaltyForLoan(loan, localCurrentDate)) {
					processLoanPenalty(loan, currentDate);
				}
			} catch (Exception e) {
				// Log the error but continue processing other loans
				e.printStackTrace();
				System.err.println(
						"Error processing penalty for loan " + loan.getLoanApplicationId() + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Check if penalty should be calculated for this loan Updated to match the new
	 * PenaltyCalculatorService logic
	 */
	private boolean shouldCalculatePenaltyForLoan(MLoanApplication loan, LocalDate currentDate) {
		// Validate loan configuration and due date
		if (loan.getLoanProductConfiguration() == null || loan.getDueDate() == null) {
			return false;
		}

		MLoanProductConfiguration config = loan.getLoanProductConfiguration();

		// CONDITION 1: Check if loan is overdue
		LocalDate dueLocal = loan.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		boolean isLoanOverdue = currentDate.isAfter(dueLocal);

		if (!isLoanOverdue) {
			// Loan is not overdue, skip penalty calculation
			return false;
		}

		// Check grace period
		Integer graceDays = loan.getPenaltyGracePeriod() != null ? loan.getPenaltyGracePeriod()
				: (config.getPenaltyGracePeriodDays() != null ? config.getPenaltyGracePeriodDays() : 0);

		LocalDate penaltyStartLocal = dueLocal.plusDays(graceDays);

		// If we're still before penalty start date (including grace period), skip
		if (currentDate.isBefore(penaltyStartLocal)) {
			return false;
		}

		// If next penalty date is set and it's in the future, skip
		Date nextPenaltyDate = loan.getNextPenaltyCalculationDate();
		if (nextPenaltyDate != null) {
			LocalDate nextPenaltyLocal = nextPenaltyDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			if (currentDate.isBefore(nextPenaltyLocal)) {
				return false;
			}
		}
		if (loan.getDueDate().before(new Date())) {
			return !loan.getLoanProductConfiguration().isLoanOverDueChargePenaltyInstallmentDue();

		}

		// All conditions met
		return true;
	}

	private void processLoanPenalty(MLoanApplication loan, Date currentDate) {
		PenaltyCalculationResult result = null;
		MInstallments installment = null;
		PenaltyBaseEnum penaltyBasedOn = loan.getLoanProductConfiguration().getPenaltyAppliesTo() != null
				? loan.getLoanProductConfiguration().getPenaltyAppliesTo()
				: PenaltyBaseEnum.PRINCIPAL;

		boolean isInstallmentBased = penaltyBasedOn.equals(PenaltyBaseEnum.CURRENT_INSTALLMENT_OVERDUE)
				&& loan.getLoanProductConfiguration().getRepaymentScheduleType()
						.equals(RepaymentScheduleTypeEnum.INSTALLMENTS);

		if (isInstallmentBased) {
			installment = installmentRepository
					.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(true, BigDecimal.ZERO, loan);

			if (installment != null) {

				result = calculatorService.calculatePenalty(loan);
			}
		} else {
			result = calculatorService.calculatePenalty(loan);
		}

		// ONLY apply penalty and update dates if penalty amount > 0
		if (result != null && result.getPenaltyAmount().compareTo(BigDecimal.ZERO) > 0) {
			applyPenalty(loan, installment, result);
		}
		// If result is null or penalty is zero, DO NOT update dates - leave them as
		// they are
	}

	private void applyPenalty(MLoanApplication loan, MInstallments installment, PenaltyCalculationResult result) {
		BigDecimal penaltyAmount = result.getPenaltyAmount();

		// Update installment if applicable
		if (installment != null) {
			updateInstallmentWithPenalty(installment, penaltyAmount, result);
		}

		// Update loan
		updateLoanWithPenalty(loan, penaltyAmount, result);

		// Record penalty and send notification
		MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
				SettingCategoriesEnum.GENERAL_SETTINGS, loan.getAdOrgID());
		if (sys != null && sys.isAllowSystemNotifications()) {
			if (chargeMonitoringService.continueCharging(loan)) {
				recordPenaltyAndNotify(loan, penaltyAmount);
			}
			
		}
	}

	private void updateInstallmentWithPenalty(MInstallments installment, BigDecimal penaltyAmount,
			PenaltyCalculationResult result) {

		if (chargeMonitoringService.continueCharging(installment.getLoan())) {
			installment.setBalance(installment.getBalance().add(penaltyAmount));

			BigDecimal currentPenalty = installment.getPenaltyEarned() != null ? installment.getPenaltyEarned()
					: BigDecimal.ZERO;
			installment.setPenaltyEarned(currentPenalty.add(penaltyAmount));

			// Update dates ONLY when penalty is actually applied
			installment.setLastPenaltyCalculationDate(result.getLastCalculationDate());
			installment.setNextPenaltyCalculationDate(result.getNextPenaltyDate());
			installment.setCummulatedAmount(penaltyAmount.add(installment.getCummulatedAmount()));

			installmentRepository.save(installment);
			loanStatementService.recordPenalty(null, installment.getInstallmentId(), penaltyAmount);

		} else {
			installment.setExempted(true);
			installment.setExemptedAmount(penaltyAmount
					.add(installment.getExemptedAmount() != null ? installment.getExemptedAmount() : BigDecimal.ZERO));
			installment.setExemptedPenalties(penaltyAmount.add(
					installment.getExemptedPenalties() != null ? installment.getExemptedPenalties() : BigDecimal.ZERO));
			installmentRepository.save(installment);

		}

	}

	private void updateLoanWithPenalty(MLoanApplication loan, BigDecimal penaltyAmount,
			PenaltyCalculationResult result) {

		if (chargeMonitoringService.continueCharging(loan)) {
			loan.setBalance(loan.getBalance().add(penaltyAmount));

			BigDecimal currentPenalty = loan.getPenaltyEarned() != null ? loan.getPenaltyEarned() : BigDecimal.ZERO;
			loan.setPenaltyEarned(currentPenalty.add(penaltyAmount));

			// Update dates ONLY when penalty is actually applied
			loan.setLastPenaltyCalculationDate(result.getLastCalculationDate());
			loan.setNextPenaltyCalculationDate(result.getNextPenaltyDate());

			loanApplicationRepository.save(loan);
			loanStatementService.recordPenalty(loan.getLoanApplicationId(), null, penaltyAmount);

		} else {
			loan.setExempted(true);
			loan.setExemptedAmount(
					penaltyAmount.add(loan.getExemptedAmount() != null ? loan.getExemptedAmount() : BigDecimal.ZERO));
			loan.setExemptedPenalties(penaltyAmount
					.add(loan.getExemptedPenalties() != null ? loan.getExemptedPenalties() : BigDecimal.ZERO));
			loanApplicationRepository.save(loan);
		}

	}

	private void recordPenaltyAndNotify(MLoanApplication loan, BigDecimal penaltyAmount) {
		MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
				SettingCategoriesEnum.GENERAL_SETTINGS, loan.getAdOrgID());
		if (sys != null && sys.isAllowSystemNotifications()) {

			String reason = null;
			String loanType = loan.getLoanProductConfiguration().getIsDebtProduct() ? "debt" : "loan";
			reason = "late " + loanType + " repayment";
			Date effectiveStartDate;
			if (loan.getLastPenaltyCalculationDate() == null) {
				// First time penalty calculation - start from penalty start date
				// Using due date + grace period as effective start
				Integer graceDays = loan.getPenaltyGracePeriod() != null ? loan.getPenaltyGracePeriod()
						: (loan.getLoanProductConfiguration().getPenaltyGracePeriodDays() != null
								? loan.getLoanProductConfiguration().getPenaltyGracePeriodDays()
								: 0);
				LocalDate dueLocal = loan.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				LocalDate penaltyStartLocal = dueLocal.plusDays(graceDays);
				effectiveStartDate = Date.from(penaltyStartLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
			} else {
				// Subsequent calculation - start from last penalty calculation date
				effectiveStartDate = loan.getLastPenaltyCalculationDate();
			}

			// Calculate days overdue for this calculation period
			// Use the updated method signature that requires MLoanApplication parameter

			boolean useInclusiveCounting = loan.getNextPenaltyCalculationDate() != null;

			int daysOverdueThisPeriod = calculatorService.calculateEffectiveDaysOverdue(loan.getDueDate(), new Date(),
					loan.getLoanProductConfiguration(), loan, useInclusiveCounting);

			// Send notification
			remindersScheduler.handlePenaltyAppliedNotification(loan, penaltyAmount, loan.getBalance(), reason,
					new Date(),null,utils.getNextDayNineAM());

			if (loan.getGuarantors().size() > 0) {
				for (MNextOfKin kin : loan.getGuarantors()) {
					remindersScheduler.handleGuarantorPenaltyCalculationNotification(kin, loan, penaltyAmount,
							getPenaltyrate(loan.getLoanProductConfiguration()), new Date(), reason,
							daysOverdueThisPeriod, calculatorService.getOverdueAmountForLoan(loan), loan.getBalance(),
							loanType + " Overdue Penalty", true,
							getPenaltyFrequency(loan.getLoanProductConfiguration()),null,utils.getNextDayNineAM());
				}
			}
		}
	}

	public BigDecimal getPenaltyrate(MLoanProductConfiguration config) {
		if (config.getPenaltyFlatRateAmount() != null
				&& config.getPenaltyFlatRateAmount().compareTo(BigDecimal.ZERO) > 0) {
			return config.getPenaltyFlatRateAmount();
		}
		return config.getPenaltyRatePercent();
	}

	public String getPenaltyFrequency(MLoanProductConfiguration config) {
		if (config.getDefaultPenaltyCalculationBase() == null) {
			config.setDefaultPenaltyCalculationBase(PenaltyCalculationBaseEnum.ONCE);
		}
		switch (config.getDefaultPenaltyCalculationBase()) {
		case PER_DAY:
			return "Daily";
		case PER_MONTH:
			return "Monthly";
		case PER_WEEK:
			return "Weekly";
		case PER_CYCLE:
			Integer days = config.getPenaltyFrequencyDays();
			if (days != null && days > 0) {
				return "After every " + days + " days";
			} else {
				return "After every 30 days";
			}
		default:
			return "Once";
		}
	}
}