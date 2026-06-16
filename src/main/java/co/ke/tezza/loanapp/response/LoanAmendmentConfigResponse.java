package co.ke.tezza.loanapp.response;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import co.ke.tezza.loanapp.enums.AmendmentFeeType;
import co.ke.tezza.loanapp.enums.AmendmentType;
import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanAmendmentConfigResponse {
	  private Long amendmentConfigId;
	    private String AD_Amendment_Configuration_UU;
	    private String configurationName;
	    private String configurationDescription;
	    private Boolean isDefaultConfiguration;
	    
	    // Audit fields
	    private Long createdBy;
	    private Date created;
	    private Long updatedBy;
	    private Date updated;
	    private Long adOrgID;

	    // ------------------------------------------
	    // 2. Amendment Type Configuration (Simplified)
	    // ------------------------------------------
	    private AmendmentType amendmentType;
	    private Set<LoanProductConfigResponse> applicableLoanProducts=new HashSet<>();
	    private Set<BorrowerTypeEnum> allowedBorrowerTypes=new HashSet<>();

	    // ------------------------------------------
	    // 3. Eligibility Criteria
	    // ------------------------------------------
	    private Boolean requireMinimumLoanAge;
	    private Integer minimumLoanAgeDays;
	    private Boolean requireMaximumLoanAge;
	    private Integer maximumLoanAgeDays;
	    private Boolean requireMinimumOutstandingBalance;
	    private BigDecimal minimumOutstandingBalance;
	    private Boolean requireMaximumOutstandingBalance;
	    private BigDecimal maximumOutstandingBalance;
	    private Boolean requireGoodRepaymentHistory;
	    private Integer minimumOnTimePayments;
	    private Boolean allowForOverdueLoans;
	    private Integer maximumDaysOverdueAllowed;
	    private Boolean requireNoActiveLegalCases;

	    // ------------------------------------------
	    // 4. Amendment Limits - COMMON FIELDS
	    // ------------------------------------------
	    private Boolean allowGenericAmendment;
	    private BigDecimal minimumAmountChange;
	    private BigDecimal maximumAmountChange;
	    private BigDecimal minimumPercentageChange;
	    private BigDecimal maximumPercentageChange;
	    private Integer minimumDaysChange;
	    private Integer maximumDaysChange;

	    // ------------------------------------------
	    // 5. Specific Amendment Type Limits
	    // ------------------------------------------
	    // TOP_UP
	    private Boolean allowTopUp;
	    private BigDecimal minimumTopUpAmount;
	    private BigDecimal maximumTopUpAmount;
	    private BigDecimal maximumTopUpPercentage;
	    
	    // PRINCIPAL_REDUCTION
	    private Boolean allowPrincipalReduction;
	    private BigDecimal maximumPrincipalReductionPercentage;
	    
	    // PRINCIPAL_RESTRUCTURING
	    private Boolean allowPrincipalRestructuring;
	    private BigDecimal maximumRestructuringAmount;
	    private Boolean allowExtendedRepaymentTerms;
	    
	    // TERM
	    private Boolean allowTermExtension;
	    private Integer minimumTermExtensionDays;
	    private Integer maximumTermExtensionDays;
	    private Boolean allowTermReduction;
	    private Integer minimumTermReductionDays;
	    private Integer maximumTermReductionDays;
	    
	    // GRACE_PERIOD_EXTENSION
	    private Boolean allowGracePeriodExtension;
	    private Integer maximumGracePeriodExtensionDays;
	    
	    // INTEREST_RATE_CHANGE
	    private Boolean allowInterestRateChange;
	    private BigDecimal minimumInterestRateChange;
	    private BigDecimal maximumInterestRateChange;
	    private Boolean allowRateDecrease;
	    private Boolean allowRateIncrease;
	    
	    // FLAT_RATE_CHANGE
	    private Boolean allowFlatRateChange;
	    private BigDecimal minimumFlatRateChange;
	    private BigDecimal maximumFlatRateChange;
	    
	    // INTEREST_CALCULATION_CHANGE
	    private Boolean allowInterestCalculationChange;
	    private Set<String> allowedCalculationMethods=new HashSet<>();
	    
	    // BORROWER_CHANGE
	    private Boolean allowBorrowerChange;
	    private Boolean requireNewCreditCheck;
	    private Boolean requireNewGuarantors;
	    
	    // BORROWER_TYPE_CHANGE
	    private Boolean allowBorrowerTypeChange;
	    private Set<BorrowerTypeEnum> allowedBorrowerTypeChanges=new HashSet<>();
	    
	    // PRODUCT_CHANGE
	    private Boolean allowProductChange;
	    private Set<LoanProductConfigResponse> allowedTargetProducts=new HashSet<>(); 
	    private Boolean requireProductCompatibilityCheck;
	    
	    // CONTRACT_TERMS_CHANGE
	    private Boolean allowContractTermsChange;
	    private Integer maximumContractTermChanges;
	    
	    // COLLATERAL_CHANGE
	    private Boolean allowCollateralChange;
	    private Integer maximumCollateralChanges;
	    
	    // GUARANTOR_CHANGE
	    private Boolean allowGuarantorChange;
	    private Integer maximumGuarantorChanges;
	    private Boolean requireNewGuarantorApproval;
	    
	    // SECURITY_ENHANCEMENT
	    private Boolean allowSecurityEnhancement;
	    private BigDecimal minimumSecurityCoverageIncrease;
	    private Boolean requireLegalDocumentation;
	    
	    // PENALTY_WAIVER
	    private Boolean allowPenaltyWaiver;
	    private BigDecimal maximumPenaltyWaiverPercentage;
	    private Boolean requireJustificationDocument;
	    
	    // FEE_WAIVER
	    private Boolean allowFeeWaiver;
	    private BigDecimal maximumFeeWaiverAmount;
	    private BigDecimal maximumFeeWaiverPercentage;
	    
	    // PENALTY_RATE_CHANGE
	    private Boolean allowPenaltyRateChange;
	    private BigDecimal minimumPenaltyRateChange;
	    private BigDecimal maximumPenaltyRateChange;
	    
	    // REPAYMENT_SCHEDULE_CHANGE
	    private Boolean allowRepaymentScheduleChange;
	    private Integer maximumScheduleChanges;
	    private Boolean requirePaymentCapacityAnalysis;
	    
	    // INSTALLMENT_RESCHEDULING
	    private Boolean allowInstallmentRescheduling;
	    private Integer maximumRescheduledInstallments;
	    private Boolean allowInstallmentAmountChange;
	    
	    // PAYMENT_HOLIDAY
	    private Boolean allowPaymentHoliday;
	    private Integer maximumPaymentHolidayDays;
	    private Boolean allowInterestCapitalization;
	    
	    // DOCUMENT_UPDATE
	    private Boolean allowDocumentUpdate;
	    private Integer maximumDocumentUpdates;
	    private Boolean requireVersionControl;
	    
	    // ADMINISTRATIVE_CORRECTION
	    private Boolean allowAdministrativeCorrection;
	    private BigDecimal maximumCorrectionAmount;
	    private Boolean requireAuditTrail;
	    
	    // EMERGENCY_AMENDMENT
	    private Boolean allowEmergencyAmendment;
	    private BigDecimal emergencyApprovalThreshold;
	    private Integer emergencyResponseTimeHours;
	    
	    // LOAN_RENEWAL
	    private Boolean allowLoanRenewal;
	    private Integer maximumRenewalCount;
	    private Boolean requireFullReassessment;
	    
	    // LOAN_RESCHEDULING
	    private Boolean allowLoanRescheduling;
	    private Integer maximumReschedulingMonths;
	    private Boolean requireDebtSustainabilityAnalysis;
	    
	    // FORBEARANCE
	    private Boolean allowForbearance;
	    private Integer maximumForbearanceMonths;
	    private Boolean requireFinancialHardshipProof;

	    // ------------------------------------------
	    // 6. Approval Configuration
	    // ------------------------------------------
	    private Boolean requiresApproval;
	    private Integer requiredApprovalSteps;
	    private Boolean allowAutoApproval;
	    private BigDecimal autoApprovalMaximumAmount;
	    private BigDecimal autoApprovalMaximumTermChange;
	    private BigDecimal autoApprovalMaximumInterestChange;
	    private Boolean allowQuickApproval;
	    private BigDecimal quickApprovalMaximumAmount;
	    private Integer quickApprovalSteps;

	    // ------------------------------------------
	    // 7. Fees & Charges
	    // ------------------------------------------
	    private Boolean chargeAmendmentFee;
	    private BigDecimal amendmentFeeAmount;
	    private BigDecimal amendmentFeePercentage;
	    private AmendmentFeeType amendmentFeeType;
	    private Boolean chargeProcessingFee;
	    private BigDecimal processingFeeAmount;
	    private Boolean chargeLegalFee;
	    private BigDecimal legalFeeAmount;
	    private Integer feeWaiverApprovalSteps;

	    // ------------------------------------------
	    // 8. Documentation Requirements
	    // ------------------------------------------
	    private Boolean requireNewApplicationForm;
	    private Boolean requireUpdatedContract;
	    private Boolean requireBorrowerConsent;
	    private Boolean requireGuarantorConsent;
	    private Boolean requireCollateralRevaluation;
	    private Boolean requireCreditReassessment;
	    private Boolean requireLegalReview;
	    private Boolean requireBoardApproval;
	    private BigDecimal boardApprovalThresholdAmount;

	    // ------------------------------------------
	    // 9. Notification & Communication
	    // ------------------------------------------
	    private Boolean notifyBorrower;
	    private Boolean notifyGuarantors;
	    private Boolean requireBorrowerAcknowledgement;
	    private Boolean sendFormalLetter;
	    private Boolean updateCreditBureau;
	    private Boolean requireRegulatoryNotification;

	    // ------------------------------------------
	    // 10. Approval Workflow
	    // ------------------------------------------
	    private Set<LoanAmendmentApprovalStepsResponse> approvalWorkflow=new HashSet<>(); 

	    // ------------------------------------------
	    // 11. Risk Controls
	    // ------------------------------------------
	    private Boolean triggerRiskReview;
	    private BigDecimal riskReviewThresholdAmount;
	    private Boolean requireCommitteeApproval;
	    private BigDecimal committeeApprovalThresholdAmount;
	    private Boolean requireCEOApproval;
	    private BigDecimal ceoApprovalThresholdAmount;
	    private Boolean allowMultipleAmendments;
	    private Integer maximumAmendmentsPerLoan;
	    private Integer coolingPeriodDays;

	    // ------------------------------------------
	    // 12. System & Integration
	    // ------------------------------------------
	    private Boolean autoUpdateLoanSystem;
	    private Boolean generateAmendmentNumber;
	    private String amendmentNumberPrefix;
	    private Boolean createAuditTrail;
	    private Boolean updateCollateralRegistry;
	    private Boolean syncWithCoreBanking;

	    // ------------------------------------------
	    // 13. Validation Rules
	    // ------------------------------------------
	    private Boolean validateCreditLimit;
	    private Boolean validateDebtServiceRatio;
	    private BigDecimal maximumDebtServiceRatio;
	    private Boolean validateTotalExposure;
	    private BigDecimal maximumTotalExposure;
	    private boolean isActive;

	  

}
