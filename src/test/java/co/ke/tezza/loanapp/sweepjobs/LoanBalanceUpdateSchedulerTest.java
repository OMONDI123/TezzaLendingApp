package co.ke.tezza.loanapp.sweepjobs;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DebtTypeEnum;
import co.ke.tezza.loanapp.enums.InterestCalculationMethodEnum;
import co.ke.tezza.loanapp.enums.InterestFrequencyEnum;
import co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.schedulers.LoanBalanceUpdateScheduler;
import co.ke.tezza.loanapp.service.ChargeMonitoringService;
import co.ke.tezza.loanapp.service.LoanInterestCalculatorService;
import co.ke.tezza.loanapp.service.LoanStatementService;
import co.ke.tezza.loanapp.service.SmsHandlersService;
import co.ke.tezza.loanapp.util.Utils;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanBalanceUpdateScheduler Unit Tests")
class LoanBalanceUpdateSchedulerTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private LoanInterestCalculatorService loanInterestCalculatorService;

    @Mock
    private Utils utils;

    @Mock
    private LoanStatementService loanStatementService;

    @Mock
    private SmsHandlersService smsHandlersService;

    @Mock
    private ChargeMonitoringService chargeMonitoringService;

    @InjectMocks
    private LoanBalanceUpdateScheduler scheduler;

    private MLoanApplication mockLoan;
    private MInstallments mockInstallment;
    private MLoanProductConfiguration mockConfig;

    @BeforeEach
    void setUp() {
        Date now = new Date();
        
        mockLoan = new MLoanApplication();
        mockLoan.setLoanApplicationId(1L);
        mockLoan.setDocumentNo("LN/2024/001");
        mockLoan.setBalance(BigDecimal.valueOf(10000));
        mockLoan.setApprovedAmount(BigDecimal.valueOf(10000));
        mockLoan.setAppliedAmount(BigDecimal.valueOf(10000));
        mockLoan.setAdOrgID(1L);
        mockLoan.setAdClientId(1L);
        mockLoan.setExpectedDisbursementDate(new Date(now.getTime() - 5 * 24 * 60 * 60 * 1000)); // 5 days ago
        mockLoan.setDueDate(new Date(now.getTime() + 25 * 24 * 60 * 60 * 1000)); // 25 days from now
        mockLoan.setGraceperiod(0);
        mockLoan.setGracePeriodToFirstInstallment(0);
        mockLoan.setTermInDays(30);
        mockLoan.setInterestsEarned(BigDecimal.ZERO);
        mockLoan.setLastInterestCalculationDate(new Date(now.getTime() - 1 * 24 * 60 * 60 * 1000)); // 1 day ago

        mockConfig = new MLoanProductConfiguration();
        mockConfig.setInterestCalculationMethod(InterestCalculationMethodEnum.SIMPLE_INTEREST);
        mockConfig.setInterestFrequency(InterestFrequencyEnum.DAILY);
        mockConfig.setRepaymentScheduleType(RepaymentScheduleTypeEnum.ONE_TIME);
        mockConfig.setIsDebtProduct(false);
        mockConfig.setDebtType(null);
        mockConfig.setCycle1DurationDays(30);
        mockConfig.setGracePeriodDays(0);
        mockConfig.setGracePeriodBeforeFirstInstallment(0);
        mockLoan.setLoanProductConfiguration(mockConfig);

        mockInstallment = new MInstallments();
        mockInstallment.setInstallmentId(1L);
        mockInstallment.setBalance(BigDecimal.valueOf(10000));
        mockInstallment.setLoan(mockLoan);
    }

    @Test
    @DisplayName("Should process active loans interest accrual successfully")
    @Transactional
    void processAllActiveLoans_success() throws Exception {
        // Arrange
        List<MLoanApplication> loans = Arrays.asList(mockLoan);
        
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(loans);
        
        when(loanInterestCalculatorService.calculateIncrementalInterest(
                any(MLoanApplication.class), any(Date.class), any(Date.class)))
                .thenReturn(BigDecimal.valueOf(100));
        when(chargeMonitoringService.continueCharging(any(MLoanApplication.class)))
                .thenReturn(true);
        

        // Act
        scheduler.processAllActiveLoans();

        // Assert
        verify(loanApplicationRepository, atLeastOnce()).save(any(MLoanApplication.class));
        verify(loanStatementService).recordInterest(anyLong(), isNull(), any(BigDecimal.class));
        verify(loanInterestCalculatorService).calculateIncrementalInterest(
                any(MLoanApplication.class), any(Date.class), any(Date.class));
        // Verify installment repository was not called
        verify(installmentRepository, never()).findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                anyBoolean(), any(BigDecimal.class), any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should skip flat rate debt products")
    @Transactional
    void processAllActiveLoans_skipFlatRateDebt() throws Exception {
        // Arrange
        mockConfig.setIsDebtProduct(true);
        mockConfig.setDebtType(DebtTypeEnum.FLAT_RATE);
        List<MLoanApplication> loans = Arrays.asList(mockLoan);
        
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(loans);

        // Act
        scheduler.processAllActiveLoans();

        // Assert
        verify(loanInterestCalculatorService, never()).calculateIncrementalInterest(
                any(MLoanApplication.class), any(Date.class), any(Date.class));
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should skip loans within grace period")
    @Transactional
    void processAllActiveLoans_skipWithinGracePeriod() throws Exception {
        // Arrange
        Date now = new Date();
        mockLoan.setExpectedDisbursementDate(now);
        mockLoan.setGraceperiod(10); // 10 days grace period
        mockLoan.setLastInterestCalculationDate(null);
        
        List<MLoanApplication> loans = Arrays.asList(mockLoan);
        
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(loans);

        // Act
        scheduler.processAllActiveLoans();

        // Assert
        verify(loanInterestCalculatorService, never()).calculateIncrementalInterest(
                any(MLoanApplication.class), any(Date.class), any(Date.class));
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should handle no active loans")
    @Transactional
    void processAllActiveLoans_noActiveLoans() throws Exception {
        // Arrange
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.processAllActiveLoans();

        // Assert
        verify(loanInterestCalculatorService, never()).calculateIncrementalInterest(
                any(MLoanApplication.class), any(Date.class), any(Date.class));
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should process loan manually")
    @Transactional
    void processLoanManually_success() throws Exception {
        // Arrange
        when(loanApplicationRepository.findById(1L))
                .thenReturn(java.util.Optional.of(mockLoan));
        
        when(loanInterestCalculatorService.calculateIncrementalInterest(
                any(MLoanApplication.class), any(Date.class), any(Date.class)))
                .thenReturn(BigDecimal.valueOf(100));
        when(chargeMonitoringService.continueCharging(any(MLoanApplication.class)))
                .thenReturn(true);

        // Act
        scheduler.processLoanManually(1L);

        // Assert
        verify(loanApplicationRepository).save(any(MLoanApplication.class));
        verify(loanStatementService).recordInterest(anyLong(), isNull(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should handle loan not found for manual processing")
    @Transactional
    void processLoanManually_loanNotFound() throws Exception {
        // Arrange
        when(loanApplicationRepository.findById(999L))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            scheduler.processLoanManually(999L);
        });
    }

    @Test
    @DisplayName("Should force interest recalculation")
    @Transactional
    void forceInterestRecalculation_success() throws Exception {
        // Arrange
        when(loanApplicationRepository.findById(1L))
                .thenReturn(java.util.Optional.of(mockLoan));
        
        when(loanInterestCalculatorService.calculateIncrementalInterest(
                any(MLoanApplication.class), any(Date.class), any(Date.class)))
                .thenReturn(BigDecimal.valueOf(100));
        when(chargeMonitoringService.continueCharging(any(MLoanApplication.class)))
                .thenReturn(true);

        // Act
        scheduler.forceInterestRecalculation(1L);

        // Assert
        verify(loanApplicationRepository, atLeastOnce()).save(any(MLoanApplication.class));
        verify(loanStatementService).recordInterest(anyLong(), isNull(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should process loan batch successfully")
    @Transactional
    void processLoanBatch_success() throws Exception {
        // Arrange
        List<Long> loanIds = Arrays.asList(1L, 2L, 3L);
        
        when(loanApplicationRepository.findById(1L))
                .thenReturn(java.util.Optional.of(mockLoan));
        
        MLoanApplication mockLoan2 = new MLoanApplication();
        mockLoan2.setLoanApplicationId(2L);
        mockLoan2.setDocumentNo("LN/2024/002");
        mockLoan2.setBalance(BigDecimal.valueOf(20000));
        mockLoan2.setApprovedAmount(BigDecimal.valueOf(20000));
        mockLoan2.setAdOrgID(1L);
        mockLoan2.setAdClientId(1L);
        mockLoan2.setExpectedDisbursementDate(new Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000));
        mockLoan2.setDueDate(new Date(System.currentTimeMillis() + 25 * 24 * 60 * 60 * 1000));
        mockLoan2.setGraceperiod(0);
        mockLoan2.setTermInDays(30);
        mockLoan2.setInterestsEarned(BigDecimal.ZERO);
        mockLoan2.setLastInterestCalculationDate(new Date(System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000));
        mockLoan2.setLoanProductConfiguration(mockConfig);
        
        when(loanApplicationRepository.findById(2L))
                .thenReturn(java.util.Optional.of(mockLoan2));
        when(loanApplicationRepository.findById(3L))
                .thenReturn(java.util.Optional.empty());
        
        when(loanInterestCalculatorService.calculateIncrementalInterest(
                any(MLoanApplication.class), any(Date.class), any(Date.class)))
                .thenReturn(BigDecimal.valueOf(100));
        when(chargeMonitoringService.continueCharging(any(MLoanApplication.class)))
                .thenReturn(true);

        // Act
        scheduler.processLoanBatch(loanIds);

        // Assert
        verify(loanApplicationRepository, atLeast(2)).save(any(MLoanApplication.class));
        verify(loanStatementService, atLeast(2)).recordInterest(anyLong(), isNull(), any(BigDecimal.class));
    }
}