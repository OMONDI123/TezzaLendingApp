package co.ke.tezza.loanapp.schedulers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import co.ke.tezza.loanapp.enums.PenaltyCalculationBaseEnum;
import co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.service.ChargeMonitoringService;
import co.ke.tezza.loanapp.service.InstallmentPenaltyCalculatorService;
import co.ke.tezza.loanapp.service.LoanStatementService;
import co.ke.tezza.loanapp.service.PenaltyCalculatorService;
import co.ke.tezza.loanapp.service.SmsHandlersService;
import co.ke.tezza.loanapp.service.InstallmentPenaltyCalculatorService.PenaltyCalculationResult;
import co.ke.tezza.loanapp.util.Utils;

@Component
public class InstallmentPenaltyCalculationScheduler {

	private static final Logger logger = LoggerFactory.getLogger(InstallmentPenaltyCalculationScheduler.class);

	@Autowired
	private PenaltyCalculatorService calculatorService;

	@Autowired
	private InstallmentPenaltyCalculatorService installmentPenaltyCalculatorService;

	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	@Autowired
	private InstallmentRepository installmentRepository;

	@Autowired
	private SmsHandlersService remindersScheduler;

	@Autowired
	private LoanStatementService loanStatementService;

	@Autowired
	private Utils utils;
	@Autowired
	private ChargeMonitoringService chargeMonitoringService;

	@Scheduled(cron = "0/30 50-59 23 * * *")
	//@Scheduled(cron = "0/30 * * * * *")
	@Transactional
	public void updateInstallmentPenalties() {

		List<MLoanApplication> loans = loanApplicationRepository
				.findByBalanceGreaterThanAndApprovalStageAndIsActive(BigDecimal.ZERO, ApprovalStage.APPROVED,true);

		if (loans.isEmpty()) {
			return;
		}

		Date now = new Date();

		for (MLoanApplication loan : loans) {
			try {
				if (!isInstallmentBasedLoan(loan)) {
					continue;
				}

				MInstallments installment = installmentRepository
						.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(true, BigDecimal.ZERO,
								loan);

				if (installment == null) {
					continue;
				}

				if (!shouldCalculatePenalty(installment, loan, now)) {
					continue;
				}

				processInstallmentPenalty(loan, installment, now);

			} catch (Exception e) {
				logger.error("Error processing installment penalties for loan {}: {}", loan.getLoanApplicationId(),
						e.getMessage(), e);
			}
		}

	}

	private boolean isInstallmentBasedLoan(MLoanApplication loan) {
		MLoanProductConfiguration config = loan.getLoanProductConfiguration();
		if (config == null) {
			logger.info("No loan product configuration found for loan {}", loan.getLoanApplicationId());
			return false;
		}

		boolean isInstallmentBased = config.getRepaymentScheduleType() != null
				&& config.getRepaymentScheduleType().equals(RepaymentScheduleTypeEnum.INSTALLMENTS)
				&& config.isInstallmentDueChargePenalty();

		return isInstallmentBased;
	}

	/**
	 * Core installment penalty condition logic
	 */
	private boolean shouldCalculatePenalty(MInstallments installment, MLoanApplication loan, Date now) {

		if (installment.getPeriodEnd() == null) {
			return false;
		}

		// nextPenaltyCalculationDate > now → skip
		if (installment.getNextPenaltyCalculationDate() != null
				&& installment.getNextPenaltyCalculationDate().after(now)) {
			return false;
		}

		Integer graceDays = loan.getLoanProductConfiguration().getPenaltyGracePeriodDays();
		if (graceDays == null)
			graceDays = 0;

		long penaltyStartMs = installment.getPeriodEnd().getTime() + (graceDays * 24 * 60 * 60 * 1000L);

		Date penaltyStartDate = new Date(penaltyStartMs);

		// Still within grace period?
		if (now.before(penaltyStartDate)) {
			return false;
		}
		if (loan.getDueDate().before(new Date())) {
			return loan.getLoanProductConfiguration().isLoanOverDueChargePenaltyInstallmentDue();
		}

		return true;
	}

	/**
	 * Executes the full penalty calculation for one overdue installment
	 */
	private void processInstallmentPenalty(MLoanApplication loan, MInstallments installment, Date now) {

		// Use the dedicated installment penalty calculator service instead of the
		// generic one
		PenaltyCalculationResult result = installmentPenaltyCalculatorService.calculateInstallmentPenalty(installment);

		if (result == null || result.getPenaltyAmount().compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}

		BigDecimal penalty = result.getPenaltyAmount();

		// Update installment
		updateInstallment(installment, penalty, result);

		// Update loan totals
		updateLoan(loan, penalty, result);

		// Record and notify
		if (chargeMonitoringService.continueCharging(loan)) {
			triggerNotificationsAndStatement(loan, installment, penalty, now);
		}

	}

