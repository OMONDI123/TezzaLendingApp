package co.ke.tezza.loanapp.entity;

import lombok.*;
import javax.persistence.*;

import co.ke.tezza.loanapp.enums.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "AD_Amendment_Configuration")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MAmendmentConfiguration extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Amendment_Configuration_ID")
    private Long amendmentConfigId;

    @Column(name = "AD_Amendment_Configuration_UU", unique = true, nullable = false)
    private String AD_Amendment_Configuration_UU;

    // ------------------------------------------
    // 1. Basic Configuration
    // ------------------------------------------
    @Column(nullable = false)
    private String configurationName;
    
    private String configurationDescription;
    
  
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDefaultConfiguration = false;

    // ------------------------------------------
    // 2. Amendment Type Configuration
    // ------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmendmentType amendmentType;

    // Which loan products can use this amendment type
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "amendment_config_product_mapping",
        joinColumns = @JoinColumn(name = "amendment_config_id"),
        inverseJoinColumns = @JoinColumn(name = "loan_product_config_id")
    )
    private Set<MLoanProductConfiguration> applicableLoanProducts = new HashSet<>();

    // Which borrower types can request this amendment
    @ElementCollection
    @CollectionTable(name = "amendment_config_borrower_types")
    @Enumerated(EnumType.STRING)
    @Column(name = "borrower_type")
    private Set<BorrowerTypeEnum> allowedBorrowerTypes = new HashSet<>();

    // ------------------------------------------
    // 3. Eligibility Criteria
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireMinimumLoanAge = false;
    
    private Integer minimumLoanAgeDays = 0;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireMaximumLoanAge = false;
    
    private Integer maximumLoanAgeDays = 3650; // 10 years default
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireMinimumOutstandingBalance = false;
    
    private BigDecimal minimumOutstandingBalance = BigDecimal.ZERO;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireMaximumOutstandingBalance = false;
    
    private BigDecimal maximumOutstandingBalance = BigDecimal.valueOf(1000000000);
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireGoodRepaymentHistory = false;
    
    private Integer minimumOnTimePayments = 0;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowForOverdueLoans = false;
    
    private Integer maximumDaysOverdueAllowed = 0;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireNoActiveLegalCases = true;

    // ------------------------------------------
    // 4. Amendment Limits - COMMON FIELDS
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowGenericAmendment = false;
    
    private BigDecimal minimumAmountChange = BigDecimal.ZERO;
    
    private BigDecimal maximumAmountChange = BigDecimal.valueOf(1000000);
    
    private BigDecimal minimumPercentageChange = BigDecimal.ZERO;
    
    private BigDecimal maximumPercentageChange = BigDecimal.valueOf(100);
    
    private Integer minimumDaysChange = 0;
    
    private Integer maximumDaysChange = 365;

    // ------------------------------------------
    // 5. Specific Amendment Type Limits
    // ------------------------------------------
    // For TOP_UP amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowTopUp = false;
    
    private BigDecimal minimumTopUpAmount = BigDecimal.ZERO;
    
    private BigDecimal maximumTopUpAmount = BigDecimal.valueOf(1000000);
    
    private BigDecimal maximumTopUpPercentage = BigDecimal.valueOf(50); // % of original
    
    // For PRINCIPAL_REDUCTION amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowPrincipalReduction = false;
    
    private BigDecimal maximumPrincipalReductionPercentage = BigDecimal.valueOf(100);
    
    // For PRINCIPAL_RESTRUCTURING amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowPrincipalRestructuring = false;
    
    private BigDecimal maximumRestructuringAmount = BigDecimal.valueOf(500000);
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowExtendedRepaymentTerms = false;
    
    // For TERM amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowTermExtension = false;
    
    private Integer minimumTermExtensionDays = 0;
    
    private Integer maximumTermExtensionDays = 365;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowTermReduction = false;
    
    private Integer minimumTermReductionDays = 0;
    
    private Integer maximumTermReductionDays = 180;
    
    // For GRACE_PERIOD_EXTENSION amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowGracePeriodExtension = false;
    
    private Integer maximumGracePeriodExtensionDays = 30;
    
    // For INTEREST_RATE_CHANGE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowInterestRateChange = false;
    
    private BigDecimal minimumInterestRateChange = BigDecimal.ZERO;
    
    private BigDecimal maximumInterestRateChange = BigDecimal.valueOf(100);
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowRateDecrease = true;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowRateIncrease = true;
    
    // For FLAT_RATE_CHANGE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowFlatRateChange = false;
    
    private BigDecimal minimumFlatRateChange = BigDecimal.ZERO;
    
    private BigDecimal maximumFlatRateChange = BigDecimal.valueOf(50);
    
    // For INTEREST_CALCULATION_CHANGE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowInterestCalculationChange = false;
    
    @ElementCollection
    @CollectionTable(name = "amendment_config_calculation_methods")
    @Column(name = "calculation_method")
    private Set<String> allowedCalculationMethods = new HashSet<>();
    
    // For BORROWER_CHANGE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowBorrowerChange = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireNewCreditCheck = true;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireNewGuarantors = true;
    
    // For BORROWER_TYPE_CHANGE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowBorrowerTypeChange = false;
    
    @ElementCollection
    @CollectionTable(name = "amendment_config_borrower_type_changes")
    @Enumerated(EnumType.STRING)
    @Column(name = "borrower_type_change")
    private Set<BorrowerTypeEnum> allowedBorrowerTypeChanges = new HashSet<>();
    
    // For PRODUCT_CHANGE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowProductChange = false;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "amendment_config_target_products",
        joinColumns = @JoinColumn(name = "amendment_config_id"),
        inverseJoinColumns = @JoinColumn(name = "loan_product_config_id")
    )
    private Set<MLoanProductConfiguration> allowedTargetProducts = new HashSet<>();
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireProductCompatibilityCheck = true;
    
    // For CONTRACT_TERMS_CHANGE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowContractTermsChange = false;
    
    private Integer maximumContractTermChanges = 5;
    
    // For COLLATERAL_CHANGE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowCollateralChange = false;
    
    private Integer maximumCollateralChanges = 3;
    
    // For GUARANTOR_CHANGE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowGuarantorChange = false;
    
    private Integer maximumGuarantorChanges = 3;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireNewGuarantorApproval = true;
    
    // For SECURITY_ENHANCEMENT amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowSecurityEnhancement = false;
    
    private BigDecimal minimumSecurityCoverageIncrease = BigDecimal.valueOf(10);
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireLegalDocumentation = true;
    
    // For PENALTY_WAIVER amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowPenaltyWaiver = false;
    
    private BigDecimal maximumPenaltyWaiverPercentage = BigDecimal.valueOf(100);
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireJustificationDocument = true;
    
    // For FEE_WAIVER amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowFeeWaiver = false;
    
    private BigDecimal maximumFeeWaiverAmount = BigDecimal.valueOf(10000);
    
    private BigDecimal maximumFeeWaiverPercentage = BigDecimal.valueOf(100);
    
    // For PENALTY_RATE_CHANGE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowPenaltyRateChange = false;
    
    private BigDecimal minimumPenaltyRateChange = BigDecimal.ZERO;
    
    private BigDecimal maximumPenaltyRateChange = BigDecimal.valueOf(100);
    
    // For REPAYMENT_SCHEDULE_CHANGE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowRepaymentScheduleChange = false;
    
    private Integer maximumScheduleChanges = 12;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requirePaymentCapacityAnalysis = true;
    
    // For INSTALLMENT_RESCHEDULING amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowInstallmentRescheduling = false;
    
    private Integer maximumRescheduledInstallments = 6;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowInstallmentAmountChange = true;
    
    // For PAYMENT_HOLIDAY amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowPaymentHoliday = false;
    
    private Integer maximumPaymentHolidayDays = 90;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowInterestCapitalization = true;
    
    // For DOCUMENT_UPDATE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowDocumentUpdate = false;
    
    private Integer maximumDocumentUpdates = 10;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireVersionControl = true;
    
    // For ADMINISTRATIVE_CORRECTION amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowAdministrativeCorrection = false;
    
    private BigDecimal maximumCorrectionAmount = BigDecimal.valueOf(100000);
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireAuditTrail = true;
    
    // For EMERGENCY_AMENDMENT amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowEmergencyAmendment = false;
    
    private BigDecimal emergencyApprovalThreshold = BigDecimal.valueOf(500000);
    
    private Integer emergencyResponseTimeHours = 24;
    
    // For LOAN_RENEWAL amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowLoanRenewal = false;
    
    private Integer maximumRenewalCount = 3;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireFullReassessment = true;
    
    // For LOAN_RESCHEDULING amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowLoanRescheduling = false;
    
    private Integer maximumReschedulingMonths = 24;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireDebtSustainabilityAnalysis = true;
    
    // For FORBEARANCE amendments
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowForbearance = false;
    
    private Integer maximumForbearanceMonths = 6;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireFinancialHardshipProof = true;

    // ------------------------------------------
    // 6. Approval Configuration
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean requiresApproval = true;
    
    private Integer requiredApprovalSteps = 1;
    
    // Auto-approve conditions
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowAutoApproval = false;
    
    private BigDecimal autoApprovalMaximumAmount = BigDecimal.ZERO;
    
    private BigDecimal autoApprovalMaximumTermChange = BigDecimal.ZERO;
    
    private BigDecimal autoApprovalMaximumInterestChange = BigDecimal.ZERO;
    
    // Quick approval (reduced steps)
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowQuickApproval = false;
    
    private BigDecimal quickApprovalMaximumAmount = BigDecimal.ZERO;
    
    private Integer quickApprovalSteps = 1;

    // ------------------------------------------
    // 7. Fees & Charges
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean chargeAmendmentFee = false;
    
    private BigDecimal amendmentFeeAmount = BigDecimal.ZERO;
    
    private BigDecimal amendmentFeePercentage = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    private AmendmentFeeType amendmentFeeType = AmendmentFeeType.NONE; // FLAT_AMOUNT, PERCENTAGE, BOTH
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean chargeProcessingFee = false;
    
    private BigDecimal processingFeeAmount = BigDecimal.ZERO;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean chargeLegalFee = false;
    
    private BigDecimal legalFeeAmount = BigDecimal.ZERO;
    
    
    
    private Integer feeWaiverApprovalSteps = 0;

    // ------------------------------------------
    // 8. Documentation Requirements
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireNewApplicationForm = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireUpdatedContract = true;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireBorrowerConsent = true;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireGuarantorConsent = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireCollateralRevaluation = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireCreditReassessment = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireLegalReview = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireBoardApproval = false;
    
    private BigDecimal boardApprovalThresholdAmount = BigDecimal.valueOf(1000000);

    // ------------------------------------------
    // 9. Notification & Communication
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean notifyBorrower = true;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean notifyGuarantors = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireBorrowerAcknowledgement = true;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean sendFormalLetter = true;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean updateCreditBureau = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireRegulatoryNotification = false;

    // ------------------------------------------
    // 10. Approval Workflow
    // ------------------------------------------
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "amendment_config_approval_steps",
        joinColumns = @JoinColumn(name = "amendment_config_id"),
        inverseJoinColumns = @JoinColumn(name = "approval_step_id")
    )
    private Set<MAmendmentApprovalSteps> approvalWorkflow = new HashSet<>();
    
    // ------------------------------------------
    // 11. Risk Controls
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean triggerRiskReview = false;
    
    private BigDecimal riskReviewThresholdAmount = BigDecimal.valueOf(500000);
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireCommitteeApproval = false;
    
    private BigDecimal committeeApprovalThresholdAmount = BigDecimal.valueOf(1000000);
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireCEOApproval = false;
    
    private BigDecimal ceoApprovalThresholdAmount = BigDecimal.valueOf(5000000);
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean allowMultipleAmendments = false;
    
    private Integer maximumAmendmentsPerLoan = 3;
    
    private Integer coolingPeriodDays = 0; // Days before another amendment can be requested

    // ------------------------------------------
    // 12. System & Integration
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean autoUpdateLoanSystem = true;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean generateAmendmentNumber = true;
    
    private String amendmentNumberPrefix = "AMEND";
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean createAuditTrail = true;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean updateCollateralRegistry = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean syncWithCoreBanking = false;
    
    // ------------------------------------------
    // 13. Validation Rules
    // ------------------------------------------
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean validateCreditLimit = true;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean validateDebtServiceRatio = true;
    
    private BigDecimal maximumDebtServiceRatio = BigDecimal.valueOf(0.7); // 70%
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean validateTotalExposure = true;
    
    private BigDecimal maximumTotalExposure = BigDecimal.valueOf(10000000);
}