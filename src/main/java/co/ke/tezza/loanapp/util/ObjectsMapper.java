package co.ke.tezza.loanapp.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import co.ke.tezza.loanapp.entity.MAmendmentApprovalHistory;
import co.ke.tezza.loanapp.entity.MAmendmentApprovalSteps;
import co.ke.tezza.loanapp.entity.MAmendmentConfiguration;
import co.ke.tezza.loanapp.entity.MApprovalSteps;
import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanAmendmentDetail;
import co.ke.tezza.loanapp.entity.MLoanAmendmentRequest;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanApprovalHistory;
import co.ke.tezza.loanapp.entity.MLoanComment;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.entity.MLoanStatement;
import co.ke.tezza.loanapp.entity.MOrg;
import co.ke.tezza.loanapp.entity.MPaymentApprovalConfiguration;
import co.ke.tezza.loanapp.entity.MPaymentApprovalHistory;
import co.ke.tezza.loanapp.entity.MPaymentGatewayConfig;
import co.ke.tezza.loanapp.entity.MPayments;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.ApprovalAction;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.repository.MLoanApprovalHistoryRepository;
import co.ke.tezza.loanapp.repository.MOrgRepository;
import co.ke.tezza.loanapp.repository.PaymentApprovalHistory;
import co.ke.tezza.loanapp.response.AmendmentDetailResponse;
import co.ke.tezza.loanapp.response.ApprovalStepsResponse;
import co.ke.tezza.loanapp.response.InstallmentResponse;
import co.ke.tezza.loanapp.response.LoanAmendmentApprovalStepsResponse;
import co.ke.tezza.loanapp.response.LoanAmendmentConfigResponse;
import co.ke.tezza.loanapp.response.LoanAmendmentRequestResponse;
import co.ke.tezza.loanapp.response.LoanCommentResponse;
import co.ke.tezza.loanapp.response.LoanProductConfigResponse;
import co.ke.tezza.loanapp.response.LoanStatementResponse;
import co.ke.tezza.loanapp.response.PaymentGateWayConfigResponse;
import co.ke.tezza.loanapp.response.PaymentResponse;
import co.ke.tezza.loanapp.response.User;
import co.ke.tezza.loanapp.service.BorrowersServices;
import co.ke.tezza.loanapp.service.LoanApplicationService;
import co.ke.tezza.loanapp.service.LoanProductConfigurationsService;


@Component
public class ObjectsMapper {

	@Autowired
	private LoanApplicationService loanApplicationService;
	@Autowired
	private LoanProductConfigurationsService loanProductConfigurationsService;

	@Autowired
	private Utils utils;

	@Autowired
	private PaymentApprovalHistory paymentApprovalHistory;
	@Autowired
	private MLoanApprovalHistoryRepository loanApprovalHistoryRepository;
	

	@Autowired
	private MOrgRepository orgRepository;

	@Autowired
	private BorrowersServices borrowersServices;
	

