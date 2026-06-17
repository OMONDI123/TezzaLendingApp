package co.ke.tezza.loanapp.sweepjobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    private MLoanProductConfiguration defaultConfig;
    private Date now;
    private Date pastDate;
    private Date overdueDate;
    private Date futureDate;

    @BeforeEach
    void setUp() {
        now = new Date();
        pastDate = Date.from(LocalDateTime.now().minusDays(10).atZone(ZoneId.systemDefault()).toInstant());
        overdueDate = Date.from(LocalDateTime.now().minusDays(35).atZone(ZoneId.systemDefault()).toInstant());
        futureDate = Date.from(LocalDateTime.now().plusDays(10).atZone(ZoneId.systemDefault()).toInstant());

        defaultConfig = new MLoanProductConfiguration();
        defaultConfig.setLoanProductConfigId(1L);
        defaultConfig.setDaysToWriteOff(30);
        defaultConfig.setDaysToCancel(7);
        defaultConfig.setReinstatementGracePeriodDays(5);
        defaultConfig.setAutoCloseOnFullPayment(true);
    }

    // ======================================================================
    // 1. TEST: UPDATE OVERDUE LOANS
    // ======================================================================

    @Test
    @DisplayName("Should update OPEN loans to OVERDUE when due date has passed")
    void updateOverdueLoans_success() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<MLoanApplication> loanCaptor = ArgumentCaptor.forClass(MLoanApplication.class);

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository).save(loanCaptor.capture());
        MLoanApplication savedLoan = loanCaptor.getValue();
        
        assertEquals(LoanStateEnum.OVERDUE, savedLoan.getLoanState());
        assertNotNull(savedLoan.getStateChangeDate());
        assertNotNull(savedLoan.getOverdueSinceDate());
        assertEquals(pastDate, savedLoan.getOverdueSinceDate());
        assertEquals("AUTO_OVERDUE_UPDATE", savedLoan.getLastStateChangeTrigger());
        
        verify(smsHandlersService).handleLoanOverdueReminder(any(MLoanApplication.class), eq(null));
    }

    @Test
    @DisplayName("Should NOT update OPEN loans to OVERDUE when due date is in future")
    void updateOverdueLoans_dueDateInFuture() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, futureDate, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
        verify(smsHandlersService, never()).handleLoanOverdueReminder(any(MLoanApplication.class), any());
    }

    @Test
    @DisplayName("Should auto-close OPEN loans with zero balance instead of marking overdue")
    void updateOverdueLoans_zeroBalanceAutoClose() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.ZERO);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<MLoanApplication> loanCaptor = ArgumentCaptor.forClass(MLoanApplication.class);

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository).save(loanCaptor.capture());
        MLoanApplication savedLoan = loanCaptor.getValue();
        
        assertEquals(LoanStateEnum.CLOSED, savedLoan.getLoanState());
        assertNotNull(savedLoan.getClosedDate());
        assertEquals("AUTO_CLOSE_ZERO_BALANCE", savedLoan.getLastStateChangeTrigger());
        assertEquals(LoanRepaymentStatus.PAID, savedLoan.getRepaymentStatus());
        
        verify(smsHandlersService, never()).handleLoanOverdueReminder(any(MLoanApplication.class), any());
        verify(smsHandlersService).handleLoanClosureNotification(any(MLoanApplication.class), any(BigDecimal.class), eq(null));
    }

    @Test
    @DisplayName("Should skip when no APPROVED loans exist")
    void updateOverdueLoans_noOpenLoans() {
        // Arrange
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Collections.emptyList());
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
        verify(smsHandlersService, never()).handleLoanOverdueReminder(any(MLoanApplication.class), any());
    }

    // ======================================================================
    // 2. TEST: UPDATE WRITTEN OFF LOANS
    // ======================================================================

    @Test
    @DisplayName("Should write off OVERDUE loans after configured days")
    void updateWrittenOffLoans_success() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OVERDUE, overdueDate, BigDecimal.valueOf(5000));
        loan.setOverdueSinceDate(overdueDate);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<MLoanApplication> loanCaptor = ArgumentCaptor.forClass(MLoanApplication.class);

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository).save(loanCaptor.capture());
        MLoanApplication savedLoan = loanCaptor.getValue();
        
        assertEquals(LoanStateEnum.WRITTEN_OFF, savedLoan.getLoanState());
        assertNotNull(savedLoan.getWriteOffDate());
        assertEquals("AUTO_WRITE_OFF", savedLoan.getLastStateChangeTrigger());
        assertEquals(LoanRepaymentStatus.DEFAULTED, savedLoan.getRepaymentStatus());
        assertTrue(savedLoan.getWriteOffReason().contains("Auto-write-off"));
        
        verify(smsHandlersService).handleLoanWriteOffNotification(
                any(MLoanApplication.class), any(BigDecimal.class), anyString(), eq(null));
    }

    @Test
    @DisplayName("Should NOT write off OVERDUE loans before configured days")
    void updateWrittenOffLoans_beforeThreshold() {
        // Arrange
        Date recentOverdue = Date.from(LocalDateTime.now().minusDays(10).atZone(ZoneId.systemDefault()).toInstant());
        MLoanApplication loan = createTestLoan(LoanStateEnum.OVERDUE, recentOverdue, BigDecimal.valueOf(5000));
        loan.setOverdueSinceDate(recentOverdue);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
        verify(smsHandlersService, never()).handleLoanWriteOffNotification(
                any(MLoanApplication.class), any(BigDecimal.class), anyString(), eq(null));
    }

    @Test
    @DisplayName("Should NOT write off when daysToWriteOff is null")
    void updateWrittenOffLoans_daysToWriteOffNull() {
        // Arrange
        MLoanProductConfiguration config = new MLoanProductConfiguration();
        config.setDaysToWriteOff(null);
        config.setDaysToCancel(7);
        config.setReinstatementGracePeriodDays(5);
        config.setAutoCloseOnFullPayment(true);
        
        MLoanApplication loan = createTestLoan(LoanStateEnum.OVERDUE, overdueDate, BigDecimal.valueOf(5000));
        loan.setOverdueSinceDate(overdueDate);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setLoanProductConfiguration(config);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    // ======================================================================
    // 3. TEST: UPDATE CLOSED LOANS
    // ======================================================================

    @Test
    @DisplayName("Should auto-close loans with zero balance regardless of state")
    void updateClosedLoans_success() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, futureDate, BigDecimal.ZERO);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<MLoanApplication> loanCaptor = ArgumentCaptor.forClass(MLoanApplication.class);

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository).save(loanCaptor.capture());
        MLoanApplication savedLoan = loanCaptor.getValue();
        
        assertEquals(LoanStateEnum.CLOSED, savedLoan.getLoanState());
        assertNotNull(savedLoan.getClosedDate());
        assertEquals("AUTO_CLOSE_ZERO_BALANCE", savedLoan.getLastStateChangeTrigger());
        assertEquals(LoanRepaymentStatus.PAID, savedLoan.getRepaymentStatus());
        
        verify(smsHandlersService).handleLoanClosureNotification(any(MLoanApplication.class), any(BigDecimal.class), eq(null));
    }

    @Test
    @DisplayName("Should NOT auto-close when autoCloseOnFullPayment is false")
    void updateClosedLoans_autoCloseDisabled() {
        // Arrange
        MLoanProductConfiguration config = new MLoanProductConfiguration();
        config.setDaysToWriteOff(30);
        config.setDaysToCancel(7);
        config.setReinstatementGracePeriodDays(5);
        config.setAutoCloseOnFullPayment(false);
        
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.ZERO);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setLoanProductConfiguration(config);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
        verify(smsHandlersService, never()).handleLoanClosureNotification(any(MLoanApplication.class), any(BigDecimal.class), any());
    }

    @Test
    @DisplayName("Should NOT auto-close when loan is in terminal state")
    void updateClosedLoans_terminalState() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.CLOSED, pastDate, BigDecimal.ZERO);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    // ======================================================================
    // 4. TEST: UPDATE CANCELLED LOANS
    // ======================================================================

    @Test
    @DisplayName("Should cancel PENDING_APPROVAL loans after configured days")
    void updateCancelledLoans_success() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.PENDING_APPROVAL, pastDate, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.DRAFT);
        loan.setActive(true);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Collections.emptyList());
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Arrays.asList(loan));

        ArgumentCaptor<MLoanApplication> loanCaptor = ArgumentCaptor.forClass(MLoanApplication.class);

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository).save(loanCaptor.capture());
        MLoanApplication savedLoan = loanCaptor.getValue();
        
        assertEquals(LoanStateEnum.CANCELLED, savedLoan.getLoanState());
        assertNotNull(savedLoan.getCancelledDate());
        assertEquals("AUTO_CANCEL", savedLoan.getLastStateChangeTrigger());
        assertTrue(savedLoan.getCancellationReason().contains("Auto-cancelled"));
        
        verify(smsHandlersService).handleLoanCancellation(any(MLoanApplication.class), anyString());
    }

    @Test
    @DisplayName("Should NOT cancel PENDING_APPROVAL loans before configured days")
    void updateCancelledLoans_beforeThreshold() {
        // Arrange
        Date recentDate = Date.from(LocalDateTime.now().minusDays(3).atZone(ZoneId.systemDefault()).toInstant());
        MLoanApplication loan = createTestLoan(LoanStateEnum.PENDING_APPROVAL, recentDate, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.DRAFT);
        loan.setActive(true);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Collections.emptyList());
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Arrays.asList(loan));

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
        verify(smsHandlersService, never()).handleLoanCancellation(any(MLoanApplication.class), anyString());
    }

    @Test
    @DisplayName("Should NOT cancel when daysToCancel is null")
    void updateCancelledLoans_daysToCancelNull() {
        // Arrange
        MLoanProductConfiguration config = new MLoanProductConfiguration();
        config.setDaysToWriteOff(30);
        config.setDaysToCancel(null);
        config.setReinstatementGracePeriodDays(5);
        config.setAutoCloseOnFullPayment(true);
        
        MLoanApplication loan = createTestLoan(LoanStateEnum.PENDING_APPROVAL, pastDate, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.DRAFT);
        loan.setActive(true);
        loan.setLoanProductConfiguration(config);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Collections.emptyList());
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Arrays.asList(loan));

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    // ======================================================================
    // 5. TEST: UPDATE REINSTATED LOANS
    // ======================================================================

    @Test
    @DisplayName("Should move reinstated loans to OVERDUE after grace period expires")
    void updateReinstatedLoans_graceExpiredToOverdue() {
        // Arrange
        Date reinstatementDate = Date.from(LocalDateTime.now().minusDays(10).atZone(ZoneId.systemDefault()).toInstant());
        MLoanApplication loan = createTestLoan(LoanStateEnum.REINSTATED, pastDate, BigDecimal.valueOf(5000));
        loan.setReinstatementDate(reinstatementDate);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setActive(true);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<MLoanApplication> loanCaptor = ArgumentCaptor.forClass(MLoanApplication.class);

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository).save(loanCaptor.capture());
        MLoanApplication savedLoan = loanCaptor.getValue();
        
        assertEquals(LoanStateEnum.OVERDUE, savedLoan.getLoanState());
        assertEquals("AUTO_REINSTATEMENT_GRACE_EXPIRED", savedLoan.getLastStateChangeTrigger());
        assertNotNull(savedLoan.getOverdueSinceDate());
    }

    @Test
    @DisplayName("Should move reinstated loans to WRITTEN_OFF if days overdue exceed threshold")
    void updateReinstatedLoans_graceExpiredToWrittenOff() {
        // Arrange
        Date reinstatementDate = Date.from(LocalDateTime.now().minusDays(40).atZone(ZoneId.systemDefault()).toInstant());
        Date dueDate = Date.from(LocalDateTime.now().minusDays(45).atZone(ZoneId.systemDefault()).toInstant());
        MLoanApplication loan = createTestLoan(LoanStateEnum.REINSTATED, dueDate, BigDecimal.valueOf(5000));
        loan.setReinstatementDate(reinstatementDate);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setActive(true);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<MLoanApplication> loanCaptor = ArgumentCaptor.forClass(MLoanApplication.class);

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository).save(loanCaptor.capture());
        MLoanApplication savedLoan = loanCaptor.getValue();
        
        assertEquals(LoanStateEnum.WRITTEN_OFF, savedLoan.getLoanState());
        assertEquals("AUTO_WRITE_OFF", savedLoan.getLastStateChangeTrigger());
        assertTrue(savedLoan.getWriteOffReason().contains("Auto-write-off"));
        
        verify(smsHandlersService).handleLoanWriteOffNotification(
                any(MLoanApplication.class), any(BigDecimal.class), anyString(), eq(null));
    }

    @Test
    @DisplayName("Should close reinstated loans with zero balance")
    void updateReinstatedLoans_graceExpiredToClosed() {
        // Arrange
        Date reinstatementDate = Date.from(LocalDateTime.now().minusDays(10).atZone(ZoneId.systemDefault()).toInstant());
        MLoanApplication loan = createTestLoan(LoanStateEnum.REINSTATED, pastDate, BigDecimal.ZERO);
        loan.setReinstatementDate(reinstatementDate);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setActive(true);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<MLoanApplication> loanCaptor = ArgumentCaptor.forClass(MLoanApplication.class);

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository).save(loanCaptor.capture());
        MLoanApplication savedLoan = loanCaptor.getValue();
        
        assertEquals(LoanStateEnum.CLOSED, savedLoan.getLoanState());
        assertEquals("AUTO_CLOSE_ZERO_BALANCE", savedLoan.getLastStateChangeTrigger());
        assertEquals(LoanRepaymentStatus.PAID, savedLoan.getRepaymentStatus());
    }

    @Test
    @DisplayName("Should NOT update reinstated loans within grace period")
    void updateReinstatedLoans_withinGracePeriod() {
        // Arrange
        Date reinstatementDate = Date.from(LocalDateTime.now().minusDays(3).atZone(ZoneId.systemDefault()).toInstant());
        MLoanApplication loan = createTestLoan(LoanStateEnum.REINSTATED, pastDate, BigDecimal.valueOf(5000));
        loan.setReinstatementDate(reinstatementDate);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setActive(true);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    // ======================================================================
    // 6. TEST: STATE TRANSITION VALIDATION
    // ======================================================================

    @Test
    @DisplayName("Should successfully transition loan state")
    void transitionLoanState_success() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanApplicationRepository.save(any(MLoanApplication.class))).thenReturn(loan);

        // Act
        boolean result = scheduler.transitionLoanState(1L, LoanStateEnum.CLOSED, "MANUAL_CLOSE");

        // Assert
        assertTrue(result);
        verify(loanApplicationRepository).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should return false when loan not found")
    void transitionLoanState_loanNotFound() {
        // Arrange
        when(loanApplicationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        boolean result = scheduler.transitionLoanState(1L, LoanStateEnum.CLOSED, "MANUAL_CLOSE");

        // Assert
        assertFalse(result);
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should reject invalid state transitions")
    void transitionLoanState_invalidTransition() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.CLOSED, pastDate, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act
        boolean result = scheduler.transitionLoanState(1L, LoanStateEnum.OPEN, "INVALID");

        // Assert
        assertFalse(result);
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should accept valid state transitions")
    void transitionLoanState_validTransitions() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanApplicationRepository.save(any(MLoanApplication.class))).thenReturn(loan);

        LoanStateEnum[][] validTransitions = {
            {LoanStateEnum.OPEN, LoanStateEnum.OVERDUE},
            {LoanStateEnum.OPEN, LoanStateEnum.CLOSED},
            {LoanStateEnum.OPEN, LoanStateEnum.CANCELLED},
            {LoanStateEnum.OVERDUE, LoanStateEnum.WRITTEN_OFF},
            {LoanStateEnum.OVERDUE, LoanStateEnum.REINSTATED},
            {LoanStateEnum.REINSTATED, LoanStateEnum.OVERDUE},
            {LoanStateEnum.REINSTATED, LoanStateEnum.CLOSED},
            {LoanStateEnum.WRITTEN_OFF, LoanStateEnum.REINSTATED}
        };

        for (LoanStateEnum[] transition : validTransitions) {
            loan.setLoanState(transition[0]);
            boolean result = scheduler.transitionLoanState(1L, transition[1], "TEST_" + transition[0] + "_TO_" + transition[1]);
            assertTrue(result, "Transition " + transition[0] + " -> " + transition[1] + " should be valid");
        }
    }

    // ======================================================================
    // 7. TEST: FORCE UPDATE LOAN STATE
    // ======================================================================

    @Test
    @DisplayName("Should force update loan state")
    void forceUpdateLoanState_success() {
        // Arrange
        MUser mockUser = new MUser();
        mockUser.setUserId(1L);
        mockUser.setFullName("Admin User");
        
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanApplicationRepository.save(any(MLoanApplication.class))).thenReturn(loan);

        // Act
        boolean result = scheduler.forceUpdateLoanState(1L, LoanStateEnum.CLOSED, "Manual intervention", mockUser);

        // Assert
        assertTrue(result);
        verify(loanApplicationRepository).save(any(MLoanApplication.class));
        
        ArgumentCaptor<MLoanApplication> loanCaptor = ArgumentCaptor.forClass(MLoanApplication.class);
        verify(loanApplicationRepository).save(loanCaptor.capture());
        assertTrue(loanCaptor.getValue().getLastStateChangeTrigger().contains("FORCED_UPDATE"));
        assertTrue(loanCaptor.getValue().getLastStateChangeTrigger().contains("Manual intervention"));
    }

    @Test
    @DisplayName("Should force update with SYSTEM as approver when user is null")
    void forceUpdateLoanState_systemApprover() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanApplicationRepository.save(any(MLoanApplication.class))).thenReturn(loan);

        // Act
        boolean result = scheduler.forceUpdateLoanState(1L, LoanStateEnum.CLOSED, "System force close", null);

        // Assert
        assertTrue(result);
        verify(loanApplicationRepository).save(any(MLoanApplication.class));
    }

    // ======================================================================
    // 8. TEST: INTEGRATION - FULL SCHEDULER RUN
    // ======================================================================

    @Test
    @DisplayName("Should process all state updates in a single run")
    void updateLoanStates_fullRun() {
        // Arrange
        // Setup OPEN loan that should become OVERDUE
        MLoanApplication overdueLoan = createTestLoan(LoanStateEnum.OPEN, 
                Date.from(LocalDateTime.now().minusDays(5).atZone(ZoneId.systemDefault()).toInstant()),
                BigDecimal.valueOf(10000));
        overdueLoan.setApprovalStage(ApprovalStage.APPROVED);

        // Setup OVERDUE loan for write-off
        MLoanApplication writeOffLoan = createTestLoan(LoanStateEnum.OVERDUE,
                Date.from(LocalDateTime.now().minusDays(35).atZone(ZoneId.systemDefault()).toInstant()),
                BigDecimal.valueOf(5000));
        writeOffLoan.setOverdueSinceDate(Date.from(LocalDateTime.now().minusDays(35).atZone(ZoneId.systemDefault()).toInstant()));
        writeOffLoan.setApprovalStage(ApprovalStage.APPROVED);

        // Setup OPEN loan with zero balance for closure
        MLoanApplication zeroBalanceLoan = createTestLoan(LoanStateEnum.OPEN,
                Date.from(LocalDateTime.now().minusDays(10).atZone(ZoneId.systemDefault()).toInstant()),
                BigDecimal.ZERO);
        zeroBalanceLoan.setApprovalStage(ApprovalStage.APPROVED);

        // Setup PENDING_APPROVAL loan for cancellation (this is DRAFT, not APPROVED)
        MLoanApplication pendingLoan = createTestLoan(LoanStateEnum.PENDING_APPROVAL,
                Date.from(LocalDateTime.now().minusDays(10).atZone(ZoneId.systemDefault()).toInstant()),
                BigDecimal.valueOf(10000));
        pendingLoan.setApprovalStage(ApprovalStage.DRAFT);
        pendingLoan.setActive(true);

        // Setup REINSTATED loan with grace period expired
        MLoanApplication reinstatedLoan = createTestLoan(LoanStateEnum.REINSTATED,
                Date.from(LocalDateTime.now().minusDays(10).atZone(ZoneId.systemDefault()).toInstant()),
                BigDecimal.valueOf(5000));
        reinstatedLoan.setReinstatementDate(Date.from(LocalDateTime.now().minusDays(10).atZone(ZoneId.systemDefault()).toInstant()));
        reinstatedLoan.setApprovalStage(ApprovalStage.APPROVED);
        reinstatedLoan.setActive(true);

        // Mock the APPROVED loans query
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(overdueLoan, writeOffLoan, zeroBalanceLoan, reinstatedLoan));
        
        // Mock the DRAFT loans query (for pending cancellation)
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Arrays.asList(pendingLoan));

        // Act
        scheduler.updateLoanStates();

        // Assert
        // Each loan should be saved once when its state changes:
        // 1. overdueLoan -> OVERDUE
        // 2. writeOffLoan -> WRITTEN_OFF  
        // 3. zeroBalanceLoan -> CLOSED
        // 4. reinstatedLoan -> OVERDUE
        // 5. pendingLoan -> CANCELLED
        verify(loanApplicationRepository, atLeast(5)).save(any(MLoanApplication.class));
        
        // Verify SMS notifications
        verify(smsHandlersService).handleLoanOverdueReminder(any(MLoanApplication.class), eq(null));
        verify(smsHandlersService).handleLoanWriteOffNotification(
                any(MLoanApplication.class), any(BigDecimal.class), anyString(), eq(null));
        verify(smsHandlersService).handleLoanClosureNotification(any(MLoanApplication.class), any(BigDecimal.class), eq(null));
        verify(smsHandlersService).handleLoanCancellation(any(MLoanApplication.class), anyString());
    }

    // ======================================================================
    // 9. TEST: EDGE CASES
    // ======================================================================

    @Test
    @DisplayName("Should handle null due date gracefully")
    void updateOverdueLoans_nullDueDate() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, null, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should handle null balance gracefully")
    void updateClosedLoans_nullBalance() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, pastDate, null);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should handle null created date gracefully")
    void updateCancelledLoans_nullCreatedDate() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.PENDING_APPROVAL, null, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.DRAFT);
        loan.setActive(true);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Collections.emptyList());
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Arrays.asList(loan));

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should handle null reinstatement date gracefully")
    void updateReinstatedLoans_nullReinstatementDate() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.REINSTATED, pastDate, BigDecimal.valueOf(5000));
        loan.setReinstatementDate(null);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setActive(true);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should handle inactive loans properly")
    void updateLoanStates_inactiveLoans() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OPEN, pastDate, BigDecimal.valueOf(10000));
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setActive(false);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Collections.emptyList());
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should handle multiple loans of different states in one run")
    void updateLoanStates_multipleStates() {
        // Arrange
        MLoanApplication openLoan = createTestLoan(LoanStateEnum.OPEN, futureDate, BigDecimal.valueOf(10000));
        openLoan.setApprovalStage(ApprovalStage.APPROVED);
        
        MLoanApplication overdueLoan = createTestLoan(LoanStateEnum.OVERDUE, 
                Date.from(LocalDateTime.now().minusDays(10).atZone(ZoneId.systemDefault()).toInstant()),
                BigDecimal.valueOf(10000));
        overdueLoan.setOverdueSinceDate(Date.from(LocalDateTime.now().minusDays(10).atZone(ZoneId.systemDefault()).toInstant()));
        overdueLoan.setApprovalStage(ApprovalStage.APPROVED);
        
        MLoanApplication reinstatedLoan = createTestLoan(LoanStateEnum.REINSTATED,
                pastDate, BigDecimal.valueOf(5000));
        reinstatedLoan.setReinstatementDate(Date.from(LocalDateTime.now().minusDays(3).atZone(ZoneId.systemDefault()).toInstant()));
        reinstatedLoan.setApprovalStage(ApprovalStage.APPROVED);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(openLoan, overdueLoan, reinstatedLoan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());
        
        // Act
        scheduler.updateLoanStates();
        
        // Assert
        // Only overdueLoan should be processed (but not written off yet)
        // openLoan has future due date - no change
        // reinstatedLoan within grace period - no change
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    // ======================================================================
    // 10. TEST: CONFIGURATION EDGE CASES
    // ======================================================================

    @Test
    @DisplayName("Should handle missing loan product configuration gracefully")
    void updateLoanStates_missingConfiguration() {
        // Arrange
        MLoanApplication loan = createTestLoan(LoanStateEnum.OVERDUE, overdueDate, BigDecimal.valueOf(5000));
        loan.setOverdueSinceDate(overdueDate);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setLoanProductConfiguration(null);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should handle zero grace period days")
    void updateReinstatedLoans_zeroGracePeriod() {
        // Arrange
        MLoanProductConfiguration config = new MLoanProductConfiguration();
        config.setDaysToWriteOff(30);
        config.setDaysToCancel(7);
        config.setReinstatementGracePeriodDays(0);
        config.setAutoCloseOnFullPayment(true);
        
        Date reinstatementDate = Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant());
        MLoanApplication loan = createTestLoan(LoanStateEnum.REINSTATED, pastDate, BigDecimal.valueOf(5000));
        loan.setReinstatementDate(reinstatementDate);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setActive(true);
        loan.setLoanProductConfiguration(config);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<MLoanApplication> loanCaptor = ArgumentCaptor.forClass(MLoanApplication.class);

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository).save(loanCaptor.capture());
        MLoanApplication savedLoan = loanCaptor.getValue();
        assertEquals(LoanStateEnum.OVERDUE, savedLoan.getLoanState());
    }

    @Test
    @DisplayName("Should handle negative daysToWriteOff gracefully")
    void updateWrittenOffLoans_negativeDaysToWriteOff() {
        // Arrange
        MLoanProductConfiguration config = new MLoanProductConfiguration();
        config.setDaysToWriteOff(-5);
        config.setDaysToCancel(7);
        config.setReinstatementGracePeriodDays(5);
        config.setAutoCloseOnFullPayment(true);
        
        MLoanApplication loan = createTestLoan(LoanStateEnum.OVERDUE, overdueDate, BigDecimal.valueOf(5000));
        loan.setOverdueSinceDate(overdueDate);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setLoanProductConfiguration(config);
        
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.APPROVED))
                .thenReturn(Arrays.asList(loan));
        when(loanApplicationRepository.findByIsActiveTrueAndApprovalStage(ApprovalStage.DRAFT))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.updateLoanStates();

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    // ======================================================================
    // HELPER METHODS
    // ======================================================================

    private MLoanApplication createTestLoan(LoanStateEnum state, Date date, BigDecimal balance) {
        MLoanApplication loan = new MLoanApplication();
        loan.setLoanApplicationId(System.currentTimeMillis());
        loan.setDocumentNo("LN/TEST/" + System.currentTimeMillis());
        loan.setLoanState(state);
        loan.setApprovalStage(ApprovalStage.APPROVED);
        loan.setBalance(balance);
        loan.setDueDate(date);
        loan.setCreated(date);
        loan.setActive(true);
        loan.setLoanProductConfiguration(defaultConfig);
        return loan;
    }
}