package co.ke.tezza.loanapp.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.LoanRepaymentStatus;
import co.ke.tezza.loanapp.enums.LoanStateEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanApplicationResponse {

    // ------------------------------------------
    // Basic Identification
    // ------------------------------------------
    private Long loanApplicationId;
    private String AD_LoanApplication_UU;

    // ------------------------------------------
    // Borrower Information
    // ------------------------------------------
    private BorrowerTypeEnum borrowerType;
    private IndividualBorrowerResponse individualBorrowerResponse;
    private InstitutionBorrowerResponse institutionBorrowerResponse;
    private GroupBorrowerResponse groupBorrowerResponse;

    // ------------------------------------------
    // Loan Product
    // ------------------------------------------
    private LoanProductConfigResponse loanProductConfigResponse;
    private Long loanProductId;
    private String loanProductName;

    // ------------------------------------------
    // Loan Details
    // ------------------------------------------
    private BigDecimal appliedAmount;
    private BigDecimal approvedAmount;
    private Integer termInDays;
    private Date expectedDisbursementDate;
    private Date actualDisbursementDate;
    private String reasonForRejection;

    // ------------------------------------------
    // User Actions
    // ------------------------------------------
    private UserResponse appliedBy;
    private UserResponse approvedBy;
    private UserResponse rejectedBy;
    private UserResponse cancelledBy; // NEW

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
    private BigDecimal serviceFeeCharged = BigDecimal.ZERO;
    private BigDecimal dailyFeeCharged = BigDecimal.ZERO;
    private BigDecimal serviceFeeWaived = BigDecimal.ZERO;
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
    private Integer graceperiod;
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
    private LoanStateEnum loanState = LoanStateEnum.PENDING_APPROVAL;
    private Date stateChangeDate;
    private Date overdueSinceDate;
    private Date writeOffDate;
    private Date cancelledDate;
    private Date closedDate;
    private Date reinstatementDate;
    private String reinstatementReason;
    private String lastStateChangeTrigger;

    // ------------------------------------------
    // Write-off Details
    // ------------------------------------------
    private String writeOffReason;
    private UserResponse writeOffApprovedBy;
    private UserResponse writtenOffBy;

    // ------------------------------------------
    // Restructuring
    // ------------------------------------------
    private boolean restructured = false;
    private Date restructuringDate;
    private String restructuringReason;
    private Long originalLoanId;

    // ------------------------------------------
    // Consolidated Billing (IMPROVED)
    // ------------------------------------------
    private String consolidatedBillingGroupId;
    private boolean isConsolidatedBilling = false;
    
    // Parent loan information
    private Long parentConsolidatedLoanId;
    private LoanApplicationResponse parentConsolidatedLoan; // Full parent object (NEW)
    
    // Child loans list (NEW - replaces single parentConsolidatedLoanId)
    private Set<LoanApplicationResponse> childConsolidatedLoans = new HashSet<>();
    
    // Helper fields for quick access
    private boolean isParentLoan; // NEW - convenience flag
    private boolean isChildLoan; // NEW - convenience flag
    private BigDecimal totalGroupBalance; // NEW - total balance of all loans in group

    // ------------------------------------------
    // Sweep Job Tracking
    // ------------------------------------------
    private Date lastSweepRunDate;
    private Integer sweepRunCount = 0;
    private String sweepNotes;

    // ------------------------------------------
    // Exemptions
    // ------------------------------------------
    private BigDecimal exemptedAmount = BigDecimal.ZERO;
    private BigDecimal exemptedInterests = BigDecimal.ZERO;
    private BigDecimal exemptedPenalties = BigDecimal.ZERO;
    private boolean exempted;

    // ------------------------------------------
    // Reference & Assignment
    // ------------------------------------------
    private String externalReferenceNo;
    private UserResponse assignee;

    // ------------------------------------------
    // Additional Metadata
    // ------------------------------------------
    private String disbursementNotes;
    private String closureNotes;
    private String cancellationReason;

    // ------------------------------------------
    // Collections
    // ------------------------------------------
    private Set<NextOfKins> guarantors = new HashSet<>();
    private Set<BorrowerAttachments> collaterals = new HashSet<>();

    // ------------------------------------------
    // Audit Fields
    // ------------------------------------------
    private DocStatus docStatus;
    private ApprovalStage approvalStage;
    private String documentNo;
    private Date created;
    private Date updated;
    private boolean isActive;
}