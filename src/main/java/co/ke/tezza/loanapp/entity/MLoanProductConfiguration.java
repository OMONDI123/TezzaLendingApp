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
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDebtProduct = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private DebtTypeEnum debtType;

    @ElementCollection
    @CollectionTable(name = "loan_product_borrower_types")
    @Enumerated(EnumType.STRING)
    @Column(name = "borrower_type", nullable = true)
    private Set<BorrowerTypeEnum> borrowerTypes;

    // ------------------------------------------
    // 2. Tenure Configuration
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private TenureTypeEnum tenureType; // FIXED, FLEXIBLE
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer fixedTenureDays;
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer minTenureDays;
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer maxTenureDays;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private TenureUnitEnum tenureUnit; // DAYS, WEEKS, MONTHS, YEARS

    // ------------------------------------------
    // 3. Principal Configuration
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal minPrincipal;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal maxPrincipal;

    // ------------------------------------------
    // 4. Service Fee Configuration
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean enableServiceFee = false;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private FeeTypeEnum serviceFeeType; // FIXED, PERCENTAGE
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal serviceFeeAmount;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal serviceFeePercentage;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private FeeTimingEnum serviceFeeTiming; // ORIGINATION, POST_DISBURSEMENT

    // ------------------------------------------
    // 5. Daily Fee Configuration
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean enableDailyFee = false;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal dailyFeeAmount;
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer dailyFeeStartDay; // Day from disbursement to start charging

    // ------------------------------------------
    // 6. Interest Configuration
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private InterestCalculationMethodEnum interestCalculationMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private InterestFrequencyEnum interestFrequency;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private FlatRateType flatRateType;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal interetsFlatRate;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal interetsFlatRateAmount;

    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer gracePeriodDays;
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer gracePeriodBeforeFirstInstallment;

    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal earlyRepaymentDiscountPercent;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal dailyInterestRate;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal weeklyInterestRate;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal monthlyInterestRate;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal annualInterestRate;

    // ------------------------------------------
    // 7. Cycle-Based Interest
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer cycle1DurationDays;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal cycle1FlatInterestPercent;

    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer cycle2DurationDays;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal cycle2DailyInterestPercent;
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer cycle2StartsAfterDay;

    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer cycle3PenaltyStartsAfterDay;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal cycle3PenaltyPercentPerPeriod;
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer cycle3PenaltyPeriodDays;

    // ------------------------------------------
    // 8. Penalty / Late Fee Configuration
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer penaltyGracePeriodDays;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal penaltyRatePercent;
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer penaltyFrequencyDays;
    
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean allowMaxPenaltyCap;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal maxPenaltyCapPercentOfPrincipal;
    
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean periodPaymentStopPenalty;
    
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean paymentReliefOnOverdueDebt;

    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowPartialRepayments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private PenaltyCalculationBaseEnum defaultPenaltyCalculationBase;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private PenaltyBaseEnum penaltyAppliesTo;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal penaltyFlatRateAmount;
    
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean installmentDueChargePenalty;
    
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean loanOverDueChargePenaltyInstallmentDue;
    
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean allowOveralChargesCap;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal allowedOveralChargesCapPercentage;

    // ------------------------------------------
    // 9. Loan State Management Rules (NEW)
    // ------------------------------------------
    // Automatic state transition rules
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer daysToWriteOff; // Days overdue before writing off
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer daysToCancel; // Days to auto-cancel if not disbursed
    
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowReinstatement = false; // Allow reactivation of written-off loans
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer reinstatementGracePeriodDays; // Grace period for reinstatement
    
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean autoCloseOnFullPayment = true; // Auto-close when balance is zero

    
    // ------------------------------------------
    // 11. Repayment Schedule Configuration
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private RepaymentScheduleTypeEnum repaymentScheduleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private InstallmentFrequencyEnum installmentFrequency;

    // ------------------------------------------
    // 12. Security & Collateral Requirements
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireGuarantors;
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer minGuarantors;

    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireCollateral;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal collateralValuePercentOfLoan;

    // ------------------------------------------
    // 13. Operational Flags
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowEarlyRepayment;
    
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowTopUpLoans;
    
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDefaultLoanProductConfig;

    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private int requiredApprovalSteps;

    @Column(nullable = true)
    private String AD_LoanProductConfiguration_UU;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, 
               mappedBy = "loanConfiguration", orphanRemoval = true)
    private Set<MApprovalSteps> approvalLevels = new HashSet<>();
}