	public LoanAmendmentConfigResponse mapLoanAmendmentConfig(MAmendmentConfiguration entity) {
		if (entity == null) {
			return null;
		}
		LoanAmendmentConfigResponse dto = new LoanAmendmentConfigResponse();
		dto.setAmendmentConfigId(entity.getAmendmentConfigId());
		dto.setAD_Amendment_Configuration_UU(entity.getAD_Amendment_Configuration_UU());
		dto.setConfigurationName(entity.getConfigurationName());
		dto.setConfigurationDescription(entity.getConfigurationDescription());
		dto.setIsDefaultConfiguration(entity.getIsDefaultConfiguration());
		dto.setActive(entity.isActive());
		dto.setCreatedBy(entity.getCreatedBy());
		dto.setCreated(entity.getCreated());
		dto.setUpdatedBy(entity.getUpdatedBy());
		dto.setUpdated(entity.getUpdated());
		dto.setAdOrgID(entity.getAdOrgID());

		dto.setAmendmentType(entity.getAmendmentType());

		// Loan Products - convert to IDs and summary DTOs

		Set<LoanProductConfigResponse> applicableLoanProducts = new HashSet<>();
		if (entity.getApplicableLoanProducts() != null) {
			for (MLoanProductConfiguration config : entity.getApplicableLoanProducts()) {
				applicableLoanProducts.add(loanProductConfigurationsService.mappLoanProductConfig(config));
			}
		}
		dto.setApplicableLoanProducts(applicableLoanProducts);
		// Borrower Types
		if (entity.getAllowedBorrowerTypes() != null && Hibernate.isInitialized(entity.getAllowedBorrowerTypes())) {
			dto.setAllowedBorrowerTypes(new HashSet<>(entity.getAllowedBorrowerTypes()));
		} else {
			dto.setAllowedBorrowerTypes(new HashSet<>());
		}

		// ------------------------------------------
		// 3. Eligibility Criteria
		// ------------------------------------------
		dto.setRequireMinimumLoanAge(entity.getRequireMinimumLoanAge());
		dto.setMinimumLoanAgeDays(entity.getMinimumLoanAgeDays());
		dto.setRequireMaximumLoanAge(entity.getRequireMaximumLoanAge());
		dto.setMaximumLoanAgeDays(entity.getMaximumLoanAgeDays());
		dto.setRequireMinimumOutstandingBalance(entity.getRequireMinimumOutstandingBalance());
		dto.setMinimumOutstandingBalance(entity.getMinimumOutstandingBalance());
		dto.setRequireMaximumOutstandingBalance(entity.getRequireMaximumOutstandingBalance());
		dto.setMaximumOutstandingBalance(entity.getMaximumOutstandingBalance());
		dto.setRequireGoodRepaymentHistory(entity.getRequireGoodRepaymentHistory());
		dto.setMinimumOnTimePayments(entity.getMinimumOnTimePayments());
		dto.setAllowForOverdueLoans(entity.getAllowForOverdueLoans());
		dto.setMaximumDaysOverdueAllowed(entity.getMaximumDaysOverdueAllowed());
		dto.setRequireNoActiveLegalCases(entity.getRequireNoActiveLegalCases());

		// ------------------------------------------
		// 4. Common Amendment Limits
		// ------------------------------------------
		dto.setAllowGenericAmendment(entity.getAllowGenericAmendment());
		dto.setMinimumAmountChange(entity.getMinimumAmountChange());
		dto.setMaximumAmountChange(entity.getMaximumAmountChange());
		dto.setMinimumPercentageChange(entity.getMinimumPercentageChange());
		dto.setMaximumPercentageChange(entity.getMaximumPercentageChange());
		dto.setMinimumDaysChange(entity.getMinimumDaysChange());
		dto.setMaximumDaysChange(entity.getMaximumDaysChange());

		// ------------------------------------------
		// 5. Specific Amendment Type Limits
		// ------------------------------------------
		// TOP_UP
		dto.setAllowTopUp(entity.getAllowTopUp());
		dto.setMinimumTopUpAmount(entity.getMinimumTopUpAmount());
		dto.setMaximumTopUpAmount(entity.getMaximumTopUpAmount());
		dto.setMaximumTopUpPercentage(entity.getMaximumTopUpPercentage());

		// PRINCIPAL_REDUCTION
		dto.setAllowPrincipalReduction(entity.getAllowPrincipalReduction());
		dto.setMaximumPrincipalReductionPercentage(entity.getMaximumPrincipalReductionPercentage());

		// PRINCIPAL_RESTRUCTURING
		dto.setAllowPrincipalRestructuring(entity.getAllowPrincipalRestructuring());
		dto.setMaximumRestructuringAmount(entity.getMaximumRestructuringAmount());
		dto.setAllowExtendedRepaymentTerms(entity.getAllowExtendedRepaymentTerms());

		// TERM
		dto.setAllowTermExtension(entity.getAllowTermExtension());
		dto.setMinimumTermExtensionDays(entity.getMinimumTermExtensionDays());
		dto.setMaximumTermExtensionDays(entity.getMaximumTermExtensionDays());
		dto.setAllowTermReduction(entity.getAllowTermReduction());
		dto.setMinimumTermReductionDays(entity.getMinimumTermReductionDays());
		dto.setMaximumTermReductionDays(entity.getMaximumTermReductionDays());

		// GRACE_PERIOD_EXTENSION
		dto.setAllowGracePeriodExtension(entity.getAllowGracePeriodExtension());
		dto.setMaximumGracePeriodExtensionDays(entity.getMaximumGracePeriodExtensionDays());

		// INTEREST_RATE_CHANGE
		dto.setAllowInterestRateChange(entity.getAllowInterestRateChange());
		dto.setMinimumInterestRateChange(entity.getMinimumInterestRateChange());
		dto.setMaximumInterestRateChange(entity.getMaximumInterestRateChange());
		dto.setAllowRateDecrease(entity.getAllowRateDecrease());
		dto.setAllowRateIncrease(entity.getAllowRateIncrease());

		// FLAT_RATE_CHANGE
		dto.setAllowFlatRateChange(entity.getAllowFlatRateChange());
		dto.setMinimumFlatRateChange(entity.getMinimumFlatRateChange());
		dto.setMaximumFlatRateChange(entity.getMaximumFlatRateChange());

		// INTEREST_CALCULATION_CHANGE
		dto.setAllowInterestCalculationChange(entity.getAllowInterestCalculationChange());
		if (entity.getAllowedCalculationMethods() != null
				&& Hibernate.isInitialized(entity.getAllowedCalculationMethods())) {
			dto.setAllowedCalculationMethods(new HashSet<>(entity.getAllowedCalculationMethods()));
		} else {
			dto.setAllowedCalculationMethods(new HashSet<>());
		}

		// BORROWER_CHANGE
		dto.setAllowBorrowerChange(entity.getAllowBorrowerChange());
		dto.setRequireNewCreditCheck(entity.getRequireNewCreditCheck());
		dto.setRequireNewGuarantors(entity.getRequireNewGuarantors());

		// BORROWER_TYPE_CHANGE
		dto.setAllowBorrowerTypeChange(entity.getAllowBorrowerTypeChange());
		if (entity.getAllowedBorrowerTypeChanges() != null) {
			dto.setAllowedBorrowerTypeChanges(new HashSet<>(entity.getAllowedBorrowerTypeChanges()));
		} else {
			dto.setAllowedBorrowerTypeChanges(new HashSet<>());
		}

		// PRODUCT_CHANGE
		dto.setAllowProductChange(entity.getAllowProductChange());

		Set<LoanProductConfigResponse> allowedTargetProducts = new HashSet<>();
		if (entity.getAllowedTargetProducts() != null) {
			for (MLoanProductConfiguration config : entity.getAllowedTargetProducts()) {
				allowedTargetProducts.add(loanProductConfigurationsService.mappLoanProductConfig(config));
			}
		}
		dto.setAllowedTargetProducts(allowedTargetProducts);

		dto.setRequireProductCompatibilityCheck(entity.getRequireProductCompatibilityCheck());

		// CONTRACT_TERMS_CHANGE
		dto.setAllowContractTermsChange(entity.getAllowContractTermsChange());
		dto.setMaximumContractTermChanges(entity.getMaximumContractTermChanges());

		// COLLATERAL_CHANGE
		dto.setAllowCollateralChange(entity.getAllowCollateralChange());
		dto.setMaximumCollateralChanges(entity.getMaximumCollateralChanges());

		// GUARANTOR_CHANGE
		dto.setAllowGuarantorChange(entity.getAllowGuarantorChange());
		dto.setMaximumGuarantorChanges(entity.getMaximumGuarantorChanges());
		dto.setRequireNewGuarantorApproval(entity.getRequireNewGuarantorApproval());

		// SECURITY_ENHANCEMENT
		dto.setAllowSecurityEnhancement(entity.getAllowSecurityEnhancement());
		dto.setMinimumSecurityCoverageIncrease(entity.getMinimumSecurityCoverageIncrease());
		dto.setRequireLegalDocumentation(entity.getRequireLegalDocumentation());

		// PENALTY_WAIVER
		dto.setAllowPenaltyWaiver(entity.getAllowPenaltyWaiver());
		dto.setMaximumPenaltyWaiverPercentage(entity.getMaximumPenaltyWaiverPercentage());
		dto.setRequireJustificationDocument(entity.getRequireJustificationDocument());

		// FEE_WAIVER
		dto.setAllowFeeWaiver(entity.getAllowFeeWaiver());
		dto.setMaximumFeeWaiverAmount(entity.getMaximumFeeWaiverAmount());
		dto.setMaximumFeeWaiverPercentage(entity.getMaximumFeeWaiverPercentage());

		// PENALTY_RATE_CHANGE
		dto.setAllowPenaltyRateChange(entity.getAllowPenaltyRateChange());
		dto.setMinimumPenaltyRateChange(entity.getMinimumPenaltyRateChange());
		dto.setMaximumPenaltyRateChange(entity.getMaximumPenaltyRateChange());

		// REPAYMENT_SCHEDULE_CHANGE
		dto.setAllowRepaymentScheduleChange(entity.getAllowRepaymentScheduleChange());
		dto.setMaximumScheduleChanges(entity.getMaximumScheduleChanges());
		dto.setRequirePaymentCapacityAnalysis(entity.getRequirePaymentCapacityAnalysis());

		// INSTALLMENT_RESCHEDULING
		dto.setAllowInstallmentRescheduling(entity.getAllowInstallmentRescheduling());
		dto.setMaximumRescheduledInstallments(entity.getMaximumRescheduledInstallments());
		dto.setAllowInstallmentAmountChange(entity.getAllowInstallmentAmountChange());

		// PAYMENT_HOLIDAY
		dto.setAllowPaymentHoliday(entity.getAllowPaymentHoliday());
		dto.setMaximumPaymentHolidayDays(entity.getMaximumPaymentHolidayDays());
		dto.setAllowInterestCapitalization(entity.getAllowInterestCapitalization());

		// DOCUMENT_UPDATE
		dto.setAllowDocumentUpdate(entity.getAllowDocumentUpdate());
		dto.setMaximumDocumentUpdates(entity.getMaximumDocumentUpdates());
		dto.setRequireVersionControl(entity.getRequireVersionControl());

		// ADMINISTRATIVE_CORRECTION
		dto.setAllowAdministrativeCorrection(entity.getAllowAdministrativeCorrection());
		dto.setMaximumCorrectionAmount(entity.getMaximumCorrectionAmount());
		dto.setRequireAuditTrail(entity.getRequireAuditTrail());

		// EMERGENCY_AMENDMENT
		dto.setAllowEmergencyAmendment(entity.getAllowEmergencyAmendment());
		dto.setEmergencyApprovalThreshold(entity.getEmergencyApprovalThreshold());
		dto.setEmergencyResponseTimeHours(entity.getEmergencyResponseTimeHours());

		// LOAN_RENEWAL
		dto.setAllowLoanRenewal(entity.getAllowLoanRenewal());
		dto.setMaximumRenewalCount(entity.getMaximumRenewalCount());
		dto.setRequireFullReassessment(entity.getRequireFullReassessment());

		// LOAN_RESCHEDULING
		dto.setAllowLoanRescheduling(entity.getAllowLoanRescheduling());
		dto.setMaximumReschedulingMonths(entity.getMaximumReschedulingMonths());
		dto.setRequireDebtSustainabilityAnalysis(entity.getRequireDebtSustainabilityAnalysis());

		// FORBEARANCE
		dto.setAllowForbearance(entity.getAllowForbearance());
		dto.setMaximumForbearanceMonths(entity.getMaximumForbearanceMonths());
		dto.setRequireFinancialHardshipProof(entity.getRequireFinancialHardshipProof());

		// ------------------------------------------
		// 6. Approval Configuration
		// ------------------------------------------
		dto.setRequiresApproval(entity.getRequiresApproval());
		dto.setRequiredApprovalSteps(entity.getRequiredApprovalSteps());
		dto.setAllowAutoApproval(entity.getAllowAutoApproval());
		dto.setAutoApprovalMaximumAmount(entity.getAutoApprovalMaximumAmount());
		dto.setAutoApprovalMaximumTermChange(entity.getAutoApprovalMaximumTermChange());
		dto.setAutoApprovalMaximumInterestChange(entity.getAutoApprovalMaximumInterestChange());
		dto.setAllowQuickApproval(entity.getAllowQuickApproval());
		dto.setQuickApprovalMaximumAmount(entity.getQuickApprovalMaximumAmount());
		dto.setQuickApprovalSteps(entity.getQuickApprovalSteps());

		// ------------------------------------------
		// 7. Fees & Charges
		// ------------------------------------------
		dto.setChargeAmendmentFee(entity.getChargeAmendmentFee());
		dto.setAmendmentFeeAmount(entity.getAmendmentFeeAmount());
		dto.setAmendmentFeePercentage(entity.getAmendmentFeePercentage());
		dto.setAmendmentFeeType(entity.getAmendmentFeeType());
		dto.setChargeProcessingFee(entity.getChargeProcessingFee());
		dto.setProcessingFeeAmount(entity.getProcessingFeeAmount());
		dto.setChargeLegalFee(entity.getChargeLegalFee());
		dto.setLegalFeeAmount(entity.getLegalFeeAmount());
		dto.setFeeWaiverApprovalSteps(entity.getFeeWaiverApprovalSteps());

		// ------------------------------------------
		// 8. Documentation Requirements
		// ------------------------------------------
		dto.setRequireNewApplicationForm(entity.getRequireNewApplicationForm());
		dto.setRequireUpdatedContract(entity.getRequireUpdatedContract());
		dto.setRequireBorrowerConsent(entity.getRequireBorrowerConsent());
		dto.setRequireGuarantorConsent(entity.getRequireGuarantorConsent());
		dto.setRequireCollateralRevaluation(entity.getRequireCollateralRevaluation());
		dto.setRequireCreditReassessment(entity.getRequireCreditReassessment());
		dto.setRequireLegalReview(entity.getRequireLegalReview());
		dto.setRequireBoardApproval(entity.getRequireBoardApproval());
		dto.setBoardApprovalThresholdAmount(entity.getBoardApprovalThresholdAmount());

		// ------------------------------------------
		// 9. Notification & Communication
		// ------------------------------------------
		dto.setNotifyBorrower(entity.getNotifyBorrower());
		dto.setNotifyGuarantors(entity.getNotifyGuarantors());
		dto.setRequireBorrowerAcknowledgement(entity.getRequireBorrowerAcknowledgement());
		dto.setSendFormalLetter(entity.getSendFormalLetter());
		dto.setUpdateCreditBureau(entity.getUpdateCreditBureau());
		dto.setRequireRegulatoryNotification(entity.getRequireRegulatoryNotification());

		// ------------------------------------------
		// 10. Approval Workflow
		// ------------------------------------------
		Set<LoanAmendmentApprovalStepsResponse> approvalWorkflow = new HashSet<>();

		if (entity.getApprovalWorkflow() != null) {
			for (MAmendmentApprovalSteps step : entity.getApprovalWorkflow()) {
				approvalWorkflow.add(mapLoanAmendmentApprovalSteps(step));
			}
		}
		dto.setApprovalWorkflow(approvalWorkflow);

		// ------------------------------------------
		// 11. Risk Controls
		// ------------------------------------------
		dto.setTriggerRiskReview(entity.getTriggerRiskReview());
		dto.setRiskReviewThresholdAmount(entity.getRiskReviewThresholdAmount());
		dto.setRequireCommitteeApproval(entity.getRequireCommitteeApproval());
		dto.setCommitteeApprovalThresholdAmount(entity.getCommitteeApprovalThresholdAmount());
		dto.setRequireCEOApproval(entity.getRequireCEOApproval());
		dto.setCeoApprovalThresholdAmount(entity.getCeoApprovalThresholdAmount());
		dto.setAllowMultipleAmendments(entity.getAllowMultipleAmendments());
		dto.setMaximumAmendmentsPerLoan(entity.getMaximumAmendmentsPerLoan());
		dto.setCoolingPeriodDays(entity.getCoolingPeriodDays());

		// ------------------------------------------
		// 12. System & Integration
		// ------------------------------------------
		dto.setAutoUpdateLoanSystem(entity.getAutoUpdateLoanSystem());
		dto.setGenerateAmendmentNumber(entity.getGenerateAmendmentNumber());
		dto.setAmendmentNumberPrefix(entity.getAmendmentNumberPrefix());
		dto.setCreateAuditTrail(entity.getCreateAuditTrail());
		dto.setUpdateCollateralRegistry(entity.getUpdateCollateralRegistry());
		dto.setSyncWithCoreBanking(entity.getSyncWithCoreBanking());

		// ------------------------------------------
		// 13. Validation Rules
		// ------------------------------------------
		dto.setValidateCreditLimit(entity.getValidateCreditLimit());
		dto.setValidateDebtServiceRatio(entity.getValidateDebtServiceRatio());
		dto.setMaximumDebtServiceRatio(entity.getMaximumDebtServiceRatio());
		dto.setValidateTotalExposure(entity.getValidateTotalExposure());
		dto.setMaximumTotalExposure(entity.getMaximumTotalExposure());

		return dto;

	}

