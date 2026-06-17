package co.ke.tezza.loanapp.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.LoanStateEnum;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.LoanRepaymentStatus;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.service.SmsHandlersService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanStateUpdateScheduler {

    private final LoanApplicationRepository loanApplicationRepository;
    private final SmsHandlersService smsHandlersService;
    
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    /**
     * Runs every hour to update loan states based on configuration rules
     */
    @Scheduled(cron = "0 0 0/1 * * *")
    @Transactional
    public void updateLoanStates() {
        log.info("Starting Loan State Update Scheduler...");
        Date now = new Date();

        try {
            // Fetch all active loans once to avoid race conditions
            List<MLoanApplication> allActiveLoans = loanApplicationRepository
                    .findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED);
            
            // Process loans through state machine
            for (MLoanApplication loan : allActiveLoans) {
                try {
                    processLoanStateTransition(loan, now);
                } catch (Exception e) {
                    log.error("Error processing loan {}: {}", loan.getLoanApplicationId(), e.getMessage(), e);
                }
            }

            log.info("Loan State Update Scheduler completed successfully.");
        } catch (Exception e) {
            log.error("Error in Loan State Update Scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Single state machine to process loan transitions
     */
    private void processLoanStateTransition(MLoanApplication loan, Date now) {
        LoanStateEnum currentState = loan.getLoanState();
        MLoanProductConfiguration config = loan.getLoanProductConfiguration();
        
        // Skip terminal states
        if (isTerminalState(currentState)) {
            return;
        }

        // 1. Handle zero balance - highest priority
        if (hasZeroBalance(loan)) {
            closeLoan(loan, now, "AUTO_CLOSE_ZERO_BALANCE");
            return;
        }

        // 2. Handle state-specific transitions
        switch (currentState) {
            case PENDING_APPROVAL:
                processPendingApproval(loan, now);
                break;
            case OPEN:
                processOpenLoan(loan, now);
                break;
            case OVERDUE:
                processOverdueLoan(loan, now);
                break;
            case REINSTATED:
                processReinstatedLoan(loan, now);
                break;
            default:
                log.debug("No processing needed for loan {} in state {}", loan.getLoanApplicationId(), currentState);
        }
    }

    /**
     * Process OPEN loans - check for overdue or closure
     */
    private void processOpenLoan(MLoanApplication loan, Date now) {
        if (loan.getDueDate() == null) {
            log.warn("Loan {} has no due date, cannot process", loan.getLoanApplicationId());
            return;
        }

        // Check if loan is overdue
        if (loan.getDueDate().before(now)) {
            // Set overdue since date to the due date (not now) for accurate write-off calculation
            loan.setOverdueSinceDate(loan.getDueDate());
            
            // Transition to OVERDUE
            transitionLoanState(loan, LoanStateEnum.OVERDUE, now, "AUTO_OVERDUE_UPDATE");
            
            // Send overdue notification
            try {
                smsHandlersService.handleLoanOverdueReminder(loan, null);
            } catch (Exception e) {
                log.warn("Failed to send overdue notification for loan {}: {}", 
                        loan.getLoanApplicationId(), e.getMessage());
            }
            
            log.info("Loan {} marked as OVERDUE (Due date: {})", 
                    loan.getLoanApplicationId(), loan.getDueDate());
        }
    }

    /**
     * Process OVERDUE loans - check for write-off
     */
    private void processOverdueLoan(MLoanApplication loan, Date now) {
        MLoanProductConfiguration config = loan.getLoanProductConfiguration();
        
        if (config == null || config.getDaysToWriteOff() == null || config.getDaysToWriteOff() <= 0) {
            log.debug("Write-off not configured for loan {}", loan.getLoanApplicationId());
            return;
        }

        Date overdueSince = loan.getOverdueSinceDate();
        if (overdueSince == null) {
            overdueSince = loan.getDueDate();
            if (overdueSince == null) {
                log.warn("Loan {} has no overdue date or due date", loan.getLoanApplicationId());
                return;
            }
        }

        long daysOverdue = calculateDaysBetween(overdueSince, now);
        
        if (daysOverdue >= config.getDaysToWriteOff()) {
            writeOffLoan(loan, now, daysOverdue);
        }
    }

    /**
     * Process PENDING_APPROVAL loans - check for cancellation
     */
    private void processPendingApproval(MLoanApplication loan, Date now) {
        // Only process DRAFT loans
        if (loan.getApprovalStage() != ApprovalStage.DRAFT) {
            return;
        }

        MLoanProductConfiguration config = loan.getLoanProductConfiguration();
        
        if (config == null || config.getDaysToCancel() == null || config.getDaysToCancel() <= 0) {
            return;
        }

        Date createdDate = loan.getCreated();
        if (createdDate == null) {
            log.warn("Loan {} has no created date", loan.getLoanApplicationId());
            return;
        }

        long daysSinceCreation = calculateDaysBetween(createdDate, now);
        
        if (daysSinceCreation >= config.getDaysToCancel()) {
            cancelLoan(loan, now, daysSinceCreation);
        }
    }

    /**
     * Process REINSTATED loans - check grace period expiry
     */
    private void processReinstatedLoan(MLoanApplication loan, Date now) {
        MLoanProductConfiguration config = loan.getLoanProductConfiguration();
        
        if (config == null || config.getReinstatementGracePeriodDays() == null 
                || config.getReinstatementGracePeriodDays() <= 0) {
            return;
        }

        Date reinstatementDate = loan.getReinstatementDate();
        if (reinstatementDate == null) {
            log.warn("Reinstated loan {} has no reinstatement date", loan.getLoanApplicationId());
            return;
        }

        long daysSinceReinstatement = calculateDaysBetween(reinstatementDate, now);
        
        // Still within grace period - do nothing
        if (daysSinceReinstatement < config.getReinstatementGracePeriodDays()) {
            return;
        }

        // Grace period expired - determine next state
        if (loan.getBalance() == null || loan.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            // Zero balance - close the loan
            closeLoan(loan, now, "AUTO_CLOSE_AFTER_REINSTATEMENT");
            return;
        }

        // Check if should write off based on original overdue days
        if (config.getDaysToWriteOff() != null && config.getDaysToWriteOff() > 0) {
            Date dueDate = loan.getDueDate();
            if (dueDate != null) {
                long totalDaysOverdue = calculateDaysBetween(dueDate, now);
                if (totalDaysOverdue >= config.getDaysToWriteOff()) {
                    writeOffLoan(loan, now, totalDaysOverdue);
                    return;
                }
            }
        }

        // Otherwise, go back to OVERDUE
        transitionLoanState(loan, LoanStateEnum.OVERDUE, now, "AUTO_REINSTATEMENT_GRACE_EXPIRED");
        log.info("Loan {} reinstatement grace period expired, returned to OVERDUE", 
                loan.getLoanApplicationId());
    }

    /**
     * Close a loan (handle all closure logic)
     */
    private void closeLoan(MLoanApplication loan, Date now, String trigger) {
        if (!isValidTransition(loan.getLoanState(), LoanStateEnum.CLOSED)) {
            log.warn("Invalid transition from {} to CLOSED for loan {}", 
                    loan.getLoanState(), loan.getLoanApplicationId());
            return;
        }

        transitionLoanState(loan, LoanStateEnum.CLOSED, now, trigger);
        loan.setClosedDate(now);
        loan.setRepaymentStatus(LoanRepaymentStatus.PAID);

        loanApplicationRepository.save(loan);
        log.info("Loan {} closed (Trigger: {})", loan.getLoanApplicationId(), trigger);

        try {
            smsHandlersService.handleLoanClosureNotification(loan, loan.getBalance(), null);
        } catch (Exception e) {
            log.warn("Failed to send closure notification for loan {}: {}", 
                    loan.getLoanApplicationId(), e.getMessage());
        }
    }

    /**
     * Write off a loan (handle all write-off logic)
     */
    private void writeOffLoan(MLoanApplication loan, Date now, long daysOverdue) {
        if (!isValidTransition(loan.getLoanState(), LoanStateEnum.WRITTEN_OFF)) {
            log.warn("Invalid transition from {} to WRITTEN_OFF for loan {}", 
                    loan.getLoanState(), loan.getLoanApplicationId());
            return;
        }

        transitionLoanState(loan, LoanStateEnum.WRITTEN_OFF, now, "AUTO_WRITE_OFF");
        loan.setWriteOffDate(now);
        loan.setWriteOffReason("Auto-write-off after " + daysOverdue + " days overdue");
        loan.setRepaymentStatus(LoanRepaymentStatus.DEFAULTED);

        loanApplicationRepository.save(loan);
        log.info("Loan {} written off (Overdue for {} days)", 
                loan.getLoanApplicationId(), daysOverdue);

        try {
            smsHandlersService.handleLoanWriteOffNotification(loan, loan.getBalance(), 
                    loan.getWriteOffReason(), null);
        } catch (Exception e) {
            log.warn("Failed to send write-off notification for loan {}: {}", 
                    loan.getLoanApplicationId(), e.getMessage());
        }
    }

    /**
     * Cancel a loan (handle all cancellation logic)
     */
    private void cancelLoan(MLoanApplication loan, Date now, long daysSinceCreation) {
        if (!isValidTransition(loan.getLoanState(), LoanStateEnum.CANCELLED)) {
            log.warn("Invalid transition from {} to CANCELLED for loan {}", 
                    loan.getLoanState(), loan.getLoanApplicationId());
            return;
        }

        transitionLoanState(loan, LoanStateEnum.CANCELLED, now, "AUTO_CANCEL");
        loan.setCancelledDate(now);
        loan.setCancellationReason("Auto-cancelled after " + daysSinceCreation + " days");

        loanApplicationRepository.save(loan);
        log.info("Loan {} auto-cancelled (Pending for {} days)", 
                loan.getLoanApplicationId(), daysSinceCreation);

        try {
            smsHandlersService.handleLoanCancellation(loan, loan.getCancellationReason());
        } catch (Exception e) {
            log.warn("Failed to send cancellation notification for loan {}: {}", 
                    loan.getLoanApplicationId(), e.getMessage());
        }
    }

    /**
     * Centralized state transition with validation
     */
    private void transitionLoanState(MLoanApplication loan, LoanStateEnum newState, Date now, String trigger) {
        if (!isValidTransition(loan.getLoanState(), newState)) {
            throw new IllegalStateException(
                String.format("Invalid transition from %s to %s for loan %d", 
                    loan.getLoanState(), newState, loan.getLoanApplicationId())
            );
        }

        loan.setLoanState(newState);
        loan.setStateChangeDate(now);
        loan.setLastStateChangeTrigger(trigger);
        
        // Set specific dates based on new state
        switch (newState) {
            case OVERDUE:
                if (loan.getOverdueSinceDate() == null) {
                    loan.setOverdueSinceDate(now);
                }
                break;
            case WRITTEN_OFF:
                loan.setWriteOffDate(now);
                loan.setRepaymentStatus(LoanRepaymentStatus.DEFAULTED);
                break;
            case CLOSED:
                loan.setClosedDate(now);
                loan.setRepaymentStatus(LoanRepaymentStatus.PAID);
                break;
            case CANCELLED:
                loan.setCancelledDate(now);
                break;
            case REINSTATED:
                loan.setReinstatementDate(now);
                break;
            default:
                break;
        }
    }

    /**
     * Helper methods
     */
    private boolean hasZeroBalance(MLoanApplication loan) {
        return loan.getBalance() != null && loan.getBalance().compareTo(BigDecimal.ZERO) == 0;
    }

    private boolean isTerminalState(LoanStateEnum state) {
        return state == LoanStateEnum.CLOSED || state == LoanStateEnum.CANCELLED 
                || state == LoanStateEnum.WRITTEN_OFF || state == LoanStateEnum.REJECTED;
    }

    private long calculateDaysBetween(Date start, Date end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(
            start.toInstant().atZone(SYSTEM_ZONE).toLocalDate(),
            end.toInstant().atZone(SYSTEM_ZONE).toLocalDate()
        );
    }

    private boolean isValidTransition(LoanStateEnum currentState, LoanStateEnum newState) {
        if (currentState == newState) return true;

        switch (currentState) {
            case PENDING_APPROVAL:
                return newState == LoanStateEnum.OPEN || newState == LoanStateEnum.CANCELLED
                        || newState == LoanStateEnum.REJECTED;
            case OPEN:
                return newState == LoanStateEnum.OVERDUE || newState == LoanStateEnum.CLOSED
                        || newState == LoanStateEnum.CANCELLED;
            case OVERDUE:
                return newState == LoanStateEnum.WRITTEN_OFF || newState == LoanStateEnum.CLOSED
                        || newState == LoanStateEnum.REINSTATED;
            case REINSTATED:
                return newState == LoanStateEnum.OVERDUE || newState == LoanStateEnum.WRITTEN_OFF
                        || newState == LoanStateEnum.CLOSED;
            case WRITTEN_OFF:
                return newState == LoanStateEnum.REINSTATED;
            case CLOSED:
            case CANCELLED:
            case REJECTED:
                return false;
            default:
                return false;
        }
    }

    /**
     * Public methods for manual intervention
     */
    @Transactional
    public boolean transitionLoanState(Long loanId, LoanStateEnum newState, String trigger) {
        try {
            MLoanApplication loan = loanApplicationRepository.findById(loanId).orElse(null);
            if (loan == null) {
                log.error("Loan not found: {}", loanId);
                return false;
            }

            Date now = new Date();
            transitionLoanState(loan, newState, now, trigger);
            loanApplicationRepository.save(loan);
            
            log.info("Loan {} transitioned from {} to {} (Trigger: {})", 
                    loanId, loan.getLoanState(), newState, trigger);
            return true;

        } catch (Exception e) {
            log.error("Error transitioning loan {} to {}: {}", loanId, newState, e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public boolean forceUpdateLoanState(Long loanId, LoanStateEnum newState, String reason, MUser approvedBy) {
        log.warn("FORCE STATE UPDATE requested for loan {} to {} by {}", 
                loanId, newState, approvedBy != null ? approvedBy.getFullName() : "SYSTEM");
        return transitionLoanState(loanId, newState, "FORCED_UPDATE: " + reason);
    }
}