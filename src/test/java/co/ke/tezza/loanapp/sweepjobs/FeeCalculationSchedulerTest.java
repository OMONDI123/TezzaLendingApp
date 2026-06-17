package co.ke.tezza.loanapp.sweepjobs;

import static org.junit.jupiter.api.Assertions.*;
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
import co.ke.tezza.loanapp.enums.FeeTimingEnum;
import co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.schedulers.FeeCalculationScheduler;
import co.ke.tezza.loanapp.service.ChargeMonitoringService;
import co.ke.tezza.loanapp.service.FeeCalculatorService;
import co.ke.tezza.loanapp.service.LoanStatementService;
import co.ke.tezza.loanapp.service.SmsHandlersService;
import co.ke.tezza.loanapp.util.Utils;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeeCalculationScheduler Unit Tests")
class FeeCalculationSchedulerTest {

    @Mock
    private FeeCalculatorService feeCalculatorService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private LoanStatementService loanStatementService;

    @Mock
    private SmsHandlersService smsHandlersService;

    @Mock
    private ChargeMonitoringService chargeMonitoringService;

    @Mock
    private Utils utils;

    @InjectMocks
    private FeeCalculationScheduler scheduler;

    private MLoanApplication mockLoan;
    private MInstallments mockInstallment;
    private MLoanProductConfiguration mockConfig;

    @BeforeEach
    void setUp() {
        mockLoan = new MLoanApplication();
        mockLoan.setLoanApplicationId(1L);
        mockLoan.setDocumentNo("LN/2024/001");
        mockLoan.setBalance(BigDecimal.valueOf(10000));
        mockLoan.setApprovedAmount(BigDecimal.valueOf(10000));
        mockLoan.setAdOrgID(1L);
        mockLoan.setAdClientId(1L);
        mockLoan.setExpectedDisbursementDate(new Date());
        mockLoan.setActualDisbursementDate(new Date());

        mockConfig = new MLoanProductConfiguration();
        mockConfig.setEnableServiceFee(true);
        mockConfig.setEnableDailyFee(true);
        mockConfig.setDailyFeeAmount(BigDecimal.valueOf(50));
        mockConfig.setServiceFeeAmount(BigDecimal.valueOf(500));
        mockConfig.setRepaymentScheduleType(RepaymentScheduleTypeEnum.INSTALLMENTS);
        mockLoan.setLoanProductConfiguration(mockConfig);

        mockInstallment = new MInstallments();
        mockInstallment.setInstallmentId(1L);
        mockInstallment.setBalance(BigDecimal.valueOf(10000));
        mockInstallment.setLoan(mockLoan);
    }

