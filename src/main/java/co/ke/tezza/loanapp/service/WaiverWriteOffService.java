package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MApprovalSteps;
import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MNextOfKin;
import co.ke.tezza.loanapp.entity.MPaymentApprovalConfiguration;
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
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.repository.PaymentApprovalConfigurationRepository;
import co.ke.tezza.loanapp.repository.PaymentMethodRepository;
import co.ke.tezza.loanapp.repository.PaymentRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.response.PaymentResponse;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

@Service
public class WaiverWriteOffService {

	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	@Autowired
	private InstallmentRepository installmentRepository;
	@Autowired
	private PaymentMethodRepository paymentMethodRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private LoanStatementService loanStatementService;

	@Autowired
	private Utils utils;
	@Autowired
	private ObjectsMapper objectsMapper;
	@Autowired
	private PaymentApprovalConfigurationRepository paymentApprovalConfigurationRepository;
	@Autowired
	private PaymentApprovalWorkflowService paymentApprovalWorkflowService;
	@Autowired
	private SmsHandlersService smsHandlersService;

	private static final Logger logger = LoggerFactory.getLogger(WaiverWriteOffService.class);

	// ======================= MAIN ENTRY POINT =======================

	public ResponseEntity<PaymentResponse> processWaiverWriteOff(PaymentRequest request) {
		String method = request.getPaymentMethod();
		if (!(method.equals(String.valueOf(PaymentType.WRITE_OFF.getValue()))
				|| method.equals(String.valueOf(PaymentType.WAIVER.getValue())))) {
			throw new SetUpExceptions("Only Write Offs and Waivers are allowed to use this window");
		}
		if (request.isUseMpesaPrompt()) {
			throw new SetUpExceptions("Waiver/Write-off cannot be processed via MPESA.");
		}
		if (request.isSecurityPayment()) {
			throw new SetUpExceptions("Waiver/Write-off cannot be a security payment.");
		}

		else {
			return processLoanWaiverWriteOff(request);
		}
	}

	// ======================= LOAN WAIVER/WRITE-OFF =======================

	private ResponseEntity<PaymentResponse> processLoanWaiverWriteOff(PaymentRequest request) {
		MPayments payment = paymentRepository.findById(request.getPaymentId()).orElse(new MPayments());
		MLoanApplication loan = loanApplicationRepository.findById(request.getLoanId())
				.orElseThrow(() -> new RuntimeException("Loan not found"));

		validateLoanWaiverRequest(loan, request.getAmount(), request.getReference(), request.getPaymentDate());

		// Set payment fields
		payment.setLoan(loan);
		payment.setWriteOffWaiverReason(request.getWriteOffWaiverReason());
		payment.setAmount(request.getAmount());
		payment.setInterestOnly(request.isInterestOnly());
		payment.setPenaltiesOnly(request.isPenaltiesOnly());
		payment.setPaymentDate(request.getPaymentDate());
		payment.setPaymentDateTime(
				request.getPaymentDateTime() != null ? request.getPaymentDateTime() : LocalDateTime.now());
		payment.setReference(request.getReference());
		payment.setPaymentMethod(PaymentType.valueOf(request.getPaymentMethod()));

		// Receipt user
		if (request.getPaymentReceivedBy() > 0) {
			MUser receiptedBy = userRepository.findById(request.getPaymentReceivedBy()).orElse(null);
			payment.setReceiptedBy(receiptedBy);
		} else {
			payment.setReceiptedBy(loan.getAssignee());
		}

		// Payment mode
		MPaymentMethod paymentMode = paymentMethodRepository.findById(request.getPaymentModeId())
				.orElseThrow(() -> new SetUpExceptions("Payment method not found"));
		payment.setPaymentMode(paymentMode);

		// Waiver-specific flags
		payment.setIspaid(false);
		payment.setWaiverWriteOff(true);
		payment.setWaiverOrWriteOff(true);

		// Check approval configuration
		MPaymentApprovalConfiguration config = paymentApprovalConfigurationRepository
				.findTop1ByIsActiveAndAdOrgIDAndPaymentMethod(true, utils.getAD_Org_ID(), paymentMode);

		if (config != null && !config.getApprovalLevels().isEmpty()) {
			// Approval workflow
			payment.setDocStatus(DocStatus.DRAFT);
			payment.setApprovalStage(ApprovalStage.DRAFT);
			payment.setApproved(false);
			payment = paymentRepository.save(payment);

			String loanTypeCode = loan.getLoanProductConfiguration().getIsDebtProduct() ? "DB" : "LN";
			String referenceNo = String.format("PWO/%s/%d/%06d", loanTypeCode, Utils.getCurrentYear(),
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
					"Waiver/Write-off request submitted successfully. Reference: %s. Approval workflow initiated – pending review by %s.",
					payment.getDocumentNo(), firstStep.getRoleInvolved().getFormattedName());
			return new ResponseEntity<>(message, 200, objectsMapper.mapPayment(payment));

		} else {
			// No approval – process immediately
			payment.setDocStatus(DocStatus.COMPLETED);
			payment.setApprovalStage(ApprovalStage.APPROVED);
			payment.setApproved(true);
			payment.setApprovalDate(new Date());

			payment = paymentRepository.save(payment);

			String loanTypeCode = loan.getLoanProductConfiguration().getIsDebtProduct() ? "DB" : "LN";
			String referenceNo = String.format("PWO/%s/%d/%06d", loanTypeCode, Utils.getCurrentYear(),
					payment.getPaymentId());
			payment.setDocumentNo(referenceNo);
			paymentRepository.save(payment);

			// Choose allocation strategy based on flags

			processApprovedWaiver(payment);

			return new ResponseEntity<>(
					"Waiver/Write-off of " + formatCurrency(request.getAmount()) + " applied to loan "
							+ loan.getDocumentNo() + ". Reference: " + payment.getDocumentNo(),
					200, objectsMapper.mapPayment(payment));
		}
	}

