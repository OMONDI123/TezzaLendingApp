package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import co.ke.tezza.loanapp.enums.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AmendmentConfigurationRequest {
    // Basic Information
	
	private long amendmentConfigId;
    private String configurationName;
    private String configurationDescription;
    private AmendmentType amendmentType;
    private Boolean isDefaultConfiguration = false;
    
    // Applicability
    private Set<Long> applicableLoanProductIds;
    private Set<BorrowerTypeEnum> allowedBorrowerTypes;
    
    // Eligibility Criteria
    private Boolean requireMinimumLoanAge = false;
    private Integer minimumLoanAgeDays = 0;
    private Boolean requireMaximumLoanAge = false;
    private Integer maximumLoanAgeDays = 3650;
    private Boolean requireMinimumOutstandingBalance = false;
    private BigDecimal minimumOutstandingBalance = BigDecimal.ZERO;
    private Boolean requireMaximumOutstandingBalance = false;
    private BigDecimal maximumOutstandingBalance = BigDecimal.valueOf(1000000000);
    private Boolean requireGoodRepaymentHistory = false;
    private Integer minimumOnTimePayments = 0;
    private Boolean allowForOverdueLoans = false;
    private Integer maximumDaysOverdueAllowed = 0;
    private Boolean requireNoActiveLegalCases = true;
    
    // Common Amendment Limits
    private BigDecimal minimumAmountChange = BigDecimal.ZERO;
    private BigDecimal maximumAmountChange = BigDecimal.valueOf(1000000);
    private BigDecimal minimumPercentageChange = BigDecimal.ZERO;
    private BigDecimal maximumPercentageChange = BigDecimal.valueOf(100);
    private Integer minimumDaysChange = 0;
    private Integer maximumDaysChange = 365;
    
    // Specific Amendment Limits (type-specific)
    private BigDecimal minimumTopUpAmount = BigDecimal.ZERO;
    private BigDecimal maximumTopUpAmount = BigDecimal.valueOf(1000000);
    private BigDecimal maximumTopUpPercentage = BigDecimal.valueOf(50);
    private BigDecimal maximumPrincipalReductionPercentage = BigDecimal.valueOf(100);
    private BigDecimal maximumRestructuringAmount = BigDecimal.valueOf(500000);
    private Boolean allowExtendedRepaymentTerms = true;
    private Integer minimumTermExtensionDays = 0;
    private Integer maximumTermExtensionDays = 365;
    private Integer minimumTermReductionDays = 0;
    private Integer maximumTermReductionDays = 180;
    private BigDecimal minimumInterestRateChange = BigDecimal.ZERO;
    private BigDecimal maximumInterestRateChange = BigDecimal.valueOf(100);
    private Integer maximumGracePeriodExtensionDays = 30;
    private BigDecimal minimumFlatRateChange = BigDecimal.ZERO;
    private BigDecimal maximumFlatRateChange = BigDecimal.valueOf(50);
    private List<String> allowedCalculationMethods;
    private Boolean requireNewCreditCheck = true;
    private Boolean requireNewGuarantors = true;
    private List<BorrowerTypeEnum> allowedBorrowerTypeChanges;
    private List<Long> allowedTargetProductIds;
    private Boolean requireProductCompatibilityCheck = true;
    private Integer maximumContractTermChanges = 5;
    private Integer maximumCollateralChanges = 3;
    private Integer maximumGuarantorChanges = 3;
    private Boolean requireNewGuarantorApproval = true;
    private BigDecimal minimumSecurityCoverageIncrease = BigDecimal.valueOf(10);
    private Boolean requireLegalDocumentation = true;
    private BigDecimal maximumPenaltyWaiverPercentage = BigDecimal.valueOf(100);
    private Boolean requireJustificationDocument = true;
    private BigDecimal maximumFeeWaiverAmount = BigDecimal.valueOf(10000);
    private BigDecimal maximumFeeWaiverPercentage = BigDecimal.valueOf(100);
    private BigDecimal minimumPenaltyRateChange = BigDecimal.ZERO;
    private BigDecimal maximumPenaltyRateChange = BigDecimal.valueOf(100);
    private Integer maximumScheduleChanges = 12;
    private Boolean requirePaymentCapacityAnalysis = true;
    private Integer maximumRescheduledInstallments = 6;
    private Boolean allowInstallmentAmountChange = true;
    private Integer maximumPaymentHolidayDays = 90;
    private Boolean allowInterestCapitalization = true;
    private Integer maximumDocumentUpdates = 10;
    private Boolean requireVersionControl = true;
    private BigDecimal maximumCorrectionAmount = BigDecimal.valueOf(100000);
    private Boolean requireAuditTrail = true;
    private BigDecimal emergencyApprovalThreshold = BigDecimal.valueOf(500000);
    private Integer emergencyResponseTimeHours = 24;
    private Integer maximumRenewalCount = 3;
    private Boolean requireFullReassessment = true;
    private Integer maximumReschedulingMonths = 24;
    private Boolean requireDebtSustainabilityAnalysis = true;
    private Integer maximumForbearanceMonths = 6;
    private Boolean requireFinancialHardshipProof = true;
    private Boolean allowRateDecrease = true;
    private Boolean allowRateIncrease = true;
    
    // Approval Configuration
    private Boolean requiresApproval = true;
    private Integer requiredApprovalSteps = 1;
    private Boolean allowAutoApproval = false;
    private BigDecimal autoApprovalMaximumAmount = BigDecimal.ZERO;
    private BigDecimal autoApprovalMaximumTermChange = BigDecimal.ZERO;
    private BigDecimal autoApprovalMaximumInterestChange = BigDecimal.ZERO;
    private Boolean allowQuickApproval = false;
    private BigDecimal quickApprovalMaximumAmount = BigDecimal.ZERO;
    private Integer quickApprovalSteps = 1;
    
    // Fees & Charges
    private Boolean chargeAmendmentFee = false;
    private BigDecimal amendmentFeeAmount = BigDecimal.ZERO;
    private BigDecimal amendmentFeePercentage = BigDecimal.ZERO;
    private AmendmentFeeType amendmentFeeType = AmendmentFeeType.NONE;
    private Boolean chargeProcessingFee = false;
    private BigDecimal processingFeeAmount = BigDecimal.ZERO;
    private Boolean chargeLegalFee = false;
    private BigDecimal legalFeeAmount = BigDecimal.ZERO;
    private Boolean allowFeeWaiver = false;
    private Integer feeWaiverApprovalSteps = 0;
    
    // Documentation Requirements
    private Boolean requireNewApplicationForm = false;
    private Boolean requireUpdatedContract = true;
    private Boolean requireBorrowerConsent = true;
    private Boolean requireGuarantorConsent = false;
    private Boolean requireCollateralRevaluation = false;
    private Boolean requireCreditReassessment = false;
    private Boolean requireLegalReview = false;
    private Boolean requireBoardApproval = false;
    private BigDecimal boardApprovalThresholdAmount = BigDecimal.valueOf(1000000);
    
    // Notification & Communication
    private Boolean notifyBorrower = true;
    private Boolean notifyGuarantors = false;
    private Boolean requireBorrowerAcknowledgement = true;
    private Boolean sendFormalLetter = true;
    private Boolean updateCreditBureau = false;
    private Boolean requireRegulatoryNotification = false;
    
    // Risk Controls
    private Boolean triggerRiskReview = false;
    private BigDecimal riskReviewThresholdAmount = BigDecimal.valueOf(500000);
    private Boolean requireCommitteeApproval = false;
    private BigDecimal committeeApprovalThresholdAmount = BigDecimal.valueOf(1000000);
    private Boolean requireCEOApproval = false;
    private BigDecimal ceoApprovalThresholdAmount = BigDecimal.valueOf(5000000);
    private Boolean allowMultipleAmendments = false;
    private Integer maximumAmendmentsPerLoan = 3;
    private Integer coolingPeriodDays = 0;
    
    // System & Integration
    private Boolean autoUpdateLoanSystem = true;
    private Boolean generateAmendmentNumber = true;
    private String amendmentNumberPrefix = "AMEND";
    private Boolean createAuditTrail = true;
    private Boolean updateCollateralRegistry = false;
    private Boolean syncWithCoreBanking = false;
    
    // Validation Rules
    private Boolean validateCreditLimit = true;
    private Boolean validateDebtServiceRatio = true;
    private BigDecimal maximumDebtServiceRatio = BigDecimal.valueOf(0.7);
    private Boolean validateTotalExposure = true;
    private BigDecimal maximumTotalExposure = BigDecimal.valueOf(10000000);
    
    // Approval Workflow
    private List<AmendmentApprovalStepRequest> approvalWorkflow;
}