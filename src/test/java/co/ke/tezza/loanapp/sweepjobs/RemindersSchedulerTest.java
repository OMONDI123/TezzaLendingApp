package co.ke.tezza.loanapp.sweepjobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MDebtor;
import co.ke.tezza.loanapp.entity.MGuarantorLoan;
import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.entity.MNextOfKin;
import co.ke.tezza.loanapp.entity.MRemindersConfiguration;
import co.ke.tezza.loanapp.entity.MSms;
import co.ke.tezza.loanapp.entity.MSmsSetup;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.ReminderFrequency;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import co.ke.tezza.loanapp.repository.GuarantorLoanRepository;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.repository.ReminderConfigRepository;
import co.ke.tezza.loanapp.repository.SmsRepository;
import co.ke.tezza.loanapp.schedulers.RemindersScheduler;
import co.ke.tezza.loanapp.service.SmsHandlersService;
import co.ke.tezza.loanapp.util.Utils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RemindersScheduler Unit Tests")
class RemindersSchedulerTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private ReminderConfigRepository reminderConfigRepository;

    @Mock
    private SmsRepository smsRepository;

    @Mock
    private Utils utils;

    @Mock
    private SmsHandlersService smsHandlersService;

    @Mock
    private GuarantorLoanRepository guarantorLoanRepository;

    @InjectMocks
    private RemindersScheduler scheduler;

    private MLoanApplication mockLoan;
    private MInstallments mockInstallment;
    private MNextOfKin mockGuarantor;
    private MRemindersConfiguration mockConfig;
    private MSmsSetup mockSmsSetup;
    private MADSysConfig mockSysConfig;

    @BeforeEach
    void setUp() {
        mockLoan = new MLoanApplication();
        mockLoan.setLoanApplicationId(1L);
        mockLoan.setDocumentNo("LN/2024/001");
        mockLoan.setBalance(BigDecimal.valueOf(10000));
        mockLoan.setAdOrgID(1L);
        mockLoan.setAdClientId(1L);
        mockLoan.setDueDate(new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));
        mockLoan.setTermInDays(30);
        mockLoan.setBorrowerType(BorrowerTypeEnum.INDIVIDUAL);

       
        MDebtor individualBorrower = new MDebtor();
        individualBorrower.setIndividualBorrowerId(100L);
        mockLoan.setIndividualBorrower(individualBorrower);

        MLoanProductConfiguration config = new MLoanProductConfiguration();
        config.setRepaymentScheduleType(co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum.INSTALLMENTS);
        mockLoan.setLoanProductConfiguration(config);

        mockInstallment = new MInstallments();
        mockInstallment.setInstallmentId(1L);
        mockInstallment.setBalance(BigDecimal.valueOf(10000));
        mockInstallment.setLoan(mockLoan);
        mockInstallment.setPeriodEnd(new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));

        mockGuarantor = new MNextOfKin();
        mockGuarantor.setNextOfKinId(1L);
        mockGuarantor.setFullName("John Guarantor");
        mockGuarantor.setPhoneNumber("0712345678");
        mockGuarantor.setEmail("guarantor@example.com");

        Set<MNextOfKin> guarantors = new HashSet<>();
        guarantors.add(mockGuarantor);
        mockLoan.setGuarantors(guarantors);

        mockSmsSetup = new MSmsSetup();
        mockSmsSetup.setActive(true);
        mockSmsSetup.setSmsType(SmsTypeEnum.LOAN_OR_DEBT_DUE_REMINDER);

        mockConfig = new MRemindersConfiguration();
        mockConfig.setReminderId(1L);
        mockConfig.setActive(true);
        mockConfig.setAdOrgID(1L);
        mockConfig.setReminderFrequency(ReminderFrequency.ONCE);
        mockConfig.setSmsMessageTemplate(mockSmsSetup);
        mockConfig.setMaxReminders(1);
        mockConfig.setSendTimeEnabled(false);
        mockConfig.setStartNoOfDaysBefore(7);
        mockConfig.setStartNoOfDaysAfter(0);

        mockSysConfig = new MADSysConfig();
        mockSysConfig.setAllowSystemNotifications(true);
        mockSysConfig.setAllowDefaultSms(true);
    }

    @Test
    @DisplayName("Should execute comprehensive reminder scheduler with active loans")
    void executeComprehensiveReminderScheduler_success() {
        // Arrange
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(Arrays.asList(mockLoan));
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);

        // Act
        scheduler.executeComprehensiveReminderScheduler();

        // Assert
        verify(smsHandlersService, atLeastOnce()).handleLoanDueReminder(
                any(MLoanApplication.class), 
                any() 
        );
    }

    @Test
    @DisplayName("Should skip when no active loans")
    void executeComprehensiveReminderScheduler_noActiveLoans() {
        // Arrange
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.executeComprehensiveReminderScheduler();

        // Assert
        verify(installmentRepository, never()).findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                anyBoolean(), any(BigDecimal.class), any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should skip when system notifications are disabled")
    void executeComprehensiveReminderScheduler_notificationsDisabled() {
        // Arrange
        mockSysConfig.setAllowSystemNotifications(false);
        
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(Arrays.asList(mockLoan));
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);

        // Act
        scheduler.executeComprehensiveReminderScheduler();

        // Assert
        verify(smsHandlersService, never()).handleLoanDueReminder(any(MLoanApplication.class), any());
    }

    @Test
    @DisplayName("Should process configured reminders for loan")
    void processConfiguredSmsTypesForLoan_success() {
        // Arrange
        when(reminderConfigRepository.findByIsActiveAndAdOrgIDOrderByReminderIdDesc(
                eq(true), anyLong()))
                .thenReturn(Arrays.asList(mockConfig));
        
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        // FIX: for an INDIVIDUAL borrower the scheduler calls
        // countBySmsTypeAndReminderIdAndLoanIdAndDocStatusAndIndividualBorrowerId,
        // not the bare countBySmsTypeAndReminderIdAndLoanIdAndDocStatus.
        when(smsRepository.countBySmsTypeAndReminderIdAndLoanIdAndDocStatusAndIndividualBorrowerId(
                any(SmsTypeEnum.class), anyLong(), anyLong(), any(), anyLong()))
                .thenReturn(0L);

        // Act
        scheduler.processConfiguredSmsTypesForLoan(mockLoan, mockInstallment, LocalDateTime.now());

        // Assert
        verify(smsHandlersService).handleLoanDueReminder(
                any(MLoanApplication.class), 
                any()
        );
    }

    @Test
    @DisplayName("Should respect max reminders limit")
    void processConfiguredSmsTypesForLoan_maxRemindersExceeded() {
        // Arrange
        mockConfig.setMaxReminders(1);
        
        when(reminderConfigRepository.findByIsActiveAndAdOrgIDOrderByReminderIdDesc(
                eq(true), anyLong()))
                .thenReturn(Arrays.asList(mockConfig));

        // FIX: the max-reminders check short-circuits to "within limit" if the
        // borrower isn't eligible, which would defeat the point of this test
        // (it would pass for the wrong reason). Borrower must be eligible so the
        // count comparison below is actually what blocks the send.
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        // FIX: same wrong-method issue as above test.
        when(smsRepository.countBySmsTypeAndReminderIdAndLoanIdAndDocStatusAndIndividualBorrowerId(
                any(SmsTypeEnum.class), anyLong(), anyLong(), any(), anyLong()))
                .thenReturn(1L); // Already sent once

        // Act
        scheduler.processConfiguredSmsTypesForLoan(mockLoan, mockInstallment, LocalDateTime.now());

        // Assert
        verify(smsHandlersService, never()).handleLoanDueReminder(any(MLoanApplication.class), any());
    }

    @Test
    @DisplayName("Should process default SMS types when configured")
    void processDefaultSmsTypesForLoan_success() {
        // Arrange
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        // FIX: for a loan-level (no installment) borrower-type SMS check with an
        // INDIVIDUAL borrower, the scheduler calls
        // existsBySmsTypeAndLoanIdAndDocStatusAndIndividualBorrowerIdAndTimesTosendAfter,
        // not the bare existsBySmsTypeAndLoanIdAndTimesTosendAfter. The original
        // stub was never invoked and the lookup it was meant to represent
        // ("hasn't been sent today") still happened to default to false, but the
        // unused stub itself tripped Mockito's strict-stubbing check.
        when(smsRepository.existsBySmsTypeAndLoanIdAndDocStatusAndIndividualBorrowerIdAndTimesTosendAfter(
                any(SmsTypeEnum.class), anyLong(), any(DocStatus.class), anyLong(), any()))
                .thenReturn(false);

        // Act
        scheduler.processDefaultSmsTypesForLoan(mockLoan, mockInstallment, LocalDateTime.now());

        // Assert
        // FIX: the default-reminder path calls handleLoanDueReminder(loan, null)
        // (no associated reminder config), and anyLong() does not match a null
        // Long argument. Use any() instead.
        verify(smsHandlersService, atLeastOnce()).handleLoanDueReminder(any(MLoanApplication.class), any());
    }

    @Test
    @DisplayName("Should send guarantor reminders")
    void processDefaultSmsTypesForLoan_guarantorReminders() {
        // Arrange
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(smsRepository.existsBySmsTypeAndLoanIdAndGuarantorIdAndTimesTosendAfter(
                any(SmsTypeEnum.class), anyLong(), anyLong(), any()))
                .thenReturn(false);

        // Act
        scheduler.processDefaultSmsTypesForLoan(mockLoan, mockInstallment, LocalDateTime.now());

        // Assert
        // FIX: default guarantor reminders are also sent with a null reminderId,
        // so anyLong() never matches the real invocation; use any() instead.
        verify(smsHandlersService, atLeastOnce()).handleGuarantorPaymentReminder(
                any(MNextOfKin.class), any(MLoanApplication.class), any());
    }

    @Test
    @DisplayName("Should check if borrower is eligible before sending")
    void processDefaultSmsTypesForLoan_borrowerNotEligible() {
        // Arrange
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(false);

        // Act
        scheduler.processDefaultSmsTypesForLoan(mockLoan, mockInstallment, LocalDateTime.now());

        // Assert
        verify(smsHandlersService, never()).handleLoanDueReminder(any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should check if SMS was sent today before resending")
    void hasBeenSentToday_success() {
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        
        when(smsRepository.existsBySmsTypeAndLoanIdAndDocStatusAndIndividualBorrowerIdAndTimesTosendAfter(
                any(SmsTypeEnum.class), anyLong(), any(DocStatus.class), anyLong(), any()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.hasBeenSentToday(
                SmsTypeEnum.LOAN_OR_DEBT_DUE_REMINDER, mockLoan, null);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should check if SMS was sent today for guarantor")
    void hasBeenSentTodayForGuarantor_success() {
        // Arrange
        when(smsRepository.existsBySmsTypeAndLoanIdAndGuarantorIdAndTimesTosendAfter(
                any(SmsTypeEnum.class), anyLong(), anyLong(), any()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.hasBeenSentTodayForGuarantor(
                SmsTypeEnum.GUARANTOR_PAYMENT_REMINDER, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should calculate days remaining correctly")
    void calculateDaysRemaining_success() {
        // Arrange
        Date futureDate = new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);

        // Act
        long result = scheduler.calculateDaysRemaining(futureDate);

        // Assert
        assertEquals(7, result);
    }

    @Test
    @DisplayName("Should calculate days overdue correctly")
    void calculateDaysOverdue_success() {
        // Arrange
        Date pastDate = new Date(System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000);

        // Act
        long result = scheduler.calculateDaysOverdue(pastDate);

        // Assert
        assertEquals(5, result);
    }

    @Test
    @DisplayName("Should return 0 for null due date in days overdue")
    void calculateDaysOverdue_nullDueDate() {
        // Act
        long result = scheduler.calculateDaysOverdue(null);

        // Assert
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should check if loan is overdue correctly")
    void isLoanOverdue_success() {
        // Arrange
        mockLoan.setDueDate(new Date(System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000));

        // Act
        boolean result = scheduler.isLoanOverdue(mockLoan);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should check if installment is overdue correctly")
    void isInstallmentOverdue_success() {
        // Arrange
        mockInstallment.setPeriodEnd(new Date(System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000));

        // Act
        boolean result = scheduler.isInstallmentOverdue(mockInstallment);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should get target guarantors correctly")
    void getTargetGuarantors_success() {
        // Act
        Set<MNextOfKin> result = scheduler.getTargetGuarantors(mockLoan, true);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockGuarantor, result.iterator().next());
    }

    // ==================== SEND GENERIC SMS BY TYPE TESTS ====================

    @Test
    @DisplayName("Should send generic SMS by type - LOAN APPLICATION REGISTRATION")
    void sendGenericSmsByType_loanApplicationRegistration() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleLoanApplicationRegistration(any(MLoanApplication.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.LOAN_APPLICATION_OR_DEBT_REGISTRATION, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleLoanApplicationRegistration(any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should handle guarantor approval request generic SMS")
    void sendGenericSmsByType_guarantorApprovalRequest() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleGuarantorApprovalRequest(any(MNextOfKin.class), any(MLoanApplication.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GUARANTOR_APPROVAL_REQUEST, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGuarantorApprovalRequest(
                any(MNextOfKin.class), any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should handle unsupported SMS type gracefully")
    void sendGenericSmsByType_unsupportedType() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);

        // Act
        boolean result = scheduler.sendGenericSmsByType(null, mockLoan, mockGuarantor);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should send loan approval generic SMS")
    void sendGenericSmsByType_loanApproval() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleLoanApproval(any(MLoanApplication.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.LOAN_APPROVAL_DEBT_APPROVAL, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleLoanApproval(any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should send loan rejection generic SMS")
    void sendGenericSmsByType_loanRejection() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleLoanRejection(any(MLoanApplication.class), anyString(), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.LOAN_REJECTION_DEBT_REJECTION, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleLoanRejection(any(MLoanApplication.class), anyString(), anyLong());
    }

    @Test
    @DisplayName("Should handle installment due reminder generic SMS")
    void sendGenericSmsByType_installmentDueReminder() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(smsHandlersService.handleInstallmentDueReminder(any(MInstallments.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.INSTALLMENT_DUE_REMINDER, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleInstallmentDueReminder(any(MInstallments.class), anyLong());
    }

    @Test
    @DisplayName("Should handle installment overdue reminder generic SMS")
    void sendGenericSmsByType_installmentOverdueReminder() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(smsHandlersService.handleInstallmentOverdueReminder(any(MInstallments.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.INSTALLMENT_OVERDUE_REMINDER, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleInstallmentOverdueReminder(any(MInstallments.class), anyLong());
    }

    @Test
    @DisplayName("Should handle loan due reminder generic SMS")
    void sendGenericSmsByType_loanDueReminder() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(smsHandlersService.handleLoanDueReminder(any(MLoanApplication.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.LOAN_OR_DEBT_DUE_REMINDER, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleLoanDueReminder(any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should handle loan overdue reminder generic SMS")
    void sendGenericSmsByType_loanOverdueReminder() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(smsHandlersService.handleLoanOverdueReminder(any(MLoanApplication.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.LOAN_OR_DEBT_OVERDUE_REMINDER, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleLoanOverdueReminder(any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should handle missed repayment alert generic SMS")
    void sendGenericSmsByType_missedRepaymentAlert() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(smsHandlersService.handleMissedRepaymentAlert(any(MLoanApplication.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.MISSED_REPAYMENT_ALERT, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleMissedRepaymentAlert(any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should handle statement ready notification generic SMS")
    void sendGenericSmsByType_statementReadyNotification() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(smsHandlersService.handleStatementReadyNotification(any(MLoanApplication.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.STATEMENT_READY_NOTIFICATION, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleStatementReadyNotification(any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should handle grace period expiry alert generic SMS")
    void sendGenericSmsByType_gracePeriodExpiryAlert() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(smsHandlersService.handleGracePeriodExpiryAlert(any(MLoanApplication.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GRACE_PERIOD_EXPIRY_ALERT, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGracePeriodExpiryAlert(any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should handle installment generation notification generic SMS")
    void sendGenericSmsByType_installmentGenerationNotification() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleInstallmentGenerationNotification(any(MInstallments.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.INSTALLMENT_GENERATION_NOTIFICATION, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleInstallmentGenerationNotification(any(MInstallments.class), anyLong());
    }

    

    @Test
    @DisplayName("Should handle repayment reschedule request generic SMS")
    void sendGenericSmsByType_repaymentRescheduleRequest() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleRepaymentRescheduleRequest(any(MLoanApplication.class), any(Date.class), anyString(), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.REPAYMENT_RESCHEDULE_REQUEST, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleRepaymentRescheduleRequest(
                any(MLoanApplication.class), any(Date.class), anyString(), anyLong());
    }

    @Test
    @DisplayName("Should handle auto debit failure generic SMS")
    void sendGenericSmsByType_autoDebitFailure() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleAutoDebitFailure(
                any(MLoanApplication.class), any(BigDecimal.class), anyString(), any(Date.class), anyString(), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.AUTO_DEBIT_FAILURE, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleAutoDebitFailure(
                any(MLoanApplication.class), any(BigDecimal.class), anyString(), any(Date.class), anyString(), anyLong());
    }

    @Test
    @DisplayName("Should handle top-up loan disbursement generic SMS")
    void sendGenericSmsByType_topUpLoanDisbursement() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleTopUpLoanDisbursement(
                any(MLoanApplication.class), any(BigDecimal.class), any(BigDecimal.class), any(Date.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.TOP_UP_LOAN_DISBURSEMENT, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleTopUpLoanDisbursement(
                any(MLoanApplication.class), any(BigDecimal.class), any(BigDecimal.class), any(Date.class), anyLong());
    }

    @Test
    @DisplayName("Should handle loan closure notification generic SMS")
    void sendGenericSmsByType_loanClosureNotification() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.calculateTotalPaid(any(MLoanApplication.class)))
                .thenReturn(BigDecimal.valueOf(10000));
        
        when(smsHandlersService.handleLoanClosureNotification(any(MLoanApplication.class), any(BigDecimal.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.LOAN_CLOSURE_NOTIFICATION, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleLoanClosureNotification(
                any(MLoanApplication.class), any(BigDecimal.class), anyLong());
    }

    @Test
    @DisplayName("Should handle loan restructuring notification generic SMS")
    void sendGenericSmsByType_loanRestructuringNotification() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleLoanRestructuringNotification(
                any(MLoanApplication.class), any(BigDecimal.class), anyInt(), 
                any(BigDecimal.class), any(BigDecimal.class), any(Date.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.LOAN_RESTRUCTURING_NOTIFICATION, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleLoanRestructuringNotification(
                any(MLoanApplication.class), any(BigDecimal.class), anyInt(), 
                any(BigDecimal.class), any(BigDecimal.class), any(Date.class), anyLong());
    }

    @Test
    @DisplayName("Should handle repayment schedule update generic SMS")
    void sendGenericSmsByType_repaymentScheduleUpdate() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleRepaymentScheduleUpdate(
                any(MLoanApplication.class), any(BigDecimal.class), any(Date.class), 
                anyInt(), anyString(), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.REPAYMENT_SCHEDULE_UPDATE, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleRepaymentScheduleUpdate(
                any(MLoanApplication.class), any(BigDecimal.class), any(Date.class), 
                anyInt(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Should handle installment payment reminder generic SMS")
    void sendGenericSmsByType_installmentPaymentReminder() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(smsHandlersService.handleInstallmentPaymentReminder(any(MInstallments.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.INSTALLMENT_PAYMENT_REMINDER, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleInstallmentPaymentReminder(any(MInstallments.class), anyLong());
    }

    @Test
    @DisplayName("Should handle installment missed payment generic SMS")
    void sendGenericSmsByType_installmentMissedPayment() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(utils.isBorrowerEligible(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(smsHandlersService.handleInstallmentMissedPayment(any(MInstallments.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.INSTALLMENT_MISSED_PAYMENT, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleInstallmentMissedPayment(any(MInstallments.class), anyLong());
    }

    @Test
    @DisplayName("Should handle repayment reschedule approval generic SMS")
    void sendGenericSmsByType_repaymentRescheduleApproval() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleRepaymentRescheduleApproval(
                any(MLoanApplication.class), any(BigDecimal.class), anyInt(), 
                anyInt(), any(Date.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.REPAYMENT_RESCHEDULE_APPROVAL, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleRepaymentRescheduleApproval(
                any(MLoanApplication.class), any(BigDecimal.class), anyInt(), 
                anyInt(), any(Date.class), anyLong());
    }

    @Test
    @DisplayName("Should handle repayment reschedule rejection generic SMS")
    void sendGenericSmsByType_repaymentRescheduleRejection() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleRepaymentRescheduleRejection(
                any(MLoanApplication.class), anyString(), any(Date.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.REPAYMENT_RESCHEDULE_REJECTION, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleRepaymentRescheduleRejection(
                any(MLoanApplication.class), anyString(), any(Date.class), anyLong());
    }

    @Test
    @DisplayName("Should handle installment reschedule notification generic SMS")
    void sendGenericSmsByType_installmentRescheduleNotification() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleInstallmentRescheduleNotification(
                any(MInstallments.class), any(Date.class), any(Date.class), anyString(), 
                any(BigDecimal.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.INSTALLMENT_RESCHEDULE_NOTIFICATION, mockLoan, null);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleInstallmentRescheduleNotification(
                any(MInstallments.class), any(Date.class), any(Date.class), anyString(), 
                any(BigDecimal.class), anyLong());
    }

   

    @Test
    @DisplayName("Should handle guarantor loan overdue alert generic SMS")
    void sendGenericSmsByType_guarantorLoanOverdueAlert() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleGuarantorLoanOverdueAlert(
                any(MNextOfKin.class), any(MLoanApplication.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GUARANTOR_LOAN_OVERDUE_ALERT, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGuarantorLoanOverdueAlert(
                any(MNextOfKin.class), any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should handle guarantor installment due reminder generic SMS")
    void sendGenericSmsByType_guarantorInstallmentDueReminder() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(
                any(MLoanApplication.class), any(MNextOfKin.class), eq(true)))
                .thenReturn(null);
        
        when(smsHandlersService.handleGuarantorInstallmentDueReminder(
                any(MNextOfKin.class), any(MInstallments.class), any(BigDecimal.class), 
                any(BigDecimal.class), any(BigDecimal.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GUARANTOR_INSTALLMENT_DUE_REMINDER, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGuarantorInstallmentDueReminder(
                any(MNextOfKin.class), any(MInstallments.class), any(BigDecimal.class), 
                any(BigDecimal.class), any(BigDecimal.class), anyLong());
    }

    @Test
    @DisplayName("Should handle guarantor installment overdue alert generic SMS")
    void sendGenericSmsByType_guarantorInstallmentOverdueAlert() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(
                any(MLoanApplication.class), any(MNextOfKin.class), eq(true)))
                .thenReturn(null);
        
        when(smsHandlersService.handleGuarantorInstallmentOverdueAlert(
                any(MNextOfKin.class), any(MInstallments.class), any(BigDecimal.class), 
                any(BigDecimal.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GUARANTOR_INSTALLMENT_OVERDUE_ALERT, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGuarantorInstallmentOverdueAlert(
                any(MNextOfKin.class), any(MInstallments.class), any(BigDecimal.class), 
                any(BigDecimal.class), anyLong());
    }

    @Test
    @DisplayName("Should handle guarantor installment missed payment generic SMS")
    void sendGenericSmsByType_guarantorInstallmentMissedPayment() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(
                any(MLoanApplication.class), any(MNextOfKin.class), eq(true)))
                .thenReturn(null);
        
        when(smsHandlersService.handleGuarantorInstallmentMissedPayment(
                any(MNextOfKin.class), any(MInstallments.class), any(BigDecimal.class), 
                any(BigDecimal.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GUARANTOR_INSTALLMENT_MISSED_PAYMENT, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGuarantorInstallmentMissedPayment(
                any(MNextOfKin.class), any(MInstallments.class), any(BigDecimal.class), 
                any(BigDecimal.class), anyLong());
    }

    @Test
    @DisplayName("Should handle guarantor loan default notification generic SMS")
    void sendGenericSmsByType_guarantorLoanDefaultNotification() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(
                any(MLoanApplication.class), any(MNextOfKin.class), eq(true)))
                .thenReturn(null);
        
        when(smsHandlersService.handleGuarantorLoanDefaultNotification(
                any(MNextOfKin.class), any(MLoanApplication.class), any(BigDecimal.class), 
                anyLong(), any(BigDecimal.class), any(Date.class), any(BigDecimal.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GUARANTOR_LOAN_DEFAULT_NOTIFICATION, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGuarantorLoanDefaultNotification(
                any(MNextOfKin.class), any(MLoanApplication.class), any(BigDecimal.class), 
                anyLong(), any(BigDecimal.class), any(Date.class), any(BigDecimal.class), anyLong());
    }

    @Test
    @DisplayName("Should handle guarantor payment reminder generic SMS")
    void sendGenericSmsByType_guarantorPaymentReminder() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleGuarantorPaymentReminder(
                any(MNextOfKin.class), any(MLoanApplication.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GUARANTOR_PAYMENT_REMINDER, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGuarantorPaymentReminder(
                any(MNextOfKin.class), any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should handle guarantor missed repayment alert generic SMS")
    void sendGenericSmsByType_guarantorMissedRepaymentAlert() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleGuarantorMissedRepaymentAlert(
                any(MNextOfKin.class), any(MLoanApplication.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GUARANTOR_MISSED_REPAYMENT_ALERT, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGuarantorMissedRepaymentAlert(
                any(MNextOfKin.class), any(MLoanApplication.class), anyLong());
    }

    @Test
    @DisplayName("Should handle guarantor approval confirmation generic SMS")
    void sendGenericSmsByType_guarantorApprovalConfirmation() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(
                any(MLoanApplication.class), any(MNextOfKin.class), eq(true)))
                .thenReturn(null);
        
        when(smsHandlersService.handleGuarantorApprovalConfirmation(
                any(MNextOfKin.class), any(MLoanApplication.class), any(Date.class), 
                any(BigDecimal.class), any(BigDecimal.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GUARANTOR_APPROVAL_CONFIRMATION, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGuarantorApprovalConfirmation(
                any(MNextOfKin.class), any(MLoanApplication.class), any(Date.class), 
                any(BigDecimal.class), any(BigDecimal.class), anyLong());
    }

    @Test
    @DisplayName("Should handle guarantor approval rejection generic SMS")
    void sendGenericSmsByType_guarantorApprovalRejection() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(smsHandlersService.handleGuarantorApprovalRejection(
                any(MNextOfKin.class), any(MLoanApplication.class), any(Date.class), 
                anyString(), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GUARANTOR_APPROVAL_REJECTION, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGuarantorApprovalRejection(
                any(MNextOfKin.class), any(MLoanApplication.class), any(Date.class), 
                anyString(), anyLong());
    }

    @Test
    @DisplayName("Should handle guarantor loan assignment notification generic SMS")
    void sendGenericSmsByType_guarantorLoanAssignmentNotification() {
        // Arrange
        when(utils.getAD_Org_ID()).thenReturn(1L);
        when(reminderConfigRepository.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
                anyLong(), eq(true), any(SmsTypeEnum.class)))
                .thenReturn(mockConfig);
        
        when(utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
                any(SettingCategoriesEnum.class), anyLong()))
                .thenReturn(mockSysConfig);
        
        when(guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(
                any(MLoanApplication.class), any(MNextOfKin.class), eq(true)))
                .thenReturn(null);
        
        when(smsHandlersService.handleGuarantorLoanAssignmentNotification(
                any(MNextOfKin.class), any(MLoanApplication.class), any(Date.class), 
                any(BigDecimal.class), any(BigDecimal.class), anyLong()))
                .thenReturn(true);

        // Act
        boolean result = scheduler.sendGenericSmsByType(
                SmsTypeEnum.GUARANTOR_LOAN_ASSIGNMENT_NOTIFICATION, mockLoan, mockGuarantor);

        // Assert
        assertTrue(result);
        verify(smsHandlersService).handleGuarantorLoanAssignmentNotification(
                any(MNextOfKin.class), any(MLoanApplication.class), any(Date.class), 
                any(BigDecimal.class), any(BigDecimal.class), anyLong());
    }
}