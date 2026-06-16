package co.ke.tezza.loanapp.service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MApprovalSteps;
import co.ke.tezza.loanapp.entity.MPaymentApprovalConfiguration;
import co.ke.tezza.loanapp.entity.MPaymentMethod;
import co.ke.tezza.loanapp.entity.MPayments;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.PaymentType;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.PaymentApprovalRequestIds;
import co.ke.tezza.loanapp.repository.PaymentApprovalConfigurationRepository;
import co.ke.tezza.loanapp.repository.PaymentRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.response.AmendmentRequestId;
import co.ke.tezza.loanapp.response.PaymentApprovalResponse;
import co.ke.tezza.loanapp.response.PaymentResponse;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

@Service
public class PaymentApprovalWorkflowService {

	@Autowired
	private PaymentRepository paymentsRepository;

	@Autowired
	private PaymentApprovalConfigurationRepository paymentApprovalConfigurationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private Utils utils;

	@Autowired
	private ObjectsMapper objectsMapper;

	@Autowired
	private PaymentsService paymentsService;

	@Autowired
	private PaymentApprovalService paymentApprovalService;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	@Autowired
	private WaiverWriteOffService waiverWriteOffService;

	private String getPaymentTypeDescription(MPayments payment) {
		return payment.isWaiverOrWriteOff() ? "Waiver/Write‑Off" : "Payment";
	}

	/**
	 * Trigger approval workflow for a payment
	 */
	@Transactional
	public void triggerPaymentApprovalStep(MApprovalSteps step, MPayments payment) {
		MRoles role = step.getRoleInvolved();
		if (role == null) {
			throw new SetUpExceptions("Approval step does not have a role assigned.");
		}

		// Get payment requester information
		MUser paymentRequester = paymentApprovalService.getPaymentRequestedBy(payment);
		String requesterName = paymentRequester != null ? paymentRequester.getFullName() : "User";
		String paymentRef = payment.getDocumentNo();
		String loanRef = payment.getLoan() != null ? payment.getLoan().getDocumentNo() : "N/A";
		String paymentAmount = "KES " + payment.getAmount();
		Integer stepNumber = step.getStep();

		// Send notifications to each approver
		notifyNextApprovers(step, paymentRequester != null ? paymentRequester : utils.getLoggedInUser(), paymentRef,
				new Date(), paymentAmount, stepNumber, loanRef, requesterName, getPaymentTypeDescription(payment));

		System.out.println("Triggered approval step " + step.getStep() + " for payment " + payment.getDocumentNo());
	}

