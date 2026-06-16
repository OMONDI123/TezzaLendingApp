package co.ke.tezza.loanapp.schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DebtTypeEnum;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.InstallmentFrequencyEnum;
import co.ke.tezza.loanapp.enums.InterestCalculationMethodEnum;
import co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.repository.GuarantorLoanRepository;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.service.LoanStatementService;
import co.ke.tezza.loanapp.service.SmsHandlersService;
import co.ke.tezza.loanapp.util.Utils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ManageInstallments {

	@Autowired
	private LoanApplicationRepository loanApplicationRepository;
	@Autowired
	private InstallmentRepository installmentRepository;
	@Autowired
	private SmsHandlersService remindersScheduler;
	@Autowired
	private Utils utils;
	@Autowired
	private LoanStatementService loanStatementService;

	@Scheduled(cron = "1 * * * * *")
	@Transactional
	public void generateInstallmentsForLoan() {
		List<MLoanApplication> activeLoans = loanApplicationRepository
				.findByBalanceGreaterThanAndApprovalStageAndIsActive(BigDecimal.ZERO, ApprovalStage.APPROVED,true);

		for (MLoanApplication loan : activeLoans) {
			try {
				MLoanProductConfiguration config = loan.getLoanProductConfiguration();
				if (config == null
						|| !RepaymentScheduleTypeEnum.INSTALLMENTS.equals(config.getRepaymentScheduleType())) {
					continue;
				}

				BigDecimal distBalance = loan.getInstallmentDistributionBalance();
				if (distBalance == null || distBalance.compareTo(BigDecimal.ZERO) <= 0)
					continue;

				int termDays = loan.getTermInDays();
				InstallmentFrequencyEnum frequency = config.getInstallmentFrequency();
				int numberOfInstallments = getNumberOfInstallments(frequency, termDays);
				if (numberOfInstallments <= 0)
					numberOfInstallments = 1;

				BigDecimal perInstallmentAmount = distBalance.divide(BigDecimal.valueOf(numberOfInstallments), 2,
						RoundingMode.HALF_UP);

				// 🔹 Set the base installment amount once (persisted)
				if (loan.getInitialInstallmentBaseAmount() == null
						|| loan.getInitialInstallmentBaseAmount().compareTo(BigDecimal.ZERO) == 0) {
					loan.setInitialInstallmentBaseAmount(perInstallmentAmount);
					loanApplicationRepository.save(loan);
				}

				// 🧩 Get existing installments for this loan
				List<MInstallments> installments = installmentRepository.findByIsActiveAndLoanOrderByPeriodEndAsc(true,
						loan);

				// 🔹 Handle DECLINING_BALANCE_EMI, FLAT, and FLAT_RATE with progressive
				// creation
				if (Objects.equals(config.getInterestCalculationMethod(),
						InterestCalculationMethodEnum.DECLINING_BALANCE_EMI)
						|| Objects.equals(config.getInterestCalculationMethod(), InterestCalculationMethodEnum.FLAT)
						|| Objects.equals(config.getDebtType(), DebtTypeEnum.FLAT_RATE)) {

					createInstallmentsProgressively(loan, config, frequency, numberOfInstallments, perInstallmentAmount,
							installments);
					continue;
				}

				// 🧩 Otherwise, progressive installment creation for other types
				if (installments.isEmpty()) {
					// For first installment: expectedDisbursementDate +
					// gracePeriodToFirstInstallment
					Date firstInstallmentStart = calculateFirstInstallmentStartDate(loan);
					createInstallment(loan, config, frequency, 1, perInstallmentAmount, firstInstallmentStart);
					continue;
				}

				MInstallments last = installments.get(installments.size() - 1);
				if (last.getBalance().compareTo(BigDecimal.ZERO) > 0) {
					continue;
				}

				int nextIndex = installments.size() + 1;
				if (nextIndex > numberOfInstallments) {
					continue;
				}

				// For consecutive installments: previous periodEnd + penaltyGracePeriod
				// (null-safe)
				int penaltyGracePeriod = getNullSafeGracePeriod(loan.getPenaltyGracePeriod());
				Date nextStart = addDays(last.getPeriodEnd(), penaltyGracePeriod);
				createInstallment(loan, config, frequency, nextIndex, perInstallmentAmount, nextStart);

				// Send notification for the newly created installment

			} catch (Exception e) {
				log.error("❌ Error generating installments for loan {}: {}", loan.getDocumentNo(), e.getMessage(), e);
			}
		}
	}

	public void updateFirstInstallmentInterest(MLoanApplication loan) {
		MInstallments inst = installmentRepository
				.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(true, BigDecimal.ZERO, loan);
		if (inst != null) {
			if (loan.getApprovedAmount().compareTo(loan.getBalance()) < 0) {
				BigDecimal interestEarned = loan.getBalance().subtract(loan.getAppliedAmount());
				inst.setBalance(interestEarned.add(inst.getBalance()));
				inst.setInterestEarned(interestEarned);
				installmentRepository.save(inst);
			}
		}
	}

	public void generateFirstInstallmentForLoan(MLoanApplication loan) {
		try {
			MLoanProductConfiguration config = loan.getLoanProductConfiguration();
			if (config == null || !RepaymentScheduleTypeEnum.INSTALLMENTS.equals(config.getRepaymentScheduleType())) {
				return;
			}

			BigDecimal distBalance = loan.getInstallmentDistributionBalance();
			if (distBalance == null || distBalance.compareTo(BigDecimal.ZERO) <= 0)
				return;

			int termDays = loan.getTermInDays();
			InstallmentFrequencyEnum frequency = config.getInstallmentFrequency();
			int numberOfInstallments = getNumberOfInstallments(frequency, termDays);
			if (numberOfInstallments <= 0)
				numberOfInstallments = 1;

			BigDecimal perInstallmentAmount = distBalance.divide(BigDecimal.valueOf(numberOfInstallments), 2,
					RoundingMode.HALF_UP);

			// 🔹 Set the base installment amount once (persisted)
			if (loan.getInitialInstallmentBaseAmount() == null
					|| loan.getInitialInstallmentBaseAmount().compareTo(BigDecimal.ZERO) == 0) {
				loan.setInitialInstallmentBaseAmount(perInstallmentAmount);
				loanApplicationRepository.save(loan);
			}

			// 🧩 Get existing installments for this loan
			List<MInstallments> installments = installmentRepository.findByIsActiveAndLoanOrderByPeriodEndAsc(true,
					loan);

			// 🔹 Handle DECLINING_BALANCE_EMI, FLAT, and FLAT_RATE with progressive
			// creation
			if (Objects.equals(config.getInterestCalculationMethod(),
					InterestCalculationMethodEnum.DECLINING_BALANCE_EMI)
					|| Objects.equals(config.getInterestCalculationMethod(), InterestCalculationMethodEnum.FLAT)
					|| Objects.equals(config.getDebtType(), DebtTypeEnum.FLAT_RATE)) {

				createInstallmentsProgressively(loan, config, frequency, numberOfInstallments, perInstallmentAmount,
						installments);
				return;
			}

			// 🧩 Otherwise, progressive installment creation for other types
			if (installments.isEmpty()) {
				// For first installment: expectedDisbursementDate +
				// gracePeriodToFirstInstallment
				Date firstInstallmentStart = calculateFirstInstallmentStartDate(loan);
				createInstallment(loan, config, frequency, 1, perInstallmentAmount, firstInstallmentStart);
				return;
			}

			MInstallments last = installments.get(installments.size() - 1);
			if (last.getBalance().compareTo(BigDecimal.ZERO) > 0) {
				return;
			}

			int nextIndex = installments.size() + 1;
			if (nextIndex > numberOfInstallments) {
				return;
			}

			// For consecutive installments: previous periodEnd + penaltyGracePeriod
			// (null-safe)
			int penaltyGracePeriod = getNullSafeGracePeriod(loan.getPenaltyGracePeriod());
			Date nextStart = addDays(last.getPeriodEnd(), penaltyGracePeriod);
			createInstallment(loan, config, frequency, nextIndex, perInstallmentAmount, nextStart);

			// Send notification for the newly created installment
			MInstallments inst = installmentRepository
					.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(true, BigDecimal.ZERO, loan);
			if (inst != null) {
				MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
						SettingCategoriesEnum.GENERAL_SETTINGS, loan.getAdOrgID());
//				if (sys != null && sys.isAllowSystemNotifications() && utils.isBorrowerEligible(loan)) {
//					remindersScheduler.handleInstallmentGenerationNotification(inst);
//				}
				remindersScheduler.handleInstallmentGenerationNotification(inst,null);
			}

		} catch (Exception e) {
			log.error("❌ Error generating installments for loan {}: {}", loan.getDocumentNo(), e.getMessage(), e);
		}
	}

	/**
	 * Calculate first installment start date: expectedDisbursementDate +
	 * gracePeriodToFirstInstallment
	 */
	private Date calculateFirstInstallmentStartDate(MLoanApplication loan) {
		Date disbursementDate = loan.getExpectedDisbursementDate();
		int gracePeriodToFirstInstallment = getNullSafeGracePeriod(loan.getGracePeriodToFirstInstallment());

		if (disbursementDate == null) {
			log.warn("⚠️ No expected disbursement date found for loan {}, using current date", loan.getDocumentNo());
			return new Date();
		}

		Date firstInstallmentStart = addDays(disbursementDate, gracePeriodToFirstInstallment);
		log.info("📅 First installment start date for loan {}: {} (Disbursement: {} + {} days grace)",
				loan.getDocumentNo(), firstInstallmentStart, disbursementDate, gracePeriodToFirstInstallment);
		return firstInstallmentStart;
	}

	/**
	 * Create installments progressively for DECLINING_BALANCE_EMI, FLAT, and
	 * FLAT_RATE loans Only create the next installment when the previous one is
	 * fully paid
	 */
	private void createInstallmentsProgressively(MLoanApplication loan, MLoanProductConfiguration config,
			InstallmentFrequencyEnum frequency, int numberOfInstallments, BigDecimal perInstallmentAmount,
			List<MInstallments> existingInstallments) {

		BigDecimal distributionBalance = loan.getInstallmentDistributionBalance();

		// If no installments exist, create the first one
		if (existingInstallments.isEmpty()) {
			BigDecimal firstInstallmentAmount = calculateInstallmentAmount(distributionBalance,
					loan.getInitialInstallmentBaseAmount(), perInstallmentAmount, 1, numberOfInstallments);

			if (firstInstallmentAmount.compareTo(BigDecimal.ZERO) > 0) {
				// For first installment: expectedDisbursementDate +
				// gracePeriodToFirstInstallment
				Date firstInstallmentStart = calculateFirstInstallmentStartDate(loan);
				createInstallment(loan, config, frequency, 1, firstInstallmentAmount, firstInstallmentStart);
				log.info("💰 Created first installment ({} amount) for loan {} (DECLINING_BALANCE_EMI/FLAT)",
						firstInstallmentAmount, loan.getDocumentNo());
			}
			return;
		}

		// Check if we've reached the maximum number of installments
		if (existingInstallments.size() >= numberOfInstallments) {
			log.debug("📊 Maximum installments ({}) already created for loan {}", numberOfInstallments,
					loan.getDocumentNo());
			return;
		}

		// Get the last installment and check if it's fully paid
		MInstallments lastInstallment = existingInstallments.get(existingInstallments.size() - 1);
		if (lastInstallment.getBalance().compareTo(BigDecimal.ZERO) > 0) {
			log.debug("⏳ Last installment not fully paid for loan {}, balance: {}", loan.getDocumentNo(),
					lastInstallment.getBalance());
			return;
		}

		// Calculate the next installment details
		int nextInstallmentNumber = existingInstallments.size() + 1;
		// For consecutive installments: previous periodEnd + penaltyGracePeriod
		// (null-safe)
		int consecutiveGracePeriod = getNullSafeGracePeriod(loan.getPenaltyGracePeriod());
		Date nextStartDate = addDays(lastInstallment.getPeriodEnd(), consecutiveGracePeriod);

		log.info("📅 Next installment {} start date for loan {}: {} (Previous period end: {} + {} days grace)",
				nextInstallmentNumber, loan.getDocumentNo(), nextStartDate, lastInstallment.getPeriodEnd(),
				consecutiveGracePeriod);

		// Calculate installment amount based on current distribution balance
		BigDecimal installmentAmount = calculateInstallmentAmount(distributionBalance,
				loan.getInitialInstallmentBaseAmount(), perInstallmentAmount, nextInstallmentNumber,
				numberOfInstallments);

		// Only create installment if there's remaining distribution balance
		if (installmentAmount.compareTo(BigDecimal.ZERO) > 0) {
			createInstallment(loan, config, frequency, nextInstallmentNumber, installmentAmount, nextStartDate);
			log.info("✅ Created progressive installment {} ({} amount) for loan {} (DECLINING_BALANCE_EMI/FLAT)",
					nextInstallmentNumber, installmentAmount, loan.getDocumentNo());
		}
	}

	/**
	 * Calculate the installment amount based on distribution balance and base
	 * amount
	 */
	private BigDecimal calculateInstallmentAmount(BigDecimal distributionBalance, BigDecimal baseAmount,
			BigDecimal perInstallmentAmount, int installmentNumber, int totalInstallments) {

		if (distributionBalance.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}

		// For the final installment or when distribution balance is less than base
		// amount
		if (installmentNumber == totalInstallments || distributionBalance.compareTo(baseAmount) <= 0) {
			return distributionBalance;
		}

		// For regular installments, use the base amount
		return baseAmount;
	}

	/**
	 * Null-safe grace period handler - defaults to 0 if null
	 */
	private int getNullSafeGracePeriod(Integer gracePeriod) {
		return gracePeriod != null ? gracePeriod : 0;
	}

	private void createInstallment(MLoanApplication loan, MLoanProductConfiguration config,
			InstallmentFrequencyEnum frequency, int installmentNumber, BigDecimal installmentAmount, Date startDate) {

		// Use penaltyGracePeriod for the installment's grace period (null-safe)
		int graceDays = getNullSafeGracePeriod(loan.getPenaltyGracePeriod());
		int frequencyDays = getFrequencyDays(frequency);

		Date periodStart = startDate;
		Date periodEnd = addDays(startDate, frequencyDays);
		Date penaltyStartDate = addDays(periodEnd, graceDays);

		BigDecimal distributionBalance = loan.getInstallmentDistributionBalance();

		if (installmentAmount.compareTo(BigDecimal.ZERO) <= 0)
			return;

		MInstallments inst = new MInstallments();
		inst.setLoan(loan);
		inst.setAmount(installmentAmount);
		inst.setCummulatedAmount(installmentAmount);
		inst.setBalance(installmentAmount);
		inst.setPaidAmount(BigDecimal.ZERO);
		inst.setInterestEarned(BigDecimal.ZERO);
		inst.setPenaltyEarned(BigDecimal.ZERO);
		inst.setNoOfRemindersSent(0);
		inst.setGracePeriod(graceDays);
		inst.setPeriodStart(periodStart);
		inst.setPeriodEnd(periodEnd);
		inst.setAdOrgID(loan.getAdOrgID());
		inst.setAdClientId(loan.getAdClientId());
		inst.setPenaltyStartDate(penaltyStartDate);
		inst.setDocStatus(DocStatus.DRAFT);
		inst.setInstallmentNo(installmentNumber);
		inst.setApprovalStage(ApprovalStage.DRAFT);
		inst.setName("Installment " + installmentNumber);
		inst.setDescription("Auto-generated installment " + installmentNumber + " for loan " + loan.getDocumentNo());
		inst.setActive(true);
		inst.setProcessed(false);

		installmentRepository.save(inst);
		Long id = inst.getInstallmentId();
		String paddedId = String.format("%06d", id);

		inst.setDocumentNo("INST/" + Utils.getCurrentYear() + "/" + paddedId);
		installmentRepository.save(inst);
		Date expected = inst.getPeriodStart();

		LocalDateTime ldt = expected.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		loanStatementService.recordDisbursement(null, inst.getInstallmentId(), inst.getBalance(), inst.getDocumentNo(),
				ldt);

		// Update loan distribution balance
		loan.setHasInstallments(true);
		BigDecimal newDistributionBalance = distributionBalance.subtract(installmentAmount);
		loan.setInstallmentDistributionBalance(newDistributionBalance);

		if (newDistributionBalance.compareTo(BigDecimal.ZERO) <= 0) {
			loan.setInstallmentDistributionBalance(BigDecimal.ZERO);
			log.info("🎯 All installment amounts distributed for loan {}", loan.getDocumentNo());
		}

		loanApplicationRepository.save(loan);

		log.info("✅ Created installment {} ({} amount) for loan {}, period: {} to {}, grace: {} days",
				installmentNumber, installmentAmount, loan.getDocumentNo(), periodStart, periodEnd, graceDays);

		if (inst != null) {
			MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
					SettingCategoriesEnum.GENERAL_SETTINGS, loan.getAdOrgID());
			if (sys != null && sys.isAllowSystemNotifications()) {
				if (utils.isBorrowerEligible(loan)) {
					remindersScheduler.handleInstallmentGenerationNotification(inst,null);
				}

			}
		}
	}

	private int getNumberOfInstallments(InstallmentFrequencyEnum freq, int termDays) {
		if (freq == null)
			return 1;
		switch (freq) {
		case DAILY:
			return termDays;
		case WEEKLY:
			return termDays / 7;
		case BIWEEKLY:
			return termDays / 14;
		case MONTHLY:
			return termDays / 30;
		case QUARTERLY:
			return termDays / 90;
		case YEARLY:
			return termDays / 365;
		default:
			return 1;
		}
	}

	private int getFrequencyDays(InstallmentFrequencyEnum freq) {
		if (freq == null)
			return 30;
		switch (freq) {
		case DAILY:
			return 1;
		case WEEKLY:
			return 7;
		case BIWEEKLY:
			return 14;
		case MONTHLY:
			return 30;
		case QUARTERLY:
			return 90;
		case YEARLY:
			return 365;
		default:
			return 30;
		}
	}

	private Date addDays(Date date, int days) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DATE, days);
		return cal.getTime();
	}
}