package co.ke.tezza.loanapp.schedulers;

import java.math.BigDecimal;
import java.time.Instant;
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
import co.ke.tezza.loanapp.enums.FeeTimingEnum;
import co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.service.ChargeMonitoringService;
import co.ke.tezza.loanapp.service.FeeCalculatorService;
import co.ke.tezza.loanapp.service.LoanStatementService;
import co.ke.tezza.loanapp.service.SmsHandlersService;
import co.ke.tezza.loanapp.util.Utils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FeeCalculationScheduler {

    private final ReentrantLock processingLock = new ReentrantLock();

    @Autowired
    private FeeCalculatorService feeCalculatorService;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private LoanStatementService loanStatementService;

    @Autowired
    private SmsHandlersService remindersScheduler;

    @Autowired
    private ChargeMonitoringService chargeMonitoringService;

    @Autowired
    private Utils utils;

   
    @Scheduled(cron = "0 5 0 * * *")  // 12:05 AM daily
    @Transactional
    public void processPostDisbursementServiceFees() {
        if (!processingLock.tryLock()) {
            log.debug("Previous service fee processing still running, skipping this execution");
            return;
        }

        try {
            Date now = Date.from(Instant.now());

            // Get all approved loans with disbursement date in the past or today
            List<MLoanApplication> approvedLoans = loanApplicationRepository
                    .findByApprovalStageAndIsActiveAndExpectedDisbursementDateBefore(
                            ApprovalStage.APPROVED, true, now);

            int processed = 0, errors = 0, skipped = 0;

            for (MLoanApplication loan : approvedLoans) {
                try {
                    ProcessResult result = processServiceFee(loan, FeeTimingEnum.POST_DISBURSEMENT);
                    switch (result) {
                        case PROCESSED:
                            processed++;
                            break;
                        case SKIPPED:
                            skipped++;
                            break;
                        case ERROR:
                            errors++;
                            break;
                    }
                } catch (Exception e) {
                    log.error("Unexpected error processing service fee for loan {}: {}",
                            loan.getLoanApplicationId(), e.getMessage(), e);
                    errors++;
                }
            }

            log.info("Post-disbursement service fee processing finished. Processed: {}, Skipped: {}, Errors: {}",
                    processed, skipped, errors);

        } catch (Exception e) {
            log.error("Fatal error in service fee scheduler: {}", e.getMessage(), e);
        } finally {
            processingLock.unlock();
        }
    }

    /**
     * Runs at approval time via triggerApprovalStep or completeLoanApproval.
     * This is called from LoanApprovalWorkFlowService, not scheduled.
     * But we keep it here for manual trigger capability.
     */
    @Transactional
    public void processOriginationServiceFee(Long loanApplicationId) {
        try {
            MLoanApplication loan = loanApplicationRepository.findById(loanApplicationId)
                    .orElseThrow(() -> new RuntimeException("Loan not found: " + loanApplicationId));

            processServiceFee(loan, FeeTimingEnum.ORIGINATION);
        } catch (Exception e) {
            log.error("Error processing origination service fee for loan {}: {}",
                    loanApplicationId, e.getMessage(), e);
            throw new RuntimeException("Failed to process origination service fee", e);
        }
    }


    /**
     * Runs daily to accrue daily fees on active loans.
     * Uses incremental calculation to avoid double-charging.
     */
    @Scheduled(cron = "0/30 00-59 23 * * *")  // Runs during 23:00-23:59, every 30 seconds
    @Transactional
    public void processDailyFees() {
        if (!processingLock.tryLock()) {
            log.debug("Previous daily fee processing still running, skipping this execution");
            return;
        }

        try {
            Date now = Date.from(Instant.now());

            List<MLoanApplication> activeLoans = loanApplicationRepository
                    .findByBalanceGreaterThanAndApprovalStageAndIsActive(
                            BigDecimal.ZERO, ApprovalStage.APPROVED, true);

            if (activeLoans.isEmpty()) {
                log.debug("No active loans found for daily fee accrual");
                return;
            }

            int processed = 0, errors = 0, skipped = 0;

            for (MLoanApplication loan : activeLoans) {
                try {
                    ProcessResult result = processDailyFee(loan, now);
                    switch (result) {
                        case PROCESSED:
                            processed++;
                            break;
                        case SKIPPED:
                            skipped++;
                            break;
                        case ERROR:
                            errors++;
                            break;
                    }
                } catch (Exception e) {
                    log.error("Unexpected error processing daily fee for loan {}: {}",
                            loan.getLoanApplicationId(), e.getMessage(), e);
                    errors++;
                }
            }

            log.info("Daily fee accrual finished. Processed: {}, Skipped: {}, Errors: {}",
                    processed, skipped, errors);

        } catch (Exception e) {
            log.error("Fatal error in daily fee scheduler: {}", e.getMessage(), e);
        } finally {
            processingLock.unlock();
        }
    }

    // ============================================================
    //  CORE PROCESSING LOGIC
    // ============================================================

    private ProcessResult processServiceFee(MLoanApplication loan, FeeTimingEnum event) {
        if (loan == null || loan.getLoanProductConfiguration() == null) {
            return ProcessResult.SKIPPED;
        }

        // Check if service fee should be charged now
        if (!feeCalculatorService.shouldChargeServiceFeeNow(loan, event)) {
            return ProcessResult.SKIPPED;
        }

        try {
            BigDecimal serviceFee = feeCalculatorService.calculateServiceFee(loan);
            if (serviceFee == null || serviceFee.compareTo(BigDecimal.ZERO) <= 0) {
                return ProcessResult.SKIPPED;
            }

            if (chargeMonitoringService.continueCharging(loan)) {
                applyServiceFeeToLoan(loan, serviceFee);
                applyServiceFeeToInstallment(loan, serviceFee);
                loanStatementService.recordServiceFee(loan.getLoanApplicationId(), null, serviceFee);
                sendServiceFeeNotification(loan, serviceFee);
                log.info("Service fee of {} applied to loan {} (Ref: {})",
                        serviceFee, loan.getLoanApplicationId(), loan.getDocumentNo());
                return ProcessResult.PROCESSED;
            } else {
                // Charge monitoring says stop charging - mark as exempted
                loan.setExempted(true);
                loan.setExemptedAmount(serviceFee.add(safe(loan.getExemptedAmount())));
                loan.setServiceFeeWaived(serviceFee.add(safe(loan.getServiceFeeWaived())));
                loanApplicationRepository.save(loan);
                log.info("Service fee of {} waived for loan {} (Ref: {})",
                        serviceFee, loan.getLoanApplicationId(), loan.getDocumentNo());
                return ProcessResult.SKIPPED;
            }

        } catch (Exception e) {
            log.error("Error calculating service fee for loan {}: {}",
                    loan.getLoanApplicationId(), e.getMessage(), e);
            return ProcessResult.ERROR;
        }
    }

    private ProcessResult processDailyFee(MLoanApplication loan, Date now) {
        if (loan == null || loan.getLoanProductConfiguration() == null) {
            return ProcessResult.SKIPPED;
        }

        MLoanProductConfiguration config = loan.getLoanProductConfiguration();
        if (!Boolean.TRUE.equals(config.getEnableDailyFee())) {
            return ProcessResult.SKIPPED;
        }

        if (config.getDailyFeeAmount() == null || config.getDailyFeeAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ProcessResult.SKIPPED;
        }

        // Determine the from date for incremental calculation
        Date fromDate = loan.getLastDailyFeeCalculationDate();
        if (fromDate == null) {
            // If no prior calculation, start from disbursement date
            fromDate = loan.getActualDisbursementDate() != null
                    ? loan.getActualDisbursementDate()
                    : loan.getExpectedDisbursementDate();
            if (fromDate == null) {
                log.warn("Loan {} has no disbursement date for daily fee calculation",
                        loan.getLoanApplicationId());
                return ProcessResult.SKIPPED;
            }
        }

        // If fromDate is today or in the future, skip
        if (!fromDate.before(now)) {
            return ProcessResult.SKIPPED;
        }

        try {
            BigDecimal incrementalFee = feeCalculatorService.calculateIncrementalDailyFee(loan, fromDate, now);
            if (incrementalFee == null || incrementalFee.compareTo(BigDecimal.ZERO) <= 0) {
                // Even if no fee, update the last calculation date to prevent re-processing
                updateLastDailyFeeDate(loan, now);
                return ProcessResult.SKIPPED;
            }

            if (chargeMonitoringService.continueCharging(loan)) {
                applyDailyFeeToLoan(loan, incrementalFee);
                applyDailyFeeToInstallment(loan, incrementalFee);
                loanStatementService.recordDailyFee(loan.getLoanApplicationId(), null, incrementalFee);
                sendDailyFeeNotification(loan, incrementalFee);
                log.info("Daily fee of {} accrued for loan {} (Ref: {})",
                        incrementalFee, loan.getLoanApplicationId(), loan.getDocumentNo());
                return ProcessResult.PROCESSED;
            } else {
                // Charge monitoring says stop charging - mark as exempted
                loan.setExempted(true);
                loan.setExemptedAmount(incrementalFee.add(safe(loan.getExemptedAmount())));
                loan.setDailyFeeWaived(incrementalFee.add(safe(loan.getDailyFeeWaived())));
                updateLastDailyFeeDate(loan, now);
                loanApplicationRepository.save(loan);
                log.info("Daily fee of {} waived for loan {} (Ref: {})",
                        incrementalFee, loan.getLoanApplicationId(), loan.getDocumentNo());
                return ProcessResult.SKIPPED;
            }

        } catch (Exception e) {
            log.error("Error calculating daily fee for loan {}: {}",
                    loan.getLoanApplicationId(), e.getMessage(), e);
            return ProcessResult.ERROR;
        }
    }

    // ============================================================
    //  HELPER METHODS - Applying Fees
    // ============================================================

    private void applyServiceFeeToLoan(MLoanApplication loan, BigDecimal fee) {
        loan.setBalance(safe(loan.getBalance()).add(fee));
        loan.setServiceFeeCharged(fee.add(safe(loan.getServiceFeeCharged())));
        loan.setLastServiceFeeCalculationDate(new Date());
        loanApplicationRepository.save(loan);
    }

    private void applyServiceFeeToInstallment(MLoanApplication loan, BigDecimal fee) {
        if (loan.getLoanProductConfiguration().getRepaymentScheduleType() != RepaymentScheduleTypeEnum.INSTALLMENTS) {
            return;
        }

        try {
            MInstallments installment = installmentRepository
                    .findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                            true, BigDecimal.ZERO, loan);
            if (installment == null) return;

            if (chargeMonitoringService.continueCharging(loan)) {
                installment.setBalance(safe(installment.getBalance()).add(fee));
                installment.setServiceFeeCharged(fee.add(safe(installment.getServiceFeeCharged())));
                installment.setCummulatedAmount(safe(installment.getCummulatedAmount()).add(fee));
                installmentRepository.save(installment);
            } else {
                installment.setExempted(true);
                installment.setExemptedAmount(fee.add(safe(installment.getExemptedAmount())));
                installment.setServiceFeeWaived(fee.add(safe(installment.getServiceFeeWaived())));
                installmentRepository.save(installment);
            }
        } catch (Exception e) {
            log.error("Error applying service fee to installment for loan {}: {}",
                    loan.getLoanApplicationId(), e.getMessage(), e);
        }
    }

    private void applyDailyFeeToLoan(MLoanApplication loan, BigDecimal fee) {
        loan.setBalance(safe(loan.getBalance()).add(fee));
        loan.setDailyFeeCharged(fee.add(safe(loan.getDailyFeeCharged())));
        updateLastDailyFeeDate(loan, new Date());
        loanApplicationRepository.save(loan);
    }

    private void applyDailyFeeToInstallment(MLoanApplication loan, BigDecimal fee) {
        if (loan.getLoanProductConfiguration().getRepaymentScheduleType() != RepaymentScheduleTypeEnum.INSTALLMENTS) {
            return;
        }

        try {
            MInstallments installment = installmentRepository
                    .findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                            true, BigDecimal.ZERO, loan);
            if (installment == null) return;

            if (chargeMonitoringService.continueCharging(loan)) {
                installment.setBalance(safe(installment.getBalance()).add(fee));
                installment.setDailyFeeCharged(fee.add(safe(installment.getDailyFeeCharged())));
                installment.setCummulatedAmount(safe(installment.getCummulatedAmount()).add(fee));
                installmentRepository.save(installment);
            } else {
                installment.setExempted(true);
                installment.setExemptedAmount(fee.add(safe(installment.getExemptedAmount())));
                installment.setDailyFeeWaived(fee.add(safe(installment.getDailyFeeWaived())));
                installmentRepository.save(installment);
            }
        } catch (Exception e) {
            log.error("Error applying daily fee to installment for loan {}: {}",
                    loan.getLoanApplicationId(), e.getMessage(), e);
        }
    }

    private void updateLastDailyFeeDate(MLoanApplication loan, Date date) {
        loan.setLastDailyFeeCalculationDate(date);
        loanApplicationRepository.save(loan);
    }

    // ============================================================
    //  NOTIFICATION METHODS
    // ============================================================

    private void sendServiceFeeNotification(MLoanApplication loan, BigDecimal feeAmount) {
        try {
            MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                    SettingCategoriesEnum.GENERAL_SETTINGS, loan.getAdOrgID());
            if (sys != null && sys.isAllowSystemNotifications()) {
                String feeType = "Service Fee";
                String feeDescription = loan.getLoanProductConfiguration().getServiceFeeType() + " fee";

                remindersScheduler.handleFeeAppliedNotification(loan, feeAmount, loan.getBalance(),
                        feeType, feeDescription, new Date(), null, utils.getNextDayNineAM());

                if (loan.getGuarantors() != null && !loan.getGuarantors().isEmpty()) {
                    for (MNextOfKin kin : loan.getGuarantors()) {
                        remindersScheduler.handleGuarantorFeeNotification(kin, loan, feeAmount,
                                loan.getBalance(), feeType, "Service fee applied to " +
                                        (loan.getLoanProductConfiguration().getIsDebtProduct() ? "debt" : "loan"),
                                new Date(), null, utils.getNextDayNineAM());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send service fee notification for loan {}: {}",
                    loan.getLoanApplicationId(), e.getMessage());
        }
    }

    private void sendDailyFeeNotification(MLoanApplication loan, BigDecimal feeAmount) {
        try {
            MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                    SettingCategoriesEnum.GENERAL_SETTINGS, loan.getAdOrgID());
            if (sys != null && sys.isAllowSystemNotifications()) {
                String feeType = "Daily Fee";
                String feeDescription = "Daily flat fee of " +
                        loan.getLoanProductConfiguration().getDailyFeeAmount() + " per day";

                remindersScheduler.handleFeeAppliedNotification(loan, feeAmount, loan.getBalance(),
                        feeType, feeDescription, new Date(), null, utils.getNextDayNineAM());

                if (loan.getGuarantors() != null && !loan.getGuarantors().isEmpty()) {
                    for (MNextOfKin kin : loan.getGuarantors()) {
                        remindersScheduler.handleGuarantorFeeNotification(kin, loan, feeAmount,
                                loan.getBalance(), feeType, "Daily fee accrued on " +
                                        (loan.getLoanProductConfiguration().getIsDebtProduct() ? "debt" : "loan"),
                                new Date(), null, utils.getNextDayNineAM());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send daily fee notification for loan {}: {}",
                    loan.getLoanApplicationId(), e.getMessage());
        }
    }

    // ============================================================
    //  MANUAL OPERATIONS
    // ============================================================

    @Transactional
    public void processServiceFeeManually(Long loanApplicationId, FeeTimingEnum timing) {
        if (loanApplicationId == null) {
            throw new RuntimeException("Loan application ID cannot be null");
        }
        try {
            MLoanApplication loan = loanApplicationRepository.findById(loanApplicationId)
                    .orElseThrow(() -> new RuntimeException("Loan not found: " + loanApplicationId));
            ProcessResult result = processServiceFee(loan, timing);
            switch (result) {
                case PROCESSED:
                    log.info("Service fee manually processed for loan {}", loanApplicationId);
                    break;
                case SKIPPED:
                    log.info("Service fee manually skipped for loan {}", loanApplicationId);
                    break;
                case ERROR:
                    throw new RuntimeException("Failed to process service fee for loan: " + loanApplicationId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Manual service fee processing failed", e);
        }
    }

    @Transactional
    public void processDailyFeeManually(Long loanApplicationId) {
        if (loanApplicationId == null) {
            throw new RuntimeException("Loan application ID cannot be null");
        }
        try {
            MLoanApplication loan = loanApplicationRepository.findById(loanApplicationId)
                    .orElseThrow(() -> new RuntimeException("Loan not found: " + loanApplicationId));
            ProcessResult result = processDailyFee(loan, new Date());
            switch (result) {
                case PROCESSED:
                    log.info("Daily fee manually processed for loan {}", loanApplicationId);
                    break;
                case SKIPPED:
                    log.info("Daily fee manually skipped for loan {}", loanApplicationId);
                    break;
                case ERROR:
                    throw new RuntimeException("Failed to process daily fee for loan: " + loanApplicationId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Manual daily fee processing failed", e);
        }
    }

    @Transactional
    public void processDailyFeeBatch(List<Long> loanApplicationIds) {
        if (loanApplicationIds == null || loanApplicationIds.isEmpty()) {
            return;
        }
        int successCount = 0, errorCount = 0, skippedCount = 0;
        for (Long loanId : loanApplicationIds) {
            try {
                if (loanId == null) {
                    skippedCount++;
                    continue;
                }
                processDailyFeeManually(loanId);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                log.error("Batch error for loan {}: {}", loanId, e.getMessage());
            }
        }
        log.info("Daily fee batch processing completed. Success: {}, Errors: {}, Skipped: {}",
                successCount, errorCount, skippedCount);
    }

    // ============================================================
    //  UTILITY METHODS
    // ============================================================

    private static BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ============================================================
    //  INNER CLASSES
    // ============================================================

    private enum ProcessResult {
        PROCESSED, SKIPPED, ERROR
    }
}