	/**
	 * Approve a payment at the current workflow step
	 */
	@Transactional
	public ResponseEntity<PaymentResponse> approvePayment(Long paymentId) {
		int code = 200;
		MPayments payment = paymentsRepository.findById(paymentId)
				.orElseThrow(() -> new SetUpExceptions("Payment not found"));

		if (payment.getApprovalStage() != null && payment.getApprovalStage().equals(ApprovalStage.APPROVED)) {
			throw new SetUpExceptions("This payment has already been approved.");
		}

		if (payment.getApprovalStage() != null && payment.getApprovalStage().equals(ApprovalStage.REJECTED)) {
			throw new SetUpExceptions("This payment has already been rejected.");
		}

		MUser currentUser = utils.getLoggedInUser();
		if (currentUser.getUserId().equals(payment.getCreatedBy())) {
			throw new SetUpExceptions("Action prohibited. You cannot approve your own request.");
		}

		// Find the payment method and its approval configuration
		MPaymentMethod paymentMethod = payment.getPaymentMode();
		if (paymentMethod == null) {
			throw new SetUpExceptions("Payment method not found for this payment.");
		}

		MPaymentApprovalConfiguration config = paymentApprovalConfigurationRepository
				.findTop1ByIsActiveAndAdOrgIDAndPaymentMethod(true, payment.getAdOrgID(), paymentMethod);

		if (config == null) {
			throw new SetUpExceptions("No approval configuration found for payment method: " + paymentMethod.getName());
		}

		List<MApprovalSteps> steps = config.getApprovalLevels().stream()
				.sorted(Comparator.comparingInt(MApprovalSteps::getStep)).collect(Collectors.toList());

		String message = "Payment has been successfully approved.";

		MApprovalSteps currentStep = steps.stream().filter(s -> s.getStep() == payment.getCurrentApprovalLevel())
				.findFirst().orElseThrow(() -> new SetUpExceptions("No approval step matching current status"));

		MApprovalSteps previousStep = objectsMapper
				.getCurrentPaymentApprovalLevel(payment.getCurrentApprovalLevel() - 1, config);
		MRoles previousRole = previousStep != null ? previousStep.getRoleInvolved() : null;
		DocStatus previousDocStatus = payment.getDocStatus();
		ApprovalStage previousApprovalStage = payment.getApprovalStage();

		// Check that the user has the right role to approve this step
		if (!currentUser.getRoles().contains(currentStep.getRoleInvolved())) {
			throw new SetUpExceptions("You are not authorized to approve at this step");
		}

		// Check if user is trying to approve their own payment
		if (currentUser.getUserId() == payment.getCreatedBy()) {
			throw new SetUpExceptions("You are not allowed to approve your own payment request.");
		}

		// Record who approved
		payment.setApprovedBy(currentUser);
		payment.setApprovalDate(new Date());

		// Is there a next step?
		int nextStepNumber = currentStep.getStep() + 1;
		MApprovalSteps nextStep = steps.stream().filter(s -> s.getStep() == nextStepNumber).findFirst().orElse(null);

		// Get payment requester information for notifications

		Integer currentStepNumber = payment.getCurrentApprovalLevel();
		Integer totalSteps = config.getRequiredAprrovalSteps();

		if (nextStep != null) {
			// Forward to next approver
			message = "Dear " + currentUser.getFullName() + ",\n\n"
					+ "You have successfully approved the payment at level " + currentStep.getStep() + ".\n"
					+ "The payment has now been forwarded to " + nextStep.getRoleInvolved().getFormattedName()
					+ " for the next level of approval.\n\n" + "Thank you.";

			// Update payment status to pending next approval
			payment.setApprovalStage(currentStep.getApprovalStage());
			payment.setDocStatus(currentStep.getTrigureStatus());
			payment.setCurrentApprovalLevel(currentStep.getStep());
			payment.setDocStatus(currentStep.getTrigureStatus());
			payment.setApprovalStage(currentStep.getApprovalStage());

			// Advance to next step
			triggerPaymentApprovalStep(nextStep, payment);

			// Notify requester (forwarded, not final approval)
			notifyRequester(payment, true, currentUser.getFullName(), null, currentStepNumber, totalSteps);

		} else {

			message = "Payment has been successfully approved and processed.";

			// Notify requester (final approval)
			completeApproval(payment);
			notifyRequester(payment, true, currentUser.getFullName(), null, currentStepNumber, totalSteps);

		}

		// Record approval history
		DocStatus newDocStatus = payment.getDocStatus();
		ApprovalStage newApprovalStage = payment.getApprovalStage();
		Integer maximumSteps = config.getRequiredAprrovalSteps();

		objectsMapper.recordPaymentApprovalHistory(payment, true, currentStep, maximumSteps, previousDocStatus,
				previousApprovalStage, previousRole, newDocStatus, newApprovalStage,
				payment.getPaymentMode().getName());

		paymentsRepository.save(payment);

		return new ResponseEntity<PaymentResponse>(message, code, objectsMapper.mapPayment(payment));
	}

	public void completeApproval(MPayments payment) {
		payment.setApprovalStage(ApprovalStage.APPROVED);
		payment.setDocStatus(DocStatus.COMPLETED);
		payment.setApproved(true);

		if (payment.isWaiverWriteOff() || payment.isWaiverOrWriteOff()) {
			// Waiver/write-off should not be marked as paid
			payment.setIspaid(true);
			waiverWriteOffService.processApprovedWaiver(payment);
		} else {
			// Normal payment – mark as paid and process via PaymentsService
			payment.setIspaid(true);
			if (payment.getLoan() != null) {

				paymentsService.processUnifiedPaymentAllocation(payment, true);

			}

			sendSmsNotificationOnPaymentApproval(payment);
		}

	}