	public LoanAmendmentApprovalStepsResponse mapLoanAmendmentApprovalSteps(MAmendmentApprovalSteps step) {
		if (step == null) {
			return null;
		}
		LoanAmendmentApprovalStepsResponse response = new LoanAmendmentApprovalStepsResponse();
		response.setId(step.getId());
		response.setNextRole(utils.mapRole(step.getNextRole()));
		response.setRequireDigitalSignature(step.getRequireDigitalSignature());
		response.setRequireDocumentReview(step.getRequireDocumentReview());
		response.setRequiredRole(utils.mapRole(step.getRequiredRole()));
		response.setStepNumber(step.getStepNumber());
		response.setTrigureStatus(step.getTrigureStatus());
		Set<User> responsiblePersons = new HashSet<>();
		if (!step.getResponsiblePersons().isEmpty()) {
			for (MUser user : step.getResponsiblePersons()) {
				responsiblePersons.add(utils.mapUserBreif(user));
			}

		}
		response.setResponsiblePersons(responsiblePersons);

		return response;
	}

	public LoanAmendmentRequestResponse mapLoanAmendmentRequest(MLoanAmendmentRequest r) {
		if (r == null) {
			return null;
		}
		LoanAmendmentRequestResponse response = new LoanAmendmentRequestResponse();
		response.setAmendmentRequestId(r.getAmendmentRequestId());
		if (!r.getAmendmentDetails().isEmpty()) {
			List<AmendmentDetailResponse> details = new ArrayList<>();
			for (MLoanAmendmentDetail d : r.getAmendmentDetails()) {
				details.add(mapAmendmentDetails(d));

			}
			response.setAmendments(details);

		}
		response.setLoanToAmend(loanApplicationService.mapLoanApplication(r.getLoanToAmend()));
		response.setProcessedBy(utils.mapUserBreif(r.getProcessedBy()));
		response.setRequestedBy(utils.mapUserBreif(r.getRequestedBy()));
		response.setRequestReason(r.getRequestReason());
		response.setDocStatus(r.getDocStatus());
		response.setApprovalStage(r.getApprovalStage());

		return response;

	}

