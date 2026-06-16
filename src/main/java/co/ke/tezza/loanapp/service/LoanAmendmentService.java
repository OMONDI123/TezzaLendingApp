package co.ke.tezza.loanapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MAmendmentApprovalSteps;
import co.ke.tezza.loanapp.entity.MAmendmentConfiguration;
import co.ke.tezza.loanapp.entity.MLoanAmendmentDetail;
import co.ke.tezza.loanapp.entity.MLoanAmendmentRequest;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.FlatRateType;
import co.ke.tezza.loanapp.enums.InterestCalculationMethodEnum;
import co.ke.tezza.loanapp.enums.LoanRepaymentStatus;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.LoanAmendmentRequest;
import co.ke.tezza.loanapp.model.LoanAmendmentRequest.AmendmentDetail;
import co.ke.tezza.loanapp.repository.AmendmentConfigurationRepository;
import co.ke.tezza.loanapp.repository.LoanAmendmentDetailRepository;
import co.ke.tezza.loanapp.repository.LoanAmendmentRequestRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.repository.LoanProductConfigRepository;
import co.ke.tezza.loanapp.response.LoanAmendmentRequestResponse;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanAmendmentService {

	@Autowired
	private AmendmentConfigurationRepository amendmentConfigurationRepository;
	@Autowired
	private LoanApplicationRepository loanApplicationRepository;
	@Autowired
	private Utils utils;
	@Autowired
	private LoanProductConfigRepository loanProductConfigRepository;
	@Autowired
	private LoanAmendmentRequestRepository loanAmendmentRequestRepository;
	@Autowired
	private ObjectsMapper objectsMapper;
	@Autowired
	private LoanAmendmentDetailRepository loanAmendmentDetailRepository;
	@Autowired private LoanAmendmentApprovalService loanAmendmentApprovalService;

	public ResponseEntity<LoanAmendmentRequestResponse> requestLoanAmendment(LoanAmendmentRequest request) {
		validateAmendmentRequest(request);
		MLoanApplication loan = loanApplicationRepository.findById(request.getLoanId())
				.orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + request.getLoanId()));
		if(loan.getDocStatus().equals(DocStatus.AMENDED)) {
			throw new SetUpExceptions("This Loan has already been amended.");
		}
		if(loan.getRepaymentStatus().equals(LoanRepaymentStatus.PAID)) {
			throw new SetUpExceptions("Amendments are only applicable to loans with balances");
		}

		MLoanAmendmentRequest amendRequest = new MLoanAmendmentRequest();
		amendRequest.setRequestedBy(utils.getLoggedInUser());
		amendRequest.setRequestReason(request.getRequestReason());
		amendRequest.setLoanToAmend(loan);
		amendRequest.setCurrentApprovalLevel(1);
		List<MLoanAmendmentDetail> amendmentDetails = new ArrayList<>();
		if (request.getAmendments().isEmpty()) {
			throw new SetUpExceptions("Amendment Details can never be empty.");
		}
		for (AmendmentDetail model : request.getAmendments()) {
			MLoanAmendmentDetail detail = new MLoanAmendmentDetail();
			MLoanProductConfiguration newLoanProduct = null;
			if (model.getNewLoanProductId() != null && model.getNewLoanProductId() > 0) {
				newLoanProduct = loanProductConfigRepository.findById(model.getNewLoanProductId()).orElse(null);
				detail.setNewLoanProduct(newLoanProduct);

			}
			detail.setNewPrincipalAmount(model.getNewPrincipalAmount());
			detail.setNewInterestRate(model.getNewInterestRate());
			detail.setNewFlatRateAmount(model.getNewFlatRateAmount());
			detail.setNewTermInDays(model.getNewTermInDays());
			detail.setEffectiveDate(model.getEffectiveDate());
			detail.setAmendmentReason(model.getAmendmentReason());
			MAmendmentConfiguration config = amendmentConfigurationRepository.findById(model.getAmendmentConfigId())
					.orElseThrow(() -> new IllegalArgumentException(
							"Amendment configuration not found: " + model.getAmendmentConfigId()));
			detail.setAmendmentConfiguration(config);
			detail.setAmendmentType(config.getAmendmentType());
			detail.setCurrentApprovalLevel(1);
			detail.setApprovalStage(ApprovalStage.SUBMITTED);
			detail.setDocStatus(DocStatus.SUBMITTED);
			detail.setLoanToAmendId(request.getLoanId());
			amendmentDetails.add(detail);

		}
		amendRequest.setAmendmentDetails(amendmentDetails);
		amendRequest.setDocStatus(DocStatus.SUBMITTED);
		amendRequest.setApprovalStage(ApprovalStage.SUBMITTED);
		String debtType = loan.getLoanProductConfiguration().getIsDebtProduct() ? "Debt" : "Loan";

		MLoanAmendmentRequest savedRequest = loanAmendmentRequestRepository.save(amendRequest);
		if (!savedRequest.getAmendmentDetails().isEmpty()) {
			for (MLoanAmendmentDetail detail : savedRequest.getAmendmentDetails()) {
				detail.setAmendmentRequestId(savedRequest.getAmendmentRequestId());
				loanAmendmentDetailRepository.save(detail);
				MAmendmentApprovalSteps currentApprovalLevel = loanAmendmentApprovalService.getCurrentApprovalLevel(1, detail.getAmendmentConfiguration());

				loanAmendmentApprovalService.notifyNextApprovers(currentApprovalLevel, utils.getLoggedInUser(),
						"AMD-" + detail.getAmendmentDetailId(), new Date(), detail.getAmendmentReason(),
						0);
			}
		}
		return new ResponseEntity<LoanAmendmentRequestResponse>(
				debtType + " Amendment Request for " + debtType + " reference number" + loan.getDocumentNo()
						+ " has been submitted for approval.",
				200, objectsMapper.mapLoanAmendmentRequest(savedRequest));

	}

	public Page<LoanAmendmentRequestResponse> getAmendmentRequests(String status, String searchTerm, Date dateFrom,
			Date dateTo, int page, int size) {

		Page<MLoanAmendmentRequest> result = null;
		if (utils.isAdmin()) {
			result = getAllAmendmentRequests(status, searchTerm, dateFrom, dateTo, page, size);
		} else {
			result = getAllAmendmentRequestsByRquestedBy(status, searchTerm, dateFrom, dateTo, page, size);
		}
		List<LoanAmendmentRequestResponse> responses = result.getContent().stream()
				.map(objectsMapper::mapLoanAmendmentRequest).collect(Collectors.toList());

		return new PageImpl<>(responses, PageRequest.of(page, size), result.getTotalElements());
	}

	public Page<MLoanAmendmentRequest> getAllAmendmentRequests(String status, String searchTerm, Date dateFrom,
			Date dateTo, int page, int size) {
		Page<MLoanAmendmentRequest> result = null;
		PageRequest pageRequest = PageRequest.of(page, size);
		long adOrgId = utils.getAD_Org_ID();

		if (status != null && !status.isEmpty()) {
			DocStatus docStatus = DocStatus.fromValue(status);
			if (searchTerm != null && !searchTerm.isEmpty()) {
				result = loanAmendmentRequestRepository.searchAllRequests(searchTerm, docStatus, dateFrom, dateTo, true,
						utils.getAD_Org_ID(), pageRequest);

			} else {
				result = loanAmendmentRequestRepository
						.findByDocStatusAndIsActiveAndAdOrgIDAndCreatedBetweenOrderByCreatedDesc(docStatus, true, size,
								dateFrom, dateTo, pageRequest);
			}

		} else {
			if (searchTerm != null && !searchTerm.isEmpty()) {
				result = loanAmendmentRequestRepository.searchAllRequests(searchTerm, null, dateFrom, dateTo, true,
						utils.getAD_Org_ID(), pageRequest);

			} else {
				result = loanAmendmentRequestRepository.findByIsActiveAndAdOrgIDAndCreatedBetweenOrderByCreatedDesc(
						true, adOrgId, dateFrom, dateTo, pageRequest);
			}
		}
		return result;

	}

	public Page<MLoanAmendmentRequest> getAllAmendmentRequestsByRquestedBy(String status, String searchTerm,
			Date dateFrom, Date dateTo, int page, int size) {
		Page<MLoanAmendmentRequest> result = null;
		PageRequest pageRequest = PageRequest.of(page, size);
		long adOrgId = utils.getAD_Org_ID();
		MUser currentUser = utils.getLoggedInUser();
		if (status != null && !status.isEmpty()) {
			DocStatus docStatus = DocStatus.fromValue(status);
			if (searchTerm != null && !searchTerm.isEmpty()) {
				result = loanAmendmentRequestRepository.searchUserRequests(currentUser, searchTerm, docStatus, dateFrom,
						dateTo, true, adOrgId, pageRequest);

			} else {
				result = loanAmendmentRequestRepository
						.findByRequestedByAndDocStatusAndIsActiveAndAdOrgIDAndCreatedBetweenOrderByCreatedDesc(
								currentUser, docStatus, true, adOrgId, dateFrom, dateTo, pageRequest);
			}

		} else {
			if (searchTerm != null && !searchTerm.isEmpty()) {
				result = loanAmendmentRequestRepository.searchUserRequests(currentUser, searchTerm, null, dateFrom,
						dateTo, true, adOrgId, pageRequest);

			} else {
				result = loanAmendmentRequestRepository
						.findByRequestedByAndIsActiveAndAdOrgIDAndCreatedBetweenOrderByCreatedDesc(currentUser, true,
								adOrgId, dateFrom, dateTo, pageRequest);
			}

		}
		return result;
	}

	@Transactional(readOnly = true)
	public void validateAmendmentRequest(LoanAmendmentRequest request) {
		// Basic validations
		if (request.getLoanId() == null) {
			throw new IllegalArgumentException("Loan ID is required");
		}

		if (request.getAmendments() == null || request.getAmendments().isEmpty()) {
			throw new IllegalArgumentException("At least one amendment must be provided");
		}

		// Get loan
		MLoanApplication loan = loanApplicationRepository.findById(request.getLoanId())
				.orElseThrow(() -> new IllegalArgumentException("Loan not found with ID: " + request.getLoanId()));

		Long currentProductId = loan.getLoanProductConfiguration().getLoanProductConfigId();
		BigDecimal currentPrincipal = loan.getApprovedAmount();
		BigDecimal currentInterestRate = BigDecimal.ZERO;
		if (loan.getLoanProductConfiguration().getInterestCalculationMethod() != null
				&& loan.getLoanProductConfiguration().getInterestCalculationMethod()
						.equals(InterestCalculationMethodEnum.FLAT)
				&& loan.getLoanProductConfiguration().getFlatRateType().equals(FlatRateType.AMOUNT_BASED)) {
			currentInterestRate = loan.getInteretsFlatRateAmount();
		} else if (loan.getLoanProductConfiguration().getInterestCalculationMethod() != null
				&& loan.getLoanProductConfiguration().getInterestCalculationMethod()
						.equals(InterestCalculationMethodEnum.FLAT)
				&& loan.getLoanProductConfiguration().getFlatRateType().equals(FlatRateType.PERCENTAGE_BASED)) {
			currentInterestRate = loan.getInteretsFlatRate();
		} else {
			if (loan.getLoanProductConfiguration().getInterestFrequency() != null) {
				switch (loan.getLoanProductConfiguration().getInterestFrequency()) {
				case DAILY:
					currentInterestRate = loan.getDailyInterestRate();
				case WEEKLY:
					currentInterestRate = loan.getWeeklyInterestRate();
				case MONTHLY:
					currentInterestRate = loan.getMonthlyInterestRate();
				case YEARLY:
					currentInterestRate = loan.getAnnualInterestRate();

				default:
					currentInterestRate = BigDecimal.ZERO;

				}
			}

		}

		// Validate each amendment
		for (LoanAmendmentRequest.AmendmentDetail detail : request.getAmendments()) {
			validateAmendmentDetail(detail, currentProductId, currentPrincipal, currentInterestRate);
		}
	}

	private void validateAmendmentDetail(LoanAmendmentRequest.AmendmentDetail detail, Long currentProductId,
			BigDecimal currentPrincipal, BigDecimal currentInterestRate) {

		// Get config
		MAmendmentConfiguration config = amendmentConfigurationRepository.findById(detail.getAmendmentConfigId())
				.orElseThrow(() -> new IllegalArgumentException(
						"Amendment configuration not found with ID: " + detail.getAmendmentConfigId()));

		// 1. Validate loan product is allowed
		validateLoanProductAllowed(config, currentProductId);

		// 2. Validate principal amount if provided
		if (detail.getNewPrincipalAmount() != null) {
			validatePrincipalAmount(detail.getNewPrincipalAmount(), currentPrincipal, config);
		}

		// 3. Validate interest rate if provided
		if (detail.getNewInterestRate() != null) {
			validateInterestRate(detail.getNewInterestRate(), currentInterestRate, config);
		}

		// 4. Validate new loan product if changing product
		if (detail.getNewLoanProductId() != null) {
			validateNewLoanProduct(detail.getNewLoanProductId(), config);
		}
	}

	private void validateLoanProductAllowed(MAmendmentConfiguration config, Long loanProductId) {
		// If specific products are configured, check if current product is allowed
		if (config.getApplicableLoanProducts() != null && !config.getApplicableLoanProducts().isEmpty()) {
			boolean isAllowed = config.getApplicableLoanProducts().stream()
					.anyMatch(product -> product.getLoanProductConfigId().equals(loanProductId));

			if (!isAllowed) {
				throw new IllegalArgumentException(
						"Loan product ID " + loanProductId + " is not allowed for this amendment");
			}
		}
	}

	private void validatePrincipalAmount(BigDecimal newPrincipal, BigDecimal currentPrincipal,
			MAmendmentConfiguration config) {

		BigDecimal changeAmount = newPrincipal.subtract(currentPrincipal);

		// Check amount limits
		if (config.getMinimumAmountChange() != null
				&& changeAmount.abs().compareTo(config.getMinimumAmountChange()) < 0) {
			throw new IllegalArgumentException("Principal change amount " + changeAmount.abs()
					+ " is less than minimum allowed " + config.getMinimumAmountChange());
		}

		if (config.getMaximumAmountChange() != null
				&& changeAmount.abs().compareTo(config.getMaximumAmountChange()) > 0) {
			throw new IllegalArgumentException("Principal change amount " + changeAmount.abs()
					+ " exceeds maximum allowed " + config.getMaximumAmountChange());
		}

		// Check percentage limits
		if (currentPrincipal.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal changePercentage = changeAmount.divide(currentPrincipal, 4, RoundingMode.HALF_UP)
					.multiply(BigDecimal.valueOf(100)).abs();

			if (config.getMinimumPercentageChange() != null
					&& changePercentage.compareTo(config.getMinimumPercentageChange()) < 0) {
				throw new IllegalArgumentException("Principal change percentage " + changePercentage
						+ "% is less than minimum allowed " + config.getMinimumPercentageChange() + "%");
			}

			if (config.getMaximumPercentageChange() != null
					&& changePercentage.compareTo(config.getMaximumPercentageChange()) > 0) {
				throw new IllegalArgumentException("Principal change percentage " + changePercentage
						+ "% exceeds maximum allowed " + config.getMaximumPercentageChange() + "%");
			}
		}
	}

	private void validateInterestRate(BigDecimal newInterestRate, BigDecimal currentInterestRate,
			MAmendmentConfiguration config) {

		if (!config.getAllowInterestRateChange()
				&& (newInterestRate != null && newInterestRate.compareTo(BigDecimal.ZERO) > 0)) {
			throw new IllegalArgumentException("Interest rate changes are not allowed");
		}

		BigDecimal rateChange = newInterestRate.subtract(currentInterestRate);

		// Check if rate increase/decrease is allowed
		if (rateChange.compareTo(BigDecimal.ZERO) > 0 && !config.getAllowRateIncrease()) {
			throw new IllegalArgumentException("Interest rate increases are not allowed");
		}

		if (rateChange.compareTo(BigDecimal.ZERO) < 0 && !config.getAllowRateDecrease()) {
			throw new IllegalArgumentException("Interest rate decreases are not allowed");
		}

		// Check change limits
		if (config.getMinimumInterestRateChange() != null
				&& rateChange.abs().compareTo(config.getMinimumInterestRateChange()) < 0) {
			throw new IllegalArgumentException("Interest rate change " + rateChange.abs()
					+ " is less than minimum allowed " + config.getMinimumInterestRateChange());
		}

		if (config.getMaximumInterestRateChange() != null
				&& rateChange.abs().compareTo(config.getMaximumInterestRateChange()) > 0) {
			throw new IllegalArgumentException("Interest rate change " + rateChange.abs() + " exceeds maximum allowed "
					+ config.getMaximumInterestRateChange());
		}
	}

	private void validateNewLoanProduct(Long newLoanProductId, MAmendmentConfiguration config) {
		if (!config.getAllowProductChange() && newLoanProductId > 0) {
			throw new IllegalArgumentException("Product changes are not allowed");
		}

		// Check if target product is allowed
		if (config.getAllowedTargetProducts() != null && !config.getAllowedTargetProducts().isEmpty()) {
			boolean isAllowed = config.getAllowedTargetProducts().stream()
					.anyMatch(product -> product.getLoanProductConfigId().equals(newLoanProductId));

			if (!isAllowed) {
				throw new IllegalArgumentException("Target product ID " + newLoanProductId + " is not allowed");
			}
		}
	}
	
	
	
	
	
	
	
	
}