	/**
	 * Reject a payment
	 */
	@Transactional
	public ResponseEntity<PaymentResponse> rejectPayment(Long paymentId, String reason) {
		String message = "The payment has been rejected successfully, and a notification has been sent to the requester.";
		int code = 200;
		MPayments payment = paymentsRepository.findById(paymentId)
				.orElseThrow(() -> new SetUpExceptions("Payment not found"));

		MUser currentUser = utils.getLoggedInUser();
		if (currentUser.getUserId().equals(payment.getCreatedBy())) {
			throw new SetUpExceptions("You are not allowed to reject your own payment request.");
		}

		if (payment.getApprovalStage() != null && payment.getApprovalStage().equals(ApprovalStage.APPROVED)) {
			throw new SetUpExceptions("This payment has already been approved.");
		}

		if (payment.getApprovalStage() != null && payment.getApprovalStage().equals(ApprovalStage.REJECTED)) {
			throw new SetUpExceptions("This payment has already been rejected.");
		}

		// Find the configuration and current step
		MPaymentMethod paymentMethod = payment.getPaymentMode();
		if (paymentMethod == null) {
			throw new SetUpExceptions("Payment method not found.");
		}

		MPaymentApprovalConfiguration config = paymentApprovalConfigurationRepository
				.findTop1ByIsActiveAndAdOrgIDAndPaymentMethod(true, payment.getAdOrgID(), paymentMethod);

		if (config == null) {
			throw new SetUpExceptions("No approval configuration found for this payment method.");
		}

		List<MApprovalSteps> steps = config.getApprovalLevels().stream()
				.sorted(Comparator.comparingInt(MApprovalSteps::getStep)).collect(Collectors.toList());

		MApprovalSteps currentStep = steps.stream().filter(s -> s.getStep() == payment.getCurrentApprovalLevel())
				.findFirst().orElseThrow(() -> new SetUpExceptions("No approval step matching current status"));

		MApprovalSteps previousStep = objectsMapper
				.getCurrentPaymentApprovalLevel(payment.getCurrentApprovalLevel() - 1, config);
		MRoles previousRole = previousStep != null ? previousStep.getRoleInvolved() : null;
		DocStatus previousDocStatus = payment.getDocStatus();
		ApprovalStage previousApprovalStage = payment.getApprovalStage();

		if (!currentUser.getRoles().contains(currentStep.getRoleInvolved())) {
			throw new SetUpExceptions("You are not authorized to reject at this step");
		}

		// Mark rejection
		payment.setRejectedBy(currentUser);
		payment.setReasonForRejection(reason);
		payment.setDocStatus(currentStep.getRejectiontrigeredStatus());
		payment.setApprovalStage(ApprovalStage.REJECTED);
		payment.setApproved(false);
		payment.setIspaid(false);

		paymentsRepository.save(payment);

		DocStatus newDocStatus = payment.getDocStatus();
		ApprovalStage newApprovalStage = payment.getApprovalStage();
		Integer maximumSteps = config.getRequiredAprrovalSteps();

		objectsMapper.recordPaymentApprovalHistory(payment, true, currentStep, maximumSteps, previousDocStatus,
				previousApprovalStage, previousRole, newDocStatus, newApprovalStage,
				payment.getPaymentMode().getName());

		// Get payment requester information for notifications
		MUser paymentRequester = paymentApprovalService.getPaymentRequestedBy(payment);
		String requesterName = paymentRequester != null ? paymentRequester.getFullName() : "User";
		String paymentRef = payment.getDocumentNo();
		String paymentAmount = "KES " + payment.getAmount();
		String loanRef = payment.getLoan() != null ? payment.getLoan().getDocumentNo() : "N/A";
		Integer currentStepNumber = payment.getCurrentApprovalLevel();
		Integer totalSteps = config.getRequiredAprrovalSteps();

		// Notify requester
		notifyRequester(payment, false, currentUser.getFullName(), reason, currentStepNumber, totalSteps);

		// Send SMS notification
		sendSmsNotificationOnPaymentRejection(payment, reason, currentUser);

		return new ResponseEntity<PaymentResponse>(message, code, objectsMapper.mapPayment(payment));
	}