	public AmendmentDetailResponse mapAmendmentDetails(MLoanAmendmentDetail d) {
		if (d == null) {
			return null;
		}

		AmendmentDetailResponse response = new AmendmentDetailResponse();
		response.setAmendmentConfigId(d.getAmendmentConfiguration().getAmendmentConfigId());
		response.setAmendmentDetailId(d.getAmendmentDetailId());
		response.setAmendmentReason(d.getAmendmentReason());
		response.setAmendmentType(d.getAmendmentType());
		response.setEffectiveDate(d.getEffectiveDate());
		response.setNewFlatRateAmount(d.getNewFlatRateAmount());
		response.setNewInterestRate(d.getNewInterestRate());
		response.setNewLoanProduct(loanProductConfigurationsService.mappLoanProductConfig(d.getNewLoanProduct()));
		response.setNewPrincipalAmount(d.getNewPrincipalAmount());
		response.setNewTermInDays(d.getNewTermInDays());
		response.setApprovalStage(d.getApprovalStage());
		response.setDocStatus(d.getDocStatus());
		response.setCurrentApprovalLevel(d.getCurrentApprovalLevel());
		response.setRejected(d.isReject());
		response.setRejectedDate(d.getRejectedDate());
		response.setRejectionReason(d.getRejectReason());
		return response;
	}

