package co.ke.tezza.loanapp.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MApprovalSteps;
import co.ke.tezza.loanapp.entity.MDebtor;
import co.ke.tezza.loanapp.entity.MGroupDebtors;
import co.ke.tezza.loanapp.entity.MGuarantorLoan;
import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MInstitutionBorrower;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MNextOfKin;
import co.ke.tezza.loanapp.entity.MPaymentApprovalConfiguration;
import co.ke.tezza.loanapp.entity.MPaymentGatewayConfig;
import co.ke.tezza.loanapp.entity.MPaymentMethod;
import co.ke.tezza.loanapp.entity.MPayments;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.InterestCalculationMethodEnum;
import co.ke.tezza.loanapp.enums.LoanRepaymentStatus;
import co.ke.tezza.loanapp.enums.PaymentType;
import co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.PaymentRequest;
import co.ke.tezza.loanapp.repository.GroupBorrowersRepository;
import co.ke.tezza.loanapp.repository.GuarantorLoanRepository;
import co.ke.tezza.loanapp.repository.IndividualBorrowersRepository;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.InstitutionBorrowersRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.repository.PaymentApprovalConfigurationRepository;
import co.ke.tezza.loanapp.repository.PaymentGatewayConfigRepository;
import co.ke.tezza.loanapp.repository.PaymentMethodRepository;
import co.ke.tezza.loanapp.repository.PaymentRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.response.PaymentResponse;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

import okhttp3.Response;

@Service
public class PaymentsService {

	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	@Autowired
	private Utils utils;
	@Autowired
	private InstallmentRepository installmentRepository;
	@Autowired
	private ObjectsMapper objectsMapper;
	@Autowired
	private MpesaService mpesaService;
	@Autowired
	private PaymentGatewayConfigRepository paymentGatewayConfigRepository;
	@Autowired
	private SmsHandlersService smsHandler;
	@Autowired
	private LoanStatementService loanStatementService;
	@Autowired
	private GuarantorLoanRepository guarantorLoanRepository;
	@Autowired
	private PaymentMethodRepository paymentMethodRepository;
	@Autowired
	private PaymentApprovalConfigurationRepository paymentApprovalConfigurationRepository;
	@Autowired
	private PaymentApprovalWorkflowService paymentApprovalWorkflowService;
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private IndividualBorrowersRepository individualBorrowersRepository;
	@Autowired
	private InstitutionBorrowersRepository institutionBorrowersRepository;
	@Autowired
	private GroupBorrowersRepository groupBorrowersRepository;

	Logger logger = LoggerFactory.getLogger(this.getClass());

	// ======================= MAIN ENTRY POINT (UPDATED) =======================
	public ResponseEntity<PaymentResponse> processPayment(PaymentRequest request) {
		MPaymentMethod paymentMode = paymentMethodRepository.findById(request.getPaymentModeId()).orElse(null);
		if (paymentMode == null) {
			throw new SetUpExceptions("Please select the allowed payment methods.");
		}
		PaymentType type = paymentMode.getPaymentType();
		if (!request.getPaymentMethod().equals(type)) {
			request.setPaymentMethod(type.getValue());
		}

		if (request.getPaymentMethod().equals(String.valueOf(PaymentType.WRITE_OFF.getValue()))) {
			throw new SetUpExceptions(
					"Write Offs are not allowed in this window. Please use the waiver/write offs module.");
		}
		if (request.getPaymentMethod().equals(String.valueOf(PaymentType.WAIVER.getValue()))) {
			throw new SetUpExceptions(
					"Waivers are not allowed in this window. Please use the waiver/write offs module.");
		}
		// Route based on billId presence
		 if (request.getLoanId() > 0) {
			return processLoanPayment(request);
		 } else {
			return null;
		}
	}

	// ======================= LOAN PAYMENT FLOW (EXISTING, UNCHANGED)
	// =======================
	private ResponseEntity<PaymentResponse> processLoanPayment(PaymentRequest request) {
		MPayments payment = paymentRepository.findById(request.getPaymentId()).orElse(new MPayments());
		MLoanApplication loan = loanApplicationRepository.findById(request.getLoanId())
				.orElseThrow(() -> new RuntimeException("Loan not found"));

		validatePaymentRequest(loan, request.getAmount(), request.getReference(), request.isUseMpesaPrompt(),
				request.getPaymentDate(), request.getPaymentMethod());

		payment.setLoan(loan);
		payment.setAmount(request.getAmount());
		payment.setPaymentDate(request.getPaymentDate());
		payment.setPaymentDateTime(request.getPaymentDateTime());
		payment.setReference(request.getReference());
		if (request.isUseMpesaPrompt()) {
			payment.setPaymentMethod(PaymentType.MPESA);
		} else {

			payment.setPaymentMethod(PaymentType.valueOf(request.getPaymentMethod()));
		}
		payment.setSecurityPayment(request.isSecurityPayment());
		payment.setExpectedAllocationDate(request.getExpectedAllocationDate());

		MUser receiptedBy = loan.getAssignee();
		if (request.getPaymentReceivedBy() > 0) {
			receiptedBy = userRepository.findById(request.getPaymentReceivedBy()).orElse(null);
		}
		payment.setReceiptedBy(receiptedBy);

		MPaymentMethod paymentMethod;
		if (!request.isUseMpesaPrompt()) {
			paymentMethod = paymentMethodRepository.findById(request.getPaymentModeId())
					.orElseThrow(() -> new SetUpExceptions("Payment method not found"));
		} else {
			paymentMethod = paymentMethodRepository.findTop1ByIsActiveAndAdOrgIDAndPaymentType(true,
					utils.getAD_Org_ID(), PaymentType.MPESA);
		}

		MPaymentApprovalConfiguration config = paymentApprovalConfigurationRepository
				.findTop1ByIsActiveAndAdOrgIDAndPaymentMethod(true, utils.getAD_Org_ID(), paymentMethod);

		if (config == null || config.getApprovalLevels().isEmpty() || request.isUseMpesaPrompt()
				|| request.isSecurityPayment()) {
			if (request.isSecurityPayment()) {
				return request.isUseMpesaPrompt() ? processMpesaPaymentsSecurity(request)
						: processNormalPaymentSecurity(request);
			} else {
				return request.isUseMpesaPrompt() ? processMpesaPayments(request) : processNormalPayment(request);
			}
		} else {
			payment.setDocStatus(DocStatus.DRAFT);
			payment.setApprovalStage(ApprovalStage.DRAFT);
			payment.setIspaid(false);
			payment.setApproved(false);
			payment.setActive(true);
			payment = paymentRepository.save(payment);

			String loanTypeCode = loan.getLoanProductConfiguration().getIsDebtProduct() ? "DB" : "LN";
			String referenceNo = String.format("PAM/%s/%d/%06d", loanTypeCode, Utils.getCurrentYear(),
					payment.getPaymentId());
			payment.setDocumentNo(referenceNo);
			paymentRepository.save(payment);

			List<MApprovalSteps> steps = config.getApprovalLevels().stream()
					.sorted(Comparator.comparingInt(MApprovalSteps::getStep)).collect(Collectors.toList());
			MApprovalSteps firstStep = steps.get(0);
			payment.setCurrentApprovalLevel(firstStep.getStep());
			paymentRepository.save(payment);
			paymentApprovalWorkflowService.triggerPaymentApprovalStep(firstStep, payment);

			String message = String.format(
					"Payment request submitted successfully. Reference: %s. Approval workflow initiated - pending review by %s.",
					payment.getDocumentNo(), firstStep.getRoleInvolved().getFormattedName());
			return new ResponseEntity<>(message, 200, objectsMapper.mapPayment(payment));
		}
	}

