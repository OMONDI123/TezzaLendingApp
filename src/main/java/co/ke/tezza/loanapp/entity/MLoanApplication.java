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
    @JoinColumn(name = "AD_Debtor_ID")
    private MDebtor individualBorrower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_Institution_Borrower_ID")
    private MInstitutionBorrower institutionBorrower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_Group_Borrower_ID")
    private MGroupDebtors groupBorrower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_LoanProduct_ID", nullable = false)
    private MLoanProductConfiguration loanProductConfiguration;

    // ------------------------------------------
    // Loan Details
    // ------------------------------------------
    @Column(name = "applied_amount", nullable = false)
    private BigDecimal appliedAmount;

    @Column(name = "approved_amount")
    private BigDecimal approvedAmount;

    @Column(name = "term_in_days", nullable = false)
    private Integer termInDays;

    @Column(name = "expected_disbursement_date")
    private Date expectedDisbursementDate;
    
    @Column(name = "actual_disbursement_date")
    private Date actualDisbursementDate;

    @Column(name = "reason_for_rejection")
    private String reasonForRejection;

    // ------------------------------------------
    // User Actions
    // ------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by")
    private MUser appliedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private MUser approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private MUser rejectedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private MUser cancelledBy;

    // ------------------------------------------
    // Financial Tracking
    // ------------------------------------------
    private BigDecimal balance;

    private BigDecimal interestsEarned;
    
    private BigDecimal penaltyEarned = BigDecimal.ZERO;
    
    private BigDecimal decliningInterest;
    
    private BigDecimal decliningPrincipal;
    
    private BigDecimal totalExpectedBalance;
    
    private BigDecimal totalExpectedInterest;
    
    private BigDecimal installmentDistributionBalance;

    // ------------------------------------------
    // Fee Tracking
    // ------------------------------------------
    @Column(name = "service_fee_charged")
    private BigDecimal serviceFeeCharged = BigDecimal.ZERO;

    @Column(name = "daily_fee_charged")
    private BigDecimal dailyFeeCharged = BigDecimal.ZERO;

    @Column(name = "service_fee_waived")
    private BigDecimal serviceFeeWaived = BigDecimal.ZERO;

    @Column(name = "daily_fee_waived")
    private BigDecimal dailyFeeWaived = BigDecimal.ZERO;

    // ------------------------------------------
    // Interest Rate Overrides
    // ------------------------------------------
    private BigDecimal dailyInterestRate = BigDecimal.ZERO;
    private BigDecimal weeklyInterestRate = BigDecimal.ZERO;
    private BigDecimal monthlyInterestRate = BigDecimal.ZERO;
    private BigDecimal annualInterestRate = BigDecimal.ZERO;
    private BigDecimal interetsFlatRateAmount = BigDecimal.ZERO;
    private BigDecimal interetsFlatRate = BigDecimal.ZERO;
    private BigDecimal cycle1FlatInterestPercent = BigDecimal.ZERO;
    private BigDecimal cycle2DailyInterestPercent = BigDecimal.ZERO;
    private BigDecimal cycle3PenaltyPercentPerPeriod = BigDecimal.ZERO;
    private BigDecimal initialInstallmentBaseAmount = BigDecimal.ZERO;

    // ------------------------------------------
    // Dates
    // ------------------------------------------
    private Date approvalDate;
    private Date rejectedDate;
    private Date dueDate;
    private Date lastReminderSent;
    private Date lastInterestCalculationDate;
    private Date nextInterestCalculationDate;
    private Date lastPenaltyCalculationDate;
    private Date nextPenaltyCalculationDate;

    // ------------------------------------------
    // Grace Periods
    // ------------------------------------------
    private Integer gracePeriodToFirstInstallment;
    private Integer graceperiod; // applies to non installment loans
    private Integer penaltyGracePeriod;

    // ------------------------------------------
    // Reminders & Notification Tracking
    // ------------------------------------------
    private Integer noOfRemindersSent;
    private Integer notificationCount = 0;
    private Date lastNotificationSentDate;
    private String notificationPreferences;

    // ------------------------------------------
    // Installment Tracking
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean hasInstallments;

    // ------------------------------------------
    // Repayment Status
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    private LoanRepaymentStatus repaymentStatus;

    // ------------------------------------------
    // Loan State Management
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_state", nullable = false)
    private LoanStateEnum loanState = LoanStateEnum.PENDING_APPROVAL;

    @Column(name = "state_change_date")
    private Date stateChangeDate;

    @Column(name = "overdue_since_date")
    private Date overdueSinceDate;

    @Column(name = "write_off_date")
    private Date writeOffDate;

    @Column(name = "cancelled_date")
    private Date cancelledDate;

    @Column(name = "closed_date")
    private Date closedDate;

    @Column(name = "reinstatement_date")
    private Date reinstatementDate;

    @Column(name = "reinstatement_reason")
    private String reinstatementReason;

    @Column(name = "last_state_change_trigger")
    private String lastStateChangeTrigger;

    // ------------------------------------------
    // Consolidated Billing (IMPROVED - With Self-Referencing)
    // ------------------------------------------
    @Column(name = "consolidated_billing_group_id")
    private String consolidatedBillingGroupId;

    @Column(name = "is_consolidated_billing",columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isConsolidatedBilling = false;

    // Parent loan reference - This loan is a child of this parent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_consolidated_loan_id")
    private MLoanApplication parentConsolidatedLoan;

    // Child loans - This loan is the parent of these children
    @OneToMany(mappedBy = "parentConsolidatedLoan", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MLoanApplication> childConsolidatedLoans = new HashSet<>();

    // ------------------------------------------
    // Write-off Details
    // ------------------------------------------
    @Column(name = "write_off_reason")
    private String writeOffReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "write_off_approved_by")
    private MUser writeOffApprovedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "written_off_by")
    private MUser writtenOffBy;

    // ------------------------------------------
    // Restructuring
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean restructured = false;

    @Column(name = "restructuring_date")
    private Date restructuringDate;

    @Column(name = "restructuring_reason")
    private String restructuringReason;

    @Column(name = "original_loan_id")
    private Long originalLoanId;

    // ------------------------------------------
    // Sweep Job Tracking
    // ------------------------------------------
    @Column(name = "last_sweep_run_date")
    private Date lastSweepRunDate;

    @Column(name = "sweep_run_count")
    private Integer sweepRunCount = 0;

    @Column(name = "sweep_notes")
    private String sweepNotes;

    // ------------------------------------------
    // Exemptions
    // ------------------------------------------
    private BigDecimal exemptedAmount = BigDecimal.ZERO;
    private BigDecimal exemptedInterests = BigDecimal.ZERO;
    private BigDecimal exemptedPenalties = BigDecimal.ZERO;
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
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
    private String externalReferenceNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignee", nullable = true)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private MUser assignee;

    // ------------------------------------------
    // Additional Metadata
    // ------------------------------------------
    @Column(name = "disbursement_notes")
    private String disbursementNotes;

    @Column(name = "closure_notes")
    private String closureNotes;

    @Column(name = "cancellation_reason")
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