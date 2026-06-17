package co.ke.tezza.loanapp.entity;

import lombok.*;

import javax.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import co.ke.tezza.loanapp.enums.LoanRepaymentStatus;
import co.ke.tezza.loanapp.enums.LoanStateEnum;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AD_LoanApplication")
public class MLoanApplication extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_LoanApplication_ID")
    private Long loanApplicationId;

    @Column(name = "AD_LoanApplication_UU", unique = true, nullable = false)
    private String AD_LoanApplication_UU;

    @Enumerated(EnumType.STRING)
    @Column(name = "borrower_type", nullable = false)
    private BorrowerTypeEnum borrowerType;

    // ------------------------------------------
    // Borrowers /Customers
    // ------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_Debtor_ID", nullable = true)
    private MDebtor individualBorrower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_Institution_Borrower_ID", nullable = true)
    private MInstitutionBorrower institutionBorrower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_Group_Borrower_ID", nullable = true)
    private MGroupDebtors groupBorrower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_LoanProduct_ID", nullable = false)
    private MLoanProductConfiguration loanProductConfiguration;

    // ------------------------------------------
    // Loan Details
    // ------------------------------------------
    @Column(name = "applied_amount", nullable = false)
    private BigDecimal appliedAmount;

    @Column(name = "approved_amount", nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal approvedAmount;

    @Column(name = "term_in_days", nullable = false)
    private Integer termInDays;

    @Column(name = "expected_disbursement_date", nullable = true)
    private Date expectedDisbursementDate;
    
    @Column(name = "actual_disbursement_date", nullable = true)
    private Date actualDisbursementDate;

    @Column(name = "reason_for_rejection", nullable = true)
    private String reasonForRejection;

    // ------------------------------------------
    // User Actions
    // ------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by", nullable = true)
    private MUser appliedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", nullable = true)
    private MUser approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by", nullable = true)
    private MUser rejectedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by", nullable = true)
    private MUser cancelledBy;

    // ------------------------------------------
    // Financial Tracking
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal balance;

    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal interestsEarned;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal penaltyEarned = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal decliningInterest;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal decliningPrincipal;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal totalExpectedBalance;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal totalExpectedInterest;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal installmentDistributionBalance;

    // ------------------------------------------
    // Fee Tracking
    // ------------------------------------------
    @Column(name = "service_fee_charged", nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal serviceFeeCharged = BigDecimal.ZERO;

    @Column(name = "daily_fee_charged", nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal dailyFeeCharged = BigDecimal.ZERO;

    @Column(name = "service_fee_waived", nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal serviceFeeWaived = BigDecimal.ZERO;

    @Column(name = "daily_fee_waived", nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal dailyFeeWaived = BigDecimal.ZERO;

    // ------------------------------------------
    // Interest Rate Overrides
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal dailyInterestRate = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal weeklyInterestRate = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal monthlyInterestRate = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal annualInterestRate = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal interetsFlatRateAmount = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal interetsFlatRate = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal cycle1FlatInterestPercent = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal cycle2DailyInterestPercent = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal cycle3PenaltyPercentPerPeriod = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal initialInstallmentBaseAmount = BigDecimal.ZERO;

    // ------------------------------------------
    // Dates
    // ------------------------------------------
    @Column(nullable = true)
    private Date approvalDate;
    
    @Column(nullable = true)
    private Date rejectedDate;
    
    @Column(nullable = true)
    private Date dueDate;
    
    @Column(nullable = true)
    private Date lastReminderSent;
    
    @Column(nullable = true)
    private Date lastInterestCalculationDate;
    
    @Column(nullable = true)
    private Date nextInterestCalculationDate;
    
    @Column(nullable = true)
    private Date lastPenaltyCalculationDate;
    
    @Column(nullable = true)
    private Date nextPenaltyCalculationDate;

    // ------------------------------------------
    // Grace Periods
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer gracePeriodToFirstInstallment;
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer graceperiod; // applies to non installment loans
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer penaltyGracePeriod;

    // ------------------------------------------
    // Reminders & Notification Tracking
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer noOfRemindersSent;
    
    @Column(nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer notificationCount = 0;
    
    @Column(nullable = true)
    private Date lastNotificationSentDate;
    
    @Column(nullable = true)
    private String notificationPreferences;

    // ------------------------------------------
    // Installment Tracking
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean hasInstallments;

    // ------------------------------------------
    // Repayment Status
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private LoanRepaymentStatus repaymentStatus;

    // ------------------------------------------
    // Loan State Management
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_state", nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'PENDING_APPROVAL'")
    private LoanStateEnum loanState = LoanStateEnum.PENDING_APPROVAL;

    @Column(name = "state_change_date", nullable = true)
    private Date stateChangeDate;

    @Column(name = "overdue_since_date", nullable = true)
    private Date overdueSinceDate;

    @Column(name = "write_off_date", nullable = true)
    private Date writeOffDate;

    @Column(name = "cancelled_date", nullable = true)
    private Date cancelledDate;

    @Column(name = "closed_date", nullable = true)
    private Date closedDate;

    @Column(name = "reinstatement_date", nullable = true)
    private Date reinstatementDate;

    @Column(name = "reinstatement_reason", nullable = true)
    private String reinstatementReason;

    @Column(name = "last_state_change_trigger", nullable = true)
    private String lastStateChangeTrigger;

    // ------------------------------------------
    // Consolidated Billing (IMPROVED - With Self-Referencing)
    // ------------------------------------------
    @Column(name = "consolidated_billing_group_id", nullable = true)
    private String consolidatedBillingGroupId;

    @Column(name = "is_consolidated_billing", nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isConsolidatedBilling = false;

    // Parent loan reference - This loan is a child of this parent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_consolidated_loan_id", nullable = true)
    private MLoanApplication parentConsolidatedLoan;

    // Child loans - This loan is the parent of these children
    @OneToMany(mappedBy = "parentConsolidatedLoan", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MLoanApplication> childConsolidatedLoans = new HashSet<>();

    // ------------------------------------------
    // Write-off Details
    // ------------------------------------------
    @Column(name = "write_off_reason", nullable = true)
    private String writeOffReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "write_off_approved_by", nullable = true)
    private MUser writeOffApprovedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "written_off_by", nullable = true)
    private MUser writtenOffBy;

    // ------------------------------------------
    // Restructuring
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean restructured = false;

    @Column(name = "restructuring_date", nullable = true)
    private Date restructuringDate;

    @Column(name = "restructuring_reason", nullable = true)
    private String restructuringReason;

    @Column(name = "original_loan_id", nullable = true, columnDefinition = "BIGINT DEFAULT 0")
    private Long originalLoanId;

    // ------------------------------------------
    // Sweep Job Tracking
    // ------------------------------------------
    @Column(name = "last_sweep_run_date", nullable = true)
    private Date lastSweepRunDate;
    
    @Column(nullable = true)
    private Date lastServiceFeeCalculationDate;
    
    @Column(nullable = true)
    private Date lastDailyFeeCalculationDate;

    @Column(name = "sweep_run_count", nullable = true, columnDefinition = "INTEGER DEFAULT 0")
    private Integer sweepRunCount = 0;

    @Column(name = "sweep_notes", nullable = true)
    private String sweepNotes;

    // ------------------------------------------
    // Exemptions
    // ------------------------------------------
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal exemptedAmount = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal exemptedInterests = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
    private BigDecimal exemptedPenalties = BigDecimal.ZERO;
    
    @Column(nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean exempted;

    // ------------------------------------------
    // Collateral & Guarantors
    // ------------------------------------------
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "AD_Borrower_Collateral", 
               joinColumns = @JoinColumn(name = "AD_LoanApplication_ID"), 
               inverseJoinColumns = @JoinColumn(name = "AD_Collateral_ID"))
    private Set<MDocuments> collateralAttachments = new HashSet<>();

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "AD_Borrower_Guarantors", 
               joinColumns = @JoinColumn(name = "AD_LoanApplication_ID"), 
               inverseJoinColumns = @JoinColumn(name = "AD_Guarantor_ID"))
    private Set<MNextOfKin> guarantors = new HashSet<>();

    // ------------------------------------------
    // Reference & Assignment
    // ------------------------------------------
    @Column(nullable = true)
    private String externalReferenceNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignee", nullable = true)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private MUser assignee;

    // ------------------------------------------
    // Additional Metadata
    // ------------------------------------------
    @Column(name = "disbursement_notes", nullable = true)
    private String disbursementNotes;

    @Column(name = "closure_notes", nullable = true)
    private String closureNotes;

    @Column(name = "cancellation_reason", nullable = true)
    private String cancellationReason;

    // ------------------------------------------
    // Helper Methods for Consolidated Billing
    // ------------------------------------------
    
    /**
     * Check if this loan is a parent loan (has children)
     */
    public boolean isParentLoan() {
        return isConsolidatedBilling && parentConsolidatedLoan == null && 
               childConsolidatedLoans != null && !childConsolidatedLoans.isEmpty();
    }

    /**
     * Check if this loan is a child loan (has a parent)
     */
    public boolean isChildLoan() {
        return parentConsolidatedLoan != null;
    }

    /**
     * Get all loans in the consolidated group (including this loan)
     */
    public Set<MLoanApplication> getAllLoansInGroup() {
        Set<MLoanApplication> allLoans = new HashSet<>();
        if (isParentLoan()) {
            allLoans.add(this);
            allLoans.addAll(childConsolidatedLoans);
        } else if (isChildLoan()) {
            allLoans.add(parentConsolidatedLoan);
            allLoans.addAll(parentConsolidatedLoan.getChildConsolidatedLoans());
        } else {
            allLoans.add(this);
        }
        return allLoans;
    }

    /**
     * Get total balance of all loans in the consolidated group
     */
    public BigDecimal getTotalGroupBalance() {
        Set<MLoanApplication> allLoans = getAllLoansInGroup();
        return allLoans.stream()
                .map(MLoanApplication::getBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}