	// Existing loan helper methods (unchanged)
	private ResponseEntity<PaymentResponse> processNormalPaymentSecurity(PaymentRequest request) {
		MPayments payment = paymentRepository.findById(request.getPaymentId()).orElse(new MPayments());
		MLoanApplication loan = loanApplicationRepository.findById(request.getLoanId())
				.orElseThrow(() -> new RuntimeException("Loan not found"));

		if (request.getPaymentMethod().equals(String.valueOf(PaymentType.WRITE_OFF.getValue()))) {
			throw new SetUpExceptions("Security payment does not allow Write Offs");
		}

		if (request.getPaymentMethod().equals(String.valueOf(PaymentType.CREDIT_NOTE.getValue()))) {
			throw new SetUpExceptions("Security payment does not allow Credit Notes");
		}

		validatePaymentRequest(loan, request.getAmount(), request.getReference(), false, request.getPaymentDate(),
				request.getPaymentMethod());

		BigDecimal paymentAmount = request.getAmount();
		MPaymentMethod paymentMode = paymentMethodRepository.findById(request.getPaymentModeId()).orElse(null);
		MUser receiptedBy = loan.getAssignee();
		if (request.getPaymentReceivedBy() > 0) {
			MUser receiptedByP = userRepository.findById(request.getPaymentReceivedBy()).orElse(null);
			payment.setReceiptedBy(receiptedByP);
		} else {
			payment.setReceiptedBy(receiptedBy);
		}

		payment.setPaymentMode(paymentMode);
		payment.setLoan(loan);
		payment.setAmount(paymentAmount);
		payment.setPaymentDate(request.getPaymentDate());
		payment.setPaymentDateTime(request.getPaymentDateTime());
		payment.setReference(request.getReference());
		payment.setPaymentMethod(PaymentType.valueOf(request.getPaymentMethod()));
		payment.setDocStatus(DocStatus.PENDING_ALLOCATION);
		payment.setApprovalStage(ApprovalStage.PENDING_ALLOCATION);
		payment.setApproved(true);
		payment.setApprovalDate(new Date());
		payment.setIspaid(true);
		payment.setSecurityPayment(true);
		payment.setExpectedAllocationDate(request.getExpectedAllocationDate());
		String loanType = loan.getLoanProductConfiguration().getIsDebtProduct() ? "Debt" : "Loan";

		processUnifiedPaymentSecurity(payment);

		return new ResponseEntity<>("Security deposit of " + formatCurrency(paymentAmount) + " received successfully. "
				+ "This amount will be held as collateral and will only be applied to the " + loanType
				+ " if the borrower defaults after " + formatDate(request.getExpectedAllocationDate()) + ". "
				+ "Reference: " + payment.getDocumentNo(), 200, objectsMapper.mapPayment(payment));
	}

	private ResponseEntity<PaymentResponse> processNormalPayment(PaymentRequest request) {
		MPayments payment = paymentRepository.findById(request.getPaymentId()).orElse(new MPayments());
		MLoanApplication loan = loanApplicationRepository.findById(request.getLoanId())
				.orElseThrow(() -> new RuntimeException("Loan not found"));

		validatePaymentRequest(loan, request.getAmount(), request.getReference(), false, request.getPaymentDate(),
				request.getPaymentMethod());

		BigDecimal paymentAmount = request.getAmount();

		MPaymentMethod paymentMode = paymentMethodRepository.findById(request.getPaymentModeId()).orElse(null);
		MUser receiptedBy = loan.getAssignee();
		if (request.getPaymentReceivedBy() > 0) {
			MUser receiptedByP = userRepository.findById(request.getPaymentReceivedBy()).orElse(null);
			payment.setReceiptedBy(receiptedByP);
		} else {
			payment.setReceiptedBy(receiptedBy);
		}

		payment.setPaymentMode(paymentMode);
		payment.setLoan(loan);
		payment.setAmount(paymentAmount);
		payment.setPaymentDate(request.getPaymentDate());
		payment.setPaymentDateTime(request.getPaymentDateTime());
		payment.setReference(request.getReference());
		payment.setPaymentMethod(PaymentType.valueOf(request.getPaymentMethod()));
		payment.setDocStatus(DocStatus.COMPLETED);
		payment.setApprovalStage(ApprovalStage.APPROVED);
		payment.setApproved(true);
		payment.setApprovalDate(new Date());
		payment.setIspaid(true);

		processUnifiedPaymentAllocation(payment, true);

		return new ResponseEntity<>(
				"Payment with reference number " + payment.getDocumentNo() + " has been made successfully.", 200,
				objectsMapper.mapPayment(payment));
	}

