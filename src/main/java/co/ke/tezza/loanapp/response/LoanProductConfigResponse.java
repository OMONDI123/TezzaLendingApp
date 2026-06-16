package co.ke.tezza.loanapp.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.enums.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanProductConfigResponse {

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

    
    @Enumerated(EnumType.STRING)
    private TenureTypeEnum tenureType;
    
    private Integer fixedTenureDays;
    private Integer minTenureDays;
    private Integer maxTenureDays;
    
    @Enumerated(EnumType.STRING)
    private TenureUnitEnum tenureUnit;

    // ------------------------------------------
    // 3. Principal Configuration
    // ------------------------------------------
    private BigDecimal minPrincipal;
    private BigDecimal maxPrincipal;

    // ------------------------------------------
    // 4. Service Fee Configuration (NEW)
    // ------------------------------------------
    private Boolean enableServiceFee = false;
    
    @Enumerated(EnumType.STRING)
    private FeeTypeEnum serviceFeeType;
    
    private BigDecimal serviceFeeAmount;
    private BigDecimal serviceFeePercentage;
    
    @Enumerated(EnumType.STRING)
    private FeeTimingEnum serviceFeeTiming;

    // ------------------------------------------
    // 5. Daily Fee Configuration
    // ------------------------------------------
    private Boolean enableDailyFee = false;
    private BigDecimal dailyFeeAmount;
    private Integer dailyFeeStartDay;

    // ------------------------------------------
    // 6. Interest Configuration
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    private InterestCalculationMethodEnum interestCalculationMethod;
    
    @Enumerated(EnumType.STRING)
    private FlatRateType flatRateType;
    
    private BigDecimal interetsFlatRate;
    private BigDecimal interetsFlatRateAmount;

    @Enumerated(EnumType.STRING)
    private InterestFrequencyEnum interestFrequency;

    private Integer gracePeriodDays;
    private Integer gracePeriodBeforeFirstInstallment;

    private BigDecimal earlyRepaymentDiscountPercent;

    private BigDecimal dailyInterestRate;
    private BigDecimal weeklyInterestRate;
    private BigDecimal monthlyInterestRate;
    private BigDecimal annualInterestRate;

    // ------------------------------------------
    // 7. Cycle-Based Interest
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
    private boolean loanOverDueChargePenaltyInstallmentDue;
    private boolean paymentReliefOnOverdueDebt;
    private boolean allowOveralChargesCap;
    private BigDecimal allowedOveralChargesCapPercentage;

    @Enumerated(EnumType.STRING)
    private PenaltyCalculationBaseEnum defaultPenaltyCalculationBase;

    @Enumerated(EnumType.STRING)
    private PenaltyBaseEnum penaltyAppliesTo;
    
    private BigDecimal penaltyFlatRateAmount;

    // ------------------------------------------
    // 9. Loan State Management Rules (NEW)
    // ------------------------------------------
    private Integer daysToWriteOff;
    private Integer daysToCancel;
    private Boolean allowReinstatement = false;
    private Integer reinstatementGracePeriodDays;
    private Boolean autoCloseOnFullPayment = true;

    // ------------------------------------------
    // 10. Sweep Job Configuration (NEW)
    // ------------------------------------------
    private Boolean enableAutoSweep = false;
    private Integer autoSweepFrequencyHours = 24;
    private Boolean sweepUpdateState = true;
    private Boolean sweepApplyPenalties = true;
    private Boolean sweepSendNotifications = true;

    // ------------------------------------------
    // 11. Repayment Schedule Configuration
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    private RepaymentScheduleTypeEnum repaymentScheduleType;

    @Enumerated(EnumType.STRING)
    private InstallmentFrequencyEnum installmentFrequency;

    // ------------------------------------------
    // 12. Security & Collateral Requirements
    // ------------------------------------------
    private Boolean requireGuarantors;
    private Integer minGuarantors;

    private Boolean requireCollateral;
    private BigDecimal collateralValuePercentOfLoan;

    // ------------------------------------------
    // 13. Operational Flags
    // ------------------------------------------
    private Boolean allowEarlyRepayment;
    private Boolean allowTopUpLoans;
    private Boolean isDefaultLoanProductConfig;

    private int requiredApprovalSteps;

    // ------------------------------------------
    // 14. Audit & Status Fields
    // ------------------------------------------
    private String documentNo;
    private String AD_LoanProductConfiguration_UU;

    @Enumerated(EnumType.STRING)
    private DocStatus docStatus;

    @Enumerated(EnumType.STRING)
    private ApprovalStage approvalStage;
    
    private boolean isActive;

    private Date created;
    private Date updated;

    // ------------------------------------------
    // 15. Approval Levels
    // ------------------------------------------
    private Set<ApprovalStepsResponse> approvalLevels = new HashSet<>();
}