    @Test
    @DisplayName("Should process post-disbursement service fees successfully")
    @Transactional
    void processPostDisbursementServiceFees_success() throws Exception {
        // Arrange
        Date now = new Date();
        List<MLoanApplication> loans = Arrays.asList(mockLoan);
        
        when(loanApplicationRepository.findByApprovalStageAndIsActiveAndExpectedDisbursementDateBefore(
                any(ApprovalStage.class), eq(true), any(Date.class)))
                .thenReturn(loans);
        
        when(feeCalculatorService.shouldChargeServiceFeeNow(any(MLoanApplication.class), eq(FeeTimingEnum.POST_DISBURSEMENT)))
                .thenReturn(true);
        when(feeCalculatorService.calculateServiceFee(any(MLoanApplication.class)))
                .thenReturn(BigDecimal.valueOf(500));
        when(chargeMonitoringService.continueCharging(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);

        // Act
        scheduler.processPostDisbursementServiceFees();

        // Assert
        verify(loanApplicationRepository).save(any(MLoanApplication.class));
        verify(installmentRepository).save(any(MInstallments.class));
        verify(loanStatementService).recordServiceFee(anyLong(), isNull(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should skip service fee when not configured")
    @Transactional
    void processPostDisbursementServiceFees_skipWhenNotConfigured() throws Exception {
        // Arrange
        List<MLoanApplication> loans = Arrays.asList(mockLoan);
        
        when(loanApplicationRepository.findByApprovalStageAndIsActiveAndExpectedDisbursementDateBefore(
                any(ApprovalStage.class), eq(true), any(Date.class)))
                .thenReturn(loans);
        
        when(feeCalculatorService.shouldChargeServiceFeeNow(any(MLoanApplication.class), eq(FeeTimingEnum.POST_DISBURSEMENT)))
                .thenReturn(false);

        // Act
        scheduler.processPostDisbursementServiceFees();

        // Assert
        verify(feeCalculatorService, never()).calculateServiceFee(any(MLoanApplication.class));
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }
    

    @Test
    @DisplayName("Should handle no active loans for service fees")
    @Transactional
    void processPostDisbursementServiceFees_noActiveLoans() throws Exception {
        // Arrange
        when(loanApplicationRepository.findByApprovalStageAndIsActiveAndExpectedDisbursementDateBefore(
                any(ApprovalStage.class), eq(true), any(Date.class)))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.processPostDisbursementServiceFees();

        // Assert
        verify(feeCalculatorService, never()).calculateServiceFee(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should process daily fees successfully")
    @Transactional
    void processDailyFees_success() throws Exception {
        // Arrange
        List<MLoanApplication> loans = Arrays.asList(mockLoan);
        Date now = new Date();
        
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(loans);
        
        when(feeCalculatorService.calculateIncrementalDailyFee(any(MLoanApplication.class), any(Date.class), any(Date.class)))
                .thenReturn(BigDecimal.valueOf(50));
        when(chargeMonitoringService.continueCharging(any(MLoanApplication.class)))
                .thenReturn(true);
        
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);

        // Act
        scheduler.processDailyFees();

        // Assert
        verify(loanApplicationRepository, atLeastOnce()).save(any(MLoanApplication.class));
        verify(installmentRepository, atLeastOnce()).save(any(MInstallments.class));
        verify(loanStatementService).recordDailyFee(anyLong(), isNull(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should skip daily fee when amount is zero but still update last calculation date")
    @Transactional
    void processDailyFees_skipZeroAmount() throws Exception {
        // Arrange
        // Set necessary fields for the daily fee calculation to proceed
        mockLoan.setExpectedDisbursementDate(new Date());
        mockLoan.setActualDisbursementDate(new Date());
        mockLoan.setLastDailyFeeCalculationDate(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)); // 1 day ago
        
        List<MLoanApplication> loans = Arrays.asList(mockLoan);
        
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(loans);
        
        // Mock the fee calculator to return zero
        when(feeCalculatorService.calculateIncrementalDailyFee(any(MLoanApplication.class), any(Date.class), any(Date.class)))
                .thenReturn(BigDecimal.ZERO);

        // Act
        scheduler.processDailyFees();

        // Assert
        // Verify the fee calculator was called
        verify(feeCalculatorService).calculateIncrementalDailyFee(any(MLoanApplication.class), any(Date.class), any(Date.class));
        
        // Verify that save was called to update the last daily fee date (even though fee was zero)
        verify(loanApplicationRepository, atLeastOnce()).save(any(MLoanApplication.class));
        
        // Verify no fee was recorded
        verify(loanStatementService, never()).recordDailyFee(anyLong(), isNull(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should skip daily fee when loan has no disbursement date")
    @Transactional
    void processDailyFees_skipNoDisbursementDate() throws Exception {
        // Arrange
        mockLoan.setExpectedDisbursementDate(null);
        mockLoan.setActualDisbursementDate(null);
        List<MLoanApplication> loans = Arrays.asList(mockLoan);
        
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(loans);

        // Act
        scheduler.processDailyFees();

        // Assert
        verify(feeCalculatorService, never()).calculateIncrementalDailyFee(any(MLoanApplication.class), any(Date.class), any(Date.class));
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should process origination service fee manually")
    @Transactional
    void processOriginationServiceFee_success() throws Exception {
        // Arrange
        when(loanApplicationRepository.findById(1L))
                .thenReturn(java.util.Optional.of(mockLoan));
        
        when(feeCalculatorService.shouldChargeServiceFeeNow(any(MLoanApplication.class), eq(FeeTimingEnum.ORIGINATION)))
                .thenReturn(true);
        when(feeCalculatorService.calculateServiceFee(any(MLoanApplication.class)))
                .thenReturn(BigDecimal.valueOf(500));
        when(chargeMonitoringService.continueCharging(any(MLoanApplication.class)))
                .thenReturn(true);

        // Act
        scheduler.processOriginationServiceFee(1L);

        // Assert
        verify(loanApplicationRepository).save(any(MLoanApplication.class));
        verify(loanStatementService).recordServiceFee(anyLong(), isNull(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should throw exception when loan not found for origination fee")
    @Transactional
    void processOriginationServiceFee_loanNotFound() throws Exception {
        // Arrange
        Long nonExistentLoanId = 999L;
        when(loanApplicationRepository.findById(nonExistentLoanId))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert - expect RuntimeException with wrapped message
        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class, 
                () -> scheduler.processOriginationServiceFee(nonExistentLoanId)
        );
        
        // Verify the exception message is the wrapped one
        assertEquals("Failed to process origination service fee", exception.getMessage());
        
        // Verify the cause is the original exception with the correct message
        assertNotNull(exception.getCause());
        assertEquals("Loan not found: " + nonExistentLoanId, exception.getCause().getMessage());
        
        // Verify that the repository method was called with the correct ID
        verify(loanApplicationRepository).findById(nonExistentLoanId);
        
        // Verify no other interactions occurred
        verifyNoInteractions(feeCalculatorService);
        verifyNoInteractions(loanStatementService);
    }

    @Test
    @DisplayName("Should handle service fee processing error gracefully")
    @Transactional
    void processPostDisbursementServiceFees_serviceError() throws Exception {
        // Arrange
        List<MLoanApplication> loans = Arrays.asList(mockLoan);
        
        when(loanApplicationRepository.findByApprovalStageAndIsActiveAndExpectedDisbursementDateBefore(
                any(ApprovalStage.class), eq(true), any(Date.class)))
                .thenReturn(loans);
        
        when(feeCalculatorService.shouldChargeServiceFeeNow(any(MLoanApplication.class), eq(FeeTimingEnum.POST_DISBURSEMENT)))
                .thenThrow(new RuntimeException("Service fee calculation error"));

        // Act
        scheduler.processPostDisbursementServiceFees();

        // Assert - should not throw exception, just log error
        verify(feeCalculatorService).shouldChargeServiceFeeNow(any(MLoanApplication.class), eq(FeeTimingEnum.POST_DISBURSEMENT));
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }

    @Test
    @DisplayName("Should handle daily fee processing error gracefully")
    @Transactional
    void processDailyFees_serviceError() throws Exception {
        // Arrange
        mockLoan.setExpectedDisbursementDate(new Date());
        mockLoan.setLastDailyFeeCalculationDate(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        
        List<MLoanApplication> loans = Arrays.asList(mockLoan);
        
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(loans);
        
        when(feeCalculatorService.calculateIncrementalDailyFee(any(MLoanApplication.class), any(Date.class), any(Date.class)))
                .thenThrow(new RuntimeException("Daily fee calculation error"));

        // Act
        scheduler.processDailyFees();

        // Assert - should not throw exception, just log error
        verify(feeCalculatorService).calculateIncrementalDailyFee(any(MLoanApplication.class), any(Date.class), any(Date.class));
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
    }
}