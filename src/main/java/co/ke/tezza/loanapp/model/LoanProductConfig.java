package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.enums.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanProductConfig {

    private Long loanProductConfigId;

    private String name;
    private String description;

    // ------------------------------------------
    // 1. Classification
    // ------------------------------------------
    private Boolean isDebtProduct = false;

    @Enumerated(EnumType.STRING)
    private DebtTypeEnum debtType;

    @Enumerated(EnumType.STRING)
    private Set<BorrowerTypeEnum> borrowerTypes;

    // ------------------------------------------
    // 2. Tenure Configuration (UPDATED)
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    private TenureTypeEnum tenureType; // FIXED, FLEXIBLE - mandatory
    
    private Integer fixedTenureDays; // Mandatory if tenureType = FIXED
    private Integer minTenureDays; // Mandatory if tenureType = FLEXIBLE
    private Integer maxTenureDays; // Mandatory if tenureType = FLEXIBLE
    
    @Enumerated(EnumType.STRING)
    private TenureUnitEnum tenureUnit; // DAYS, WEEKS, MONTHS, YEARS - mandatory

    // ------------------------------------------
    // 3. Principal Configuration
    // ------------------------------------------
    private BigDecimal minPrincipal; // mandatory and should not be zero
    private BigDecimal maxPrincipal; // mandatory and should not be zero

    // ------------------------------------------
    // 4. Service Fee Configuration (NEW)
    // ------------------------------------------
    private Boolean enableServiceFee = false;
    
    @Enumerated(EnumType.STRING)
    private FeeTypeEnum serviceFeeType; // FIXED, PERCENTAGE - mandatory if enableServiceFee = true
    
    private BigDecimal serviceFeeAmount; // Mandatory if serviceFeeType = FIXED
    private BigDecimal serviceFeePercentage; // Mandatory if serviceFeeType = PERCENTAGE
    
    @Enumerated(EnumType.STRING)
    private FeeTimingEnum serviceFeeTiming; // ORIGINATION, POST_DISBURSEMENT - mandatory if enableServiceFee = true

    // ------------------------------------------
    // 5. Daily Fee Configuration (NEW)
    // ------------------------------------------
    private Boolean enableDailyFee = false;
    private BigDecimal dailyFeeAmount; // Mandatory if enableDailyFee = true
    private Integer dailyFeeStartDay; // Mandatory if enableDailyFee = true - day from disbursement to start charging

    // ------------------------------------------
    // 6. Interest Configuration
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    private InterestCalculationMethodEnum interestCalculationMethod; // mandatory
    
    @Enumerated(EnumType.STRING)
    private FlatRateType flatRateType; // mandatory if interestCalculationMethod = FLAT
    
    private BigDecimal interetsFlatRateAmount; // mandatory if interestCalculationMethod = FLAT and flatRateType = AMOUNT_BASED
    
    private BigDecimal interetsFlatRate; // mandatory if interestCalculationMethod = FLAT and flatRateType = PERCENTAGE_BASED

    @Enumerated(EnumType.STRING)
    private InterestFrequencyEnum interestFrequency; // mandatory if interestCalculationMethod != FLAT

    private Integer gracePeriodDays; // applies if repaymentScheduleType = ONE_TIME
    private Integer gracePeriodBeforeFirstInstallment; // applies if repaymentScheduleType = INSTALLMENTS

    private BigDecimal earlyRepaymentDiscountPercent;

    private BigDecimal dailyInterestRate; // mandatory if interestFrequency = DAILY
    private BigDecimal weeklyInterestRate; // mandatory if interestFrequency = WEEKLY
    private BigDecimal monthlyInterestRate; // mandatory if interestFrequency = MONTHLY
    private BigDecimal annualInterestRate; // mandatory if interestFrequency = YEARLY

    // ------------------------------------------
    // 7. Cycle-Based Interest (mandatory if interestCalculationMethod = CYCLE_BASED)
    // ------------------------------------------
    private Integer cycle1DurationDays;
    private BigDecimal cycle1FlatInterestPercent;

    private Integer cycle2DurationDays;
    private BigDecimal cycle2DailyInterestPercent;
    private Integer cycle2StartsAfterDay;

    private Integer cycle3PenaltyStartsAfterDay;
    private BigDecimal cycle3PenaltyPercentPerPeriod;
    private Integer cycle3PenaltyPeriodDays;

    // ------------------------------------------
    // 8. Penalty / Late Fee Configuration
    // ------------------------------------------
    private Integer penaltyGracePeriodDays;
    private BigDecimal penaltyRatePercent;
    private Integer penaltyFrequencyDays;
    private BigDecimal maxPenaltyCapPercentOfPrincipal;
    private boolean allowMaxPenaltyCap;
    private Boolean allowPartialRepayments;
    private boolean installmentDueChargePenalty;
    private boolean periodPaymentStopPenalty;
    
    @Enumerated(EnumType.STRING)
    private PenaltyCalculationBaseEnum defaultPenaltyCalculationBase;

    @Enumerated(EnumType.STRING)
    private PenaltyBaseEnum penaltyAppliesTo;
    
    private BigDecimal penaltyFlatRateAmount;
    private boolean loanOverDueChargePenaltyInstallmentDue;
    private boolean paymentReliefOnOverdueDebt;

    private boolean allowOveralChargesCap;
    private BigDecimal allowedOveralChargesCapPercentage;

    // ------------------------------------------
    // 9. Loan State Management Rules (NEW)
    // ------------------------------------------
    private Integer daysToWriteOff; // Days overdue before writing off
    private Integer daysToCancel; // Days to auto-cancel if not disbursed
    private Boolean allowReinstatement = false; // Allow reactivation of written-off loans
    private Integer reinstatementGracePeriodDays; // Grace period for reinstatement
    private Boolean autoCloseOnFullPayment = true; // Auto-close when balance is zero

    // ------------------------------------------
    // 10. Sweep Job Configuration (NEW)
    // ------------------------------------------
    private Boolean enableAutoSweep = false; // Enable automated sweep jobs
    private Integer autoSweepFrequencyHours = 24; // How often to run sweep job
    private Boolean sweepUpdateState = true; // Update loan state during sweep
    private Boolean sweepApplyPenalties = true; // Apply penalties during sweep
    private Boolean sweepSendNotifications = true; // Send notifications during sweep

    // ------------------------------------------
    // 11. Repayment Schedule Configuration
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    private RepaymentScheduleTypeEnum repaymentScheduleType; // ONE_TIME, INSTALLMENTS - mandatory

    @Enumerated(EnumType.STRING)
    private InstallmentFrequencyEnum installmentFrequency; // mandatory if repaymentScheduleType = INSTALLMENTS

    // ------------------------------------------
    // 12. Security & Collateral Requirements
    // ------------------------------------------
    private Boolean requireGuarantors;
    private Integer minGuarantors; // mandatory if requireGuarantors = true

    private Boolean requireCollateral;
    private BigDecimal collateralValuePercentOfLoan; // mandatory if requireCollateral = true

    // ------------------------------------------
    // 13. Operational Flags
    // ------------------------------------------
    private Boolean allowEarlyRepayment;
    private Boolean allowTopUpLoans;
    private Boolean isDefaultLoanProductConfig;

    private int requiredApprovalSteps;

    // ------------------------------------------
    // 14. Approval Levels
    // ------------------------------------------
    private List<ApprovalStepsModel> approvalLevels = new ArrayList<>(); // mandatory if requiredApprovalSteps > 0
}