	public PaymentResponse mapPayment(MPayments p) {
		if (p == null) {
			return null;
		}
		PaymentResponse response = new PaymentResponse();
		response.setAmount(p.getAmount());
		response.setLoan(loanApplicationService.mapLoanApplication(p.getLoan()));
		Set<InstallmentResponse> installments = new HashSet<>();
		if (p.getInstallments().size() > 0) {
			for (MInstallments installment : p.getInstallments()) {
				installments.add(mapInstallments(installment));

			}
		}
		response.setWriteOffWaiverReason(p.getWriteOffWaiverReason());
		response.setApprovedBy(utils.mapUserBreif(p.getApprovedBy()));
		response.setRejectedBy(utils.mapUserBreif(p.getRejectedBy()));
		response.setInstallments(installments);
		response.setPaymentId(p.getPaymentId());
		response.setActive(p.isActive());
		response.setPaymentDate(p.getPaymentDate());
		response.setReference(p.getReference());
		response.setPaymentMethod(p.getPaymentMethod());
		response.setDocStatus(p.getDocStatus());
		response.setApprovalStage(p.getApprovalStage());
		response.setPaymentDateTime(p.getPaymentDateTime());
		response.setCreated(p.getCreated());
		response.setSecurityPayment(p.isSecurityPayment());
		response.setExpectedAllocationDate(p.getExpectedAllocationDate());
		response.setPaymentMode(p.getPaymentMode());
		response.setReasonForRejection(p.getReasonForRejection());
		if (p.getReceiptedBy() != null) {
			response.setReceiptedBy(utils.mapUser(p.getReceiptedBy()));
		}
		response.setDocStatus(p.getDocStatus());
		response.setApprovalStage(p.getApprovalStage());
		response.setActive(p.isActive());
		response.setCreated(p.getCreated());

		return response;

	}

