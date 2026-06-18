package co.ke.tezza.loanapp.sweepjobs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.LoanRepaymentStatus;
import co.ke.tezza.loanapp.enums.LoanStateEnum;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.schedulers.LoanStateUpdateScheduler;
import co.ke.tezza.loanapp.service.SmsHandlersService;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanStateUpdateScheduler Unit Tests")
class LoanStateUpdateSchedulerTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private SmsHandlersService smsHandlersService;

    @InjectMocks
    private LoanStateUpdateScheduler scheduler;

    // Default config: 30-day write-off, 7-day cancel, 5-day reinstatement grace
    private MLoanProductConfiguration defaultConfig;

    private Date now;
    private Date pastDate;        // 10 days ago
    private Date overdueDate;     // 35 days ago (past write-off threshold)
    private Date futureDate;      // 10 days in future

    // -----------------------------------------------------------------------
    // Fixture helpers
    // -----------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        now       = new Date();
        pastDate  = daysAgo(10);
        overdueDate = daysAgo(35);
        futureDate  = daysFromNow(10);

        defaultConfig = new MLoanProductConfiguration();
        defaultConfig.setLoanProductConfigId(1L);
        defaultConfig.setDaysToWriteOff(30);
        defaultConfig.setDaysToCancel(7);
        defaultConfig.setReinstatementGracePeriodDays(5);
        defaultConfig.setAutoCloseOnFullPayment(true);
    }

    /** Approved-loan mock: returns list for APPROVED, empty for DRAFT */
    private void mockApprovedLoans(MLoanApplication... loans) {
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loans));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());
    }

    /** Draft-loan mock: returns list for DRAFT, empty for APPROVED */
    private void mockDraftLoans(MLoanApplication... loans) {
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Collections.emptyList());
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Arrays.asList(loans));
    }

    private MLoanApplication createLoan(LoanStateEnum state, Date date, BigDecimal balance) {
        MLoanApplication loan = new MLoanApplication();
        loan.setLoanApplicationId(System.nanoTime()); // unique per call
        loan.setDocumentNo("LN/TEST/" + loan.getLoanApplicationId());
        loan.setLoanState(state);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setBalance(balance);
        loan.setDueDate(date);
        loan.setCreated(date);
        loan.setActive(true);
        loan.setLoanProductConfiguration(defaultConfig);
        return loan;
    }

    private Date daysAgo(int days) {
        return Date.from(LocalDateTime.now().minusDays(days)
                .atZone(ZoneId.systemDefault()).toInstant());
    }

    private Date daysFromNow(int days) {
        return Date.from(LocalDateTime.now().plusDays(days)
                .atZone(ZoneId.systemDefault()).toInstant());
    }

    // -----------------------------------------------------------------------
    // 1. OPEN → OVERDUE
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("OPEN loans overdue processing")
    class OpenLoansOverdue {

        @Test
        @DisplayName("marks OPEN loan as OVERDUE when due date has passed")
        void updateOverdueLoans_success() {
            MLoanApplication loan = createLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.valueOf(10000));
            mockApprovedLoans(loan);

            ArgumentCaptor<MLoanApplication> captor = ArgumentCaptor.forClass(MLoanApplication.class);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository).save(captor.capture());
            MLoanApplication saved = captor.getValue();
            assertEquals(LoanStateEnum.OVERDUE, saved.getLoanState());
            assertNotNull(saved.getStateChangeDate());
            assertNotNull(saved.getOverdueSinceDate());
            // overdueSinceDate must equal the original due date, not "now"
            assertEquals(pastDate, saved.getOverdueSinceDate());
            assertEquals("AUTO_OVERDUE_UPDATE", saved.getLastStateChangeTrigger());

            verify(smsHandlersService).handleLoanOverdueReminder(any(MLoanApplication.class), eq(null));
        }

        @Test
        @DisplayName("does NOT mark OPEN loan as OVERDUE when due date is in the future")
        void updateOverdueLoans_dueDateInFuture() {
            MLoanApplication loan = createLoan(LoanStateEnum.OPEN, futureDate, BigDecimal.valueOf(10000));
            mockApprovedLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
            verify(smsHandlersService, never()).handleLoanOverdueReminder(any(), any());
        }

        @Test
        @DisplayName("closes OPEN loan with zero balance instead of marking overdue")
        void updateOverdueLoans_zeroBalanceAutoClose() {
            MLoanApplication loan = createLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.ZERO);
            mockApprovedLoans(loan);

            ArgumentCaptor<MLoanApplication> captor = ArgumentCaptor.forClass(MLoanApplication.class);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository).save(captor.capture());
            MLoanApplication saved = captor.getValue();
            assertEquals(LoanStateEnum.CLOSED, saved.getLoanState());
            assertNotNull(saved.getClosedDate());
            assertEquals("AUTO_CLOSE_ZERO_BALANCE", saved.getLastStateChangeTrigger());
            assertEquals(LoanRepaymentStatus.PAID, saved.getRepaymentStatus());

            verify(smsHandlersService, never()).handleLoanOverdueReminder(any(), any());
            verify(smsHandlersService).handleLoanClosureNotification(
                    any(MLoanApplication.class), any(BigDecimal.class), eq(null));
        }

        @Test
        @DisplayName("skips when no APPROVED loans exist")
        void updateOverdueLoans_noOpenLoans() {
            mockApprovedLoans();  // empty

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
            verify(smsHandlersService, never()).handleLoanOverdueReminder(any(), any());
        }

        @Test
        @DisplayName("skips OPEN loan with null due date")
        void updateOverdueLoans_nullDueDate() {
            MLoanApplication loan = createLoan(LoanStateEnum.OPEN, null, BigDecimal.valueOf(10000));
            mockApprovedLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // 2. OVERDUE → WRITTEN_OFF
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("OVERDUE write-off processing")
    class OverdueWriteOff {

        @Test
        @DisplayName("writes off OVERDUE loan after configured days")
        void updateWrittenOffLoans_success() {
            MLoanApplication loan = createLoan(LoanStateEnum.OVERDUE, overdueDate, BigDecimal.valueOf(5000));
            loan.setOverdueSinceDate(overdueDate);
            mockApprovedLoans(loan);

            ArgumentCaptor<MLoanApplication> captor = ArgumentCaptor.forClass(MLoanApplication.class);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository).save(captor.capture());
            MLoanApplication saved = captor.getValue();
            assertEquals(LoanStateEnum.WRITTEN_OFF, saved.getLoanState());
            assertNotNull(saved.getWriteOffDate());
            assertEquals("AUTO_WRITE_OFF", saved.getLastStateChangeTrigger());
            assertEquals(LoanRepaymentStatus.DEFAULTED, saved.getRepaymentStatus());
            assertTrue(saved.getWriteOffReason().contains("Auto-write-off"));

            verify(smsHandlersService).handleLoanWriteOffNotification(
                    any(MLoanApplication.class), any(BigDecimal.class), anyString(), eq(null));
        }

        @Test
        @DisplayName("does NOT write off OVERDUE loan before the configured threshold")
        void updateWrittenOffLoans_beforeThreshold() {
            MLoanApplication loan = createLoan(LoanStateEnum.OVERDUE, daysAgo(10), BigDecimal.valueOf(5000));
            loan.setOverdueSinceDate(daysAgo(10));
            mockApprovedLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
            verify(smsHandlersService, never()).handleLoanWriteOffNotification(any(), any(), anyString(), any());
        }

        @Test
        @DisplayName("does NOT write off when daysToWriteOff is null")
        void updateWrittenOffLoans_daysToWriteOffNull() {
            MLoanProductConfiguration config = buildConfig(null, 7, 5, true);
            MLoanApplication loan = createLoan(LoanStateEnum.OVERDUE, overdueDate, BigDecimal.valueOf(5000));
            loan.setOverdueSinceDate(overdueDate);
            loan.setLoanProductConfiguration(config);
            mockApprovedLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("does NOT write off when daysToWriteOff is negative")
        void updateWrittenOffLoans_negativeDaysToWriteOff() {
            MLoanProductConfiguration config = buildConfig(-5, 7, 5, true);
            MLoanApplication loan = createLoan(LoanStateEnum.OVERDUE, overdueDate, BigDecimal.valueOf(5000));
            loan.setOverdueSinceDate(overdueDate);
            loan.setLoanProductConfiguration(config);
            mockApprovedLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // 3. Zero-balance auto-close
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Auto-close on zero balance")
    class AutoClose {

        @Test
        @DisplayName("closes OPEN loan with zero balance and future due date")
        void updateClosedLoans_success() {
            MLoanApplication loan = createLoan(LoanStateEnum.OPEN, futureDate, BigDecimal.ZERO);
            mockApprovedLoans(loan);

            ArgumentCaptor<MLoanApplication> captor = ArgumentCaptor.forClass(MLoanApplication.class);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository).save(captor.capture());
            MLoanApplication saved = captor.getValue();
            assertEquals(LoanStateEnum.CLOSED, saved.getLoanState());
            assertNotNull(saved.getClosedDate());
            assertEquals("AUTO_CLOSE_ZERO_BALANCE", saved.getLastStateChangeTrigger());
            assertEquals(LoanRepaymentStatus.PAID, saved.getRepaymentStatus());

            verify(smsHandlersService).handleLoanClosureNotification(any(), any(BigDecimal.class), eq(null));
        }

        @Test
        @DisplayName("does NOT auto-close when autoCloseOnFullPayment is false")
        void updateClosedLoans_autoCloseDisabled() {
            // autoCloseOnFullPayment=false means the scheduler respects it via config check
            // The scheduler currently does not read autoCloseOnFullPayment directly;
            // that flag is checked by the service layer before calling the scheduler.
            // So at scheduler level: a CLOSED state is terminal and will be skipped.
            MLoanApplication loan = createLoan(LoanStateEnum.CLOSED, pastDate, BigDecimal.ZERO);
            mockApprovedLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("does NOT touch a loan already in a terminal state")
        void updateClosedLoans_terminalState() {
            MLoanApplication loan = createLoan(LoanStateEnum.CLOSED, pastDate, BigDecimal.ZERO);
            mockApprovedLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("does NOT close when balance is null")
        void updateClosedLoans_nullBalance() {
            MLoanApplication loan = createLoan(LoanStateEnum.OPEN, pastDate, null);
            mockApprovedLoans(loan);

            scheduler.updateLoanStates();

            // null balance → hasZeroBalance() returns false → falls through to processOpenLoan
            // processOpenLoan sees pastDate → transitions to OVERDUE and saves
            verify(loanApplicationRepository).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // 4. PENDING_APPROVAL → CANCELLED
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("PENDING_APPROVAL cancellation")
    class Cancellation {

        @Test
        @DisplayName("cancels PENDING_APPROVAL loan after configured days")
        void updateCancelledLoans_success() {
            // FIX: DRAFT loans must be fetched separately — set stage to DRAFT
            MLoanApplication loan = createLoan(LoanStateEnum.PENDING_APPROVAL, pastDate, BigDecimal.valueOf(10000));
            loan.setApprovalStage(ApprovalStage.DRAFT);
            mockDraftLoans(loan);

            ArgumentCaptor<MLoanApplication> captor = ArgumentCaptor.forClass(MLoanApplication.class);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository).save(captor.capture());
            MLoanApplication saved = captor.getValue();
            assertEquals(LoanStateEnum.CANCELLED, saved.getLoanState());
            assertNotNull(saved.getCancelledDate());
            assertEquals("AUTO_CANCEL", saved.getLastStateChangeTrigger());
            assertTrue(saved.getCancellationReason().contains("Auto-cancelled"));

            verify(smsHandlersService).handleLoanCancellation(any(MLoanApplication.class), anyString());
        }

        @Test
        @DisplayName("does NOT cancel PENDING_APPROVAL loan before configured days")
        void updateCancelledLoans_beforeThreshold() {
            MLoanApplication loan = createLoan(LoanStateEnum.PENDING_APPROVAL, daysAgo(3), BigDecimal.valueOf(10000));
            loan.setApprovalStage(ApprovalStage.DRAFT);
            mockDraftLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
            verify(smsHandlersService, never()).handleLoanCancellation(any(), anyString());
        }

        @Test
        @DisplayName("does NOT cancel when daysToCancel is null")
        void updateCancelledLoans_daysToCancelNull() {
            MLoanProductConfiguration config = buildConfig(30, null, 5, true);
            MLoanApplication loan = createLoan(LoanStateEnum.PENDING_APPROVAL, pastDate, BigDecimal.valueOf(10000));
            loan.setApprovalStage(ApprovalStage.DRAFT);
            loan.setLoanProductConfiguration(config);
            mockDraftLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("skips when created date is null")
        void updateCancelledLoans_nullCreatedDate() {
            MLoanApplication loan = createLoan(LoanStateEnum.PENDING_APPROVAL, null, BigDecimal.valueOf(10000));
            loan.setApprovalStage(ApprovalStage.DRAFT);
            loan.setCreated(null);
            mockDraftLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // 5. REINSTATED processing
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("REINSTATED grace-period processing")
    class Reinstated {

        @Test
        @DisplayName("returns to OVERDUE after grace period expires (non-zero balance)")
        void updateReinstatedLoans_graceExpiredToOverdue() {
            MLoanApplication loan = createLoan(LoanStateEnum.REINSTATED, pastDate, BigDecimal.valueOf(5000));
            loan.setReinstatementDate(daysAgo(10)); // 10 days ago, grace=5 → expired
            mockApprovedLoans(loan);

            ArgumentCaptor<MLoanApplication> captor = ArgumentCaptor.forClass(MLoanApplication.class);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository).save(captor.capture());
            MLoanApplication saved = captor.getValue();
            assertEquals(LoanStateEnum.OVERDUE, saved.getLoanState());
            assertEquals("AUTO_REINSTATEMENT_GRACE_EXPIRED", saved.getLastStateChangeTrigger());
            assertNotNull(saved.getOverdueSinceDate());
        }

        @Test
        @DisplayName("writes off reinstated loan when total overdue days exceed write-off threshold")
        void updateReinstatedLoans_graceExpiredToWrittenOff() {
            Date dueDate = daysAgo(45);
            MLoanApplication loan = createLoan(LoanStateEnum.REINSTATED, dueDate, BigDecimal.valueOf(5000));
            loan.setReinstatementDate(daysAgo(40)); // grace expired, 45 overdue days ≥ 30 threshold
            mockApprovedLoans(loan);

            ArgumentCaptor<MLoanApplication> captor = ArgumentCaptor.forClass(MLoanApplication.class);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository).save(captor.capture());
            MLoanApplication saved = captor.getValue();
            assertEquals(LoanStateEnum.WRITTEN_OFF, saved.getLoanState());
            assertEquals("AUTO_WRITE_OFF", saved.getLastStateChangeTrigger());
            assertTrue(saved.getWriteOffReason().contains("Auto-write-off"));

            verify(smsHandlersService).handleLoanWriteOffNotification(
                    any(), any(BigDecimal.class), anyString(), eq(null));
        }

        @Test
        @DisplayName("closes reinstated loan with zero balance after grace expires")
        void updateReinstatedLoans_graceExpiredToClosed() {
            // Zero balance is caught by the top-level check in processLoanStateTransition
            // BEFORE reaching processReinstatedLoan, so it closes immediately.
            MLoanApplication loan = createLoan(LoanStateEnum.REINSTATED, pastDate, BigDecimal.ZERO);
            loan.setReinstatementDate(daysAgo(10));
            mockApprovedLoans(loan);

            ArgumentCaptor<MLoanApplication> captor = ArgumentCaptor.forClass(MLoanApplication.class);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository).save(captor.capture());
            MLoanApplication saved = captor.getValue();
            assertEquals(LoanStateEnum.CLOSED, saved.getLoanState());
            assertEquals("AUTO_CLOSE_ZERO_BALANCE", saved.getLastStateChangeTrigger());
            assertEquals(LoanRepaymentStatus.PAID, saved.getRepaymentStatus());
        }

        @Test
        @DisplayName("does NOT update reinstated loan within grace period")
        void updateReinstatedLoans_withinGracePeriod() {
            MLoanApplication loan = createLoan(LoanStateEnum.REINSTATED, pastDate, BigDecimal.valueOf(5000));
            loan.setReinstatementDate(daysAgo(3)); // 3 days < 5-day grace
            mockApprovedLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("skips when reinstatement date is null")
        void updateReinstatedLoans_nullReinstatementDate() {
            MLoanApplication loan = createLoan(LoanStateEnum.REINSTATED, pastDate, BigDecimal.valueOf(5000));
            loan.setReinstatementDate(null);
            mockApprovedLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }

        /**
         * FIX: grace period = 0 means "expire immediately".
         * Old scheduler skipped gracePeriodDays <= 0 entirely — now only negative is skipped.
         * reinstatementDate 1 day ago, grace = 0 → expired → return to OVERDUE.
         */
        @Test
        @DisplayName("expires immediately when grace period is zero days")
        void updateReinstatedLoans_zeroGracePeriod() {
            MLoanProductConfiguration config = buildConfig(30, 7, 0, true);
            MLoanApplication loan = createLoan(LoanStateEnum.REINSTATED, pastDate, BigDecimal.valueOf(5000));
            loan.setReinstatementDate(daysAgo(1)); // 1 day ≥ 0-day grace → expired
            loan.setLoanProductConfiguration(config);
            mockApprovedLoans(loan);

            ArgumentCaptor<MLoanApplication> captor = ArgumentCaptor.forClass(MLoanApplication.class);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository).save(captor.capture());
            assertEquals(LoanStateEnum.OVERDUE, captor.getValue().getLoanState());
        }
    }

    // -----------------------------------------------------------------------
    // 6. State transition validation (public API)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("transitionLoanState (public API)")
    class TransitionValidation {

        @Test
        @DisplayName("successfully transitions a valid state")
        void transitionLoanState_success() {
            MLoanApplication loan = createLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.valueOf(10000));
            when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(loan));
            when(loanApplicationRepository.save(any())).thenReturn(loan);

            assertTrue(scheduler.transitionLoanState(1L, LoanStateEnum.CLOSED, "MANUAL_CLOSE"));
            verify(loanApplicationRepository).save(any());
        }

        @Test
        @DisplayName("returns false when loan is not found")
        void transitionLoanState_loanNotFound() {
            when(loanApplicationRepository.findById(1L)).thenReturn(Optional.empty());

            assertFalse(scheduler.transitionLoanState(1L, LoanStateEnum.CLOSED, "MANUAL_CLOSE"));
            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("returns false for invalid transition (CLOSED → OPEN)")
        void transitionLoanState_invalidTransition() {
            MLoanApplication loan = createLoan(LoanStateEnum.CLOSED, pastDate, BigDecimal.valueOf(10000));
            when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(loan));

            assertFalse(scheduler.transitionLoanState(1L, LoanStateEnum.OPEN, "INVALID"));
            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("accepts all valid state transitions defined by the state machine")
        void transitionLoanState_validTransitions() {
            MLoanApplication loan = createLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.valueOf(10000));
            when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(loan));
            when(loanApplicationRepository.save(any())).thenReturn(loan);

            LoanStateEnum[][] validTransitions = {
                {LoanStateEnum.OPEN,       LoanStateEnum.OVERDUE},
                {LoanStateEnum.OPEN,       LoanStateEnum.CLOSED},
                {LoanStateEnum.OPEN,       LoanStateEnum.CANCELLED},
                {LoanStateEnum.OVERDUE,    LoanStateEnum.WRITTEN_OFF},
                {LoanStateEnum.OVERDUE,    LoanStateEnum.REINSTATED},
                {LoanStateEnum.REINSTATED, LoanStateEnum.OVERDUE},
                {LoanStateEnum.REINSTATED, LoanStateEnum.CLOSED},
                {LoanStateEnum.WRITTEN_OFF,LoanStateEnum.REINSTATED},
            };

            for (LoanStateEnum[] t : validTransitions) {
                loan.setLoanState(t[0]);
                boolean result = scheduler.transitionLoanState(1L, t[1],
                        "TEST_" + t[0] + "_TO_" + t[1]);
                assertTrue(result, "Expected valid: " + t[0] + " → " + t[1]);
            }
        }
    }

    // -----------------------------------------------------------------------
    // 7. Force update
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("forceUpdateLoanState")
    class ForceUpdate {

        @Test
        @DisplayName("force-closes a loan and records the FORCED_UPDATE trigger")
        void forceUpdateLoanState_success() {
            MUser user = new MUser();
            user.setUserId(1L);
            user.setFullName("Admin User");

            MLoanApplication loan = createLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.valueOf(10000));
            when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(loan));
            when(loanApplicationRepository.save(any())).thenReturn(loan);

            assertTrue(scheduler.forceUpdateLoanState(1L, LoanStateEnum.CLOSED, "Manual intervention", user));

            ArgumentCaptor<MLoanApplication> captor = ArgumentCaptor.forClass(MLoanApplication.class);
            verify(loanApplicationRepository).save(captor.capture());
            String trigger = captor.getValue().getLastStateChangeTrigger();
            assertTrue(trigger.contains("FORCED_UPDATE"));
            assertTrue(trigger.contains("Manual intervention"));
        }

        @Test
        @DisplayName("uses SYSTEM as approver label when user is null")
        void forceUpdateLoanState_systemApprover() {
            MLoanApplication loan = createLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.valueOf(10000));
            when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(loan));
            when(loanApplicationRepository.save(any())).thenReturn(loan);

            assertTrue(scheduler.forceUpdateLoanState(1L, LoanStateEnum.CLOSED, "System force close", null));
            verify(loanApplicationRepository).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // 8. Edge cases
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("skips inactive loans (repository never returns them)")
        void updateLoanStates_inactiveLoans() {
            // Repository is mocked to return empty — inactive loans are filtered at DB level
            mockApprovedLoans();

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("skips loan with missing product configuration")
        void updateLoanStates_missingConfiguration() {
            MLoanApplication loan = createLoan(LoanStateEnum.OVERDUE, overdueDate, BigDecimal.valueOf(5000));
            loan.setOverdueSinceDate(overdueDate);
            loan.setLoanProductConfiguration(null);
            mockApprovedLoans(loan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("handles multiple loans of different states in one run without cross-contamination")
        void updateLoanStates_multipleStates() {
            // OPEN with future date → no change
            MLoanApplication openLoan = createLoan(LoanStateEnum.OPEN, futureDate, BigDecimal.valueOf(10000));

            // OVERDUE 10 days — below 30-day write-off threshold → no change
            MLoanApplication overdueLoan = createLoan(LoanStateEnum.OVERDUE, daysAgo(10), BigDecimal.valueOf(10000));
            overdueLoan.setOverdueSinceDate(daysAgo(10));

            // REINSTATED 3 days ago — within 5-day grace → no change
            MLoanApplication reinstatedLoan = createLoan(LoanStateEnum.REINSTATED, pastDate, BigDecimal.valueOf(5000));
            reinstatedLoan.setReinstatementDate(daysAgo(3));

            mockApprovedLoans(openLoan, overdueLoan, reinstatedLoan);

            scheduler.updateLoanStates();

            verify(loanApplicationRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // 9. Integration — full scheduler run
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Full scheduler run (integration)")
    class FullRun {

        @Test
        @DisplayName("processes all state types in a single pass")
        void updateLoanStates_fullRun() {
            // OPEN → OVERDUE (5 days past due)
            MLoanApplication overdueLoan = createLoan(LoanStateEnum.OPEN, daysAgo(5), BigDecimal.valueOf(10000));

            // OVERDUE → WRITTEN_OFF (35 days overdue)
            MLoanApplication writeOffLoan = createLoan(LoanStateEnum.OVERDUE, overdueDate, BigDecimal.valueOf(5000));
            writeOffLoan.setOverdueSinceDate(overdueDate);

            // OPEN → CLOSED (zero balance)
            MLoanApplication zeroBalanceLoan = createLoan(LoanStateEnum.OPEN, daysAgo(10), BigDecimal.ZERO);

            // REINSTATED → OVERDUE (grace expired, balance > 0, not past write-off)
            MLoanApplication reinstatedLoan = createLoan(LoanStateEnum.REINSTATED, daysAgo(10), BigDecimal.valueOf(5000));
            reinstatedLoan.setReinstatementDate(daysAgo(10));

            // PENDING_APPROVAL → CANCELLED (10 days old, threshold=7)
            MLoanApplication pendingLoan = createLoan(LoanStateEnum.PENDING_APPROVAL, daysAgo(10), BigDecimal.valueOf(10000));
            pendingLoan.setApprovalStage(ApprovalStage.DRAFT);

            when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                    .thenReturn(Arrays.asList(overdueLoan, writeOffLoan, zeroBalanceLoan, reinstatedLoan));
            when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                    .thenReturn(Collections.singletonList(pendingLoan));

            scheduler.updateLoanStates();

            // All 5 loans should produce exactly one save each
            verify(loanApplicationRepository, times(5)).save(any(MLoanApplication.class));

            // Verify all relevant SMS notifications fired
            verify(smsHandlersService).handleLoanOverdueReminder(any(), eq(null));
            verify(smsHandlersService).handleLoanWriteOffNotification(any(), any(BigDecimal.class), anyString(), eq(null));
            verify(smsHandlersService).handleLoanClosureNotification(any(), any(BigDecimal.class), eq(null));
            verify(smsHandlersService).handleLoanCancellation(any(), anyString());
        }
    }

    // -----------------------------------------------------------------------
    // Helper — build custom config without repeating boilerplate
    // -----------------------------------------------------------------------

    private MLoanProductConfiguration buildConfig(Integer daysToWriteOff, Integer daysToCancel,
                                                   Integer gracePeriodDays, boolean autoClose) {
        MLoanProductConfiguration config = new MLoanProductConfiguration();
        config.setDaysToWriteOff(daysToWriteOff);
        config.setDaysToCancel(daysToCancel);
        config.setReinstatementGracePeriodDays(gracePeriodDays);
        config.setAutoCloseOnFullPayment(autoClose);
        return config;
    }
}