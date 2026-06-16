package co.ke.tezza.loanapp.entity;

import lombok.*;
import javax.persistence.*;

import co.ke.tezza.loanapp.enums.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "AD_Loan_product_configuration")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MLoanProductConfiguration extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Loan_product_configuration_id")
    private Long loanProductConfigId;

    // ------------------------------------------
    // 1. Classification
    // ------------------------------------------
    private Boolean isDebtProduct = false;

    @Enumerated(EnumType.STRING)
    private DebtTypeEnum debtType;

    @ElementCollection
    @CollectionTable(name = "loan_product_borrower_types")
    @Enumerated(EnumType.STRING)
    @Column(name = "borrower_type")
    private Set<BorrowerTypeEnum> borrowerTypes;

    // ------------------------------------------
    // 2. Tenure Configuration
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    private TenureTypeEnum tenureType; // FIXED, FLEXIBLE
    
    private Integer fixedTenureDays;
    private Integer minTenureDays;
    private Integer maxTenureDays;
    
    @Enumerated(EnumType.STRING)
    private TenureUnitEnum tenureUnit; // DAYS, WEEKS, MONTHS, YEARS

    // ------------------------------------------
    // 3. Principal Configuration
    // ------------------------------------------
    private BigDecimal minPrincipal;
    private BigDecimal maxPrincipal;

    // ------------------------------------------
    // 4. Service Fee Configuration
    // ------------------------------------------
    private Boolean enableServiceFee = false;
    
    @Enumerated(EnumType.STRING)
    private FeeTypeEnum serviceFeeType; // FIXED, PERCENTAGE
    
    private BigDecimal serviceFeeAmount;
    private BigDecimal serviceFeePercentage;
    
    @Enumerated(EnumType.STRING)
    private FeeTimingEnum serviceFeeTiming; // ORIGINATION, POST_DISBURSEMENT

    // ------------------------------------------
    // 5. Daily Fee Configuration
    // ------------------------------------------
    private Boolean enableDailyFee = false;
    private BigDecimal dailyFeeAmount;
    private Integer dailyFeeStartDay; // Day from disbursement to start charging

    // ------------------------------------------
    // 6. Interest Configuration
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    private InterestCalculationMethodEnum interestCalculationMethod;

    @Enumerated(EnumType.STRING)
    private InterestFrequencyEnum interestFrequency;
    
    @Enumerated(EnumType.STRING)
    private FlatRateType flatRateType;
    
    private BigDecimal interetsFlatRate;
    private BigDecimal interetsFlatRateAmount;

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
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean allowMaxPenaltyCap;
    
    private BigDecimal maxPenaltyCapPercentOfPrincipal;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean periodPaymentStopPenalty;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean paymentReliefOnOverdueDebt;

    private Boolean allowPartialRepayments;

    @Enumerated(EnumType.STRING)
    private PenaltyCalculationBaseEnum defaultPenaltyCalculationBase;
    
    @Enumerated(EnumType.STRING)
    private PenaltyBaseEnum penaltyAppliesTo;
    
    private BigDecimal penaltyFlatRateAmount;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean installmentDueChargePenalty;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean loanOverDueChargePenaltyInstallmentDue;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean allowOveralChargesCap;
    
    private BigDecimal allowedOveralChargesCapPercentage;

    // ------------------------------------------
    // 9. Loan State Management Rules (NEW)
    // ------------------------------------------
    // Automatic state transition rules
    private Integer daysToWriteOff; // Days overdue before writing off
    private Integer daysToCancel; // Days to auto-cancel if not disbursed
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowReinstatement = false; // Allow reactivation of written-off loans
    
    private Integer reinstatementGracePeriodDays; // Grace period for reinstatement
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean autoCloseOnFullPayment = true; // Auto-close when balance is zero

    // ------------------------------------------
    // 10. Sweep Job Configuration (NEW)
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean enableAutoSweep = false; // Enable automated sweep jobs
    
    private Integer autoSweepFrequencyHours = 24; // How often to run sweep job
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean sweepUpdateState = true; // Update loan state during sweep
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean sweepApplyPenalties = true; // Apply penalties during sweep
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean sweepSendNotifications = true; // Send notifications during sweep

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

    private String AD_LoanProductConfiguration_UU;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, 
               mappedBy = "loanConfiguration", orphanRemoval = true)
    private Set<MApprovalSteps> approvalLevels = new HashSet<>();
}