	private ResponseEntity<PaymentResponse> processMpesaPaymentsSecurity(PaymentRequest request) {
		try {
			MLoanApplication loan = loanApplicationRepository.findById(request.getLoanId())
					.orElseThrow(() -> new RuntimeException("Loan not found"));

			validatePaymentRequest(loan, request.getAmount(), request.getReference(), true, request.getPaymentDate(),
					request.getPaymentMethod());

			MPayments payment = new MPayments();
			MPaymentMethod paymentMode = paymentMethodRepository.findTop1ByIsActiveAndAdOrgIDAndPaymentType(true,
					utils.getAD_Org_ID(), PaymentType.MPESA);
			MUser receiptedBy = loan.getAssignee();
			if (request.getPaymentReceivedBy() > 0) {
				MUser receiptedByP = userRepository.findById(request.getPaymentReceivedBy()).orElse(null);
				payment.setReceiptedBy(receiptedByP);
			} else {
				payment.setReceiptedBy(receiptedBy);
			}

			payment.setPaymentMode(paymentMode);
			payment.setLoan(loan);
			payment.setAmount(request.getAmount());
			payment.setPaymentDate(request.getPaymentDate());
			payment.setPaymentDateTime(request.getPaymentDateTime());
			payment.setDocStatus(DocStatus.PENDING_ALLOCATION);
			payment.setApprovalStage(ApprovalStage.PENDING_ALLOCATION);
			payment.setPaymentMethod(PaymentType.MPESA);
			payment.setSecurityPayment(true);
			payment.setExpectedAllocationDate(request.getExpectedAllocationDate());
			payment.setPhoneNumber(request.getPhoneNo());
			payment.setReference(request.getReference());

			paymentRepository.save(payment);

			MPaymentGatewayConfig config = paymentGatewayConfigRepository
					.findTop1ByIsActiveAndAdOrgIDOrderByCreatedDesc(true, utils.getAD_Org_ID());

			if (config == null)
				throw new RuntimeException("M-Pesa payment gateway configuration not found");

			String businessShortCode = config.getBusinessShortCode();
			String transactionType = config.getTransactionType();
			String partyB = config.getPartyB();
			String passKey = config.getPassKey();
			String callBackURL = config.getStkCallBackUrl();

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			String formattedTimestamp = dateFormat.format(System.currentTimeMillis());
			String password = businessShortCode + passKey + formattedTimestamp;
			String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes("ISO-8859-1"));

			String phoneNo;
			if (request.isUseOtherPhone()) {
				phoneNo = utils.normalizePhone(request.getPhoneNo());
			} else {
				phoneNo = utils.normalizePhone(utils.getBorrowerPhone(loan));
			}

			Response stkPushResponse = mpesaService.STKPushSimulation(businessShortCode, encodedPassword,
					formattedTimestamp, transactionType, String.valueOf(request.getAmount()), phoneNo, phoneNo, partyB,
					callBackURL, loan.getDocumentNo(), "Loan Payment", utils.getAD_Org_ID());

			String responseString = extractResponseBody(stkPushResponse);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode responseJson = mapper.readTree(responseString);

			if (responseJson.has("MerchantRequestID") && responseJson.has("CheckoutRequestID")) {
				payment.setMerchantrequest(responseJson.get("MerchantRequestID").asText());
				payment.setCheckoutrequest(responseJson.get("CheckoutRequestID").asText());
				paymentRepository.save(payment);
				return new ResponseEntity<>(
						"M-Pesa payment request sent to your phone. Please complete the transaction.", 200,
						objectsMapper.mapPayment(payment));
			} else if (responseJson.has("errorCode")) {
				String errMsg = responseJson.get("errorMessage").asText();
				throw new RuntimeException("M-Pesa STK Push failed: " + errMsg);
			} else {
				throw new RuntimeException("Unexpected M-Pesa response: " + responseString);
			}
		} catch (Exception e) {
			logger.error("Error processing M-Pesa payment: " + e.getMessage(), e);
			throw new RuntimeException("Failed to initiate M-Pesa payment: " + e.getMessage(), e);
		}
	}

	private ResponseEntity<PaymentResponse> processMpesaPayments(PaymentRequest request) {
		try {
			MLoanApplication loan = loanApplicationRepository.findById(request.getLoanId())
					.orElseThrow(() -> new RuntimeException("Loan not found"));

			validatePaymentRequest(loan, request.getAmount(), request.getReference(), true, request.getPaymentDate(),
					request.getPaymentMethod());

			MPayments payment = new MPayments();
			MPaymentMethod paymentMode = paymentMethodRepository.findTop1ByIsActiveAndAdOrgIDAndPaymentType(true,
					utils.getAD_Org_ID(), PaymentType.MPESA);
			MUser receiptedBy = loan.getAssignee();
			if (request.getPaymentReceivedBy() > 0) {
				MUser receiptedByP = userRepository.findById(request.getPaymentReceivedBy()).orElse(null);
				payment.setReceiptedBy(receiptedByP);
			} else {
				payment.setReceiptedBy(receiptedBy);
			}

			payment.setPaymentMode(paymentMode);
			payment.setLoan(loan);
			payment.setAmount(request.getAmount());
			payment.setPaymentDate(request.getPaymentDate());
			payment.setDocStatus(DocStatus.PENDING);
			payment.setPaymentMethod(PaymentType.MPESA);
			payment.setPhoneNumber(request.getPhoneNo());
			payment.setReference(request.getReference());
			paymentRepository.save(payment);

			MPaymentGatewayConfig config = paymentGatewayConfigRepository
					.findTop1ByIsActiveAndAdOrgIDOrderByCreatedDesc(true, utils.getAD_Org_ID());

			if (config == null)
				throw new RuntimeException("M-Pesa payment gateway configuration not found");

			String businessShortCode = config.getBusinessShortCode();
			String transactionType = config.getTransactionType();
			String partyB = config.getPartyB();
			String passKey = config.getPassKey();
			String callBackURL = config.getStkCallBackUrl();

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			String formattedTimestamp = dateFormat.format(System.currentTimeMillis());
			String password = businessShortCode + passKey + formattedTimestamp;
			String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes("ISO-8859-1"));

			String phoneNo;
			if (request.isUseOtherPhone()) {
				phoneNo = utils.normalizePhone(request.getPhoneNo());
			} else {
				phoneNo = utils.normalizePhone(utils.getBorrowerPhone(loan));
			}
			String loanType = loan.getLoanProductConfiguration().getIsDebtProduct() ? "Debt" : "Loan";

			Response stkPushResponse = mpesaService.STKPushSimulation(businessShortCode, encodedPassword,
					formattedTimestamp, transactionType, String.valueOf(request.getAmount()), phoneNo, phoneNo, partyB,
					callBackURL, loan.getDocumentNo(), loanType + " Payment", utils.getAD_Org_ID());

			String responseString = extractResponseBody(stkPushResponse);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode responseJson = mapper.readTree(responseString);

			if (responseJson.has("MerchantRequestID") && responseJson.has("CheckoutRequestID")) {
				payment.setMerchantrequest(responseJson.get("MerchantRequestID").asText());
				payment.setCheckoutrequest(responseJson.get("CheckoutRequestID").asText());
				paymentRepository.save(payment);
				return new ResponseEntity<>(
						"M-Pesa payment request sent to your phone. Please complete the transaction.", 200,
						objectsMapper.mapPayment(payment));
			} else if (responseJson.has("errorCode")) {
				String errMsg = responseJson.get("errorMessage").asText();
				throw new RuntimeException("M-Pesa STK Push failed: " + errMsg);
			} else {
				throw new RuntimeException("Unexpected M-Pesa response: " + responseString);
			}
		} catch (Exception e) {
			logger.error("Error processing M-Pesa payment: " + e.getMessage(), e);
			throw new RuntimeException("Failed to initiate M-Pesa payment: " + e.getMessage(), e);
		}
	}

	private void validatePaymentRequest(MLoanApplication loan, BigDecimal amount, String reference, boolean useMpesa,
			String paymentDate, String paymentMethod) {
		if (loan.getBalance() == null || loan.getBalance().compareTo(BigDecimal.ZERO) <= 0)
			throw new RuntimeException("This loan has already been fully paid.");

		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
				&& !paymentMethod.equals(PaymentType.WALLET_PAYMENT.getValue()))
			throw new RuntimeException("Invalid payment amount. Must be greater than zero.");

		MPayments existingPayment = paymentRepository
				.findTop1ByIsActiveAndAdOrgIDAndReferenceAndApprovalStageAndIspaidOrderByPaymentIdDesc(true,
						utils.getAD_Org_ID(), reference, ApprovalStage.APPROVED, true);
		if (existingPayment != null && !useMpesa) {
			throw new RuntimeException("Payment with reference number: " + existingPayment.getReference()
					+ "  has already been receipted in the system.");
		}

		if (loan.getBalance().compareTo(amount) < 0) {
			throw new RuntimeException(
					"The repayment amount should not be more than the remaining loan balance. The loan balance for this loan (Ref No. "
							+ loan.getDocumentNo() + ") is " + loan.getBalance());
		}
		if (paymentDate != null && !paymentDate.trim().isEmpty()) {
			try {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				LocalDate paymentLocalDate = LocalDate.parse(paymentDate.trim(), formatter);
				LocalDate disbursementLocalDate = loan.getExpectedDisbursementDate().toInstant()
						.atZone(ZoneId.systemDefault()).toLocalDate();
				LocalDate today = LocalDate.now();
				String disbursementTense;

				if (disbursementLocalDate.isBefore(today)) {
					disbursementTense = "was disbursed on";
				} else if (disbursementLocalDate.isAfter(today)) {
					disbursementTense = "will be disbursed on";
				} else {
					disbursementTense = "has been disbursed today";
				}

				if (paymentLocalDate.isBefore(disbursementLocalDate)) {
					String errorMessage = String.format(
							"Payment cannot be made before the loan disbursement date. This loan %s %s",
							disbursementTense,
							disbursementLocalDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
					throw new SetUpExceptions(errorMessage);
				}
			} catch (DateTimeParseException e) {
				throw new RuntimeException(
						"Invalid payment date format. Expected format: yyyy-MM-dd, but got: " + paymentDate, e);
			}
		}
	}

	private void processUnifiedPaymentSecurity(MPayments payment) {
		paymentRepository.save(payment);
		String loanTypeCode = payment.getLoan().getLoanProductConfiguration().getIsDebtProduct() ? "DB" : "LN";
		String referenceNo = String.format("PAM/%s/%d/%06d", loanTypeCode, Utils.getCurrentYear(),
				payment.getPaymentId());
		payment.setDocumentNo(referenceNo);
		paymentRepository.saveAndFlush(payment);
	}

	public void processUnifiedPaymentAllocation(MPayments payment, boolean isNormalPayment) {
		MLoanApplication loan = payment.getLoan();
		BigDecimal paymentAmount = payment.getAmount();
		BigDecimal outstanding = loan.getBalance();
		BigDecimal amountToAllocate = paymentAmount.min(outstanding);
		BigDecimal overpayment = paymentAmount.subtract(amountToAllocate);

		// 1. Allocate to loan (if any)
		if (amountToAllocate.compareTo(BigDecimal.ZERO) > 0) {
			List<MInstallments> unpaidInstallments = installmentRepository
					.findByIsActiveAndLoanAndBalanceGreaterThanOrderByPeriodEndAsc(true, loan, BigDecimal.ZERO);

			BigDecimal remainingPayment = amountToAllocate; // use only the allocated portion
			Set<MInstallments> affectedInstallments = new HashSet<>();
			BigDecimal installmentAmount = BigDecimal.ZERO;

			if (!unpaidInstallments.isEmpty()) {
				for (MInstallments inst : unpaidInstallments) {
					if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0)
						break;

					BigDecimal instBalance = inst.getBalance();
					BigDecimal paymentToApply = remainingPayment.min(instBalance);

					inst.setBalance(instBalance.subtract(paymentToApply));
					installmentAmount = installmentAmount.add(paymentToApply);
					inst.setPaidAmount(
							inst.getPaidAmount() == null ? paymentToApply : inst.getPaidAmount().add(paymentToApply));

					remainingPayment = remainingPayment.subtract(paymentToApply);
					affectedInstallments.add(inst);
					installmentRepository.save(inst);
					paymentRepository.save(payment);

					Long paymentId = payment.getPaymentId();
					String ref = payment.getReference();
					Long installmentId = inst.getInstallmentId();
					LocalDateTime paymentTime = payment.getPaymentDateTime();

					// Preserve original method selection
					if (payment.getPaymentMethod().equals(PaymentType.CREDIT_NOTE)) {
						loanStatementService.recordCreditNote(null, installmentId, paymentToApply, null, ref,
								paymentTime, paymentId);
					} else if (payment.getPaymentMethod().equals(PaymentType.WRITE_OFF)) {
						loanStatementService.recordWriteOffs(null, installmentId, paymentToApply, null, ref,
								paymentTime, paymentId);
					} else {
						loanStatementService.recordRepayment(null, inst.getInstallmentId(), paymentToApply,
								payment.getReference(), payment.getPaymentDateTime(), payment.getPaymentId());
					}
				}
			} else {
				BigDecimal amountToApply = amountToAllocate.min(loan.getBalance());
				loan.setBalance(loan.getBalance().subtract(amountToApply).max(BigDecimal.ZERO));
				remainingPayment = remainingPayment.subtract(amountToApply);
				logger.info("No installments found — applied directly to loan balance: " + amountToApply);
			}

			// Handle installment distribution balance adjustment (original logic)
			if (amountToAllocate.compareTo(installmentAmount) > 0 && payment.getLoan().getLoanProductConfiguration()
					.getRepaymentScheduleType().equals(RepaymentScheduleTypeEnum.INSTALLMENTS)) {
				BigDecimal amountToSubtract = amountToAllocate.subtract(installmentAmount);
				loan.setInstallmentDistributionBalance(loan.getInstallmentDistributionBalance() != null
						? loan.getInstallmentDistributionBalance().subtract(amountToSubtract)
						: BigDecimal.ZERO.subtract(amountToSubtract));
				loan.setBalance(loan.getBalance().subtract(amountToAllocate).max(BigDecimal.ZERO));
			} else {
				BigDecimal totalPaid = amountToAllocate.subtract(remainingPayment);
				if (!unpaidInstallments.isEmpty() && totalPaid.compareTo(BigDecimal.ZERO) > 0) {
					loan.setBalance(loan.getBalance().subtract(totalPaid).max(BigDecimal.ZERO));
				}
			}

			// Declining balance interest adjustment (original logic)
			if (loan.getLoanProductConfiguration().getInterestCalculationMethod() != null
					&& loan.getLoanProductConfiguration().getInterestCalculationMethod()
							.equals(InterestCalculationMethodEnum.DECLINING_BALANCE)) {
				if (amountToAllocate.compareTo(loan.getDecliningInterest()) >= 0) {
					BigDecimal remainingAmount = amountToAllocate.subtract(loan.getDecliningInterest());
					loan.setDecliningPrincipal(loan.getDecliningPrincipal().subtract(remainingAmount));
					loan.setDecliningInterest(BigDecimal.ZERO);
				} else {
					loan.setDecliningInterest(loan.getDecliningInterest().subtract(amountToAllocate));
				}
			}

			// Final loan status update
			if (loan.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
				loan.setDocStatus(DocStatus.COMPLETED);
				loan.setRepaymentStatus(LoanRepaymentStatus.PAID);
			} else {
				loan.setRepaymentStatus(LoanRepaymentStatus.PARTIALLY_PAID);
			}
			loanApplicationRepository.saveAndFlush(loan);

			// Record loan statement (preserve credit note / write-off distinction)
			if (payment.getPaymentMethod().equals(PaymentType.CREDIT_NOTE)) {
				loanStatementService.recordCreditNote(loan.getLoanApplicationId(), null, amountToAllocate, null,
						payment.getReference(), payment.getPaymentDateTime(), payment.getPaymentId());
			} else if (payment.getPaymentMethod().equals(PaymentType.WRITE_OFF)) {
				loanStatementService.recordWriteOffs(loan.getLoanApplicationId(), null, amountToAllocate, null,
						payment.getReference(), payment.getPaymentDateTime(), payment.getPaymentId());
			} else {
				loanStatementService.recordRepayment(loan.getLoanApplicationId(), null, amountToAllocate,
						payment.getReference(), payment.getPaymentDateTime(), payment.getPaymentId());
			}

			payment.setInstallments(affectedInstallments);
		}

		String loanTypeCode = loan.getLoanProductConfiguration().getIsDebtProduct() ? "DB" : "LN";
		String referenceNo = String.format("PAM/%s/%d/%06d", loanTypeCode, Utils.getCurrentYear(),
				payment.getPaymentId());
		payment.setDocumentNo(referenceNo);
		payment.setDocStatus(DocStatus.COMPLETED);
		payment.setApprovalStage(ApprovalStage.APPROVED);
		payment.setApproved(true);
		payment.setIspaid(true);
		payment.setAmount(amountToAllocate);
		paymentRepository.saveAndFlush(payment);

		sendPaymentNotifications(payment);

		logger.info(String.format(
				"Loan Payment → Loan[%d]: Outstanding=%s, Allocated=%s, Overpayment=%s, FinalPaymentAmount=%s",
				loan.getLoanApplicationId(), outstanding, amountToAllocate, overpayment, payment.getAmount()));

	}

	public void processUnifiedPaymentAllocationPendingSecurityPayments(MPayments payment, boolean isMpesa) {
		MLoanApplication loan = payment.getLoan();
		BigDecimal paymentAmount = payment.getAmount();
		BigDecimal originalBalance = loan.getBalance();

		List<MInstallments> unpaidInstallments = installmentRepository
				.findByIsActiveAndLoanAndBalanceGreaterThanOrderByPeriodEndAsc(true, loan, BigDecimal.ZERO);

		BigDecimal remainingPayment = paymentAmount;
		Set<MInstallments> affectedInstallments = new HashSet<>();

		BigDecimal installmentAmount = BigDecimal.ZERO;
		if (!unpaidInstallments.isEmpty()) {
			for (MInstallments inst : unpaidInstallments) {
				if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0)
					break;

				BigDecimal instBalance = inst.getBalance();
				BigDecimal paymentToApply = remainingPayment.min(instBalance);

				inst.setBalance(instBalance.subtract(paymentToApply));
				installmentAmount = installmentAmount.add(paymentToApply);
				inst.setPaidAmount(
						inst.getPaidAmount() == null ? paymentToApply : inst.getPaidAmount().add(paymentToApply));

				remainingPayment = remainingPayment.subtract(paymentToApply);
				affectedInstallments.add(inst);
				installmentRepository.save(inst);
				paymentRepository.save(payment);
				loanStatementService.recordRepayment(null, inst.getInstallmentId(), paymentToApply,
						payment.getReference(), payment.getPaymentDateTime(), payment.getPaymentId());
			}
		} else {
			BigDecimal amountToApply = paymentAmount.min(loan.getBalance());
			loan.setBalance(loan.getBalance().subtract(amountToApply).max(BigDecimal.ZERO));
			remainingPayment = remainingPayment.subtract(amountToApply);
			logger.info("No installments found — applied directly to loan balance: " + amountToApply);
		}
		if (paymentAmount.compareTo(installmentAmount) > 0 && payment.getLoan().getLoanProductConfiguration()
				.getRepaymentScheduleType().equals(RepaymentScheduleTypeEnum.INSTALLMENTS)) {
			BigDecimal amountToSubtractFromInstallmentDistributionBalance = paymentAmount.subtract(installmentAmount);
			loan.setInstallmentDistributionBalance(
					loan.getInstallmentDistributionBalance() != null ? loan.getInstallmentDistributionBalance()
							: BigDecimal.ZERO.subtract(amountToSubtractFromInstallmentDistributionBalance));
			loan.setBalance(loan.getBalance().subtract(paymentAmount).max(BigDecimal.ZERO));
		} else {
			BigDecimal totalPaid = paymentAmount.subtract(remainingPayment);
			if (!unpaidInstallments.isEmpty() && totalPaid.compareTo(BigDecimal.ZERO) > 0) {
				loan.setBalance(loan.getBalance().subtract(totalPaid).max(BigDecimal.ZERO));
			}
		}

		if (loan.getLoanProductConfiguration().getInterestCalculationMethod() != null
				&& loan.getLoanProductConfiguration().getInterestCalculationMethod()
						.equals(InterestCalculationMethodEnum.DECLINING_BALANCE)) {
			if (payment.getAmount().compareTo(loan.getDecliningInterest()) >= 0) {
				BigDecimal remainingAmount = payment.getAmount().subtract(loan.getDecliningInterest());
				loan.setDecliningPrincipal(loan.getDecliningPrincipal().subtract(remainingAmount));
				loan.setDecliningInterest(BigDecimal.ZERO);
			} else {
				loan.setDecliningInterest(loan.getDecliningInterest().subtract(payment.getAmount()));
			}
		}

		if (loan.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
			loan.setDocStatus(DocStatus.COMPLETED);
			loan.setApprovalStage(ApprovalStage.APPROVED);
			loan.setRepaymentStatus(LoanRepaymentStatus.PAID);
		} else {
			loan.setRepaymentStatus(LoanRepaymentStatus.PARTIALLY_PAID);
		}

		loanApplicationRepository.saveAndFlush(loan);

		loanStatementService.recordRepayment(loan.getLoanApplicationId(), null, payment.getAmount(),
				payment.getReference(), payment.getPaymentDateTime(), payment.getPaymentId());

		payment.setInstallments(affectedInstallments);
		String loanTypeCode = loan.getLoanProductConfiguration().getIsDebtProduct() ? "DB" : "LN";
		String referenceNo = String.format("PAM/%s/%d/%06d", loanTypeCode, Utils.getCurrentYear(),
				payment.getPaymentId());
		payment.setDocumentNo(referenceNo);
		payment.setDocStatus(DocStatus.COMPLETED);
		payment.setApprovalDate(new Date());
		payment.setApprovalStage(ApprovalStage.APPROVED);
		payment.setPaymentDateTime(LocalDateTime.now());
		SimpleDateFormat fm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date today = new Date();
		String stringDate = fm.format(today);
		payment.setPaymentDate(stringDate);
		payment.setIspaid(true);

		paymentRepository.saveAndFlush(payment);

		sendPaymentNotifications(payment);

		logger.info(String.format("Payment → Loan[%d]: Before=%s, Paid=%s, After=%s, Type=%s",
				loan.getLoanApplicationId(), originalBalance, payment.getAmount(), loan.getBalance(),
				isMpesa ? "M-Pesa" : "Normal Receipt"));
	}

	private void sendPaymentNotifications(MPayments payment) {
		MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
				SettingCategoriesEnum.GENERAL_SETTINGS, payment.getLoan().getAdOrgID());
		if (sys != null && sys.isAllowSystemNotifications()) {
			if (payment.getLoan().getBalance() != null
					&& payment.getLoan().getBalance().compareTo(BigDecimal.ZERO) > 0) {
				smsHandler.handlePartialRepaymentNotification(payment, null);
				if (payment.getLoan().getGuarantors().size() > 0) {
					for (MNextOfKin kin : payment.getLoan().getGuarantors()) {
						smsHandler.handleGuarantorPartialPaymentNotification(kin, payment.getLoan(),
								payment.getAmount(), payment.getLoan().getBalance(), payment.getLoan().getBalance(),
								new Date(), null);
					}
				}
			} else {
				smsHandler.handleFullRepaymentNotification(payment, null);
				if (payment.getLoan().getGuarantors().size() > 0) {
					for (MNextOfKin kin : payment.getLoan().getGuarantors()) {
						smsHandler.handleGuarantorLoanClosure(kin, payment.getLoan(), new Date(),
								smsHandler.calculateTotalPaid(payment.getLoan()), payment.getAmount(), new Date(),
								null);
						MGuarantorLoan gLoan = guarantorLoanRepository
								.findTop1ByLoanAndGuarantorAndIsActive(payment.getLoan(), kin, true);
						if (gLoan != null) {
							gLoan.setGuaranteeAmountBalance(BigDecimal.ZERO);
							guarantorLoanRepository.save(gLoan);
						}
					}
				}
			}
		}
	}

	// ======================= BILL PAYMENT FLOW (NEW) =======================

	// ======================= FORMATTING HELPERS =======================
	private String formatCurrency(BigDecimal amount) {
		return "Ksh " + amount.setScale(2, RoundingMode.HALF_UP).toString();
	}

	private String formatDate(LocalDateTime date) {
		return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
	}

	// ======================= EXTRACT RESPONSE BODY =======================
	private String extractResponseBody(Response response) throws IOException {
		if (response == null || response.body() == null) {
			return "{\"errorCode\":\"500\", \"errorMessage\":\"Empty response from M-Pesa\"}";
		}
		try {
			String responseBody = response.body().string();
			logger.info("M-Pesa STK Push Response: {}", responseBody);
			return responseBody;
		} catch (IllegalStateException e) {
			logger.error("Response body already consumed: {}", e.getMessage());
			return "{\"errorCode\":\"500\", \"errorMessage\":\"Response body already consumed\"}";
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

	// ======================= MPESA CALLBACK HANDLER (UPDATED)
	// =======================
	@Transactional
	public void handleMpesaCallback(String callbackResponse) {
		try {
			logger.info("M-Pesa Callback Response: " + callbackResponse);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode responseJson = mapper.readTree(callbackResponse);

			int resultCode = responseJson.get("Body").get("stkCallback").get("ResultCode").asInt();
			String merchantRequestID = responseJson.get("Body").get("stkCallback").get("MerchantRequestID").asText();
			String checkoutRequestID = responseJson.get("Body").get("stkCallback").get("CheckoutRequestID").asText();

			MPayments payment = paymentRepository.findTop1ByMerchantrequestAndCheckoutrequest(merchantRequestID,
					checkoutRequestID);

			if (payment == null) {
				logger.error("Payment not found for MerchantRequestID: " + merchantRequestID);
				return;
			}

			if (resultCode == 0) {
				JsonNode items = responseJson.get("Body").get("stkCallback").get("CallbackMetadata").get("Item");

				BigDecimal amount = BigDecimal.ZERO;
				String mpesaReceiptNumber = "", transactionDate = "", phoneNumber = "";

				for (JsonNode item : items) {
					String name = item.get("Name").asText();
					switch (name) {
					case "Amount":
						amount = new BigDecimal(item.get("Value").asText());
						break;
					case "MpesaReceiptNumber":
						mpesaReceiptNumber = item.get("Value").asText();
						break;
					case "TransactionDate":
						transactionDate = item.get("Value").asText();
						break;
					case "PhoneNumber":
						phoneNumber = item.get("Value").asText();
						break;
					}
				}

				SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
				Date parsedDate = format.parse(transactionDate);
				LocalDateTime paymentDate = parsedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

				// Route to appropriate allocation
				if (payment.getLoan() != null) {
					if (payment.isSecurityPayment()
							&& payment.getApprovalStage().equals(ApprovalStage.PENDING_ALLOCATION)) {
						processUnifiedPaymentSecurity(payment);
					} else {
						processUnifiedPaymentAllocation(payment, false);
						payment.setDocStatus(DocStatus.COMPLETED);
						payment.setApprovalStage(ApprovalStage.APPROVED);
					}
				}

				else {
					logger.error("Payment has neither loan nor bill attached. Cannot allocate.");
					payment.setDocStatus(DocStatus.REJECTED);
					payment.setResponsedescription("No associated loan or bill");
				}

				payment.setPaymentDateTime(paymentDate);
				payment.setMpesareceitNumber(mpesaReceiptNumber);
				payment.setTransactionDate(transactionDate);
				payment.setPaymentDate(transactionDate);
				payment.setPhoneNumber(phoneNumber);
				payment.setAmount(amount);
				payment.setApprovalDate(new Date());
				payment.setApproved(true);
				payment.setActive(true);
				payment.setIspaid(true);
				payment.setReference(mpesaReceiptNumber);

				logger.info("M-Pesa payment completed successfully → Receipt: " + mpesaReceiptNumber);
			} else {
				payment.setDocStatus(DocStatus.REJECTED);
				String resultDesc = responseJson.get("Body").get("stkCallback").get("ResultDesc").asText();
				payment.setResponsedescription(resultDesc);
				paymentRepository.save(payment);
				logger.error("M-Pesa payment failed: " + resultDesc);
			}
		} catch (Exception e) {
			logger.error("Error handling M-Pesa callback: " + e.getMessage(), e);
		}
	}

	// ======================= MANUAL TRANSACTION STATUS QUERY (UPDATED)
	// =======================
	@Transactional
	public void queryTransactionStatus(String checkoutRequestID) {
		try {
			logger.info("Querying transaction status for CheckoutRequestID: {}", checkoutRequestID);

			MPayments payment = paymentRepository.findTop1ByCheckoutrequestOrderByPaymentIdDesc(checkoutRequestID);
			if (payment == null) {
				logger.error("Payment not found for CheckoutRequestID: {}", checkoutRequestID);
				return;
			}

			if (payment.getDocStatus() == DocStatus.COMPLETED) {
				logger.info("Payment already completed for CheckoutRequestID: {}", checkoutRequestID);
				return;
			}

			MPaymentGatewayConfig config = paymentGatewayConfigRepository
					.findTop1ByIsActiveAndAdOrgIDOrderByCreatedDesc(true, payment.getAdOrgID());

			if (config == null) {
				logger.error("M-Pesa configuration not found");
				return;
			}

			String businessShortCode = config.getBusinessShortCode();
			String passKey = config.getPassKey();

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			String formattedTimestamp = dateFormat.format(System.currentTimeMillis());
			String password = businessShortCode + passKey + formattedTimestamp;
			String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes("ISO-8859-1"));

			Response statusResponse = mpesaService.STKPushTransactionStatus(businessShortCode, encodedPassword,
					formattedTimestamp, checkoutRequestID, payment.getAdOrgID());

			String responseString = extractResponseBody(statusResponse);
			logger.info("Transaction Status Query Response: {}", responseString);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode responseJson = mapper.readTree(responseString);

			if (responseJson.has("ResultCode")) {
				int resultCode = responseJson.get("ResultCode").asInt();

				if (resultCode == 0) {
					JsonNode resultDesc = responseJson.get("ResultDesc");
					logger.info("Transaction successful via manual query: {}", resultDesc.asText());

					// Route to appropriate allocation
					if (payment.getLoan() != null) {
						if (payment.isSecurityPayment()
								&& payment.getApprovalStage().equals(ApprovalStage.PENDING_ALLOCATION)) {
							processUnifiedPaymentAllocationPendingSecurityPayments(payment, false);
						} else {
							processUnifiedPaymentAllocation(payment, false);
							payment.setDocStatus(DocStatus.COMPLETED);
							payment.setApprovalStage(ApprovalStage.APPROVED);
						}
					}
					payment.setApprovalDate(new Date());
					payment.setApproved(true);
					payment.setIspaid(true);
					payment.setResponsedescription("Completed via manual status query");
					if (payment.getReference() == null) {
						String reference = "PAY-"
								+ UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
						payment.setReference(reference);
					}
					paymentRepository.save(payment);

				} else {
					String resultDesc = responseJson.get("ResultDesc").asText();
					logger.error("Transaction failed via manual query: {}", resultDesc);

					payment.setDocStatus(DocStatus.REJECTED);
					payment.setResponsedescription(resultDesc);
					if (payment.getReference() == null) {
						String reference = "REJ-"
								+ UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
						payment.setReference(reference);
					}
					paymentRepository.save(payment);
				}
			} else {
				logger.error("Invalid response from transaction status query: {}", responseString);
			}
		} catch (Exception e) {
			logger.error("Error querying transaction status for CheckoutRequestID {}: {}", checkoutRequestID,
					e.getMessage(), e);
		}
	}

	// ======================= C2B CALLBACK HANDLER (UPDATED)
	// =======================
	@Transactional
	public void handleC2BCallback(String payload) {
		try {
			logger.info("Received C2B Callback: {}", payload);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(payload);

			String transactionType = json.path("TransactionType").asText();
			String transID = json.path("TransID").asText();
			BigDecimal transAmount = new BigDecimal(json.path("TransAmount").asText("0"));
			String transTime = json.path("TransTime").asText();
			String msisdn = json.path("MSISDN").asText();
			String billRefNumber = json.path("BillRefNumber").asText();
			String orgAccountBalance = json.path("OrgAccountBalance").asText();
			String shortCode = json.path("BusinessShortCode").asText();

			MPayments existingPayment = paymentRepository.findByReference(transID);
			if (existingPayment != null) {
				logger.warn("Duplicate C2B transaction ignored: {}", transID);
				return;
			}

			// Try to find loan first, then bill
			MLoanApplication loan = loanApplicationRepository.findByDocumentNo(billRefNumber).orElse(null);

			MPayments payment = new MPayments();
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
			Date parsedDate = format.parse(transTime);
			LocalDateTime paymentDateTime = parsedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			payment.setReference(transID);
			payment.setAmount(transAmount);
			payment.setPhoneNumber(msisdn);
			payment.setTransactionDate(transTime);
			payment.setPaymentDateTime(paymentDateTime);
			payment.setPaymentDate(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
			payment.setDocStatus(DocStatus.COMPLETED);
			payment.setApprovalStage(ApprovalStage.APPROVED);
			payment.setApprovalDate(new Date());
			payment.setApproved(true);
			payment.setActive(true);
			payment.setIspaid(true);
			payment.setPaymentMethod(PaymentType.MPESA);
			payment.setResponsedescription("Received via C2B callback - " + transactionType);

			if (loan != null) {
				payment.setLoan(loan);
				paymentRepository.save(payment);
				processUnifiedPaymentAllocation(payment, false);
				logger.info("C2B Payment applied to loan [{}] → amount: {}, transID: {}", loan.getDocumentNo(),
						transAmount, transID);
			} else {
				logger.warn("No matching loan or bill found for BillRefNumber: {}. Stored as unmatched payment.",
						billRefNumber);
				payment.setDocStatus(DocStatus.DRAFT);
				payment.setApprovalStage(ApprovalStage.DRAFT);
				payment.setApproved(false);
				paymentRepository.save(payment);
			}
		} catch (Exception e) {
			logger.error("Error processing C2B callback: {}", e.getMessage(), e);
		}
	}

	// ======================= BATCH STATUS QUERY =======================
	@Transactional
	public void queryPendingMpesaPayments() {
		try {
			List<MPayments> pendingPayments = paymentRepository.findByIsActiveAndDocStatusAndIspaid(true,
					DocStatus.PENDING, false);
			if (pendingPayments.isEmpty()) {
				return;
			}
			for (MPayments payment : pendingPayments) {
				if (payment.getCheckoutrequest() != null && !payment.getCheckoutrequest().isEmpty()) {
					try {
						Thread.sleep(1000);
						queryTransactionStatus(payment.getCheckoutrequest());
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					} catch (Exception e) {
						logger.error("Error querying payment {}: {}", payment.getCheckoutrequest(), e.getMessage());
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error in batch status query: {}", e.getMessage(), e);
		}
	}

	// ======================= MANUAL STATUS QUERY ENDPOINT =======================
	public ResponseEntity<PaymentResponse> manuallyQueryPaymentStatus(Long paymentId) {
		try {
			MPayments payment = paymentRepository.findById(paymentId)
					.orElseThrow(() -> new RuntimeException("Payment not found"));

			if (payment.getCheckoutrequest() == null || payment.getCheckoutrequest().isEmpty()) {
				return new ResponseEntity<>("Payment does not have a CheckoutRequestID", 400,
						objectsMapper.mapPayment(payment));
			}

			if (payment.getDocStatus() == DocStatus.COMPLETED) {
				return new ResponseEntity<>("Payment is already completed", 200, objectsMapper.mapPayment(payment));
			}

			queryTransactionStatus(payment.getCheckoutrequest());

			payment = paymentRepository.findById(paymentId).orElse(payment);
			return new ResponseEntity<>("Status query completed. Current status: " + payment.getDocStatus(), 200,
					objectsMapper.mapPayment(payment));
		} catch (Exception e) {
			logger.error("Error in manual status query for payment {}: {}", paymentId, e.getMessage());
			return new ResponseEntity<>("Error querying payment status: " + e.getMessage(), 500,
					objectsMapper.mapPayment(null));
		}
	}

	// ======================= QUERY METHODS (unchanged, they return all payments)
	// =======================
	public Page<PaymentResponse> getAllPayments(int page, int size, String dateFrom, String dateTo, String search,
			String paymentMethod, String status) {
		PaymentType paymentMethod1 = null;
		if (paymentMethod != null && !paymentMethod.isEmpty()) {
			paymentMethod1 = PaymentType.valueOf(paymentMethod);
		}

		try {
			logger.debug(
					"Getting all payments with filters - page: {}, size: {}, dateFrom: {}, dateTo: {}, search: {}, paymentMethod: {}, status: {}",
					page, size, dateFrom, dateTo, search, paymentMethod, status);

			Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paymentId"));

			long adOrgID = utils.getAD_Org_ID();

			Page<MPayments> paymentPage;

			if (search != null && !search.trim().isEmpty()) {
				paymentPage = paymentRepository.searchPayments(true, adOrgID, dateFrom, dateTo, search.toLowerCase(),
						false, false, pageable);
			} else if (paymentMethod1 != null && status == null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndPaymentMethodAndPaymentDateBetweenAndApprovalStageAndDocStatusOrderByPaymentIdDesc(
								true, false, false, adOrgID,
								co.ke.tezza.loanapp.enums.PaymentType.valueOf(paymentMethod), dateFrom, dateTo,
								ApprovalStage.APPROVED, DocStatus.COMPLETED, pageable);
			} else if (paymentMethod1 == null && status != null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndDocStatusAndPaymentDateBetween(
								true, false, false, adOrgID, co.ke.tezza.loanapp.enums.DocStatus.valueOf(status),
								dateFrom, dateTo, pageable);
			} else if (paymentMethod1 != null && status != null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndDocStatusAndPaymentMethodAndPaymentDateBetweenOrderByPaymentIdDesc(
								true, false, false, adOrgID, co.ke.tezza.loanapp.enums.DocStatus.valueOf("COMPLETED"),
								co.ke.tezza.loanapp.enums.PaymentType.valueOf(paymentMethod), dateFrom, dateTo,
								pageable);
			} else {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndPaymentDateBetweenAndDocStatusOrderByPaymentIdDesc(
								true, false, false, adOrgID, dateFrom, dateTo, DocStatus.COMPLETED, pageable);
			}

			return paymentPage.map(objectsMapper::mapPayment);

		} catch (Exception e) {
			logger.error("Error getting payments: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to retrieve payments: " + e.getMessage(), e);
		}
	}

	public Page<PaymentResponse> getAllRejectedPayments(int page, int size, String dateFrom, String dateTo,
			String search, String paymentMethod, String status) {
		PaymentType paymentMethod1 = null;
		if (paymentMethod != null && !paymentMethod.isEmpty()) {
			paymentMethod1 = PaymentType.valueOf(paymentMethod);
		}

		try {
			logger.debug(
					"Getting all payments with filters - page: {}, size: {}, dateFrom: {}, dateTo: {}, search: {}, paymentMethod: {}, status: {}",
					page, size, dateFrom, dateTo, search, paymentMethod, status);

			Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paymentId"));

			long adOrgID = utils.getAD_Org_ID();

			Page<MPayments> paymentPage;

			if (search != null && !search.trim().isEmpty()) {
				paymentPage = paymentRepository.searchRejectedPayments(true, adOrgID, dateFrom, dateTo,
						search.toLowerCase(), false, false, pageable);
			} else if (paymentMethod1 != null && status == null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndPaymentMethodAndPaymentDateBetweenAndApprovalStageAndDocStatusOrderByPaymentIdDesc(
								true, false, false, adOrgID,
								co.ke.tezza.loanapp.enums.PaymentType.valueOf(paymentMethod), dateFrom, dateTo,
								ApprovalStage.REJECTED, DocStatus.REJECTED, pageable);
			} else if (paymentMethod1 == null && status != null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndDocStatusAndPaymentDateBetween(
								true, false, false, adOrgID, co.ke.tezza.loanapp.enums.DocStatus.valueOf(status),
								dateFrom, dateTo, pageable);
			} else if (paymentMethod1 != null && status != null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndDocStatusAndPaymentMethodAndPaymentDateBetweenOrderByPaymentIdDesc(
								true, false, false, adOrgID, co.ke.tezza.loanapp.enums.DocStatus.valueOf("REJECTED"),
								co.ke.tezza.loanapp.enums.PaymentType.valueOf(paymentMethod), dateFrom, dateTo,
								pageable);
			} else {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndPaymentDateBetweenAndDocStatusOrderByPaymentIdDesc(
								true, false, false, adOrgID, dateFrom, dateTo, DocStatus.REJECTED, pageable);
			}

			return paymentPage.map(objectsMapper::mapPayment);

		} catch (Exception e) {
			logger.error("Error getting payments: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to retrieve payments: " + e.getMessage(), e);
		}
	}

	public Page<PaymentResponse> getAllWriteOffOrWaiverPayments(int page, int size, String dateFrom, String dateTo,
			String search, String paymentMethod, String status) {
		PaymentType paymentMethod1 = null;
		if (paymentMethod != null && !paymentMethod.isEmpty()) {
			paymentMethod1 = PaymentType.valueOf(paymentMethod);
		}

		try {
			logger.debug(
					"Getting all payments with filters - page: {}, size: {}, dateFrom: {}, dateTo: {}, search: {}, paymentMethod: {}, status: {}",
					page, size, dateFrom, dateTo, search, paymentMethod, status);

			Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paymentId"));

			long adOrgID = utils.getAD_Org_ID();

			Page<MPayments> paymentPage;

			if (search != null && !search.trim().isEmpty()) {
				paymentPage = paymentRepository.searchPayments(true, adOrgID, dateFrom, dateTo, search.toLowerCase(),
						true, false, pageable);
			} else if (paymentMethod1 != null && status == null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndPaymentMethodAndPaymentDateBetweenAndApprovalStageAndDocStatusOrderByPaymentIdDesc(
								true, true, false, adOrgID,
								co.ke.tezza.loanapp.enums.PaymentType.valueOf(paymentMethod), dateFrom, dateTo,
								ApprovalStage.APPROVED, DocStatus.COMPLETED, pageable);
			} else if (paymentMethod1 == null && status != null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndDocStatusAndPaymentDateBetween(
								true, true, false, adOrgID, co.ke.tezza.loanapp.enums.DocStatus.valueOf(status),
								dateFrom, dateTo, pageable);
			} else if (paymentMethod1 != null && status != null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndDocStatusAndPaymentMethodAndPaymentDateBetweenOrderByPaymentIdDesc(
								true, true, false, adOrgID, co.ke.tezza.loanapp.enums.DocStatus.valueOf("COMPLETED"),
								co.ke.tezza.loanapp.enums.PaymentType.valueOf(paymentMethod), dateFrom, dateTo,
								pageable);
			} else {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndPaymentDateBetweenAndDocStatusOrderByPaymentIdDesc(
								true, true, false, adOrgID, dateFrom, dateTo, DocStatus.COMPLETED, pageable);
			}

			return paymentPage.map(objectsMapper::mapPayment);

		} catch (Exception e) {
			logger.error("Error getting payments: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to retrieve payments: " + e.getMessage(), e);
		}
	}

	public Page<PaymentResponse> getAllWriteOffOrWaiverRejectedPayments(int page, int size, String dateFrom,
			String dateTo, String search, String paymentMethod, String status) {
		PaymentType paymentMethod1 = null;
		if (paymentMethod != null && !paymentMethod.isEmpty()) {
			paymentMethod1 = PaymentType.valueOf(paymentMethod);
		}

		try {
			logger.debug(
					"Getting all payments with filters - page: {}, size: {}, dateFrom: {}, dateTo: {}, search: {}, paymentMethod: {}, status: {}",
					page, size, dateFrom, dateTo, search, paymentMethod, status);

			Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paymentId"));

			long adOrgID = utils.getAD_Org_ID();

			Page<MPayments> paymentPage;

			if (search != null && !search.trim().isEmpty()) {
				paymentPage = paymentRepository.searchRejectedPayments(true, adOrgID, dateFrom, dateTo,
						search.toLowerCase(), true, false, pageable);
			} else if (paymentMethod1 != null && status == null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndPaymentMethodAndPaymentDateBetweenAndApprovalStageAndDocStatusOrderByPaymentIdDesc(
								true, true, false, adOrgID,
								co.ke.tezza.loanapp.enums.PaymentType.valueOf(paymentMethod), dateFrom, dateTo,
								ApprovalStage.REJECTED, DocStatus.REJECTED, pageable);
			} else if (paymentMethod1 == null && status != null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndDocStatusAndPaymentDateBetween(
								true, true, false, adOrgID, co.ke.tezza.loanapp.enums.DocStatus.valueOf(status),
								dateFrom, dateTo, pageable);
			} else if (paymentMethod1 != null && status != null) {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndDocStatusAndPaymentMethodAndPaymentDateBetweenOrderByPaymentIdDesc(
								true, true, false, adOrgID, co.ke.tezza.loanapp.enums.DocStatus.valueOf("REJECTED"),
								co.ke.tezza.loanapp.enums.PaymentType.valueOf(paymentMethod), dateFrom, dateTo,
								pageable);
			} else {
				paymentPage = paymentRepository
						.findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndPaymentDateBetweenAndDocStatusOrderByPaymentIdDesc(
								true, true, false, adOrgID, dateFrom, dateTo, DocStatus.REJECTED, pageable);
			}

			return paymentPage.map(objectsMapper::mapPayment);

		} catch (Exception e) {
			logger.error("Error getting payments: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to retrieve payments: " + e.getMessage(), e);
		}
	}

	public Map<String, Object> getPaymentStatistics(String dateFrom, String dateTo) {
		try {
			long adOrgID = utils.getAD_Org_ID();

			Map<String, Object> stats = new HashMap<>();

			Long totalPayments = paymentRepository.countByIsActiveAndAdOrgIDAndPaymentDateBetween(true, adOrgID,
					dateFrom, dateTo);
			stats.put("totalPayments", totalPayments != null ? totalPayments : 0L);

			BigDecimal totalAmount = paymentRepository.sumAmountByIsActiveAndAdOrgIDAndPaymentDateBetween(true, adOrgID,
					dateFrom, dateTo);
			stats.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);

			Long successfulPayments = paymentRepository.countByIsActiveAndAdOrgIDAndIspaidAndPaymentDateBetween(true,
					adOrgID, true, dateFrom, dateTo);
			stats.put("successfulPayments", successfulPayments != null ? successfulPayments : 0L);

			BigDecimal averageAmount = (totalPayments != null && totalPayments > 0)
					? totalAmount.divide(BigDecimal.valueOf(totalPayments), 2, RoundingMode.HALF_UP)
					: BigDecimal.ZERO;
			stats.put("averagePayment", averageAmount);

			List<Object[]> paymentsByMethod = paymentRepository.countPaymentsByMethodAndAdOrgID(true, adOrgID, dateFrom,
					dateTo);
			Map<String, Long> methodStats = new HashMap<>();
			for (Object[] result : paymentsByMethod) {
				methodStats.put(String.valueOf(result[0]), (Long) result[1]);
			}
			stats.put("paymentsByMethod", methodStats);

			return stats;

		} catch (Exception e) {
			logger.error("Error getting payment statistics: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to retrieve payment statistics: " + e.getMessage(), e);
		}
	}

}