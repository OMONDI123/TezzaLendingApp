package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanApplicationRequest {

    // ------------------------------------------
    // Borrower Information
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    private BorrowerTypeEnum borrowerType;

    private Long individualBorrowerId;
    private Long institutionBorrowerId;
    private Long groupBorrowerId;

    // ------------------------------------------
    // Loan Product
    // ------------------------------------------
    private Long loanProductId;

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
    private Long cancelledBy; // NEW - User who cancelled the loan

    // ------------------------------------------
    // Grace Periods
    // ------------------------------------------
    private Integer gracePeriodToFirstInstallment;
    private Integer graceperiod; // applies to non-installment loans
    private Integer penaltyGracePeriod;

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

    // ------------------------------------------
    // Collections
    // ------------------------------------------
    private List<NextOfKins> guarantors = new ArrayList<>();
    private List<BorrowerAttachments> collateralValue = new ArrayList<>();

    // ------------------------------------------
    // Reference & Assignment
    // ------------------------------------------
    private String externalReferenceNo;
    private Long loanAssignedTo; // assignee

    // ------------------------------------------
    // Consolidated Billing (IMPROVED)
    // ------------------------------------------
    private String consolidatedBillingGroupId; // Group ID for consolidated billing
    private boolean isConsolidatedBilling = false; // Flag for consolidated billing
    
    // Parent-child relationship
    private Long parentConsolidatedLoanId; // ID of parent loan if this is a child
    
    // Child loans - for creating parent with children
    private List<Long> childConsolidatedLoanIds = new ArrayList<>(); // IDs of child loans to link
    private List<LoanApplicationRequest> childConsolidatedLoans = new ArrayList<>(); // NEW - For creating children inline

    // ------------------------------------------
    // Restructuring
    // ------------------------------------------
    private boolean isRestructured = false;
    private String restructuringReason;
    private Long originalLoanId;

    // ------------------------------------------
    // Write-off Details
    // ------------------------------------------
    private String writeOffReason;
    private Long writeOffApprovedBy; // User who approved write-off
    private Long writtenOffBy; // NEW - User who performed write-off

    // ------------------------------------------
    // Reinstatement
    // ------------------------------------------
    private String reinstatementReason;

    // ------------------------------------------
    // Notification Preferences
    // ------------------------------------------
    private String notificationPreferences;

    // ------------------------------------------
    // Additional Metadata
    // ------------------------------------------
    private String disbursementNotes;
    private String cancellationReason;
    private String closureNotes; // NEW - Notes about loan closure

    // ------------------------------------------
    // Loan State Management
    // ------------------------------------------
    private String lastStateChangeTrigger; // NEW - What triggered state change
}