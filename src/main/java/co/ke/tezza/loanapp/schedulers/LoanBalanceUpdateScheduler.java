package co.ke.tezza.loanapp.schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

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
import co.ke.tezza.loanapp.enums.DebtTypeEnum;
import co.ke.tezza.loanapp.enums.FlatRateType;
import co.ke.tezza.loanapp.enums.InterestCalculationMethodEnum;
import co.ke.tezza.loanapp.enums.InterestFrequencyEnum;
import co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.service.ChargeMonitoringService;
import co.ke.tezza.loanapp.service.LoanInterestCalculatorService;
import co.ke.tezza.loanapp.service.LoanStatementService;
import co.ke.tezza.loanapp.service.SmsHandlersService;
import co.ke.tezza.loanapp.util.Utils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LoanBalanceUpdateScheduler {

    private final ReentrantLock processingLock = new ReentrantLock();

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private LoanInterestCalculatorService loanInterestCalculatorService;

    @Autowired
    private Utils utils;

    @Autowired
    private LoanStatementService loanStatementService;

    @Autowired
    private SmsHandlersService remindersScheduler;

    @Autowired
    private ChargeMonitoringService chargeMonitoringService;

    // -----------------------
    // SCHEDULED PROCESSOR
    // -----------------------

    @Scheduled(cron = "0/30 00-59 23 * * *")   // runs daily near midnight
    @Transactional
    public void processAllActiveLoans() {
        if (!processingLock.tryLock()) {
            log.debug("Previous interest accrual still running, skipping this execution");
            return;
        }

        try {
            Date now = Date.from(Instant.now());

            List<MLoanApplication> activeLoans = loanApplicationRepository
                    .findByBalanceGreaterThanAndApprovalStageAndIsActive(
                            BigDecimal.ZERO, ApprovalStage.APPROVED, true);

            if (activeLoans.isEmpty()) {
                log.debug("No active loans found for interest accrual");
                return;
            }

            int processed = 0, errors = 0, skipped = 0, flatRateSkipped = 0;

            for (MLoanApplication loan : activeLoans) {
                try {
                    ProcessResult result = processSingleLoan(loan, now);
                    switch (result) {
                        case PROCESSED:
                            processed++;
                            break;
                        case SKIPPED_FLAT_RATE:
                            flatRateSkipped++;
                            break;
                        case SKIPPED:
                            skipped++;
                            break;
                        case ERROR:
                            errors++;
                            break;
                    }
                } catch (Exception e) {
                    log.error("Unexpected error for loan {}: {}", loan.getLoanApplicationId(), e.getMessage(), e);
                    errors++;
                }
            }

            log.info("Interest accrual finished. Processed: {}, Flat-rate skipped: {}, Skipped: {}, Errors: {}",
                    processed, flatRateSkipped, skipped, errors);

        } catch (Exception e) {
            log.error("Fatal error in interest accrual scheduler: {}", e.getMessage(), e);
        } finally {
            processingLock.unlock();
        }
    }

    // -----------------------
    // CORE PROCESSING LOGIC
    // -----------------------

    private ProcessResult processSingleLoan(MLoanApplication loan, Date now) {
        if (loan == null || loan.getLoanProductConfiguration() == null) {
            return ProcessResult.SKIPPED;
        }

        if (isFlatRateDebtType(loan)) {
            return ProcessResult.SKIPPED_FLAT_RATE;
        }

        Date effectiveStartDate = determineStartDate(loan);
        if (effectiveStartDate == null) {
            log.warn("Loan {} has no disbursement date and no last calculation date", loan.getLoanApplicationId());
            return ProcessResult.SKIPPED;
        }

        if (isWithinGracePeriod(loan, effectiveStartDate, now)) {
            return ProcessResult.SKIPPED;
        }

        InterestFrequencyEnum frequency = loan.getLoanProductConfiguration().getInterestFrequency();
        if (!shouldAccrueNow(effectiveStartDate, now, frequency)) {
            return ProcessResult.SKIPPED;
        }

        BigDecimal incrementalInterest = calculateIncrementalInterest(loan, effectiveStartDate, now);
        if (incrementalInterest == null || incrementalInterest.compareTo(BigDecimal.ZERO) <= 0) {
            return ProcessResult.SKIPPED;
        }

        updateLoanWithInterest(loan, incrementalInterest,
                loan.getLoanProductConfiguration().getInterestCalculationMethod(), now);

        if (chargeMonitoringService.continueCharging(loan)) {
            loanStatementService.recordInterest(loan.getLoanApplicationId(), null, incrementalInterest);
            sendInterestAccrualNotification(loan, incrementalInterest);
        }

        return ProcessResult.PROCESSED;
    }

    /**
     * Determines if at least one full interest period has elapsed.
     * Uses calendar‑based arithmetic for months and years.
     */
    private boolean shouldAccrueNow(Date fromDate, Date toDate, InterestFrequencyEnum frequency) {
        if (frequency == null) {
            frequency = InterestFrequencyEnum.DAILY;
        }

        LocalDate from = fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate to = toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        switch (frequency) {
            case DAILY:
                return ChronoUnit.DAYS.between(from, to) >= 1;
            case WEEKLY:
                return ChronoUnit.WEEKS.between(from, to) >= 1;
            case MONTHLY:
                return ChronoUnit.MONTHS.between(from, to) >= 1;
            case YEARLY:
                return ChronoUnit.YEARS.between(from, to) >= 1;
            default:
                return false;
        }
    }

    private boolean isWithinGracePeriod(MLoanApplication loan, Date fromDate, Date toDate) {
        Date disbursementDate = loan.getExpectedDisbursementDate();
        if (disbursementDate == null) {
            return false;
        }
        long daysSinceDisbursement = ChronoUnit.DAYS.between(
                disbursementDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        int gracePeriodDays = getEffectiveGracePeriod(loan);
        return daysSinceDisbursement <= gracePeriodDays;
    }

    private BigDecimal calculateIncrementalInterest(MLoanApplication loan, Date fromDate, Date toDate) {
        try {
            return loanInterestCalculatorService.calculateIncrementalInterest(loan, fromDate, toDate);
        } catch (Exception e) {
            log.error("Error calculating incremental interest for loan {}: {}", loan.getLoanApplicationId(), e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    private Date determineStartDate(MLoanApplication loan) {
        if (loan.getLastInterestCalculationDate() != null) {
            return loan.getLastInterestCalculationDate();
        } else if (loan.getExpectedDisbursementDate() != null) {
            return loan.getExpectedDisbursementDate();
        } else {
            return null;
        }
    }

    private void updateLoanWithInterest(MLoanApplication loan, BigDecimal incrementalInterest,
                                        InterestCalculationMethodEnum method, Date processingTime) {
        if (chargeMonitoringService.continueCharging(loan)) {
            BigDecimal currentBalance = safe(loan.getBalance());
            BigDecimal currentInterest = safe(loan.getInterestsEarned());

            loan.setInterestsEarned(currentInterest.add(incrementalInterest));
            loan.setDecliningInterest(safe(loan.getDecliningInterest()).add(incrementalInterest));

            switch (method) {
                case COMPOUND:
                case CYCLE_BASED:
                case DECLINING_BALANCE:
                case SIMPLE_INTEREST:
                    loan.setBalance(currentBalance.add(incrementalInterest));
                    break;
                case FLAT:
                case DECLINING_BALANCE_EMI:
                    loan.setBalance(safe(loan.getApprovedAmount()).add(loan.getInterestsEarned()));
                    break;
                default:
                    loan.setBalance(currentBalance.add(incrementalInterest));
                    break;
            }

            updateInstallmentWithInterest(loan, incrementalInterest);

            loan.setLastInterestCalculationDate(processingTime);
            loanApplicationRepository.save(loan);
        } else {
            loan.setExempted(true);
            loan.setExemptedAmount(incrementalInterest.add(safe(loan.getExemptedAmount())));
            loan.setExemptedInterests(incrementalInterest.add(safe(loan.getExemptedInterests())));
            loanApplicationRepository.save(loan);
        }
    }

    private void updateInstallmentWithInterest(MLoanApplication loan, BigDecimal interestAmount) {
        if (interestAmount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (loan.getLoanProductConfiguration().getRepaymentScheduleType() != RepaymentScheduleTypeEnum.INSTALLMENTS) return;

        try {
            MInstallments installment = installmentRepository
                    .findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(true, BigDecimal.ZERO, loan);
            if (installment == null) return;

            if (chargeMonitoringService.continueCharging(loan)) {
                installment.setBalance(safe(installment.getBalance()).add(interestAmount));
                installment.setInterestEarned(safe(installment.getInterestEarned()).add(interestAmount));
                installment.setCummulatedAmount(safe(installment.getCummulatedAmount()).add(interestAmount));
                installmentRepository.save(installment);
                loanStatementService.recordInterest(null, installment.getInstallmentId(), interestAmount);
            } else {
                installment.setExempted(true);
                installment.setExemptedAmount(safe(installment.getExemptedAmount()).add(interestAmount));
                installment.setExemptedInterests(safe(installment.getExemptedInterests()).add(interestAmount));
                installmentRepository.save(installment);
            }
        } catch (Exception e) {
            log.error("Error updating installment interest for loan {}", loan.getLoanApplicationId(), e);
        }
    }

    // -----------------------
    // NOTIFICATION METHODS
    // -----------------------

    private void sendInterestAccrualNotification(MLoanApplication loan, BigDecimal currentInterestAmount) {
        try {
            MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                    SettingCategoriesEnum.GENERAL_SETTINGS, loan.getAdOrgID());
            if (sys != null && sys.isAllowSystemNotifications()) {
                BigDecimal totalInterest = loan.getInterestsEarned();
                String interestRate = getInterestRateDescription(loan);
                remindersScheduler.handleInterestCalculationNotification(loan, currentInterestAmount, totalInterest,
                        new Date(), interestRate, null, utils.getNextDayNineAM());

                if (loan.getGuarantors() != null && !loan.getGuarantors().isEmpty()) {
                    for (MNextOfKin kin : loan.getGuarantors()) {
                        remindersScheduler.handleGuarantorInterestAccrualNotification(kin, loan, currentInterestAmount,
                                totalInterest, new Date(),
                                safe(loan.getDailyInterestRate()), safe(loan.getWeeklyInterestRate()),
                                safe(loan.getMonthlyInterestRate()), safe(loan.getAnnualInterestRate()),
                                safe(loan.getBalance()),
                                loan.getLoanProductConfiguration().getInterestCalculationMethod().getDescription(),
                                loan.getLoanProductConfiguration().getInterestFrequency().toString().toLowerCase(),
                                new Date(System.currentTimeMillis() + 86400000),
                                getCurrentInterestPeriod(loan.getLoanProductConfiguration().getInterestFrequency()),
                                null, utils.getNextDayNineAM());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send interest accrual notifications for loan {}", loan.getLoanApplicationId(), e);
        }
    }

    private String getCurrentInterestPeriod(InterestFrequencyEnum frequency) {
        if (frequency == null) {
            return "current period";
        }
        switch (frequency) {
            case DAILY:
                return "today";
            case WEEKLY:
                return "this week";
            case MONTHLY:
                return "this month";
            case YEARLY:
                return "this year";
            default:
                return "current period";
        }
    }

    private String getInterestRateDescription(MLoanApplication loan) {
        MLoanProductConfiguration config = loan.getLoanProductConfiguration();
        if (config == null) return "Rate not configured";
        if (config.getIsDebtProduct() && config.getDebtType() == DebtTypeEnum.FLAT_RATE) {
            return getFlatRateDescription(loan);
        }
        InterestFrequencyEnum freq = config.getInterestFrequency();
        if (freq == null) return "Interest rate not configured";
        BigDecimal rate = null;
        switch (freq) {
            case DAILY:
                rate = loan.getDailyInterestRate();
                break;
            case WEEKLY:
                rate = loan.getWeeklyInterestRate();
                break;
            case MONTHLY:
                rate = loan.getMonthlyInterestRate();
                break;
            case YEARLY:
                rate = loan.getAnnualInterestRate();
                break;
        }
        if (rate == null) return freq.toString().toLowerCase() + " rate not configured";
        return rate.setScale(2, RoundingMode.HALF_UP) + "% " + getFrequencyDescription(freq);
    }

    private String getFrequencyDescription(InterestFrequencyEnum frequency) {
        switch (frequency) {
            case DAILY:
                return "daily";
            case WEEKLY:
                return "weekly";
            case MONTHLY:
                return "per month";
            case YEARLY:
                return "per annum";
            default:
                return frequency.toString().toLowerCase();
        }
    }

    private String getFlatRateDescription(MLoanApplication application) {
        MLoanProductConfiguration config = application.getLoanProductConfiguration();
        FlatRateType flatRateType = config.getFlatRateType();
        if (flatRateType == null) return "Flat rate not configured";
        switch (flatRateType) {
            case PERCENTAGE_BASED:
                if (application.getInteretsFlatRate() != null)
                    return application.getInteretsFlatRate() + "% flat rate";
                break;
            case AMOUNT_BASED:
                if (application.getInteretsFlatRateAmount() != null)
                    return "KES " + utils.formatAmount(application.getInteretsFlatRateAmount(), "KES") + " flat amount";
                break;
        }
        return "Flat rate not configured";
    }

    public String getFormattedInterestRate(MLoanApplication application) {
        MLoanProductConfiguration config = application.getLoanProductConfiguration();
        if (config == null) return "Rate not configured";
        if (config.getIsDebtProduct() && config.getDebtType() == DebtTypeEnum.FLAT_RATE) {
            return getFlatRateDescription(application);
        }
        return getInterestRateDescription(application);
    }

    // -----------------------
    // MANUAL OPERATIONS
    // -----------------------

    @Transactional
    public void processLoanManually(Long loanApplicationId) {
        if (loanApplicationId == null) {
            throw new RuntimeException("Loan application ID cannot be null");
        }
        try {
            MLoanApplication loan = loanApplicationRepository.findById(loanApplicationId)
                    .orElseThrow(() -> new RuntimeException("Loan not found: " + loanApplicationId));
            ProcessResult result = processSingleLoan(loan, new Date());
            switch (result) {
                case PROCESSED:
                    break;
                case SKIPPED_FLAT_RATE:
                    break;
                case SKIPPED:
                    break;
                case ERROR:
                    throw new RuntimeException("Failed to process loan: " + safeGetDocumentNo(loan));
            }
        } catch (Exception e) {
            throw new RuntimeException("Manual processing failed", e);
        }
    }

    @Transactional
    public void processLoanBatch(List<Long> loanApplicationIds) {
        if (loanApplicationIds == null || loanApplicationIds.isEmpty()) return;
        int successCount = 0, errorCount = 0, skippedCount = 0;
        for (Long loanId : loanApplicationIds) {
            try {
                if (loanId == null) {
                    skippedCount++;
                    continue;
                }
                processLoanManually(loanId);
                successCount++;
            } catch (Exception e) {
                errorCount++;
            }
        }
        log.info("Batch processing completed. Success: {}, Errors: {}, Skipped: {}", successCount, errorCount, skippedCount);
    }

    @Transactional
    public void forceInterestRecalculation(Long loanApplicationId) {
        if (loanApplicationId == null) {
            throw new RuntimeException("Loan application ID cannot be null");
        }
        try {
            MLoanApplication loan = loanApplicationRepository.findById(loanApplicationId)
                    .orElseThrow(() -> new RuntimeException("Loan not found: " + loanApplicationId));
            loan.setInterestsEarned(BigDecimal.ZERO);
            loan.setDecliningInterest(BigDecimal.ZERO);
            loan.setBalance(safe(loan.getApprovedAmount()));
            loan.setInstallmentDistributionBalance(BigDecimal.ZERO);
            loan.setLastInterestCalculationDate(null);
            loanApplicationRepository.save(loan);
            processLoanManually(loanApplicationId);
        } catch (Exception e) {
            throw new RuntimeException("Force recalculation failed", e);
        }
    }

    // -----------------------
    // HELPER METHODS
    // -----------------------

    private boolean isFlatRateDebtType(MLoanApplication loan) {
        if (loan == null || loan.getLoanProductConfiguration() == null) return false;
        MLoanProductConfiguration config = loan.getLoanProductConfiguration();
        return config.getIsDebtProduct() != null && config.getIsDebtProduct() && config.getDebtType() != null
                && config.getDebtType() == DebtTypeEnum.FLAT_RATE;
    }

    private int getEffectiveGracePeriod(MLoanApplication loan) {
        if (loan == null || loan.getLoanProductConfiguration() == null) return 0;
        MLoanProductConfiguration config = loan.getLoanProductConfiguration();
        InterestCalculationMethodEnum method = config.getInterestCalculationMethod();
        RepaymentScheduleTypeEnum scheduleType = config.getRepaymentScheduleType();

        if (isFlatRateDebtType(loan)) return Integer.MAX_VALUE;
        if (method == InterestCalculationMethodEnum.CYCLE_BASED) return safeInt(config.getCycle1DurationDays());
        if (scheduleType == RepaymentScheduleTypeEnum.ONE_TIME) return safeInt(loan.getGraceperiod());
        else return safeInt(loan.getGracePeriodToFirstInstallment());
    }

    // -----------------------
    // UTILITY METHODS
    // -----------------------

    private static BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private static String safeGetDocumentNo(MLoanApplication loan) {
        return loan != null ? loan.getDocumentNo() : "NULL_LOAN";
    }

    // -----------------------
    // INNER CLASSES
    // -----------------------

    private enum ProcessResult {
        PROCESSED, SKIPPED, SKIPPED_FLAT_RATE, ERROR
    }
}