	private void updateInstallment(MInstallments inst, BigDecimal penalty, PenaltyCalculationResult result) {
		if (chargeMonitoringService.continueCharging(inst.getLoan())) {
			BigDecimal balanceBefore = inst.getBalance();
			inst.setBalance(inst.getBalance().add(penalty));

			BigDecimal currentPenalty = inst.getPenaltyEarned() == null ? BigDecimal.ZERO : inst.getPenaltyEarned();
			inst.setPenaltyEarned(currentPenalty.add(penalty));

			inst.setLastPenaltyCalculationDate(result.getLastCalculationDate());
			inst.setNextPenaltyCalculationDate(result.getNextPenaltyDate());
			inst.setCummulatedAmount(
					penalty.add(inst.getCummulatedAmount() != null ? inst.getCummulatedAmount() : BigDecimal.ZERO));

			installmentRepository.save(inst);
			loanStatementService.recordPenalty(null, inst.getInstallmentId(), penalty);

			logger.info("Installment {} updated - Balance: {} -> {}, Penalty Earned: {} -> {}", inst.getInstallmentId(),
					balanceBefore, inst.getBalance(), currentPenalty, inst.getPenaltyEarned());
		} else {
			inst.setExempted(true);
			inst.setExemptedAmount(
					penalty.add(inst.getExemptedAmount() != null ? inst.getExemptedAmount() : BigDecimal.ZERO));
			inst.setExemptedPenalties(
					penalty.add(inst.getExemptedPenalties() != null ? inst.getExemptedPenalties() : BigDecimal.ZERO));
			installmentRepository.save(inst);

		}

	}

	private void updateLoan(MLoanApplication loan, BigDecimal penalty, PenaltyCalculationResult result) {
		if (chargeMonitoringService.continueCharging(loan)) {

			BigDecimal balanceBefore = loan.getBalance();
			loan.setBalance(loan.getBalance().add(penalty));

			BigDecimal loanPenalty = loan.getPenaltyEarned() == null ? BigDecimal.ZERO : loan.getPenaltyEarned();
			loan.setPenaltyEarned(loanPenalty.add(penalty));

			loan.setLastPenaltyCalculationDate(result.getLastCalculationDate());
			loan.setNextPenaltyCalculationDate(result.getNextPenaltyDate());

			loanApplicationRepository.save(loan);
			loanStatementService.recordPenalty(loan.getLoanApplicationId(), null, penalty);

			logger.info("Loan {} updated - Balance: {} -> {}, Penalty Earned: {} -> {}", loan.getLoanApplicationId(),
					balanceBefore, loan.getBalance(), loanPenalty, loan.getPenaltyEarned());

		} else {
			loan.setExempted(true);
			loan.setExemptedAmount(
					penalty.add(loan.getExemptedAmount() != null ? loan.getExemptedAmount() : BigDecimal.ZERO));
			loan.setExemptedPenalties(
					penalty.add(loan.getExemptedPenalties() != null ? loan.getExemptedPenalties() : BigDecimal.ZERO));
			loanApplicationRepository.save(loan);

		}

	}

	private void triggerNotificationsAndStatement(MLoanApplication loan, MInstallments inst, BigDecimal penalty,
			Date now) {

		MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
				SettingCategoriesEnum.GENERAL_SETTINGS, loan.getAdOrgID());

		if (sys == null || !sys.isAllowSystemNotifications()) {
			logger.info("System notifications disabled for organization {}", loan.getAdOrgID());
			return;
		}

		String loanType = loan.getLoanProductConfiguration().getIsDebtProduct() ? "debt" : "loan";
		String reason = "late installment repayment";

		Date effectiveStart = inst.getLastPenaltyCalculationDate() == null ? inst.getPeriodEnd()
				: inst.getLastPenaltyCalculationDate();

		int overdueDays = installmentPenaltyCalculatorService.calculateEffectiveDaysOverdue(inst.getPeriodEnd(), now,
				loan.getLoanProductConfiguration(), loan, inst.getNextPenaltyCalculationDate() == null);

		logger.info("Overdue Days: {}, Effective Start: {}", overdueDays, effectiveStart);

		// Borrower
//		if (utils.isBorrowerEligible(loan)) {
//			remindersScheduler.handlePenaltyAppliedNotification(loan, penalty, loan.getBalance(), reason, now);
//
//			logger.info("Sent penalty notification to borrower");
//		}
		remindersScheduler.handlePenaltyAppliedNotification(loan, penalty, loan.getBalance(), reason, now,null,utils.getNextDayNineAM());

		// Guarantors
		if (loan.getGuarantors() != null && !loan.getGuarantors().isEmpty()) {
			for (MNextOfKin kin : loan.getGuarantors()) {

				remindersScheduler.handleGuarantorPenaltyCalculationNotification(kin, loan, penalty,
						getPenaltyRate(loan.getLoanProductConfiguration()), now, reason, overdueDays,
						calculatorService.getOverdueAmountForLoan(loan), loan.getBalance(),
						loanType + " Installment Overdue Penalty", true,
						getPenaltyFrequency(loan.getLoanProductConfiguration()),null,utils.getNextDayNineAM());
			}
			logger.info("Sent penalty notifications to {} guarantors", loan.getGuarantors().size());
		}
	}

	private BigDecimal getPenaltyRate(MLoanProductConfiguration config) {
		if (config.getPenaltyFlatRateAmount() != null
				&& config.getPenaltyFlatRateAmount().compareTo(BigDecimal.ZERO) > 0) {
			return config.getPenaltyFlatRateAmount();
		}
		return config.getPenaltyRatePercent();
	}

	private String getPenaltyFrequency(MLoanProductConfiguration config) {

		if (config.getDefaultPenaltyCalculationBase() == null) {
			config.setDefaultPenaltyCalculationBase(PenaltyCalculationBaseEnum.ONCE);
		}

		switch (config.getDefaultPenaltyCalculationBase()) {
		case PER_DAY:
			return "Daily";
		case PER_WEEK:
			return "Weekly";
		case PER_MONTH:
			return "Monthly";
		case PER_CYCLE:
			return "After every " + config.getPenaltyFrequencyDays() + " days";
		default:
			return "Once";
		}
	}
}