	/**
	 * Flag‑based waiver: only affects interest and/or penalty fields. Does NOT
	 * touch the loan balance or installments.
	 */
	private void applyFlagBasedWaiver(MPayments payment, MLoanApplication loan) {
		BigDecimal amount = payment.getAmount();
		boolean interestOnly = payment.isInterestOnly();
		boolean penaltiesOnly = payment.isPenaltiesOnly();

		BigDecimal penalties = loan.getPenaltyEarned() != null ? loan.getPenaltyEarned() : BigDecimal.ZERO;
		BigDecimal interests = loan.getInterestsEarned() != null ? loan.getInterestsEarned() : BigDecimal.ZERO;
		BigDecimal totalExemptedInterests = loan.getExemptedInterests() != null ? loan.getExemptedInterests()
				: BigDecimal.ZERO;
		BigDecimal totalExemptedPenalties = loan.getExemptedPenalties() != null ? loan.getExemptedPenalties()
				: BigDecimal.ZERO;
		BigDecimal totalExempted = loan.getExemptedAmount() != null ? loan.getExemptedAmount() : BigDecimal.ZERO;

		if (penaltiesOnly && !interestOnly) {
			// Penalty‑only
			BigDecimal penaltyPortion = amount.min(penalties);
			if (penaltyPortion.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal previousPenaltyAmount = loan.getPenaltyEarned();
				loan.setPenaltyEarned(penalties.subtract(penaltyPortion));
				loan.setExemptedPenalties(penaltyPortion.add(totalExemptedPenalties));
				loan.setExemptedAmount(penaltyPortion.add(totalExempted));
				loan.setExempted(true);
				BigDecimal currentPenaltyAmount = loan.getPenaltyEarned();

				BigDecimal writtenOffPenaltyAmount = penaltyPortion;
				String writeOffReason = payment.getWriteOffWaiverReason() != null ? payment.getWriteOffWaiverReason()
						: "The debtor was not able to complete the remaining balance due to some circumstances.";

				Date writeOffDate = new Date();
				String approvedBy = payment.getApprovedBy() != null ? payment.getApprovedBy().getFullName() : "System";

				Long reminderId = null;

				if (payment.getPaymentMethod() != null && payment.getPaymentMethod().equals(PaymentType.WRITE_OFF)) {
					writeOffReason = payment.getWriteOffWaiverReason() != null ? payment.getWriteOffWaiverReason()
							: "The debtor was not able to pay the penalties due to some unavoidable circumstances.";

					smsHandlersService.handlePenaltyWriteOffNotification(loan, writtenOffPenaltyAmount, writeOffReason,
							writeOffDate, approvedBy, reminderId);
					if (!loan.getGuarantors().isEmpty()) {
						for (MNextOfKin guarantor : loan.getGuarantors()) {
							smsHandlersService.handleGuarantorPenaltyWriteOffNotification(guarantor, loan,
									writtenOffPenaltyAmount, writeOffReason, writeOffDate, approvedBy, reminderId);
						}
					}
				}
				if (payment.getPaymentMethod() != null && payment.getPaymentMethod().equals(PaymentType.WAIVER)) {
					writeOffReason = payment.getWriteOffWaiverReason() != null ? payment.getWriteOffWaiverReason()
							: "Penalty waived for account adjustments and correction.";

					smsHandlersService.handlePenaltyWaiverNotification(loan, writtenOffPenaltyAmount,
							currentPenaltyAmount, writeOffDate, writeOffReason, approvedBy, reminderId,
							LocalDateTime.now());
					if (!loan.getGuarantors().isEmpty()) {
						for (MNextOfKin guarantor : loan.getGuarantors()) {
							smsHandlersService.handleGuarantorPenaltyWaiverNotification(guarantor, loan,
									writtenOffPenaltyAmount, writeOffReason, writeOffDate, previousPenaltyAmount,
									approvedBy, reminderId, LocalDateTime.now());
						}
					}
				}
				loanStatementService.recordPenaltyWaiver(loan.getLoanApplicationId(), null, penaltyPortion,
						"Penalty-only waiver", payment.getReference());

				logger.info("Penalty-only waiver: Amount={}, penalties reduced from {} to {}", penaltyPortion,
						penalties, loan.getPenaltyEarned());
			}
		} else if (interestOnly && !penaltiesOnly) {
			// Interest‑only
			BigDecimal interestPortion = amount.min(interests);
			if (interestPortion.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal previousinterestAmount = loan.getInterestsEarned();
				loan.setInterestsEarned(interests.subtract(interestPortion));
				loan.setExemptedInterests(interestPortion.add(totalExemptedInterests));
				loan.setExemptedAmount(interestPortion.add(totalExempted));
				loan.setExempted(true);
				BigDecimal interestWriteOff = interestPortion;
				String writeOffReason = payment.getWriteOffWaiverReason() != null ? payment.getWriteOffWaiverReason()
						: "The debtor was not able to complete the remaining balance due to some circumstances.";

				Date writeOffDate = new Date();
				String approvedBy = payment.getApprovedBy() != null ? payment.getApprovedBy().getFullName() : "System";

				Long reminderId = null;
				if (payment.getPaymentMethod() != null && payment.getPaymentMethod().equals(PaymentType.WRITE_OFF)) {
					smsHandlersService.handleInterestWriteOffNotification(loan, interestWriteOff, writeOffReason,
							writeOffDate, approvedBy, reminderId);
					if (!loan.getGuarantors().isEmpty()) {
						for (MNextOfKin guarantor : loan.getGuarantors()) {
							smsHandlersService.handleGuarantorInterestWriteOffNotification(guarantor, loan,
									interestWriteOff, writeOffReason, writeOffDate, approvedBy, reminderId);
						}
					}
				}
				if (payment.getPaymentMethod() != null && payment.getPaymentMethod().equals(PaymentType.WAIVER)) {
					smsHandlersService.handleInterestWaiverNotification(loan, interestWriteOff, writeOffReason,
							writeOffDate, approvedBy, reminderId);
					if (!loan.getGuarantors().isEmpty()) {
						for (MNextOfKin guarantor : loan.getGuarantors()) {
							smsHandlersService.handleGuarantorInterestWaiverNotification(guarantor, loan,
									interestWriteOff, writeOffReason, writeOffDate, previousinterestAmount, approvedBy,
									reminderId, LocalDateTime.now());
						}
					}
				}

				loanStatementService.recordInterestWaiver(loan.getLoanApplicationId(), null, interestPortion,
						"Interest-only waiver", payment.getReference());

				logger.info("Interest-only waiver: Amount={}, interests reduced from {} to {}", interestPortion,
						interests, loan.getInterestsEarned());
			}
		} else {
			// Both flags true or both false → proportional between penalties and interests
			BigDecimal totalPenaltiesAndInterests = penalties.add(interests);
			if (totalPenaltiesAndInterests.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal penaltyPortion, interestPortion;
				if (amount.compareTo(totalPenaltiesAndInterests) <= 0) {
					if (penalties.compareTo(BigDecimal.ZERO) == 0) {
						penaltyPortion = BigDecimal.ZERO;
						interestPortion = amount;
					} else if (interests.compareTo(BigDecimal.ZERO) == 0) {
						penaltyPortion = amount;
						interestPortion = BigDecimal.ZERO;
					} else {
						BigDecimal ratio = penalties.divide(totalPenaltiesAndInterests, 10, RoundingMode.HALF_UP);
						penaltyPortion = amount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
						interestPortion = amount.subtract(penaltyPortion);
					}

					loan.setPenaltyEarned(penalties.subtract(penaltyPortion));
					loan.setInterestsEarned(interests.subtract(interestPortion));
					loan.setExemptedPenalties(penaltyPortion.add(totalExemptedPenalties));
					loan.setExemptedInterests(interestPortion.add(totalExemptedInterests));
					loan.setExemptedAmount(amount.add(totalExempted));
					loan.setExempted(true);

					if (penaltyPortion.compareTo(BigDecimal.ZERO) > 0) {
						loanStatementService.recordPenaltyWaiver(loan.getLoanApplicationId(), null, penaltyPortion,
								"Waiver applied to penalties", payment.getReference());
					}
					if (interestPortion.compareTo(BigDecimal.ZERO) > 0) {
						loanStatementService.recordInterestWaiver(loan.getLoanApplicationId(), null, interestPortion,
								"Waiver applied to interests", payment.getReference());
					}
				} else {
					// Amount exceeds total penalties+interests – clear both
					loan.setPenaltyEarned(BigDecimal.ZERO);
					loan.setInterestsEarned(BigDecimal.ZERO);
					loan.setExemptedPenalties(penalties.add(totalExemptedPenalties));
					loan.setExemptedInterests(interests.add(totalExemptedInterests));
					loan.setExemptedAmount(penalties.add(interests).add(totalExempted));
					loan.setExempted(true);

					if (penalties.compareTo(BigDecimal.ZERO) > 0) {
						loanStatementService.recordPenaltyWaiver(loan.getLoanApplicationId(), null, penalties,
								"Full penalty waiver", payment.getReference());
					}
					if (interests.compareTo(BigDecimal.ZERO) > 0) {
						loanStatementService.recordInterestWaiver(loan.getLoanApplicationId(), null, interests,
								"Full interest waiver", payment.getReference());
					}
				}
			}
			BigDecimal writtenOffAmount = totalExempted;
			String writeOffReason = payment.getWriteOffWaiverReason() != null ? payment.getWriteOffWaiverReason()
					: "The debtor was not able to complete the remaining balance due to some circumstances.";

			Date writeOffDate = new Date();
			String approvedBy = payment.getApprovedBy() != null ? payment.getApprovedBy().getFullName() : "System";

			Long reminderId = null;
			if (payment.getPaymentMethod() != null && payment.getPaymentMethod().equals(PaymentType.WRITE_OFF)) {
				smsHandlersService.handleWriteOffNotification(loan, writtenOffAmount, writeOffReason, writeOffDate,
						approvedBy, reminderId);
				if (!loan.getGuarantors().isEmpty()) {
					for (MNextOfKin guarantor : loan.getGuarantors()) {
						smsHandlersService.handleGuarantorWriteOffNotification(guarantor, loan, writtenOffAmount,
								writeOffReason, writeOffDate, approvedBy, reminderId);

					}
				}
			}
			if (payment.getPaymentMethod() != null && payment.getPaymentMethod().equals(PaymentType.WAIVER)) {
				smsHandlersService.handleWaiverNotification(loan, writtenOffAmount, writeOffReason, writeOffDate,
						approvedBy, reminderId);
				if (!loan.getGuarantors().isEmpty()) {
					for (MNextOfKin guarantor : loan.getGuarantors()) {
						smsHandlersService.handleGuarantorWaiverNotification(guarantor, loan, writtenOffAmount,
								writeOffReason, writeOffDate, approvedBy, reminderId);
					}
				}
			}

		}

		// Save updated loan and mark payment as applied
		loanApplicationRepository.save(loan);

	}