	public PaymentGateWayConfigResponse mapPaymentGateWayConfig(MPaymentGatewayConfig c) {
		if (c == null) {
			return null;
		}
		PaymentGateWayConfigResponse response = new PaymentGateWayConfigResponse();
		response.setActive(c.isActive());
		response.setBusinessShortCode(c.getBusinessShortCode());
		response.setCallBackUrl(c.getCallBackUrl());
		response.setMpesaApiKey(c.getMpesaApiKey());
		response.setMpesaApiSecret(c.getMpesaApiSecrete());
		response.setMpesaOrganizationShortCode(c.getMpesaOrganizationShortCode());
		response.setMpesaProductionAllowed(c.isMpesaProductionAllowed());
		response.setMpesaProductionBaseUrl(c.getMpesaProductionBaseUrl());
		response.setMpesaTestBaseUrl(c.getMpesaTestBaseUrl());
		response.setPartyB(c.getPartyB());
		response.setPassKey(c.getPassKey());
		response.setPaymentGatewayConfigId(c.getPaymentGatewayConfigId());
		response.setTransactionType(c.getTransactionType()); // ADD THIS LINE
		response.setStkCallBackUrl(c.getStkCallBackUrl());
		response.setValidationUrl(c.getValidationUrl());

		return response;
	}

	public LoanCommentResponse mapCardex(MLoanComment c) {
		if (c == null) {
			return null;
		}

		LoanCommentResponse response = new LoanCommentResponse();

		// Map related entities
		response.setLoan(loanApplicationService.mapLoanApplication(c.getLoan()));
		response.setInstallment(mapInstallments(c.getInstallment()));

		// Direct field mappings
		response.setCommentId(c.getCommentId());
		response.setNotes(c.getNotes());
		response.setStatus(c.getStatus());
		response.setActionDate(c.getActionDate());
		response.setCallDateTime(c.getCallDateTime());
		response.setNextCallDate(c.getNextCallDate());
		response.setNotesTakenBy(utils.mapUserBreif(c.getNotesTakenBy()));
		response.setPriority(c.getPriority());
		response.setContactMethod(c.getContactMethod());
		response.setCallDuration(c.getCallDuration());
		response.setPromiseAmount(c.getPromiseAmount());
		return response;
	}

	public LoanStatementResponse mapLoanStatement(MLoanStatement s) {
		if (s == null) {
			return null;
		}

		LoanStatementResponse response = new LoanStatementResponse();

		// Basic field mappings
		response.setStatementId(s.getStatementId());
		response.setTransactionType(s.getTransactionType());
		response.setTransactionRef(s.getTransactionRef());
		response.setTransactionDate(s.getTransactionDate());
		response.setDescription(s.getDescription());
		response.setDebitAmount(s.getDebitAmount());
		response.setCreditAmount(s.getCreditAmount());
		response.setBalance(s.getBalance());
		response.setIsReversed(s.getIsReversed());
		response.setNotes(s.getNotes());

		// Related entities
		if (s.getLoan() != null) {
			response.setLoan(loanApplicationService.mapLoanApplication(s.getLoan()));
		}

		return response;
	}