	/**
	 * Notify the next approvers in the workflow (now includes payment type).
	 */
	private void notifyNextApprovers(MApprovalSteps nextStep, MUser actionedBy, String referenceNo, Date dateActioned,
			String paymentAmount, Integer stepNumber, String loanRef, String requesterName, String paymentTypeDesc) {
		Set<MUser> usersWithNextRole = nextStep.getResponsiblePersons();

		if (usersWithNextRole.isEmpty()) {
			return;
		}

		String smsMessage = null;
		String emailSubject = null;
		String emailMessage = null;

		for (MUser user : usersWithNextRole) {
			if (stepNumber == 1) {
				// Initial request notification
				smsMessage = String.format(
						"Dear %s, %s has submitted a %s approval request (Ref: %s for loan %s, Amount: %s). "
								+ "Please review and take appropriate action promptly.",
						user.getFullName(), actionedBy.getFullName(), paymentTypeDesc, referenceNo, loanRef,
						paymentAmount);

				emailSubject = String.format("New %s Approval Request - %s", paymentTypeDesc, referenceNo);
				emailMessage = buildInitialRequestEmailContent(user, actionedBy, referenceNo, dateActioned,
						paymentAmount, loanRef, requesterName, paymentTypeDesc);
			} else {
				// Forwarded request notification
				smsMessage = String.format(
						"Dear %s, %s has forwarded %s request %s (Loan: %s, Amount: %s) for your review. "
								+ "Please process at your earliest convenience.",
						user.getFullName(), actionedBy.getFullName(), paymentTypeDesc, referenceNo, loanRef,
						paymentAmount);

				emailSubject = String.format("Forwarded: %s Approval Request - %s", paymentTypeDesc, referenceNo);
				emailMessage = buildForwardedRequestEmailContent(user, actionedBy, referenceNo, dateActioned,
						paymentAmount, loanRef, requesterName, stepNumber - 1, paymentTypeDesc);
			}

			// Send email
			utils.sendEmail(user, emailMessage, emailSubject);

			// Send SMS if user has phone number
			if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
				utils.saveSmsLoanAmendmentAprrovalSms(user.getPhoneNumber(), smsMessage, user.getAdOrgId(),
						user.getAdClientId(), LocalDateTime.now());
			}
		}
	}

	/**
	 * Notify payment requester about approval/rejection – now includes payment
	 * type.
	 */
	private void notifyRequester(MPayments payment, boolean isApproved, String actionedBy, String reason,
			Integer currentStep, Integer totalSteps) {

		MUser requester = paymentApprovalService.getPaymentRequestedBy(payment);
		if (requester == null) {
			return;
		}

		String paymentTypeDesc = getPaymentTypeDescription(payment);
		String referenceNo = payment.getDocumentNo();
		String smsMessage = null;
		String emailSubject = null;
		String emailMessage = null;
		String paymentAmount = "KES " + payment.getAmount();
		String loanRef = payment.getLoan() != null ? payment.getLoan().getDocumentNo() : "";
		String paymentMethod = payment.getPaymentMode() != null ? payment.getPaymentMode().getName() : paymentTypeDesc;

		if (isApproved) {
			if (currentStep < totalSteps) {
				// Forwarded to next level (not final approval)
				smsMessage = String.format(
						"Dear %s, your %s request %s (Loan: %s, Amount: %s) has been approved at level %d by %s "
								+ "and forwarded for further review.",
						requester.getFullName(), paymentTypeDesc, referenceNo, loanRef, paymentAmount, currentStep,
						actionedBy);

				emailSubject = String.format("Update: %s Request Forwarded - %s", paymentTypeDesc, referenceNo);
				emailMessage = buildRequesterForwardedEmailContent(requester, referenceNo, actionedBy, currentStep,
						totalSteps, paymentAmount, loanRef, paymentMethod, paymentTypeDesc);
			} else {
				// Final approval
				smsMessage = String.format(
						"Dear %s, your %s request %s (Loan: %s, Amount: %s) has been fully approved by %s. "
								+ "The %s has been processed successfully.",
						requester.getFullName(), paymentTypeDesc, referenceNo, loanRef, paymentAmount, actionedBy,
						paymentTypeDesc.toLowerCase());

				emailSubject = String.format("Approved: %s Request - %s", paymentTypeDesc, referenceNo);
				emailMessage = buildRequesterApprovedEmailContent(requester, referenceNo, actionedBy, paymentAmount,
						loanRef, paymentMethod, payment.getReference(), paymentTypeDesc);
			}
		} else {
			// Rejected
			smsMessage = String.format(
					"Dear %s, your %s request %s (Loan: %s, Amount: %s) has been rejected by %s. Reason: %s",
					requester.getFullName(), paymentTypeDesc, referenceNo, loanRef, paymentAmount, actionedBy, reason);

			emailSubject = String.format("Rejected: %s Request - %s", paymentTypeDesc, referenceNo);
			emailMessage = buildRequesterRejectedEmailContent(requester, referenceNo, actionedBy, reason, paymentAmount,
					loanRef, paymentMethod, paymentTypeDesc);
		}

		// Send email
		utils.sendEmail(requester, emailMessage, emailSubject);

		// Send SMS if requester has phone number
		if (requester.getPhoneNumber() != null && !requester.getPhoneNumber().trim().isEmpty()) {
			utils.saveSmsLoanAmendmentAprrovalSms(requester.getPhoneNumber(), smsMessage, payment.getAdOrgID(),
					payment.getAdClientId(), LocalDateTime.now());
		}
	}

	// ==================== EMAIL CONTENT BUILDERS (updated with paymentType)
	// ====================

	private String buildInitialRequestEmailContent(MUser recipient, MUser requester, String referenceNo,
			Date dateActioned, String paymentAmount, String loanRef, String borrowerName, String paymentTypeDesc) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

		return String.format("A new %s approval request has been submitted for your review.<br><br>"
				+ "<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;\">"
				+ "<p><strong>%s Reference:</strong> %s</p>" + "<p><strong>Loan Reference:</strong> %s</p>"
				+ "<p><strong>Submitted By:</strong> %s</p>" + "<p><strong>Borrower:</strong> %s</p>"
				+ "<p><strong>Amount:</strong> %s</p>" + "<p><strong>Submission Date:</strong> %s</p>" + "</div>"
				+ "<p>Please log in to the system to review this request and take appropriate action.</p>"
				+ "<p>Kindly process this request at your earliest convenience.</p>", paymentTypeDesc, paymentTypeDesc,
				referenceNo, loanRef, requester.getFullName(), borrowerName, paymentAmount,
				dateFormat.format(dateActioned));
	}

	private String buildForwardedRequestEmailContent(MUser recipient, MUser requester, String referenceNo,
			Date dateActioned, String paymentAmount, String loanRef, String borrowerName, Integer previousStep,
			String paymentTypeDesc) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

		return String.format("A %s approval request has been forwarded to you for further review.<br><br>"
				+ "<div style=\"background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;\">"
				+ "<p><strong>%s Reference:</strong> %s</p>" + "<p><strong>Loan Reference:</strong> %s</p>"
				+ "<p><strong>Forwarded By:</strong> %s</p>" + "<p><strong>Borrower:</strong> %s</p>"
				+ "<p><strong>Amount:</strong> %s</p>" + "<p><strong>Previous Step:</strong> Level %d</p>"
				+ "<p><strong>Forward Date:</strong> %s</p>" + "</div>"
				+ "<p>Please log in to the system to review this request and proceed with the next approval step.</p>"
				+ "<p>Your prompt attention to this matter is appreciated.</p>", paymentTypeDesc, paymentTypeDesc,
				referenceNo, loanRef, requester.getFullName(), borrowerName, paymentAmount, previousStep,
				dateFormat.format(dateActioned));
	}

	private String buildRequesterForwardedEmailContent(MUser requester, String referenceNo, String actionedBy,
			Integer currentStep, Integer totalSteps, String paymentAmount, String loanRef, String paymentMethod,
			String paymentTypeDesc) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

		return String.format("Your %s request has been reviewed and forwarded to the next approval level.<br><br>"
				+ "<div style=\"background-color: #f0f8ff; padding: 15px; border-radius: 5px; margin: 15px 0;\">"
				+ "<p><strong>%s Reference:</strong> %s</p>" + "<p><strong>Loan Reference:</strong> %s</p>"
				+ "<p><strong>Amount:</strong> %s</p>" + "<p><strong>%s Method:</strong> %s</p>"
				+ "<p><strong>Actioned By:</strong> %s</p>"
				+ "<p><strong>Current Status:</strong> Approved at Level %d of %d</p>"
				+ "<p><strong>Action Date:</strong> %s</p>" + "</div>"
				+ "<p>The request is now pending review at the next approval level.</p>"
				+ "<p>You will be notified once a final decision is made.</p>", paymentTypeDesc, paymentTypeDesc,
				referenceNo, loanRef, paymentAmount, paymentTypeDesc, paymentMethod, actionedBy, currentStep,
				totalSteps, dateFormat.format(new Date()));
	}

	private String buildRequesterApprovedEmailContent(MUser requester, String referenceNo, String actionedBy,
			String paymentAmount, String loanRef, String paymentMethod, String transactionReference,
			String paymentTypeDesc) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

		return String.format("Congratulations! Your %s request has been fully approved.<br><br>"
				+ "<div style=\"background-color: #d4edda; padding: 15px; border-radius: 5px; margin: 15px 0;\">"
				+ "<p><strong>%s Reference:</strong> %s</p>" + "<p><strong>Loan Reference:</strong> %s</p>"
				+ "<p><strong>Amount:</strong> %s</p>" + "<p><strong>%s Method:</strong> %s</p>"
				+ "<p><strong>Approved By:</strong> %s</p>"
				+ (transactionReference != null ? "<p><strong>Transaction Reference:</strong> %s</p>" : "")
				+ "<p><strong>Approval Date:</strong> %s</p>"
				+ "<p><strong>Status:</strong> <span style=\"color: #28a745; font-weight: bold;\">APPROVED</span></p>"
				+ "</div>" + "<p>The %s has been processed successfully and applied to the loan.</p>"
				+ "<p>You can now view the updated loan balance in your account.</p>", paymentTypeDesc, paymentTypeDesc,
				referenceNo, loanRef, paymentAmount, paymentTypeDesc, paymentMethod, actionedBy,
				transactionReference != null ? transactionReference : "", dateFormat.format(new Date()),
				paymentTypeDesc.toLowerCase());
	}

	private String buildRequesterRejectedEmailContent(MUser requester, String referenceNo, String actionedBy,
			String reason, String paymentAmount, String loanRef, String paymentMethod, String paymentTypeDesc) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

		return String.format("Your %s request has been reviewed and requires your attention.<br><br>"
				+ "<div style=\"background-color: #f8d7da; padding: 15px; border-radius: 5px; margin: 15px 0;\">"
				+ "<p><strong>%s Reference:</strong> %s</p>" + "<p><strong>Loan Reference:</strong> %s</p>"
				+ "<p><strong>Amount:</strong> %s</p>" + "<p><strong>%s Method:</strong> %s</p>"
				+ "<p><strong>Actioned By:</strong> %s</p>" + "<p><strong>Action Date:</strong> %s</p>"
				+ "<p><strong>Status:</strong> <span style=\"color: #dc3545; font-weight: bold;\">REJECTED</span></p>"
				+ "<p><strong>Rejection Reason:</strong> %s</p>" + "</div>"
				+ "<p>You may need to review and resubmit the %s request with corrections.</p>"
				+ "<p>If you have any questions, please contact the approval team.</p>", paymentTypeDesc,
				paymentTypeDesc, referenceNo, loanRef, paymentAmount, paymentTypeDesc, paymentMethod, actionedBy,
				dateFormat.format(new Date()), reason != null ? reason : "Not specified",
				paymentTypeDesc.toLowerCase());
	}

	// ==================== SMS NOTIFICATIONS ====================

	private void sendSmsNotificationOnPaymentApproval(MPayments payment) {
		try {
			MUser requester = paymentApprovalService.getPaymentRequestedBy(payment);
			if (requester != null && requester.getPhoneNumber() != null
					&& !requester.getPhoneNumber().trim().isEmpty()) {

				String paymentTypeDesc = getPaymentTypeDescription(payment);
				String paymentRef = payment.getDocumentNo();
				String paymentAmount = "KES " + payment.getAmount();
				String loanRef = payment.getLoan() != null ? payment.getLoan().getDocumentNo() : "";

				String smsMessage = String.format(
						"Dear %s, your %s %s of %s for loan %s has been approved and processed successfully. Thank you.",
						requester.getFullName(), paymentTypeDesc.toLowerCase(), paymentRef, paymentAmount, loanRef);

				utils.saveSmsLoanAmendmentAprrovalSms(requester.getPhoneNumber(), smsMessage, payment.getAdOrgID(),
						payment.getAdClientId(), LocalDateTime.now());
			}
		} catch (Exception e) {
			System.err.println("Failed to send SMS notification on payment approval: " + e.getMessage());
		}
	}

	private void sendSmsNotificationOnPaymentRejection(MPayments payment, String reason, MUser rejectedBy) {
		try {
			MUser requester = paymentApprovalService.getPaymentRequestedBy(payment);
			if (requester != null && requester.getPhoneNumber() != null
					&& !requester.getPhoneNumber().trim().isEmpty()) {

				String paymentTypeDesc = getPaymentTypeDescription(payment);
				String paymentRef = payment.getDocumentNo();
				String paymentAmount = "KES " + payment.getAmount();
				String loanRef = payment.getLoan() != null ? payment.getLoan().getDocumentNo() : "";

				String smsMessage = String.format(
						"Dear %s, your %s %s of %s for loan %s has been rejected. Reason: %s. Contact support for assistance.",
						requester.getFullName(), paymentTypeDesc.toLowerCase(), paymentRef, paymentAmount, loanRef,
						reason);

				utils.saveSmsLoanAmendmentAprrovalSms(requester.getPhoneNumber(), smsMessage, payment.getAdOrgID(),
						payment.getAdClientId(), LocalDateTime.now());
			}
		} catch (Exception e) {
			System.err.println("Failed to send SMS notification on payment rejection: " + e.getMessage());
		}
	}

	public Page<PaymentApprovalResponse> getAllPendingPaymentsPendingApprovals(int page, int size, String dateFrom,
			String dateTo, String searchTerm, boolean waiverOrWriteOff) {
		long adOrgId = utils.getAD_Org_ID();
		Set<MRoles> currentRoles = utils.getLogedInUserRoles();
		MRoles role = currentRoles.stream().findFirst().orElse(null);
		Long requiredRoleId = role.getId();

		List<PaymentApprovalRequestIds> ids = getIds(page, size, searchTerm, dateFrom, dateTo, requiredRoleId, adOrgId,
				waiverOrWriteOff);
		List<PaymentApprovalResponse> approvals = new ArrayList<>();

		// Get total count for pagination
		long totalCount = getTotalCount(searchTerm, dateFrom, dateTo, requiredRoleId, adOrgId);
		if (!ids.isEmpty()) {
			for (PaymentApprovalRequestIds id : ids) {
				PaymentApprovalResponse response = new PaymentApprovalResponse();
				if (id.getPaymentApprovalConfigId() > 0) {
					MPaymentApprovalConfiguration config = paymentApprovalConfigurationRepository
							.findById(id.getPaymentApprovalConfigId()).orElse(null);
					response.setPaymentApprovalConfig(config);
				}
				if (id.getPaymentId() > 0) {
					response.setPayment(
							objectsMapper.mapPayment(paymentsRepository.findById(id.getPaymentId()).orElse(null)));
				}
				approvals.add(response);
			}
		}
		PageRequest pageRequest = PageRequest.of(page, size);
		return new PageImpl<>(approvals, pageRequest, totalCount);
	}

	private List<PaymentApprovalRequestIds> getIds(int page, int size, String searchTerm, String dateFrom,
			String dateTo, Long requiredRoleId, long adOrgId, boolean waiverOrWriteOff) {

		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("dateFrom", dateFrom);
		parameters.addValue("adOrgId", adOrgId);
		parameters.addValue("dateTo", dateTo);
		parameters.addValue("requiredRoleId", requiredRoleId);
		parameters.addValue("waiverOrWriteOff", waiverOrWriteOff);
		parameters.addValue("offset", page * size);
		parameters.addValue("limit", size);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT p.AD_Payment_ID, config.AD_Payment_Approval_Config_ID ");
		sql.append("FROM AD_Payment p ");
		sql.append("INNER JOIN AD_Payment_Method pm ON pm.AD_Payment_Method_ID = p.AD_Payment_Method_ID ");
		sql.append(
				"INNER JOIN AD_Payment_Approval_Config config ON config.AD_Payment_Method_ID = pm.AD_Payment_Method_ID ");
		sql.append(
				"INNER JOIN AD_Payment_Approval_Steps apstep ON apstep.AD_Payment_Approval_Config_ID = config.AD_Payment_Approval_Config_ID ");
		sql.append("INNER JOIN AD_Approval_Steps step ON apstep.AD_Approval_Step_ID = step.id ");
		sql.append("INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = p.AD_Loan_Application_ID ");

		// Add LEFT JOINs for search functionality
		sql.append("LEFT JOIN AD_Debtor ind ON ind.AD_Debtor_ID = l.AD_Debtor_ID ");
		sql.append(
				"LEFT JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID ");
		sql.append("LEFT JOIN AD_Group_Borrower grp ON grp.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID ");

		sql.append("WHERE step.AD_Role_ID = :requiredRoleId ");
		sql.append("AND step.step = p.current_approval_level ");
		sql.append("AND p.isactive = true ");
		sql.append("AND p.AD_Org_ID = :adOrgId ");
		sql.append("AND p.waiver_Or_Write_Off = :waiverOrWriteOff ");

		sql.append("AND p.is_paid = false ");
		sql.append("AND p.approvalstage !='APPROVED' ");
		sql.append("AND p.approvalstage !='REJECTED' ");
		sql.append("AND p.docstatus !='REJECTED' ");

		// FIX: Cast string parameters to timestamp for comparison with p.created
		if (dateFrom != null && dateTo != null) {
			sql.append("AND p.created BETWEEN CAST(:dateFrom AS TIMESTAMP) AND CAST(:dateTo AS TIMESTAMP) ");
		} else if (dateFrom != null) {
			sql.append("AND p.created >= CAST(:dateFrom AS TIMESTAMP) ");
		} else if (dateTo != null) {
			sql.append("AND p.created <= CAST(:dateTo AS TIMESTAMP) ");
		}

		// Add search condition if searchTerm is provided
		if (searchTerm != null && !searchTerm.trim().isEmpty()) {
			parameters.addValue("searchTerm", "%" + searchTerm.toLowerCase() + "%");

			sql.append("AND ( ");
			sql.append("   LOWER(p.documentNo) LIKE :searchTerm OR ");
			sql.append("   LOWER(p.description) LIKE :searchTerm OR ");
			sql.append("   LOWER(p.referenceNo) LIKE :searchTerm OR ");
			sql.append("   LOWER(p.payment_type) LIKE :searchTerm OR ");
			sql.append("   LOWER(l.documentNo) LIKE :searchTerm OR ");
			sql.append("   LOWER(pm.name) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.first_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.last_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.national_Id) LIKE :searchTerm OR ");
			sql.append("   ind.phone LIKE :searchTerm OR ");
			sql.append("   LOWER(inst.institution_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(inst.registration_Number) LIKE :searchTerm OR ");
			sql.append("   LOWER(grp.group_Name) LIKE :searchTerm ");
			sql.append(") ");
		}

		sql.append("ORDER BY p.updated ASC ");
		sql.append("LIMIT :limit OFFSET :offset");

		return namedParameterJdbcTemplate.query(sql.toString(), parameters, (rs, rowNum) -> {
			PaymentApprovalRequestIds id = new PaymentApprovalRequestIds();
			id.setPaymentId(rs.getLong("AD_Payment_ID"));
			id.setPaymentApprovalConfigId(rs.getLong("AD_Payment_Approval_Config_ID"));
			return id;
		});
	}

	private long getTotalCount(String searchTerm, String dateFrom, String dateTo, Long requiredRoleId, long adOrgId) {

		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("dateFrom", dateFrom);
		parameters.addValue("adOrgId", adOrgId);
		parameters.addValue("dateTo", dateTo);
		parameters.addValue("requiredRoleId", requiredRoleId);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(DISTINCT p.AD_Payment_ID) ");
		sql.append("FROM AD_Payment p ");
		sql.append("INNER JOIN AD_Payment_Method pm ON pm.AD_Payment_Method_ID = p.AD_Payment_Method_ID ");
		sql.append(
				"INNER JOIN AD_Payment_Approval_Config config ON config.AD_Payment_Method_ID = pm.AD_Payment_Method_ID ");
		sql.append(
				"INNER JOIN AD_Payment_Approval_Steps apstep ON apstep.AD_Payment_Approval_Config_ID = config.AD_Payment_Approval_Config_ID ");
		sql.append("INNER JOIN AD_Approval_Steps step ON apstep.AD_Approval_Step_ID = step.id ");
		sql.append("INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = p.AD_Loan_Application_ID ");

		// Add LEFT JOINs for search functionality
		sql.append("LEFT JOIN AD_Debtor ind ON ind.AD_Debtor_ID = l.AD_Debtor_ID ");
		sql.append(
				"LEFT JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID ");
		sql.append("LEFT JOIN AD_Group_Borrower grp ON grp.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID ");

		sql.append("WHERE step.AD_Role_ID = :requiredRoleId ");
		sql.append("AND step.step = p.current_approval_level ");
		sql.append("AND p.isactive = true ");
		sql.append("AND p.AD_Org_ID = :adOrgId ");
		sql.append("AND p.is_paid = false ");
		sql.append("AND p.approvalstage !='APPROVED' ");
		sql.append("AND p.approvalstage !='REJECTED' ");
		sql.append("AND p.docstatus !='REJECTED' ");

		// FIX: Cast string parameters to timestamp for comparison with p.created
		if (dateFrom != null && dateTo != null) {
			sql.append("AND p.created BETWEEN CAST(:dateFrom AS TIMESTAMP) AND CAST(:dateTo AS TIMESTAMP) ");
		} else if (dateFrom != null) {
			sql.append("AND p.created >= CAST(:dateFrom AS TIMESTAMP) ");
		} else if (dateTo != null) {
			sql.append("AND p.created <= CAST(:dateTo AS TIMESTAMP) ");
		}

		// Add search condition if searchTerm is provided
		if (searchTerm != null && !searchTerm.trim().isEmpty()) {
			parameters.addValue("searchTerm", "%" + searchTerm.toLowerCase() + "%");

			sql.append("AND ( ");
			sql.append("   LOWER(p.documentNo) LIKE :searchTerm OR ");
			sql.append("   LOWER(p.description) LIKE :searchTerm OR ");
			sql.append("   LOWER(p.referenceNo) LIKE :searchTerm OR ");
			sql.append("   LOWER(p.payment_type) LIKE :searchTerm OR ");
			sql.append("   LOWER(l.documentNo) LIKE :searchTerm OR ");
			sql.append("   LOWER(pm.name) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.first_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.last_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(ind.national_Id) LIKE :searchTerm OR ");
			sql.append("   ind.phone LIKE :searchTerm OR ");
			sql.append("   LOWER(inst.institution_Name) LIKE :searchTerm OR ");
			sql.append("   LOWER(inst.registration_Number) LIKE :searchTerm OR ");
			sql.append("   LOWER(grp.group_Name) LIKE :searchTerm ");
			sql.append(") ");
		}

		Long count = namedParameterJdbcTemplate.queryForObject(sql.toString(), parameters, Long.class);
		return count != null ? count : 0L;
	}

}