	/**
	 * Installment‑based waiver: replicates normal payment allocation. Reduces
	 * installments and loan balance accordingly. Does NOT call the flag‑based
	 * method.
	 */
	private void applyInstallmentBasedWaiver(MPayments payment) {
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

				// Record waiver statement for the installment
				loanStatementService.recordWaiver(null, inst.getInstallmentId(), paymentToApply,
						"Waiver applied to installment", payment.getReference());
			}
		} else {
			// No installments – apply directly to loan balance
			BigDecimal amountToApply = paymentAmount.min(loan.getBalance());
			loan.setBalance(loan.getBalance().subtract(amountToApply).max(BigDecimal.ZERO));
			remainingPayment = remainingPayment.subtract(amountToApply);
			logger.info("No installments found — applied directly to loan balance: " + amountToApply);
		}

		// Adjust installment distribution balance if needed
		if (paymentAmount.compareTo(installmentAmount) > 0 && loan.getLoanProductConfiguration()
				.getRepaymentScheduleType().equals(RepaymentScheduleTypeEnum.INSTALLMENTS)) {
			BigDecimal extra = paymentAmount.subtract(installmentAmount);
			loan.setInstallmentDistributionBalance(
					(loan.getInstallmentDistributionBalance() != null ? loan.getInstallmentDistributionBalance()
							: BigDecimal.ZERO).subtract(extra));
			loan.setBalance(loan.getBalance().subtract(paymentAmount).max(BigDecimal.ZERO));
		} else {
			BigDecimal totalPaid = paymentAmount.subtract(remainingPayment);
			if (!unpaidInstallments.isEmpty() && totalPaid.compareTo(BigDecimal.ZERO) > 0) {
				loan.setBalance(loan.getBalance().subtract(totalPaid).max(BigDecimal.ZERO));
			}
		}

		// Handle declining balance interest
		if (loan.getLoanProductConfiguration().getInterestCalculationMethod() != null
				&& loan.getLoanProductConfiguration().getInterestCalculationMethod()
						.equals(InterestCalculationMethodEnum.DECLINING_BALANCE)) {
			if (paymentAmount.compareTo(loan.getDecliningInterest()) >= 0) {
				BigDecimal remaining = paymentAmount.subtract(loan.getDecliningInterest());
				loan.setDecliningPrincipal(loan.getDecliningPrincipal().subtract(remaining));
				loan.setDecliningInterest(BigDecimal.ZERO);
			} else {
				loan.setDecliningInterest(loan.getDecliningInterest().subtract(paymentAmount));
			}
		}

		// Update loan repayment status
		if (loan.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
			loan.setDocStatus(DocStatus.CLOSED);
			loan.setRepaymentStatus(LoanRepaymentStatus.PAID);
		} else {
			loan.setRepaymentStatus(LoanRepaymentStatus.PARTIALLY_PAID);
		}

		loanApplicationRepository.saveAndFlush(loan);

		payment.setInstallments(affectedInstallments);
		payment.setIspaid(true);
		payment.setWaiverWriteOff(true);
		payment.setWaiverOrWriteOff(true);
		paymentRepository.saveAndFlush(payment);

		logger.info(String.format("Waiver applied → Loan[%d]: Before=%s, Waived=%s, After=%s",
				loan.getLoanApplicationId(), originalBalance, paymentAmount, loan.getBalance()));
	}

	// ======================= PUBLIC METHOD FOR WORKFLOW =======================

	@Transactional
	public void processApprovedWaiver(MPayments payment) {
		if (payment.getLoan() != null) {

			applyFlagBasedWaiver(payment, payment.getLoan());

			applyInstallmentBasedWaiver(payment);

		} else {
			throw new SetUpExceptions("Approved waiver has no associated loan or bill.");
		}
	}

	// ======================= VALIDATION METHODS =======================

	private void validateLoanWaiverRequest(MLoanApplication loan, BigDecimal amount, String reference,
			String paymentDate) {
		if (loan.getBalance() == null || loan.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
			throw new RuntimeException("This loan has already been fully paid.");
		}
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new RuntimeException("Invalid waiver amount. Must be greater than zero.");
		}
		if (loan.getBalance().compareTo(amount) < 0) {
			throw new RuntimeException(
					"Waiver amount cannot exceed the outstanding loan balance. Balance: " + loan.getBalance());
		}

		MPayments existing = paymentRepository
				.findTop1ByIsActiveAndAdOrgIDAndReferenceAndApprovalStageAndIspaidOrderByPaymentIdDesc(true,
						utils.getAD_Org_ID(), reference, ApprovalStage.APPROVED, true);
		if (existing != null) {
			throw new RuntimeException("A payment with reference " + reference + " already exists.");
		}

		if (paymentDate != null && !paymentDate.trim().isEmpty()) {
			try {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				LocalDate paymentLocalDate = LocalDate.parse(paymentDate.trim(), formatter);
				LocalDate disbursementDate = loan.getExpectedDisbursementDate().toInstant()
						.atZone(ZoneId.systemDefault()).toLocalDate();
				if (paymentLocalDate.isBefore(disbursementDate)) {
					throw new SetUpExceptions("Waiver date cannot be before loan disbursement date.");
				}
			} catch (DateTimeParseException e) {
				throw new RuntimeException("Invalid date format. Expected yyyy-MM-dd");
			}
		}
	}

	// ======================= HELPERS =======================

	private String formatCurrency(BigDecimal amount) {
		return "Ksh " + amount.setScale(2, RoundingMode.HALF_UP).toString();
	}
}