	public InstallmentResponse mapInstallments(MInstallments c) {
		if (c == null) {
			return null;
		}
		InstallmentResponse response = new InstallmentResponse();
		response.setAmount(c.getAmount());
		response.setBalance(c.getBalance());
		response.setGracePeriod(c.getGracePeriod());
		response.setInstallmentId(c.getInstallmentId());
		response.setInterestEarned(c.getInterestEarned());
		response.setLastReminderSent(c.getLastReminderSent());
		response.setLoan(loanApplicationService.mapLoanApplication(c.getLoan()));
		response.setNoOfRemindersSent(c.getNoOfRemindersSent());
		response.setPenaltyEarned(c.getPenaltyEarned());
		response.setPeriodEnd(c.getPeriodEnd());
		response.setPeriodStart(c.getPeriodStart());
		return response;
	}

	public void recordLoanApplicationApprovalHistory(MLoanApplication detail, boolean approved,
			MApprovalSteps currentApprovalLevel, Integer maximumApprovalLevel, DocStatus previousDocStatus,
			ApprovalStage previousApprovalStage, MRoles previousRequiredRole, DocStatus newDocStatus,
			ApprovalStage newApprovalStage) {
		MLoanApprovalHistory history = new MLoanApprovalHistory();

		// Set action based on approval status
		history.setAction(approved ? ApprovalAction.APPROVE : ApprovalAction.REJECT);

		// Set dates
		Date currentDate = new Date();
		history.setActionDate(currentDate);

		// Use the detail's creation date as received date
		Date receivedDate = detail.getCreated() != null ? detail.getCreated() : currentDate;
		history.setReceivedDate(receivedDate);

		// Set user and role
		MUser loggedInUser = utils.getLoggedInUser();
		history.setActionedBy(loggedInUser);

		// Set required role from parameter
		if (previousRequiredRole != null) {
			history.setRequiredRole(previousRequiredRole);
		} else if (currentApprovalLevel != null && currentApprovalLevel.getRoleInvolved() != null) {
			// Fallback to current approval level's role
			history.setRequiredRole(currentApprovalLevel.getRoleInvolved());
		} else {
			// Fallback to logged-in user's role
			Set<MRoles> userRoles = utils.getLogedInUserRoles();
			if (userRoles != null && !userRoles.isEmpty()) {
				history.setRequiredRole(userRoles.iterator().next());
			}
		}

		// Set loan association
		if (detail != null) {
			history.setLoan(detail);
		}

		// Set the amendment detail

		// Set step information
		Integer stepNumber = null;
		if (currentApprovalLevel != null) {
			stepNumber = currentApprovalLevel.getStep();
		} else if (detail.getCurrentApprovalLevel() != null) {
			// For rejection cases where we might not have currentApprovalLevel
			stepNumber = detail.getCurrentApprovalLevel() - 1; // Previous step
		}
		history.setStepNumber(stepNumber);

		// Set previous and new statuses from parameters
		history.setPreviousDocStatus(previousDocStatus);
		history.setPreviousApprovalStage(previousApprovalStage);
		history.setNewDocStatus(newDocStatus);
		history.setNewApprovalStage(newApprovalStage);

		// Set processing time (calculate time since the detail was created/request
		// received)
		if (receivedDate != null) {
			long diffInMillis = currentDate.getTime() - receivedDate.getTime();
			int hours = diffInMillis > 0 ? (int) (diffInMillis / (1000 * 60 * 60)) : 0;
			history.setProcessingTimeHours(hours);
		}

		// Set audit information
		history.setDocStatus(DocStatus.CO); // History records are always completed
		history.setDigitalSignature(generateDigitalSignature(loggedInUser, detail));
		history.setIpAddress(getClientIpAddress());
		history.setUserAgent(getUserAgent());

		// Set comments based on approval status
		if (!approved) {
			history.setComments("Loan Application rejected");

		} else {
			String stepInfo = stepNumber != null ? "at step " + stepNumber : "";
			history.setComments("Loan Application approved " + stepInfo);

		}

		// Save the history record
		try {
			loanApprovalHistoryRepository.save(history);
		} catch (Exception e) {
			// Log the error but don't throw to prevent interrupting the main flow
			System.err.println("Error saving loan application approval history: " + e.getMessage());
			// Consider using a proper logger: log.error("Error saving amendment approval
			// history", e);
		}
	}

	

