package co.ke.tezza.loanapp.sweepjobs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.service.ChargeMonitoringService;
import co.ke.tezza.loanapp.service.LoanStatementService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChargeMonitoringService Unit Tests")
class ChargeMonitoringServiceTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanStatementService loanStatementService;

    @Mock
    private InstallmentRepository installmentRepository;

    @InjectMocks
    private ChargeMonitoringService service;

    private MLoanApplication mockLoan;
    private MLoanProductConfiguration mockConfig;
    private MInstallments mockInstallment;

    @BeforeEach
    void setUp() {
        mockLoan = new MLoanApplication();
        mockLoan.setLoanApplicationId(1L);
        mockLoan.setDocumentNo("LN/2024/001");
        mockLoan.setApprovedAmount(BigDecimal.valueOf(10000));
        mockLoan.setBalance(BigDecimal.valueOf(10000));
        mockLoan.setInterestsEarned(BigDecimal.valueOf(500));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(200));
        mockLoan.setAdOrgID(1L);
        mockLoan.setAdClientId(1L);

        mockConfig = new MLoanProductConfiguration();
        mockConfig.setAllowOveralChargesCap(true);
        mockConfig.setAllowedOveralChargesCapPercentage(BigDecimal.valueOf(10));
        mockLoan.setLoanProductConfiguration(mockConfig);

        mockInstallment = new MInstallments();
        mockInstallment.setInstallmentId(1L);
        mockInstallment.setBalance(BigDecimal.valueOf(10000));
        mockInstallment.setLoan(mockLoan);
        mockInstallment.setExemptedAmount(BigDecimal.ZERO);
        mockInstallment.setExemptedInterests(BigDecimal.ZERO);
        mockInstallment.setExemptedPenalties(BigDecimal.ZERO);
        mockInstallment.setInterestEarned(BigDecimal.valueOf(900));
        mockInstallment.setPenaltyEarned(BigDecimal.valueOf(300));
        mockInstallment.setCummulatedAmount(BigDecimal.ZERO);
        mockInstallment.setServiceFeeCharged(BigDecimal.ZERO);
        mockInstallment.setDailyFeeCharged(BigDecimal.ZERO);
        mockInstallment.setServiceFeeWaived(BigDecimal.ZERO);
        mockInstallment.setDailyFeeWaived(BigDecimal.ZERO);
        mockInstallment.setDocumentNo("INST/2024/000001");
    }

    // ========================================================================
    // Basic Calculation Tests
    // ========================================================================

    @Test
    @DisplayName("Should return total charges correctly")
    void getTotalCharges_success() {
        // Act
        BigDecimal result = service.getTotalCharges(mockLoan);

        // Assert
        assertEquals(BigDecimal.valueOf(700), result);
    }

    @Test
    @DisplayName("Should return zero total charges for null loan")
    void getTotalCharges_nullLoan() {
        // Act
        BigDecimal result = service.getTotalCharges(null);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Should calculate total percentage charged correctly")
    void getTotalPercentageCharged_success() {
        // Act
        BigDecimal result = service.getTotalPercentageCharged(mockLoan);

        // Assert
        assertEquals(BigDecimal.valueOf(7.00).setScale(2, RoundingMode.HALF_UP), 
                result.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Should return zero percentage when principal is zero")
    void getTotalPercentageCharged_zeroPrincipal() {
        // Arrange
        mockLoan.setApprovedAmount(BigDecimal.ZERO);

        // Act
        BigDecimal result = service.getTotalPercentageCharged(mockLoan);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    // ========================================================================
    // Charge Continuation Tests
    // ========================================================================

    @Test
    @DisplayName("Should continue charging when caps are disabled")
    void continueCharging_capsDisabled() {
        // Arrange
        mockConfig.setAllowOveralChargesCap(false);

        // Act
        boolean result = service.continueCharging(mockLoan);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should continue charging when percentage is below cap")
    void continueCharging_belowCap() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.valueOf(500));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(200));
        // 7% < 10% cap

        // Act
        boolean result = service.continueCharging(mockLoan);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should stop charging when percentage exceeds cap")
    void continueCharging_exceedsCap() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.valueOf(900));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(300));
        // 12% > 10% cap

        // Act
        boolean result = service.continueCharging(mockLoan);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle null loan product configuration")
    void continueCharging_nullConfig() {
        // Arrange
        mockLoan.setLoanProductConfiguration(null);

        // Act
        boolean result = service.continueCharging(mockLoan);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle null allowed percentage")
    void continueCharging_nullAllowedPercentage() {
        // Arrange
        mockConfig.setAllowedOveralChargesCapPercentage(null);

        // Act
        boolean result = service.continueCharging(mockLoan);

        // Assert
        assertTrue(result);
    }

    // ========================================================================
    // Cap Exceedance Tests
    // ========================================================================

    @Test
    @DisplayName("Should detect charges exceed cap")
    void chargesExceedAllowedPercentageCap_true() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.valueOf(900));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(300));
        // 12% > 10% cap

        // Act
        boolean result = service.chargesExceedAllowedPercentageCap(mockLoan);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should detect charges not exceed cap")
    void chargesExceedAllowedPercentageCap_false() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.valueOf(500));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(200));
        // 7% < 10% cap

        // Act
        boolean result = service.chargesExceedAllowedPercentageCap(mockLoan);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when caps are disabled for exceed check")
    void chargesExceedAllowedPercentageCap_capsDisabled() {
        // Arrange
        mockConfig.setAllowOveralChargesCap(false);
        mockLoan.setInterestsEarned(BigDecimal.valueOf(900));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(300));

        // Act
        boolean result = service.chargesExceedAllowedPercentageCap(mockLoan);

        // Assert
        assertFalse(result);
    }

    // ========================================================================
    // Exemption Tests
    // ========================================================================

    @Test
    @DisplayName("Should exempt excess amount above cap - verifies loan-level interest waiver")
    void exemptAmountOverChargesAboveAllowedPercentageCap_loanInterestWaiver() {
        // Arrange
        setupExemptionTestData();

        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);

        when(loanApplicationRepository.save(any(MLoanApplication.class)))
                .thenReturn(mockLoan);

        when(installmentRepository.save(any(MInstallments.class)))
                .thenReturn(mockInstallment);

        // Act
        service.exemptAmountOverChargesAboveAllowedPercentageCap(mockLoan);

        // Assert - Verify loan-level interest waiver was called with correct arguments
        verify(loanStatementService).recordInterestWaiver(
                eq(1L), 
                isNull(), 
                any(BigDecimal.class), 
                anyString(), 
                anyString()
        );
    }

    @Test
    @DisplayName("Should exempt excess amount above cap - verifies loan-level penalty waiver")
    void exemptAmountOverChargesAboveAllowedPercentageCap_loanPenaltyWaiver() {
        // Arrange
        setupExemptionTestData();

        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);

        when(loanApplicationRepository.save(any(MLoanApplication.class)))
                .thenReturn(mockLoan);

        when(installmentRepository.save(any(MInstallments.class)))
                .thenReturn(mockInstallment);

        // Act
        service.exemptAmountOverChargesAboveAllowedPercentageCap(mockLoan);

        // Assert - Verify loan-level penalty waiver was called with correct arguments
        verify(loanStatementService).recordPenaltyWaiver(
                eq(1L), 
                isNull(), 
                any(BigDecimal.class), 
                anyString(), 
                anyString()
        );
    }
    @Test
    @DisplayName("Should exempt excess amount above cap - verifies installment-level interest waiver")
    void exemptAmountOverChargesAboveAllowedPercentageCap_installmentInterestWaiver() {
        // Arrange
        setupExemptionTestData();

        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);

        when(loanApplicationRepository.save(any(MLoanApplication.class)))
                .thenReturn(mockLoan);

        when(installmentRepository.save(any(MInstallments.class)))
                .thenReturn(mockInstallment);

        // Act
        service.exemptAmountOverChargesAboveAllowedPercentageCap(mockLoan);

        // Assert - Verify installment-level interest waiver was called with correct arguments
        verify(loanStatementService).recordInterestWaiver(
                isNull(), 
                eq(1L), 
                any(BigDecimal.class), 
                anyString(), 
                anyString()
        );
    }

    @Test
    @DisplayName("Should exempt excess amount above cap - verifies installment-level penalty waiver")
    void exemptAmountOverChargesAboveAllowedPercentageCap_installmentPenaltyWaiver() {
        // Arrange
        setupExemptionTestData();

        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);

        when(loanApplicationRepository.save(any(MLoanApplication.class)))
                .thenReturn(mockLoan);

        when(installmentRepository.save(any(MInstallments.class)))
                .thenReturn(mockInstallment);

        // Act
        service.exemptAmountOverChargesAboveAllowedPercentageCap(mockLoan);

        // Assert - Verify installment-level penalty waiver was called with correct arguments
        verify(loanStatementService).recordPenaltyWaiver(
                isNull(), 
                eq(1L), 
                any(BigDecimal.class), 
                anyString(), 
                anyString()
        );
    }

    @Test
    @DisplayName("Should exempt excess amount above cap - verifies all four waivers are called")
    void exemptAmountOverChargesAboveAllowedPercentageCap_allWaivers() {
        // Arrange
        setupExemptionTestData();

        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(mockInstallment);

        when(loanApplicationRepository.save(any(MLoanApplication.class)))
                .thenReturn(mockLoan);

        when(installmentRepository.save(any(MInstallments.class)))
                .thenReturn(mockInstallment);

        // Act
        service.exemptAmountOverChargesAboveAllowedPercentageCap(mockLoan);

        // Assert - Verify total number of calls (4 total: 2 interest + 2 penalty)
        verify(loanStatementService, times(2)).recordInterestWaiver(
                any(), any(), any(BigDecimal.class), anyString(), anyString()
        );
        verify(loanStatementService, times(2)).recordPenaltyWaiver(
                any(), any(), any(BigDecimal.class), anyString(), anyString()
        );
        
        // Verify no more interactions
        verifyNoMoreInteractions(loanStatementService);
    }

    @Test
    @DisplayName("Should not exempt when charges are below cap")
    void exemptAmountOverChargesAboveAllowedPercentageCap_belowCap() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.valueOf(500));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(200));
        // 7% < 10% cap

        // Act
        service.exemptAmountOverChargesAboveAllowedPercentageCap(mockLoan);

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
        verify(installmentRepository, never()).save(any(MInstallments.class));
        verify(loanStatementService, never()).recordInterestWaiver(
                anyLong(), anyLong(), any(BigDecimal.class), anyString(), anyString()
        );
        verify(loanStatementService, never()).recordPenaltyWaiver(
                anyLong(), anyLong(), any(BigDecimal.class), anyString(), anyString()
        );
    }

    @Test
    @DisplayName("Should handle loan with no installments gracefully")
    void exemptAmountOverChargesAboveAllowedPercentageCap_noInstallments() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.valueOf(900));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(300));
        mockLoan.setExemptedAmount(BigDecimal.ZERO);
        mockLoan.setExemptedInterests(BigDecimal.ZERO);
        mockLoan.setExemptedPenalties(BigDecimal.ZERO);
        
        // Mock installment repository to return null (no installments)
        when(installmentRepository.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(
                eq(true), any(BigDecimal.class), any(MLoanApplication.class)))
                .thenReturn(null);
        
        when(loanApplicationRepository.save(any(MLoanApplication.class)))
                .thenReturn(mockLoan);

        // Act
        service.exemptAmountOverChargesAboveAllowedPercentageCap(mockLoan);

        // Assert
        verify(loanApplicationRepository).save(any(MLoanApplication.class));
        verify(installmentRepository, never()).save(any(MInstallments.class));
        
        // Only loan-level waivers should be recorded (1 interest + 1 penalty)
        verify(loanStatementService, times(1)).recordInterestWaiver(
                any(), any(), any(BigDecimal.class), anyString(), anyString()
        );
        verify(loanStatementService, times(1)).recordPenaltyWaiver(
                any(), any(), any(BigDecimal.class), anyString(), anyString()
        );
        
        // Verify no more interactions
        verifyNoMoreInteractions(loanStatementService);
    }

    @Test
    @DisplayName("Should handle null loan in exemption")
    void exemptAmountOverChargesAboveAllowedPercentageCap_nullLoan() {
        // Act
        service.exemptAmountOverChargesAboveAllowedPercentageCap(null);

        // Assert
        verify(loanApplicationRepository, never()).save(any(MLoanApplication.class));
        verify(installmentRepository, never()).save(any(MInstallments.class));
        verify(loanStatementService, never()).recordInterestWaiver(
                anyLong(), anyLong(), any(BigDecimal.class), anyString(), anyString()
        );
    }

    // ========================================================================
    // Calculation Helper Tests
    // ========================================================================

    @Test
    @DisplayName("Should return zero excess when charges are below cap")
    void calculateExcessAmount_belowCap() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.valueOf(500));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(200));

        // Act
        BigDecimal result = service.calculateExcessAmount(mockLoan);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Should calculate excess amount correctly")
    void calculateExcessAmount_success() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.valueOf(900));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(300));

        // Act
        BigDecimal result = service.calculateExcessAmount(mockLoan);

        // Assert
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should calculate required exemption correctly")
    void calculateRequiredExemption_success() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.valueOf(900));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(300));
        // 12% > 10% cap, excess should be ~200

        // Act
        BigDecimal result = service.calculateRequiredExemption(mockLoan);

        // Assert
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should return zero required exemption when charges below cap")
    void calculateRequiredExemption_belowCap() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.valueOf(500));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(200));

        // Act
        BigDecimal result = service.calculateRequiredExemption(mockLoan);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    // ========================================================================
    // Eligibility Tests
    // ========================================================================

    @Test
    @DisplayName("Should determine loan eligibility for monitoring")
    void isLoanEligibleForChargeMonitoring_success() {
        // Act
        boolean result = service.isLoanEligibleForChargeMonitoring(mockLoan);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for ineligible loan")
    void isLoanEligibleForChargeMonitoring_ineligible() {
        // Arrange
        mockConfig.setAllowOveralChargesCap(false);

        // Act
        boolean result = service.isLoanEligibleForChargeMonitoring(mockLoan);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for null loan in eligibility check")
    void isLoanEligibleForChargeMonitoring_nullLoan() {
        // Act
        boolean result = service.isLoanEligibleForChargeMonitoring(null);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for loan with zero principal")
    void isLoanEligibleForChargeMonitoring_zeroPrincipal() {
        // Arrange
        mockLoan.setApprovedAmount(BigDecimal.ZERO);

        // Act
        boolean result = service.isLoanEligibleForChargeMonitoring(mockLoan);

        // Assert
        assertFalse(result);
    }

    // ========================================================================
    // Charge Breakdown Tests
    // ========================================================================

    @Test
    @DisplayName("Should return charge breakdown")
    void getChargeBreakdown_success() {
        // Act
        ChargeMonitoringService.ChargeBreakdown result = service.getChargeBreakdown(mockLoan);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(10000), result.getPrincipal());
        assertEquals(BigDecimal.valueOf(500), result.getInterestEarned());
        assertEquals(BigDecimal.valueOf(200), result.getPenaltyEarned());
        assertEquals(BigDecimal.valueOf(700), result.getTotalCharges());
        assertNotNull(result.getPercentageCharged());
        assertNotNull(result.getInterestProportion());
        assertNotNull(result.getPenaltyProportion());
        assertTrue(result.isCapsEnabled());
        assertEquals(BigDecimal.valueOf(10), result.getAllowedPercentage());
    }

    @Test
    @DisplayName("Should handle null loan in charge breakdown")
    void getChargeBreakdown_nullLoan() {
        // Act
        ChargeMonitoringService.ChargeBreakdown result = service.getChargeBreakdown(null);

        // Assert
        assertNotNull(result);
        assertNull(result.getPrincipal());
        assertNull(result.getInterestEarned());
        assertNull(result.getPenaltyEarned());
        assertNull(result.getTotalCharges());
    }

    // ========================================================================
    // Charge Proportion Tests
    // ========================================================================

    @Test
    @DisplayName("Should calculate charge proportions correctly")
    void calculateChargeProportions_success() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.valueOf(700));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(300));

        // Act
        BigDecimal[] proportions = service.calculateChargeProportions(mockLoan);

        // Assert
        assertNotNull(proportions);
        assertEquals(2, proportions.length);
        assertEquals(new BigDecimal("0.700000"), proportions[0].setScale(6, RoundingMode.HALF_UP));
        assertEquals(new BigDecimal("0.300000"), proportions[1].setScale(6, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Should return zero proportions when no charges")
    void calculateChargeProportions_noCharges() {
        // Arrange
        mockLoan.setInterestsEarned(BigDecimal.ZERO);
        mockLoan.setPenaltyEarned(BigDecimal.ZERO);

        // Act
        BigDecimal[] proportions = service.calculateChargeProportions(mockLoan);

        // Assert
        assertNotNull(proportions);
        assertEquals(BigDecimal.ZERO, proportions[0]);
        assertEquals(BigDecimal.ZERO, proportions[1]);
    }

    @Test
    @DisplayName("Should return zero proportions for null loan")
    void calculateChargeProportions_nullLoan() {
        // Act
        BigDecimal[] proportions = service.calculateChargeProportions(null);

        // Assert
        assertNotNull(proportions);
        assertEquals(BigDecimal.ZERO, proportions[0]);
        assertEquals(BigDecimal.ZERO, proportions[1]);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private void setupExemptionTestData() {
        mockLoan.setInterestsEarned(BigDecimal.valueOf(900));
        mockLoan.setPenaltyEarned(BigDecimal.valueOf(300));
        mockLoan.setExemptedAmount(BigDecimal.ZERO);
        mockLoan.setExemptedInterests(BigDecimal.ZERO);
        mockLoan.setExemptedPenalties(BigDecimal.ZERO);
    }
}