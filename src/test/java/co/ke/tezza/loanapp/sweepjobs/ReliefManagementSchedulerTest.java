package co.ke.tezza.loanapp.sweepjobs;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.schedulers.ReliefManagementScheduler;
import co.ke.tezza.loanapp.service.ChargeMonitoringService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReliefManagementScheduler Unit Tests")
class ReliefManagementSchedulerTest {

    @Mock
    private ChargeMonitoringService chargeMonitoringService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @InjectMocks
    private ReliefManagementScheduler scheduler;

    private MLoanApplication mockLoan;
    private MLoanProductConfiguration mockConfig;

    @BeforeEach
    void setUp() {
        mockLoan = new MLoanApplication();
        mockLoan.setLoanApplicationId(1L);
        mockLoan.setDocumentNo("LN/2024/001");
        mockLoan.setBalance(BigDecimal.valueOf(10000));
        mockLoan.setApprovedAmount(BigDecimal.valueOf(10000));
        mockLoan.setInterestsEarned(BigDecimal.valueOf(900));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(300));

        mockConfig = new MLoanProductConfiguration();
        mockConfig.setAllowOveralChargesCap(true);
        mockConfig.setAllowedOveralChargesCapPercentage(BigDecimal.valueOf(10));
        mockLoan.setLoanProductConfiguration(mockConfig);
    }

    @Test
    @DisplayName("Should monitor interest and penalty caps for active loans")
    void monitorInterestsAndPenaltyCaps_success() {
        // Arrange
        List<MLoanApplication> loans = Arrays.asList(mockLoan);
        
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(loans);

        // Act
        scheduler.monitorInterestsAndPenaltyCaps();

        // Assert
        verify(chargeMonitoringService).exemptAmountOverChargesAboveAllowedPercentageCap(mockLoan);
    }

    @Test
    @DisplayName("Should skip when no active loans found")
    void monitorInterestsAndPenaltyCaps_noActiveLoans() {
        // Arrange
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(Collections.emptyList());

        // Act
        scheduler.monitorInterestsAndPenaltyCaps();

        // Assert
        verify(chargeMonitoringService, never()).exemptAmountOverChargesAboveAllowedPercentageCap(any());
    }

    @Test
    @DisplayName("Should handle null loans list")
    void monitorInterestsAndPenaltyCaps_nullLoans() {
        // Arrange
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(null);

        // Act
        scheduler.monitorInterestsAndPenaltyCaps();

        // Assert
        verify(chargeMonitoringService, never()).exemptAmountOverChargesAboveAllowedPercentageCap(any());
    }

    @Test
    @DisplayName("Should process each loan in the list")
    void monitorInterestsAndPenaltyCaps_multipleLoans() {
        // Arrange
        MLoanApplication mockLoan2 = new MLoanApplication();
        mockLoan2.setLoanApplicationId(2L);
        mockLoan2.setDocumentNo("LN/2024/002");
        mockLoan2.setBalance(BigDecimal.valueOf(20000));
        mockLoan2.setApprovedAmount(BigDecimal.valueOf(20000));
        mockLoan2.setInterestsEarned(BigDecimal.valueOf(1800));
        mockLoan2.setPenaltyEarned(BigDecimal.valueOf(600));
        mockLoan2.setLoanProductConfiguration(mockConfig);

        List<MLoanApplication> loans = Arrays.asList(mockLoan, mockLoan2);
        
        when(loanApplicationRepository.findByBalanceGreaterThanAndApprovalStageAndIsActive(
                any(BigDecimal.class), any(ApprovalStage.class), eq(true)))
                .thenReturn(loans);

        // Act
        scheduler.monitorInterestsAndPenaltyCaps();

        // Assert
        verify(chargeMonitoringService, times(2)).exemptAmountOverChargesAboveAllowedPercentageCap(any(MLoanApplication.class));
    }
}