	public void recordPaymentApprovalHistory(MPayments detail, boolean approved, MApprovalSteps currentApprovalLevel,
			Integer maximumApprovalLevel, DocStatus previousDocStatus, ApprovalStage previousApprovalStage,
			MRoles previousRequiredRole, DocStatus newDocStatus, ApprovalStage newApprovalStage, String paymentMethod) {
		MPaymentApprovalHistory history = new MPaymentApprovalHistory();

		// Set action based on approval status
		history.setAction(approved ? ApprovalAction.APPROVE : ApprovalAction.REJECT);

		// Set dates
		Date currentDate = new Date();
		history.setActionDate(currentDate);

		// Use the detail's creation date as received date
		Date receivedDate = detail.getCreated() != null ? detail.getCreated() : currentDate;
		history.setReceivedDate(receivedDate);

		// Set user and role
		MUser loggedInUser = utils.getLoggedInUser();
		history.setActionedBy(loggedInUser);

		// Set required role from parameter
		if (previousRequiredRole != null) {
			history.setRequiredRole(previousRequiredRole);
		} else if (currentApprovalLevel != null && currentApprovalLevel.getRoleInvolved() != null) {
			// Fallback to current approval level's role
			history.setRequiredRole(currentApprovalLevel.getRoleInvolved());
		} else {
			// Fallback to logged-in user's role
			Set<MRoles> userRoles = utils.getLogedInUserRoles();
			if (userRoles != null && !userRoles.isEmpty()) {
				history.setRequiredRole(userRoles.iterator().next());
			}
		}

		// Set loan association
		if (detail != null) {
			history.setPayment(detail);
		}

		// Set the amendment detail

		// Set step information
		Integer stepNumber = null;
		if (currentApprovalLevel != null) {
			stepNumber = currentApprovalLevel.getStep();
		} else if (detail.getCurrentApprovalLevel() != null) {
			// For rejection cases where we might not have currentApprovalLevel
			stepNumber = detail.getCurrentApprovalLevel() - 1; // Previous step
		}
		history.setStepNumber(stepNumber);

		// Set previous and new statuses from parameters
		history.setPreviousDocStatus(previousDocStatus);
		history.setPreviousApprovalStage(previousApprovalStage);
		history.setNewDocStatus(newDocStatus);
		history.setNewApprovalStage(newApprovalStage);

		// Set processing time (calculate time since the detail was created/request
		// received)
		if (receivedDate != null) {
			long diffInMillis = currentDate.getTime() - receivedDate.getTime();
			int hours = diffInMillis > 0 ? (int) (diffInMillis / (1000 * 60 * 60)) : 0;
			history.setProcessingTimeHours(hours);
		}

		// Set audit information
		history.setDocStatus(DocStatus.CO); // History records are always completed
		history.setDigitalSignature(generatePaymentDigitalSignature(loggedInUser, detail));
		history.setIpAddress(getClientIpAddress());
		history.setUserAgent(getUserAgent());

		// Set comments based on approval status
		if (!approved) {
			history.setComments(paymentMethod + " rejected");

		} else {
			String stepInfo = stepNumber != null ? "at step " + stepNumber : "";
			history.setComments(paymentMethod + " approved " + stepInfo);

		}

		// Save the history record
		try {
			paymentApprovalHistory.save(history);
		} catch (Exception e) {
			// Log the error but don't throw to prevent interrupting the main flow
			System.err.println("Error saving payment approval history: " + e.getMessage());
			// Consider using a proper logger: log.error("Error saving amendment approval
			// history", e);
		}
	}

	private String generateDigitalSignature(MUser user, MLoanApplication detail) {
		if (user == null)
			return null;
		// Generate a simple digital signature
		return user.getUserId() + "-" + detail.getLoanApplicationId() + "-" + System.currentTimeMillis();
	}

	private String generatePaymentDigitalSignature(MUser user, MPayments detail) {
		if (user == null)
			return null;
		// Generate a simple digital signature
		return user.getUserId() + "-" + detail.getPaymentId() + "-" + System.currentTimeMillis();
	}

	private String getClientIpAddress() {
		// Get client IP address from request context
		// This depends on your framework (Spring Security, Servlet API, etc.)
		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
					.getRequest();
			return request.getRemoteAddr();
		} catch (Exception e) {
			return "127.0.0.1";
		}
	}

	private String getUserAgent() {
		// Get user agent from request
		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
					.getRequest();
			return request.getHeader("User-Agent");
		} catch (Exception e) {
			return "Unknown";
		}
	}

	public MApprovalSteps getCurrentApprovalLevel(Integer currentApprovalStep, MLoanProductConfiguration config) {
		Set<MApprovalSteps> steps = config.getApprovalLevels();
		if (!steps.isEmpty()) {
			for (MApprovalSteps step : steps) {
				if (step.getStep() == currentApprovalStep) {
					return step;
				}
			}
		}
		return null;
	}

	

	public MApprovalSteps getCurrentPaymentApprovalLevel(Integer currentApprovalStep,
			MPaymentApprovalConfiguration config) {
		Set<MApprovalSteps> steps = config.getApprovalLevels();
		if (!steps.isEmpty()) {
			for (MApprovalSteps step : steps) {
				if (step.getStep() == currentApprovalStep) {
					return step;
				}
			}
		}
		return null;
	}

}
