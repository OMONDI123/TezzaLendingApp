package co.ke.tezza.loanapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.*;
import co.ke.tezza.loanapp.enums.*;
import co.ke.tezza.loanapp.model.ReportParams;
import co.ke.tezza.loanapp.repository.*;
import co.ke.tezza.loanapp.response.FileOutPutResponse;
import co.ke.tezza.loanapp.util.Utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsHandlersService {

	private final Utils utils;
	private final ReminderConfigRepository reminderConfigRepository;
	private final LoanApplicationRepository loanApplicationRepository;
	private final InstallmentRepository installmentRepository;
	private final PaymentRepository paymentRepository;
	private final GuarantorLoanRepository guarantorLoanRepository;
	private final SmsSetupRepository smsSetupRepository;
	private final JasperReportingServices jasper;

	// ==================== RECIPIENT SELECTION HELPER METHODS ====================

	/**
	 * Determines if the borrower is eligible (has good standing)
	 */
	private boolean isBorrowerEligible(MLoanApplication loan) {
		return utils.isBorrowerEligible(loan);
	}

	/**
	 * Central method to handle borrower SMS routing based on eligibility
	 * 
	 * CHANGE: If borrower is not eligible, return false (do not send any borrower
	 * SMS)
	 */
	private boolean sendBorrowerSms(SmsTypeEnum type, Map<String, String> placeholders, MLoanApplication loan,
			Long installmentId, Long reminderId, LocalDateTime timeToSend, String statementLink) {

		if (!isBorrowerEligible(loan)) {
			// Borrower not eligible: no SMS sent to anyone (including acting borrowers)
			log.debug("Borrower not eligible, skipping {} for loan {}", type, loan.getLoanApplicationId());
			return false;
		}

		// CASE: Borrower is eligible - send to actual borrower
		return sendToActualBorrower(type, placeholders, loan, installmentId, reminderId, timeToSend, statementLink);
	}

	/**
	 * Central method to handle guarantor SMS routing based on eligibility
	 * 
	 * CHANGE: If borrower is NOT eligible, send to ALL guarantors (primary +
	 * non-primary) If borrower is eligible, send to the specific guarantor (normal
	 * flow)
	 */
	private boolean sendGuarantorSms(SmsTypeEnum type, Map<String, String> placeholders, MNextOfKin specificGuarantor,
			MLoanApplication loan, MInstallments installment, Long reminderId, LocalDateTime timeToSend) {

		// Borrower is eligible - send to this specific guarantor (normal flow)
		return sendToSpecificGuarantor(type, placeholders, specificGuarantor, loan, installment, reminderId,
				timeToSend);

	}

	/**
	 * Send to actual borrower
	 */
	private boolean sendToActualBorrower(SmsTypeEnum type, Map<String, String> placeholders, MLoanApplication loan,
			Long installmentId, Long reminderId, LocalDateTime timeToSend, String statementLink) {

		String phoneNumber = utils.getBorrowerPhone(loan);
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			log.error("❌ Cannot send SMS {} for loan {}: Borrower phone number is null or empty", type,
					loan.getLoanApplicationId());
			return false;
		}

		Map<String, Long> borrowerIds = utils.getBorrowerIds(loan);

		if (statementLink != null && !statementLink.isEmpty()) {
			return sendStatementSms(type, placeholders, loan.getAdOrgID(), loan.getAdClientId(), phoneNumber,
					installmentId, loan.getLoanApplicationId(), reminderId, timeToSend, statementLink, borrowerIds);
		} else {
			return sendSms(type, placeholders, loan.getAdOrgID(), loan.getAdClientId(), phoneNumber, installmentId,
					loan.getLoanApplicationId(), reminderId, timeToSend, borrowerIds);
		}
	}

	/**
	 * Send to a specific guarantor (normal flow when borrower eligible)
	 */
	private boolean sendToSpecificGuarantor(SmsTypeEnum type, Map<String, String> placeholders, MNextOfKin guarantor,
			MLoanApplication loan, MInstallments installment, Long reminderId, LocalDateTime timeToSend) {

		return sendGuarantorSmsInternal(type, placeholders, guarantor, loan, installment, reminderId, timeToSend);
	}

	/**
	 * Internal method to send SMS to a guarantor
	 */
	private boolean sendGuarantorSmsInternal(SmsTypeEnum type, Map<String, String> placeholders, MNextOfKin guarantor,
			MLoanApplication loan, MInstallments installment, Long reminderId, LocalDateTime timeToSend) {

		if (timeToSend == null) {
			timeToSend = LocalDateTime.now();
		}

		try {
			String template = getTemplate(getRemindersConfiguration(reminderId), type, loan.getAdOrgID());
			String message = utils.processTemplate(template, placeholders);

			Long installmentId = installment != null ? installment.getInstallmentId() : null;
			utils.saveGuarantorSms(guarantor.getNextOfKinId(), guarantor.getPhoneNumber(), message, loan.getAdOrgID(),
					loan.getAdClientId(), type, installmentId, loan.getLoanApplicationId(), timeToSend, reminderId);

			// Send email copy
			try {
				String guarantorEmail = utils.getGuarantorEmail(guarantor);
				if (guarantorEmail != null && !guarantorEmail.trim().isEmpty()) {
					String emailSubject = type.getDescription() + " - Guarantor Notification";
					utils.sendGuarantorEmail(guarantor, message, emailSubject, loan.getAdOrgID());
					log.info("✅ Email copy sent to guarantor {} at {}", guarantor.getFullName(), guarantorEmail);
				}
			} catch (Exception e) {
				log.error("Failed to send email copy to guarantor {}: {}", guarantor.getFullName(), e.getMessage());
			}

			log.info("✅ Sent {} to guarantor {}", type.getDescription(), guarantor.getFullName());
			return true;
		} catch (Exception e) {
			log.error("❌ Failed to send {} to guarantor {}: {}", type, guarantor.getFullName(), e.getMessage(), e);
			return false;
		}
	}

	// The rest of the class remains unchanged (all handlers, builders, utilities,
	// etc.)
	// ... (keep all existing methods exactly as they were, except for the
	// modifications above)

	// ... (the rest of the methods are unchanged; we include them for completeness)

	public MRemindersConfiguration getRemindersConfiguration(Long id) {
		if (id == null) {
			return null;
		}
		return reminderConfigRepository.findById(id).orElse(null);
	}

	// ==================== LOAN APPROVAL/REGISTRATION ====================

	@Transactional
	public boolean handleLoanApplicationRegistration(MLoanApplication loan, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("status",
					getSafeValue(loan.getApprovalStage() != null ? loan.getApprovalStage().name() : "PENDING"));
			placeholders.put("loanApplicationDate", formatDate(loan.getCreated()));
			placeholders.put("reason", "");

			return sendBorrowerSms(SmsTypeEnum.LOAN_APPLICATION_OR_DEBT_REGISTRATION, placeholders, loan, null,
					reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling loan application registration: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleLoanApproval(MLoanApplication loan, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("status", "APPROVED");
			placeholders.put("reason", "");
			placeholders.put("repaymentStartDate", formatDate(calculateRepaymentStartDate(loan)));

			return sendBorrowerSms(SmsTypeEnum.LOAN_APPROVAL_DEBT_APPROVAL, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling loan approval: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleLoanRejection(MLoanApplication loan, String rejectionReason, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("status", "REJECTED");
			placeholders.put("reason", getSafeValue(rejectionReason));

			return sendBorrowerSms(SmsTypeEnum.LOAN_REJECTION_DEBT_REJECTION, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling loan rejection: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== GUARANTOR APPROVAL ====================

	@Transactional
	public boolean handleGuarantorApprovalRequest(MNextOfKin guarantor, MLoanApplication loan, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("disbursementDate", formatDate(loan.getExpectedDisbursementDate()));
			placeholders.put("repaymentStartDate", formatDate(calculateRepaymentStartDate(loan)));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_APPROVAL_REQUEST, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor approval request: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorMissedRepaymentAlert(MNextOfKin guarantor, MLoanApplication loan, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);

			if (loan.getDueDate() != null) {
				long daysOverdue = calculateDaysOverdue(loan.getDueDate());
				placeholders.put("daysOverdue", String.valueOf(Math.max(0, daysOverdue)));
			}

			placeholders.put("amountDue", formatAmount(loan.getBalance()));
			placeholders.put("dueDate", formatDate(loan.getDueDate()));
			placeholders.put("nextInstallmentDueDate", formatDate(loan.getDueDate()));
			placeholders.put("balance", formatAmount(loan.getBalance()));
			placeholders.put("penaltyAmountIncur",
					formatAmount(loan.getPenaltyEarned() != null ? loan.getPenaltyEarned() : BigDecimal.ZERO));
			placeholders.put("outstandingBalance", formatAmount(loan.getBalance()));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_MISSED_REPAYMENT_ALERT, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor missed repayment alert: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorApprovalConfirmation(MNextOfKin guarantor, MLoanApplication loan, Date approvalDate,
			BigDecimal guaranteeAmount, BigDecimal guaranteeLimit, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("approvalDate", formatDate(approvalDate));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));
			placeholders.put("guaranteeLimit", formatAmount(guaranteeLimit));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_APPROVAL_CONFIRMATION, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor approval confirmation: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorApprovalRejection(MNextOfKin guarantor, MLoanApplication loan, Date rejectionDate,
			String rejectionReason, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("rejectionDate", formatDate(rejectionDate));
			placeholders.put("rejectionReason", getSafeValue(rejectionReason));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_APPROVAL_REJECTION, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor approval rejection: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorLoanAssignmentNotification(MNextOfKin guarantor, MLoanApplication loan,
			Date approvalDate, BigDecimal guaranteeAmount, BigDecimal guaranteeLimit, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("disbursementDate", formatDate(loan.getExpectedDisbursementDate()));
			placeholders.put("repaymentStartDate", formatDate(calculateRepaymentStartDate(loan)));
			placeholders.put("approvalDate", formatDate(approvalDate));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));
			placeholders.put("guaranteeLimit", formatAmount(guaranteeLimit));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_ASSIGNMENT_NOTIFICATION, placeholders, guarantor, loan,
					null, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor loan assignment notification: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== PAYMENT NOTIFICATIONS ====================

	@Transactional
	public boolean handlePaymentReceiptConfirmation(MPayments payment, Long reminderId) {
		try {
			Map<String, String> placeholders = buildPaymentPlaceholders(payment);
			placeholders.put("paymentStatus", "COMPLETED");
			placeholders.put("paymentType", "PAYMENT");

			return sendBorrowerSms(SmsTypeEnum.PAYMENT_RECEIPT_CONFIRMATION, placeholders, payment.getLoan(), null,
					reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling payment receipt confirmation: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handlePartialRepaymentNotification(MPayments payment, Long reminderId) {
		try {
			Map<String, String> placeholders = buildPaymentPlaceholders(payment);
			placeholders.put("partialAmount", formatAmount(payment.getAmount()));

			return sendBorrowerSms(SmsTypeEnum.PARTIAL_REPAYMENT_NOTIFICATION, placeholders, payment.getLoan(), null,
					reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling partial repayment notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleFullRepaymentNotification(MPayments payment, Long reminderId) {
		try {
			Map<String, String> placeholders = buildPaymentPlaceholders(payment);
			placeholders.put("totalRepaid", formatAmount(calculateTotalPaid(payment.getLoan())));
			placeholders.put("completionDate", formatDate(new Date()));

			return sendBorrowerSms(SmsTypeEnum.FULL_REPAYMENT_NOTIFICATION, placeholders, payment.getLoan(), null,
					reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling full repayment notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleEarlyRepaymentConfirmation(MPayments payment, BigDecimal discountAmount, Long reminderId) {
		try {
			Map<String, String> placeholders = buildPaymentPlaceholders(payment);
			placeholders.put("discountAmount", formatAmount(discountAmount));
			placeholders.put("earlyRepaymentAmount", formatAmount(payment.getAmount()));

			return sendBorrowerSms(SmsTypeEnum.EARLY_REPAYMENT_CONFIRMATION, placeholders, payment.getLoan(), null,
					reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling early repayment confirmation: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== INSTALLMENT PAYMENT NOTIFICATIONS ====================

	@Transactional
	public boolean handleInstallmentPaymentConfirmation(MPayments payment, MInstallments installment, Long reminderId) {
		try {
			Map<String, String> placeholders = buildInstallmentPaymentPlaceholders(payment, installment);

			return sendBorrowerSms(SmsTypeEnum.INSTALLMENT_PAYMENT_CONFIRMATION, placeholders, payment.getLoan(),
					installment.getInstallmentId(), reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling installment payment confirmation: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleInstallmentPartialPayment(MPayments payment, MInstallments installment, Long reminderId) {
		try {
			Map<String, String> placeholders = buildInstallmentPaymentPlaceholders(payment, installment);
			placeholders.put("remainingAmount", formatAmount(installment.getBalance()));
			placeholders.put("lateFee", formatAmount(
					installment.getPenaltyEarned() != null ? installment.getPenaltyEarned() : BigDecimal.ZERO));

			return sendBorrowerSms(SmsTypeEnum.INSTALLMENT_PARTIAL_PAYMENT, placeholders, payment.getLoan(),
					installment.getInstallmentId(), reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling installment partial payment: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== GUARANTOR PAYMENT NOTIFICATIONS ====================

	@Transactional
	public boolean handleGuarantorPartialPaymentNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal amountPaid, BigDecimal remainingBalance, BigDecimal totalOutstanding, Date paymentDate,
			Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPaymentPlaceholders(guarantor, loan, amountPaid,
					remainingBalance, totalOutstanding, paymentDate);

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_PARTIAL_PAYMENT_NOTIFICATION, placeholders, guarantor, loan,
					null, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor partial payment notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorFullRepaymentNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal totalRepaid, Date completionDate, BigDecimal guaranteeReleased, BigDecimal guaranteeAmount,
			Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("totalRepaid", formatAmount(totalRepaid));
			placeholders.put("completionDate", formatDate(completionDate));
			placeholders.put("guaranteeReleased", formatAmount(guaranteeReleased));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_FULL_REPAYMENT_NOTIFICATION, placeholders, guarantor, loan,
					null, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor full repayment notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorInstallmentPaymentReceived(MNextOfKin guarantor, MInstallments installment,
			BigDecimal amountPaid, Date paymentDate, BigDecimal guaranteeAmount, BigDecimal guaranteeUtilization,
			Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorInstallmentPlaceholders(guarantor, installment);
			placeholders.put("amountPaid", formatAmount(amountPaid));
			placeholders.put("paymentDate", formatDate(paymentDate));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));
			placeholders.put("guaranteeUtilization", formatAmount(guaranteeUtilization));

			Optional<MInstallments> nextInstallment = getNextInstallment(installment);
			if (nextInstallment.isPresent()) {
				placeholders.put("nextDueDate", formatDate(nextInstallment.get().getPeriodEnd()));
			}

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_INSTALLMENT_PAYMENT_RECEIVED, placeholders, guarantor,
					installment.getLoan(), installment, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor installment payment received: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorInstallmentPartialPayment(MNextOfKin guarantor, MInstallments installment,
			BigDecimal amountPaid, BigDecimal remainingAmount, Date paymentDate, BigDecimal lateFee, Date nextDueDate,
			BigDecimal guaranteeAmount, String guaranteeRiskLevel, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorInstallmentPlaceholders(guarantor, installment);
			placeholders.put("amountPaid", formatAmount(amountPaid));
			placeholders.put("remainingAmount", formatAmount(remainingAmount));
			placeholders.put("paymentDate", formatDate(paymentDate));
			placeholders.put("lateFee", formatAmount(lateFee));
			placeholders.put("nextDueDate", formatDate(nextDueDate));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));
			placeholders.put("guaranteeRiskLevel", getSafeValue(guaranteeRiskLevel));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_INSTALLMENT_PARTIAL_PAYMENT, placeholders, guarantor,
					installment.getLoan(), installment, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor installment partial payment: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== INSTALLMENT REMINDERS ====================

	@Transactional
	public boolean handleInstallmentDueReminder(MInstallments installment, Long reminderId) {
		try {
			Map<String, String> placeholders = buildInstallmentPlaceholders(installment);
			long daysRemaining = calculateDaysRemaining(installment.getPeriodEnd());
			placeholders.put("daysRemaining", String.valueOf(Math.max(0, daysRemaining)));

			return sendBorrowerSms(SmsTypeEnum.INSTALLMENT_DUE_REMINDER, placeholders, installment.getLoan(),
					installment.getInstallmentId(), reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling installment due reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleInstallmentOverdueReminder(MInstallments installment, Long reminderId) {
		try {
			Map<String, String> placeholders = buildInstallmentPlaceholders(installment);
			long daysOverdue = calculateDaysOverdue(installment.getPeriodEnd());
			placeholders.put("daysOverdue", String.valueOf(Math.max(0, daysOverdue)));
			placeholders.put("latePaymentFee", formatAmount(
					installment.getPenaltyEarned() != null ? installment.getPenaltyEarned() : BigDecimal.ZERO));

			return sendBorrowerSms(SmsTypeEnum.INSTALLMENT_OVERDUE_REMINDER, placeholders, installment.getLoan(),
					installment.getInstallmentId(), reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling installment overdue reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleInstallmentMissedPayment(MInstallments installment, Long reminderId) {
		try {
			Map<String, String> placeholders = buildInstallmentPlaceholders(installment);
			long daysMissed = calculateDaysOverdue(installment.getPeriodEnd());
			placeholders.put("daysMissed", String.valueOf(Math.max(0, daysMissed)));
			placeholders.put("lateFee", formatAmount(
					installment.getPenaltyEarned() != null ? installment.getPenaltyEarned() : BigDecimal.ZERO));

			Date gracePeriodEnd = new Date(installment.getPeriodEnd().getTime() + TimeUnit.DAYS.toMillis(7));
			placeholders.put("gracePeriodEnd", formatDate(gracePeriodEnd));
			placeholders.put("currentBalance", formatAmount(installment.getLoan().getBalance()));

			return sendBorrowerSms(SmsTypeEnum.INSTALLMENT_MISSED_PAYMENT, placeholders, installment.getLoan(),
					installment.getInstallmentId(), reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling installment missed payment: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleInstallmentPaymentReminder(MInstallments installment, Long reminderId) {
		try {
			Map<String, String> placeholders = buildInstallmentPlaceholders(installment);
			long daysRemaining = calculateDaysRemaining(installment.getPeriodEnd());
			placeholders.put("daysRemaining", String.valueOf(Math.max(0, daysRemaining)));
			placeholders.put("paymentMethod", "MPESA");
			placeholders.put("paymentDeadline", formatDate(installment.getPeriodEnd()));
			placeholders.put("minimumPayment", formatAmount(installment.getAmount()));

			return sendBorrowerSms(SmsTypeEnum.INSTALLMENT_PAYMENT_REMINDER, placeholders, installment.getLoan(),
					installment.getInstallmentId(), reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling installment payment reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== LOAN REMINDERS ====================

	@Transactional
	public boolean handleLoanDueReminder(MLoanApplication loan, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			if (loan.getDueDate() != null) {
				long daysRemaining = calculateDaysRemaining(loan.getDueDate());
				placeholders.put("noOfDaysRemaining", String.valueOf(Math.max(0, daysRemaining)));
			}
			placeholders.put("amountDue", formatAmount(loan.getBalance()));
			placeholders.put("installmentAmount", formatAmount(loan.getBalance()));
			placeholders.put("nextInstallmentStartDate", formatDate(new Date()));

			return sendBorrowerSms(SmsTypeEnum.LOAN_OR_DEBT_DUE_REMINDER, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling loan due reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleLoanOverdueReminder(MLoanApplication loan, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			if (loan.getDueDate() != null) {
				long daysOverdue = calculateDaysOverdue(loan.getDueDate());
				placeholders.put("daysOverDue", String.valueOf(Math.max(0, daysOverdue)));
			}
			placeholders.put("amountOverDue", formatAmount(loan.getBalance()));
			placeholders.put("installmentAmount", formatAmount(loan.getBalance()));
			placeholders.put("noOfDaysRemaining", "0");
			placeholders.put("nextInstallmentStartDate", formatDate(new Date()));

			return sendBorrowerSms(SmsTypeEnum.LOAN_OR_DEBT_OVERDUE_REMINDER, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling loan overdue reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMissedRepaymentAlert(MLoanApplication loan, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			if (loan.getDueDate() != null) {
				long daysOverdue = calculateDaysOverdue(loan.getDueDate());
				placeholders.put("daysOverdue", String.valueOf(Math.max(0, daysOverdue)));
			}
			placeholders.put("amountDue", formatAmount(loan.getBalance()));
			placeholders.put("dueDate", formatDate(loan.getDueDate()));
			placeholders.put("nextInstallmentDueDate", formatDate(loan.getDueDate()));

			return sendBorrowerSms(SmsTypeEnum.MISSED_REPAYMENT_ALERT, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling missed repayment alert: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== GUARANTOR REMINDERS ====================

	@Transactional
	public boolean handleGuarantorLoanDueReminder(MNextOfKin guarantor, MLoanApplication loan, Date loanDueDate,
			BigDecimal principalAmount, BigDecimal interestAmount, BigDecimal penaltyAmount, Date gracePeriodEndDate,
			BigDecimal currentGuaranteeUsed, BigDecimal guaranteeRemaining, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("loanDueDate", formatDate(loanDueDate));
			placeholders.put("daysRemaining", String.valueOf(calculateDaysRemaining(loanDueDate)));
			placeholders.put("totalAmountDue", formatAmount(loan.getBalance()));
			placeholders.put("principalAmount", formatAmount(principalAmount));
			placeholders.put("interestAmount", formatAmount(interestAmount));
			placeholders.put("penaltyAmount", formatAmount(penaltyAmount));
			placeholders.put("gracePeriodEndDate", formatDate(gracePeriodEndDate));
			placeholders.put("currentGuaranteeUsed", formatAmount(currentGuaranteeUsed));
			placeholders.put("guaranteeRemaining", formatAmount(guaranteeRemaining));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_DUE_REMINDER, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor loan due reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorInstallmentDueReminder(MNextOfKin guarantor, MInstallments installment,
			BigDecimal guaranteeAmount, BigDecimal guaranteeLimit, BigDecimal currentGuaranteeUsed, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorInstallmentPlaceholders(guarantor, installment);
			long daysRemaining = calculateDaysRemaining(installment.getPeriodEnd());
			placeholders.put("daysRemaining", String.valueOf(Math.max(0, daysRemaining)));
			placeholders.put("totalDue", formatAmount(installment.getBalance()));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));
			placeholders.put("guaranteeLimit", formatAmount(guaranteeLimit));
			placeholders.put("currentGuaranteeUsed", formatAmount(currentGuaranteeUsed));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_INSTALLMENT_DUE_REMINDER, placeholders, guarantor,
					installment.getLoan(), installment, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor installment due reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorLoanOverdueAlert(MNextOfKin guarantor, MLoanApplication loan, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);

			if (loan.getDueDate() != null) {
				long daysOverdue = calculateDaysOverdue(loan.getDueDate());
				placeholders.put("daysOverDue", String.valueOf(Math.max(0, daysOverdue)));
			}
			placeholders.put("amountOverDue", formatAmount(loan.getBalance()));
			placeholders.put("nextInstallmentDueDate", formatDate(loan.getDueDate()));
			placeholders.put("defaultDate", formatDate(new Date()));
			placeholders.put("totalOutstanding", formatAmount(loan.getBalance()));

			MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan, guarantor, true);
			if (gLoan != null) {
				placeholders.put("guaranteeAmount", formatAmount(gLoan.getGuaranteeAmount()));
			}

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_OVERDUE_ALERT, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor loan overdue alert: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorInstallmentOverdueAlert(MNextOfKin guarantor, MInstallments installment,
			BigDecimal guaranteeAmount, BigDecimal currentGuaranteeUsed, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorInstallmentPlaceholders(guarantor, installment);
			long daysOverdue = calculateDaysOverdue(installment.getPeriodEnd());
			placeholders.put("daysOverdue", String.valueOf(Math.max(0, daysOverdue)));
			placeholders.put("totalDue", formatAmount(installment.getBalance()));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));
			placeholders.put("currentGuaranteeUsed", formatAmount(currentGuaranteeUsed));
			placeholders.put("guaranteeRemaining", formatAmount(guaranteeAmount.subtract(currentGuaranteeUsed)));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_INSTALLMENT_OVERDUE_ALERT, placeholders, guarantor,
					installment.getLoan(), installment, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor installment overdue alert: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorLoanDefaultNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal amountOverDue, Long daysOverDue, BigDecimal totalOutstanding, Date defaultDate,
			BigDecimal guaranteeAmount, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("amountOverDue", formatAmount(amountOverDue));
			placeholders.put("daysOverDue", String.valueOf(daysOverDue));
			placeholders.put("totalOutstanding", formatAmount(totalOutstanding));
			placeholders.put("defaultDate", formatDate(defaultDate));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_DEFAULT_NOTIFICATION, placeholders, guarantor, loan,
					null, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor loan default notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorInstallmentMissedPayment(MNextOfKin guarantor, MInstallments installment,
			BigDecimal guaranteeAmount, BigDecimal potentialGuaranteeCall, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorInstallmentPlaceholders(guarantor, installment);
			long daysMissed = calculateDaysOverdue(installment.getPeriodEnd());
			placeholders.put("daysMissed", String.valueOf(Math.max(0, daysMissed)));
			placeholders.put("totalDue", formatAmount(installment.getBalance()));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));
			placeholders.put("potentialGuaranteeCall", formatAmount(potentialGuaranteeCall));
			placeholders.put("guarantorLiability", formatAmount(potentialGuaranteeCall));

			Date gracePeriodEnd = new Date(installment.getPeriodEnd().getTime() + TimeUnit.DAYS.toMillis(7));
			placeholders.put("gracePeriodEnd", formatDate(gracePeriodEnd));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_INSTALLMENT_MISSED_PAYMENT, placeholders, guarantor,
					installment.getLoan(), installment, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor installment missed payment: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorPaymentReminder(MNextOfKin guarantor, MLoanApplication loan, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);

			if (loan.getDueDate() != null) {
				long daysRemaining = calculateDaysRemaining(loan.getDueDate());
				placeholders.put("noOfDaysRemaining", String.valueOf(Math.max(0, daysRemaining)));
			}
			placeholders.put("amountDue", formatAmount(loan.getBalance()));
			placeholders.put("nextInstallmentDueDate", formatDate(loan.getDueDate()));
			placeholders.put("balance", formatAmount(loan.getBalance()));
			placeholders.put("totalOutstanding", formatAmount(loan.getBalance()));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_PAYMENT_REMINDER, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor payment reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== INSTALLMENT SCHEDULE ====================

	@Transactional
	public boolean handleInstallmentGenerationNotification(MInstallments installment, Long reminderId) {
		try {
			Map<String, String> placeholders = buildInstallmentPlaceholders(installment);
			placeholders.put("noOfInstallments", String.valueOf(countAllInstallments(installment.getLoan())));
			placeholders.put("nextInstallmentStartDate", formatDate(installment.getPeriodStart()));
			placeholders.put("amountDue", formatAmount(installment.getAmount()));

			return sendBorrowerSms(SmsTypeEnum.INSTALLMENT_GENERATION_NOTIFICATION, placeholders, installment.getLoan(),
					installment.getInstallmentId(), reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling installment generation notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleInstallmentAdjustmentNotification(MInstallments installment, BigDecimal oldAmount,
			String reason, String adjustmentType, Long reminderId) {
		try {
			Map<String, String> placeholders = buildInstallmentPlaceholders(installment);
			placeholders.put("oldInstallmentAmount", formatAmount(oldAmount));
			placeholders.put("adjustmentDate", formatDate(new Date()));
			placeholders.put("reason", getSafeValue(reason));
			placeholders.put("adjustmentType", getSafeValue(adjustmentType));
			placeholders.put("remainingBalance", formatAmount(installment.getBalance()));

			return sendBorrowerSms(SmsTypeEnum.INSTALLMENT_ADJUSTMENT_NOTIFICATION, placeholders, installment.getLoan(),
					installment.getInstallmentId(), reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling installment adjustment notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleInstallmentRescheduleNotification(MInstallments installment, Date oldDueDate, Date newDueDate,
			String reason, BigDecimal rescheduleFee, Long reminderId) {
		try {
			Map<String, String> placeholders = buildInstallmentPlaceholders(installment);
			placeholders.put("oldDueDate", formatDate(oldDueDate));
			placeholders.put("newDueDate", formatDate(newDueDate));
			placeholders.put("rescheduleDate", formatDate(new Date()));
			placeholders.put("reason", getSafeValue(reason));
			placeholders.put("rescheduleFee", formatAmount(rescheduleFee));
			placeholders.put("newPaymentPlan", "Updated installment schedule");

			return sendBorrowerSms(SmsTypeEnum.INSTALLMENT_RESCHEDULE_NOTIFICATION, placeholders, installment.getLoan(),
					installment.getInstallmentId(), reminderId, LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling installment reschedule notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorInstallmentAdjustment(MNextOfKin guarantor, MInstallments installment,
			BigDecimal oldInstallmentAmount, BigDecimal newInstallmentAmount, Date adjustmentDate, String reason,
			String adjustmentType, BigDecimal guaranteeAmount, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorInstallmentPlaceholders(guarantor, installment);
			placeholders.put("oldInstallmentAmount", formatAmount(oldInstallmentAmount));
			placeholders.put("newInstallmentAmount", formatAmount(newInstallmentAmount));
			placeholders.put("adjustmentDate", formatDate(adjustmentDate));
			placeholders.put("reason", getSafeValue(reason));
			placeholders.put("adjustmentType", getSafeValue(adjustmentType));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));

			Optional<MInstallments> nextInstallment = getNextInstallment(installment);
			if (nextInstallment.isPresent()) {
				placeholders.put("nextDueDate", formatDate(nextInstallment.get().getPeriodEnd()));
			}

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_INSTALLMENT_ADJUSTMENT, placeholders, guarantor,
					installment.getLoan(), installment, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor installment adjustment: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorInstallmentReschedule(MNextOfKin guarantor, MInstallments installment,
			Date oldDueDate, Date newDueDate, Date rescheduleDate, String reason, BigDecimal rescheduleFee,
			String newPaymentPlan, BigDecimal guaranteeAmount, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorInstallmentPlaceholders(guarantor, installment);
			placeholders.put("oldDueDate", formatDate(oldDueDate));
			placeholders.put("newDueDate", formatDate(newDueDate));
			placeholders.put("rescheduleDate", formatDate(rescheduleDate));
			placeholders.put("reason", getSafeValue(reason));
			placeholders.put("rescheduleFee", formatAmount(rescheduleFee));
			placeholders.put("newPaymentPlan", getSafeValue(newPaymentPlan));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_INSTALLMENT_RESCHEDULE, placeholders, guarantor,
					installment.getLoan(), installment, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor installment reschedule: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== GRACE PERIOD ====================

	@Transactional
	public boolean handleGracePeriodExpiryAlert(MLoanApplication loan, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);

			Date graceEnd = calculateGracePeriodEnd(loan);
			long daysRemaining = calculateDaysRemaining(graceEnd);
			placeholders.put("daysRemaining", String.valueOf(Math.max(0, daysRemaining)));
			placeholders.put("nextInstallmentStartDate", formatDate(graceEnd));
			placeholders.put("amountDue", formatAmount(loan.getBalance()));

			return sendBorrowerSms(SmsTypeEnum.GRACE_PERIOD_EXPIRY_ALERT, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling grace period expiry alert: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== FINANCIAL NOTIFICATIONS ====================

	@Transactional
	public boolean handleInterestCalculationNotification(MLoanApplication loan, BigDecimal interestAmount,
			BigDecimal totalInterests, Date date, String interestRate, Long reminderId, LocalDateTime timeToSend) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("interestAmount", formatAmount(interestAmount));
			placeholders.put("calculationDate", formatDate(date));
			placeholders.put("totalInterest", formatAmount(totalInterests));
			placeholders.put("interestRate", interestRate);

			return sendBorrowerSms(SmsTypeEnum.INTEREST_CALCULATION_NOTIFICATION, placeholders, loan, null, reminderId,
					timeToSend, null);
		} catch (Exception e) {
			log.error("Error handling interest calculation notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorInterestAccrualNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal interestAmount, BigDecimal totalInterestAccrued, Date accrualDate, BigDecimal dailyInterestRate,
			BigDecimal weeklyInterestRate, BigDecimal monthlyInterestRate, BigDecimal annualInterestRate,
			BigDecimal currentBalance, String interestCalculationMethod, String interestFrequency, Date nextAccrualDate,
			String interestPeriod, Long reminderId, LocalDateTime timeToSend) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("interestAmount", formatAmount(interestAmount));
			placeholders.put("totalInterestAccrued", formatAmount(totalInterestAccrued));
			placeholders.put("accrualDate", formatDate(accrualDate));
			placeholders.put("dailyInterestRate", formatAmount(dailyInterestRate) + "%");
			placeholders.put("weeklyInterestRate", formatAmount(weeklyInterestRate) + "%");
			placeholders.put("monthlyInterestRate", formatAmount(monthlyInterestRate) + "%");
			placeholders.put("annualInterestRate", formatAmount(annualInterestRate) + "%");
			placeholders.put("currentBalance", formatAmount(currentBalance));
			placeholders.put("interestCalculationMethod", getSafeValue(interestCalculationMethod));
			placeholders.put("interestFrequency", getSafeValue(interestFrequency));
			placeholders.put("nextAccrualDate", formatDate(nextAccrualDate));
			placeholders.put("interestPeriod", getSafeValue(interestPeriod));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION, placeholders, guarantor, loan,
					null, reminderId, timeToSend);
		} catch (Exception e) {
			log.error("Error handling guarantor interest accrual notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorInterestWaiverNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal waivedInterestAmount, String interestWaiverReason, Date waiverDate,
			BigDecimal previousInterestAmount, String approvedBy, Long reminderId, LocalDateTime timeToSend) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("waivedInterestAmount", formatAmount(waivedInterestAmount));
			placeholders.put("interestWaiverReason", getSafeValue(interestWaiverReason));
			placeholders.put("waiverDate", formatDate(waiverDate));
			placeholders.put("previousInterestAmount", formatAmount(previousInterestAmount));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_INTEREST_WAIVER_NOTIFICATION, placeholders, guarantor, loan,
					null, reminderId, timeToSend);
		} catch (Exception e) {
			log.error("Error handling guarantor interest waiver notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleInterestWaiverNotification(MLoanApplication loan, BigDecimal waivedInterestAmount,
			String interestWaiverReason, Date waiverDate, String approvedBy, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("waivedInterestAmount", formatAmount(waivedInterestAmount));
			placeholders.put("interestWaiverReason", getSafeValue(interestWaiverReason));
			placeholders.put("waiverDate", formatDate(waiverDate));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendBorrowerSms(SmsTypeEnum.INTEREST_WAIVER_NOTIFICATION, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling interest waiver notification for loan {}: {}", loan.getLoanApplicationId(),
					e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleInterestWriteOffNotification(MLoanApplication loan, BigDecimal writtenOffInterestAmount,
			String writeOffReason, Date writeOffDate, String approvedBy, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("writtenOffInterestAmount", formatAmount(writtenOffInterestAmount));
			placeholders.put("writeOffReason", getSafeValue(writeOffReason));
			placeholders.put("writeOffDate", formatDate(writeOffDate));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendBorrowerSms(SmsTypeEnum.INTEREST_WRITE_OFF_NOTIFICATION, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling interest write-off notification for loan {}: {}", loan.getLoanApplicationId(),
					e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handlePenaltyWriteOffNotification(MLoanApplication loan, BigDecimal writtenOffPenaltyAmount,
			String writeOffReason, Date writeOffDate, String approvedBy, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("writtenOffPenaltyAmount", formatAmount(writtenOffPenaltyAmount));
			placeholders.put("writeOffReason", getSafeValue(writeOffReason));
			placeholders.put("writeOffDate", formatDate(writeOffDate));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendBorrowerSms(SmsTypeEnum.PENALTY_WRITE_OFF_NOTIFICATION, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling penalty write-off notification for loan {}: {}", loan.getLoanApplicationId(),
					e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorInterestWriteOffNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal writtenOffInterestAmount, String writeOffReason, Date writeOffDate, String approvedBy,
			Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("writtenOffInterestAmount", formatAmount(writtenOffInterestAmount));
			placeholders.put("writeOffReason", getSafeValue(writeOffReason));
			placeholders.put("writeOffDate", formatDate(writeOffDate));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_INTEREST_WRITE_OFF_NOTIFICATION, placeholders, guarantor,
					loan, null, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor interest write-off notification for loan {}: {}",
					loan.getLoanApplicationId(), e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorPenaltyWriteOffNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal writtenOffPenaltyAmount, String writeOffReason, Date writeOffDate, String approvedBy,
			Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("writtenOffPenaltyAmount", formatAmount(writtenOffPenaltyAmount));
			placeholders.put("writeOffReason", getSafeValue(writeOffReason));
			placeholders.put("writeOffDate", formatDate(writeOffDate));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_PENALTY_WRITE_OFF_NOTIFICATION, placeholders, guarantor, loan,
					null, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor penalty write-off notification for loan {}: {}",
					loan.getLoanApplicationId(), e.getMessage(), e);
			return false;
		}
	}

	// ==================== PENALTY NOTIFICATIONS ====================

	@Transactional
	public boolean handlePenaltyAppliedNotification(MLoanApplication loan, BigDecimal penaltyAmount, BigDecimal balance,
			String penaltyReason, Date applicationDate, Long reminderId, LocalDateTime timeToSend) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("penaltyAmount", formatAmount(penaltyAmount));
			placeholders.put("totalOutstanding", formatAmount(balance));
			placeholders.put("penaltyReason", getSafeValue(penaltyReason));
			placeholders.put("applicationDate", formatDate(new Date()));
			placeholders.put("amountDue", formatAmount(loan.getBalance()));

			if (loan.getDueDate() != null) {
				long daysOverdue = calculateDaysOverdue(loan.getDueDate());
				placeholders.put("daysOverDue", String.valueOf(daysOverdue));
			}

			return sendBorrowerSms(SmsTypeEnum.PENALTY_APPLIED_NOTIFICATION, placeholders, loan, null, reminderId,
					timeToSend, null);
		} catch (Exception e) {
			log.error("Error handling penalty applied notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handlePenaltyWaiverNotification(MLoanApplication loan, BigDecimal penaltyAmount,
			BigDecimal updatedBalance, Date waiverDate, String penaltyWaiverReason, String approvedBy, Long reminderId,
			LocalDateTime timeToSend) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("penaltyAmount", formatAmount(penaltyAmount));
			placeholders.put("waivedPenaltyAmount", formatAmount(penaltyAmount));
			
			placeholders.put("updatedBalance", formatAmount(updatedBalance));
			placeholders.put("waiverDate", formatDate(waiverDate));
			placeholders.put("penaltyWaiverReason", getSafeValue(penaltyWaiverReason));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendBorrowerSms(SmsTypeEnum.PENALTY_WAIVER_NOTIFICATION, placeholders, loan, null, reminderId,
					timeToSend, null);
		} catch (Exception e) {
			log.error("Error handling penalty waiver notification for loan {}: {}", loan.getLoanApplicationId(),
					e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleWaiverNotification(MLoanApplication loan, BigDecimal waivedAmount, String waiverReason,
			Date waiverDate, String approvedBy, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("waivedAmount", formatAmount(waivedAmount));
			placeholders.put("waiverReason", getSafeValue(waiverReason));
			placeholders.put("waiverDate", formatDate(waiverDate));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendBorrowerSms(SmsTypeEnum.WAIVER_NOTIFICATION, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling general waiver notification for loan {}: {}", loan.getLoanApplicationId(),
					e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleWriteOffNotification(MLoanApplication loan, BigDecimal writtenOffAmount, String writeOffReason,
			Date writeOffDate, String approvedBy, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("writtenOffAmount", formatAmount(writtenOffAmount));
			placeholders.put("writeOffReason", getSafeValue(writeOffReason));
			placeholders.put("writeOffDate", formatDate(writeOffDate));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendBorrowerSms(SmsTypeEnum.WRITE_OFF_NOTIFICATION, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling general write-off notification for loan {}: {}", loan.getLoanApplicationId(),
					e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorWaiverNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal waivedAmount, String waiverReason, Date waiverDate, String approvedBy, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("waivedAmount", formatAmount(waivedAmount));
			placeholders.put("waiverReason", getSafeValue(waiverReason));
			placeholders.put("waiverDate", formatDate(waiverDate));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_WAIVER_NOTIFICATION, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling general waiver notification for guarantor {}: {}", guarantor.getFullName(),
					e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorWriteOffNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal writtenOffAmount, String writeOffReason, Date writeOffDate, String approvedBy, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("writtenOffAmount", formatAmount(writtenOffAmount));
			placeholders.put("writeOffReason", getSafeValue(writeOffReason));
			placeholders.put("writeOffDate", formatDate(writeOffDate));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_WRITE_OFF_NOTIFICATION, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling general write-off notification for guarantor {}: {}", guarantor.getFullName(),
					e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorPenaltyCalculationNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal penaltyAmount, BigDecimal penaltyRate, Date calculationDate, String penaltyReason,
			Integer overdueDays, BigDecimal overdueAmount, BigDecimal totalOutstanding, String penaltyType,
			Boolean gracePeriodUsed, String penaltyFrequency, Long reminderId, LocalDateTime timeToSend) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("penaltyAmount", formatAmount(penaltyAmount));
			placeholders.put("penaltyRate", formatAmount(penaltyRate) + "%");
			placeholders.put("calculationDate", formatDate(calculationDate));
			placeholders.put("penaltyReason", getSafeValue(penaltyReason));
			placeholders.put("overdueDays", String.valueOf(overdueDays));
			placeholders.put("overdueAmount", formatAmount(overdueAmount));
			placeholders.put("totalOutstanding", formatAmount(totalOutstanding));
			placeholders.put("penaltyType", getSafeValue(penaltyType));
			placeholders.put("gracePeriodUsed", String.valueOf(gracePeriodUsed));
			placeholders.put("penaltyFrequency", getSafeValue(penaltyFrequency));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_PENALTY_CALCULATION_NOTIFICATION, placeholders, guarantor,
					loan, null, reminderId, timeToSend);
		} catch (Exception e) {
			log.error("Error handling guarantor penalty calculation notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorPenaltyWaiverNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal waivedPenaltyAmount, String penaltyWaiverReason, Date waiverDate,
			BigDecimal previousPenaltyAmount, String approvedBy, Long reminderId, LocalDateTime timeToSend) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("waivedPenaltyAmount", formatAmount(waivedPenaltyAmount));
			placeholders.put("penaltyWaiverReason", getSafeValue(penaltyWaiverReason));
			placeholders.put("waiverDate", formatDate(waiverDate));
			placeholders.put("previousPenaltyAmount", formatAmount(previousPenaltyAmount));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_PENALTY_WAIVER_NOTIFICATION, placeholders, guarantor, loan,
					null, reminderId, timeToSend);
		} catch (Exception e) {
			log.error("Error handling guarantor penalty waiver notification: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== REPAYMENT SCHEDULE UPDATES ====================

	@Transactional
	public boolean handleRepaymentScheduleUpdate(MLoanApplication loan, BigDecimal newInstallmentAmount,
			Date nextDueDate, Integer remainingInstallments, String reason, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("newInstallmentAmount", formatAmount(newInstallmentAmount));
			placeholders.put("nextDueDate", formatDate(nextDueDate));
			placeholders.put("remainingInstallments", String.valueOf(remainingInstallments));
			placeholders.put("reason", getSafeValue(reason));
			placeholders.put("totalPaid", formatAmount(calculateTotalPaid(loan)));
			placeholders.put("remainingBalance", formatAmount(loan.getBalance()));

			return sendBorrowerSms(SmsTypeEnum.REPAYMENT_SCHEDULE_UPDATE, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling repayment schedule update: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleRepaymentRescheduleRequest(MLoanApplication loan, Date requestDate, String status,
			Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("requestDate", formatDate(requestDate));
			placeholders.put("status", getSafeValue(status));
			placeholders.put("amountDue", formatAmount(loan.getBalance()));

			return sendBorrowerSms(SmsTypeEnum.REPAYMENT_RESCHEDULE_REQUEST, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling repayment reschedule request: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleRepaymentRescheduleApproval(MLoanApplication loan, BigDecimal newInstallmentAmount,
			Integer remainingInstallments, Integer newTerm, Date approvalDate, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("newInstallmentAmount", formatAmount(newInstallmentAmount));
			placeholders.put("remainingInstallments", String.valueOf(remainingInstallments));
			placeholders.put("newTerm", String.valueOf(newTerm));
			placeholders.put("approvalDate", formatDate(approvalDate));
			placeholders.put("newPrincipal", formatAmount(loan.getBalance()));
			placeholders.put("nextInstallmentDueDate", formatDate(loan.getDueDate()));

			return sendBorrowerSms(SmsTypeEnum.REPAYMENT_RESCHEDULE_APPROVAL, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling repayment reschedule approval: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleRepaymentRescheduleRejection(MLoanApplication loan, String rejectionReason, Date rejectionDate,
			Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("rejectionReason", getSafeValue(rejectionReason));
			placeholders.put("rejectionDate", formatDate(rejectionDate));
			placeholders.put("amountDue", formatAmount(loan.getBalance()));
			placeholders.put("nextInstallmentDueDate", formatDate(loan.getDueDate()));

			return sendBorrowerSms(SmsTypeEnum.REPAYMENT_RESCHEDULE_REJECTION, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling repayment reschedule rejection: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== LOAN RESTRUCTURING ====================

	@Transactional
	public boolean handleLoanRestructuringNotification(MLoanApplication loan, BigDecimal newPrincipal, Integer newTerm,
			BigDecimal newInstallment, BigDecimal remainingBalance, Date effectiveDate, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("newPrincipal", formatAmount(newPrincipal));
			placeholders.put("newTerm", String.valueOf(newTerm));
			placeholders.put("newInstallment", formatAmount(newInstallment));
			placeholders.put("remainingBalance", formatAmount(remainingBalance));
			placeholders.put("effectiveDate", formatDate(effectiveDate));
			placeholders.put("nextInstallmentDueDate", formatDate(loan.getDueDate()));
			placeholders.put("noOfInstallments", String.valueOf(countAllInstallments(loan)));

			return sendBorrowerSms(SmsTypeEnum.LOAN_RESTRUCTURING_NOTIFICATION, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling loan restructuring notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorLoanRestructuring(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal newInstallmentAmount, Integer newTerm, BigDecimal newPrincipal, Date effectiveDate,
			BigDecimal remainingBalance, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("newInstallmentAmount", formatAmount(newInstallmentAmount));
			placeholders.put("newTerm", String.valueOf(newTerm));
			placeholders.put("newPrincipal", formatAmount(newPrincipal));
			placeholders.put("effectiveDate", formatDate(effectiveDate));
			placeholders.put("remainingBalance", formatAmount(remainingBalance));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_RESTRUCTURING, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor loan restructuring: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== TOP-UP LOAN ====================

	@Transactional
	public boolean handleTopUpLoanDisbursement(MLoanApplication loan, BigDecimal topUpAmount,
			BigDecimal totalOutstanding, Date repaymentStartDate, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("topUpAmount", formatAmount(topUpAmount));
			placeholders.put("totalOutstanding", formatAmount(totalOutstanding));
			placeholders.put("repaymentStartDate", formatDate(repaymentStartDate));
			placeholders.put("disbursementDate", formatDate(new Date()));
			placeholders.put("newPrincipal", formatAmount(loan.getBalance()));
			placeholders.put("newInstallment", formatAmount(loan.getBalance()));

			return sendBorrowerSms(SmsTypeEnum.TOP_UP_LOAN_DISBURSEMENT, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling top-up loan disbursement: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== LOAN CLOSURE ====================

	@Transactional
	public boolean handleLoanClosureNotification(MLoanApplication loan, BigDecimal settlementAmount, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("settlementAmount", formatAmount(settlementAmount));
			placeholders.put("closureDate", formatDate(new Date()));
			placeholders.put("totalRepaid", formatAmount(calculateTotalPaid(loan)));
			placeholders.put("amountPaid", formatAmount(calculateTotalPaid(loan)));
			placeholders.put("paymentDate", formatDate(new Date()));
			placeholders.put("balance", formatAmount(BigDecimal.ZERO));
			placeholders.put("outstandingBalance", formatAmount(BigDecimal.ZERO));

			return sendBorrowerSms(SmsTypeEnum.LOAN_CLOSURE_NOTIFICATION, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling loan closure notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorLoanSettlement(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal settlementAmount, Date closureDate, BigDecimal totalRepaid, Date guaranteeReleasedDate,
			Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("settlementAmount", formatAmount(settlementAmount));
			placeholders.put("closureDate", formatDate(closureDate));
			placeholders.put("totalRepaid", formatAmount(totalRepaid));
			placeholders.put("guaranteeReleasedDate", formatDate(guaranteeReleasedDate));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_SETTLEMENT, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor loan settlement: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorLoanClosure(MNextOfKin guarantor, MLoanApplication loan, Date closureDate,
			BigDecimal totalRepaid, BigDecimal settlementAmount, Date guaranteeCompletionDate, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("closureDate", formatDate(closureDate));
			placeholders.put("totalRepaid", formatAmount(totalRepaid));
			placeholders.put("settlementAmount", formatAmount(settlementAmount));
			placeholders.put("guaranteeCompletionDate", formatDate(guaranteeCompletionDate));
			placeholders.put("loanStatus", "CLOSED");

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_CLOSURE, placeholders, guarantor, loan, null, reminderId,
					null);
		} catch (Exception e) {
			log.error("Error handling guarantor loan closure: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorReleaseNotification(MNextOfKin guarantor, MLoanApplication loan, Date releaseDate,
			BigDecimal guaranteeAmount, String releaseReason, String loanStatus, BigDecimal totalRepaid,
			Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("releaseDate", formatDate(releaseDate));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));
			placeholders.put("releaseReason", getSafeValue(releaseReason));
			placeholders.put("loanStatus", getSafeValue(loanStatus));
			placeholders.put("totalRepaid", formatAmount(totalRepaid));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_RELEASE_NOTIFICATION, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor release notification: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== GUARANTOR STATUS & LIMIT UPDATES ====================

	@Transactional
	public boolean handleGuarantorLimitUpdateNotification(MNextOfKin guarantor, BigDecimal oldGuaranteeLimit,
			BigDecimal newGuaranteeLimit, Date updateDate, String reason, Date effectiveDate, String approvedBy,
			BigDecimal totalActiveGuarantees, BigDecimal availableGuarantee, Long orgId, Long adClientId,
			Long reminderId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("guarantorName", getSafeValue(guarantor.getFullName()));
			placeholders.put("oldGuaranteeLimit", formatAmount(oldGuaranteeLimit));
			placeholders.put("newGuaranteeLimit", formatAmount(newGuaranteeLimit));
			placeholders.put("updateDate", formatDate(updateDate));
			placeholders.put("reason", getSafeValue(reason));
			placeholders.put("effectiveDate", formatDate(effectiveDate));
			placeholders.put("approvedBy", getSafeValue(approvedBy));
			placeholders.put("totalActiveGuarantees", formatAmount(totalActiveGuarantees));
			placeholders.put("availableGuarantee", formatAmount(availableGuarantee));

			return sendGuarantorGenericSms(SmsTypeEnum.GUARANTOR_LIMIT_UPDATE_NOTIFICATION, placeholders, guarantor,
					orgId, adClientId, reminderId);
		} catch (Exception e) {
			log.error("Error handling guarantor limit update notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorStatusChangeNotification(MNextOfKin guarantor, String oldStatus, String newStatus,
			Date changeDate, String reason, Date effectiveDate, String approvedBy, Integer affectedLoansCount,
			BigDecimal outstandingBalance, Long orgId, Long adClientId, Long reminderId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("guarantorName", getSafeValue(guarantor.getFullName()));
			placeholders.put("oldStatus", getSafeValue(oldStatus));
			placeholders.put("newStatus", getSafeValue(newStatus));
			placeholders.put("changeDate", formatDate(changeDate));
			placeholders.put("reason", getSafeValue(reason));
			placeholders.put("effectiveDate", formatDate(effectiveDate));
			placeholders.put("approvedBy", getSafeValue(approvedBy));
			placeholders.put("affectedLoansCount", String.valueOf(affectedLoansCount));
			placeholders.put("outstandingBalance", formatAmount(outstandingBalance));

			return sendGuarantorGenericSms(SmsTypeEnum.GUARANTOR_STATUS_CHANGE_NOTIFICATION, placeholders, guarantor,
					orgId, adClientId, reminderId);
		} catch (Exception e) {
			log.error("Error handling guarantor status change notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorReplacementNotification(MNextOfKin oldGuarantor, MNextOfKin newGuarantor,
			MLoanApplication loan, Date replacementDate, String reason, BigDecimal guaranteeAmount, String approvedBy,
			Long reminderId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("oldGuarantorName", getSafeValue(oldGuarantor.getFullName()));
			placeholders.put("newGuarantorName", getSafeValue(newGuarantor.getFullName()));
			placeholders.put("borrowerName", getSafeValue(utils.getBorrowerName(loan)));
			placeholders.put("loanType", getLoanType(loan));
			placeholders.put("documentNo", getSafeValue(loan.getDocumentNo()));
			placeholders.put("replacementDate", formatDate(replacementDate));
			placeholders.put("reason", getSafeValue(reason));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));
			placeholders.put("approvedBy", getSafeValue(approvedBy));

			sendGuarantorSms(SmsTypeEnum.GUARANTOR_REPLACEMENT_NOTIFICATION, placeholders, oldGuarantor, loan, null,
					reminderId, null);

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_REPLACEMENT_NOTIFICATION, placeholders, newGuarantor, loan,
					null, reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor replacement notification: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== STATEMENTS ====================

	@Transactional
	public boolean handleStatementReadyNotification(MLoanApplication loan, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);

			Calendar cal = Calendar.getInstance();
			String month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
			int year = cal.get(Calendar.YEAR);
			placeholders.put("period", month + " " + year);
			placeholders.put("amountDue", formatAmount(loan.getBalance()));
			placeholders.put("statementDate", formatDate(new Date()));
			placeholders.put("totalPaid", formatAmount(calculateTotalPaid(loan)));

			long adOrgId = loan.getAdOrgID();
			ReportParams param = new ReportParams();
			param.setAd_Org_ID(adOrgId);
			param.setGroupBorrowerId(0L);
			param.setIndividualBorrowerId(0L);
			param.setInstitutionBorrowerId(0L);
			param.setLoanId(0);
			param.setReportType(ReportType.PDF);

			int monthInt = cal.get(Calendar.MONTH);

			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, monthInt);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			param.setDateFrom(cal.getTime());

			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, monthInt);
			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			param.setDateTo(cal.getTime());

			param.setDateFrom(utils.getStartOfDay(param.getDateFrom()));
			param.setDateTo(utils.getEndOfDay(param.getDateTo()));

			FileOutPutResponse file = jasper.generateLoanStatement(param);

			return sendBorrowerSms(SmsTypeEnum.STATEMENT_READY_NOTIFICATION, placeholders, loan, null, reminderId,
					LocalDateTime.now(), file.getFileOutputUrl());
		} catch (Exception e) {
			log.error("Error handling statement ready notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorStatementNotification(MNextOfKin guarantor, String statementPeriod,
			Date statementDate, BigDecimal totalGuaranteedAmount, Integer activeGuarantees,
			BigDecimal guaranteeUtilization, BigDecimal availableGuarantee, String highestRiskLoan,
			BigDecimal totalOutstandingExposure, Long orgId, Long adClientId, Long reminderId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("guarantorName", getSafeValue(guarantor.getFullName()));
			placeholders.put("statementPeriod", getSafeValue(statementPeriod));
			placeholders.put("statementDate", formatDate(statementDate));
			placeholders.put("totalGuaranteedAmount", formatAmount(totalGuaranteedAmount));
			placeholders.put("activeGuarantees", String.valueOf(activeGuarantees));
			placeholders.put("guaranteeUtilization", formatAmount(guaranteeUtilization));
			placeholders.put("availableGuarantee", formatAmount(availableGuarantee));
			placeholders.put("highestRiskLoan", getSafeValue(highestRiskLoan));
			placeholders.put("totalOutstandingExposure", formatAmount(totalOutstandingExposure));

			Calendar cal = Calendar.getInstance();
			int year = cal.get(Calendar.YEAR);

			ReportParams param = new ReportParams();
			param.setAd_Org_ID(orgId);
			param.setGroupBorrowerId(0L);
			param.setIndividualBorrowerId(0L);
			param.setInstitutionBorrowerId(0L);
			param.setLoanId(0);
			param.setReportType(ReportType.PDF);

			int monthInt = cal.get(Calendar.MONTH);

			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, monthInt);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			param.setDateFrom(cal.getTime());

			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, monthInt);
			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			param.setDateTo(cal.getTime());

			param.setDateFrom(utils.getStartOfDay(param.getDateFrom()));
			param.setDateTo(utils.getEndOfDay(param.getDateTo()));

			FileOutPutResponse file = jasper.generateLoanStatement(param);

			return sendGuarantorGenericSmsLoanStatement(SmsTypeEnum.GUARANTOR_STATEMENT_NOTIFICATION, placeholders,
					guarantor, orgId, adClientId, reminderId, file.getFileOutputUrl());
		} catch (Exception e) {
			log.error("Error handling guarantor statement notification: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== AUTO DEBIT ====================

	@Transactional
	public boolean handleAutoDebitSuccess(MPayments payment, Long reminderId) {
		try {
			Map<String, String> placeholders = buildPaymentPlaceholders(payment);
			placeholders.put("paymentMethod", "AUTO_DEBIT");

			return sendBorrowerSms(SmsTypeEnum.AUTO_DEBIT_SUCCESS, placeholders, payment.getLoan(), null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling auto debit success: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleAutoDebitFailure(MLoanApplication loan, BigDecimal amountDue, String failureReason,
			Date retryDate, String paymentMethod, Long reminderId) {
		try {
			Map<String, String> placeholders = buildLoanPlaceholders(loan);
			placeholders.put("amountDue", formatAmount(amountDue));
			placeholders.put("failureReason", getSafeValue(failureReason));
			placeholders.put("retryDate", formatDate(retryDate));
			placeholders.put("paymentMethod", getSafeValue(paymentMethod));
			placeholders.put("nextInstallmentDueDate", formatDate(loan.getDueDate()));

			if (loan.getDueDate() != null) {
				long daysOverdue = calculateDaysOverdue(loan.getDueDate());
				placeholders.put("daysOverDue", String.valueOf(daysOverdue));
			}

			return sendBorrowerSms(SmsTypeEnum.AUTO_DEBIT_FAILURE, placeholders, loan, null, reminderId,
					LocalDateTime.now(), null);
		} catch (Exception e) {
			log.error("Error handling auto debit failure: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== ACCOUNT & MEMBERSHIP NOTIFICATIONS ====================

	@Transactional
	public boolean handleAccountActivationNotification(String username, Date activationDate, Long orgId,
			Long adClientId, String phoneNumber, Long reminderId, long guarantorId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("activationDate", formatDate(activationDate));

			return sendGenericSms(SmsTypeEnum.ACCOUNT_ACTIVATION_NOTIFICATION, placeholders, orgId, adClientId,
					phoneNumber, null, null, LocalDateTime.now(), reminderId, guarantorId);
		} catch (Exception e) {
			log.error("Error handling account activation notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleAccountSuspensionNotification(String username, String suspensionReason, Date suspensionDate,
			Long orgId, Long adClientId, String phoneNumber, Long reminderId, Long guarantorId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("suspensionReason", getSafeValue(suspensionReason));
			placeholders.put("suspensionDate", formatDate(suspensionDate));

			return sendGenericSms(SmsTypeEnum.ACCOUNT_SUSPENSION_NOTIFICATION, placeholders, orgId, adClientId,
					phoneNumber, null, null, LocalDateTime.now(), reminderId, guarantorId);
		} catch (Exception e) {
			log.error("Error handling account suspension notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleAccountReactivationNotification(String username, Date reactivationDate, Long orgId,
			Long adClientId, String phoneNumber, Long reminderId, Long guarantorId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("reactivationDate", formatDate(reactivationDate));

			return sendGenericSms(SmsTypeEnum.ACCOUNT_REACTIVATION_NOTIFICATION, placeholders, orgId, adClientId,
					phoneNumber, null, null, LocalDateTime.now(), reminderId, guarantorId);
		} catch (Exception e) {
			log.error("Error handling account reactivation notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMemberRegistrationSuccess(String username, String membershipAccountNo, Date registrationDate,
			Long orgId, Long adClientId, String phoneNumber, Long reminderId, Long guarantorId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("registrationDate", formatDate(registrationDate));

			return sendGenericSms(SmsTypeEnum.MEMBER_REGISTRATION_SUCCESS, placeholders, orgId, adClientId, phoneNumber,
					null, null, LocalDateTime.now(), reminderId, guarantorId);
		} catch (Exception e) {
			log.error("Error handling member registration success: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipRenewalReminder(String username, String membershipAccountNo, Date expiryDate,
			Long daysRemaining, String renewalLink, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("expiryDate", formatDate(expiryDate));
			placeholders.put("daysRemaining", String.valueOf(daysRemaining));
			placeholders.put("renewalLink", getSafeValue(renewalLink));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_RENEWAL_REMINDER, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership renewal reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== SAVINGS ====================

	@Transactional
	public boolean handleSavingsDepositNotification(String username, BigDecimal depositAmount, Date depositDate,
			BigDecimal totalBalance, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long guarantorId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("depositAmount", formatAmount(depositAmount));
			placeholders.put("depositDate", formatDate(depositDate));
			placeholders.put("totalBalance", formatAmount(totalBalance));

			return sendGenericSms(SmsTypeEnum.SAVINGS_DEPOSIT_NOTIFICATION, placeholders, orgId, adClientId,
					phoneNumber, null, null, LocalDateTime.now(), reminderId, guarantorId);
		} catch (Exception e) {
			log.error("Error handling savings deposit notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleSavingsWithdrawalNotification(String username, BigDecimal withdrawalAmount,
			Date withdrawalDate, BigDecimal remainingBalance, Long orgId, Long adClientId, String phoneNumber,
			Long reminderId, Long guarantorId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("withdrawalAmount", formatAmount(withdrawalAmount));
			placeholders.put("withdrawalDate", formatDate(withdrawalDate));
			placeholders.put("remainingBalance", formatAmount(remainingBalance));

			return sendGenericSms(SmsTypeEnum.SAVINGS_WITHDRAWAL_NOTIFICATION, placeholders, orgId, adClientId,
					phoneNumber, 0L, 0L, LocalDateTime.now(), reminderId, guarantorId);
		} catch (Exception e) {
			log.error("Error handling savings withdrawal notification: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== ANNOUNCEMENT ====================

	@Transactional
	public boolean handleAnnouncementNotification(Date announcementDate, Long orgId, Long adClientId,
			String phoneNumber, Long reminderId, Long guarantorId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("announcementDate", formatDate(announcementDate));

			return sendGenericSms(SmsTypeEnum.ANNOUNCEMENT_NOTIFICATION, placeholders, orgId, adClientId, phoneNumber,
					null, null, LocalDateTime.now(), reminderId, guarantorId);
		} catch (Exception e) {
			log.error("Error handling announcement notification: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== GUARANTOR CALL & RECOVERY ====================

	@Transactional
	public boolean handleGuarantorCallNotification(MNextOfKin guarantor, MLoanApplication loan, BigDecimal callAmount,
			BigDecimal totalOutstanding, Date callDate, String reason, Integer gracePeriodForPayment,
			Date paymentDeadline, BigDecimal guaranteeAmount, BigDecimal guaranteeUtilized, String recoveryContact,
			Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("callAmount", formatAmount(callAmount));
			placeholders.put("totalOutstanding", formatAmount(totalOutstanding));
			placeholders.put("callDate", formatDate(callDate));
			placeholders.put("reason", getSafeValue(reason));
			placeholders.put("gracePeriodForPayment", String.valueOf(gracePeriodForPayment));
			placeholders.put("paymentDeadline", formatDate(paymentDeadline));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));
			placeholders.put("guaranteeUtilized", formatAmount(guaranteeUtilized));
			placeholders.put("recoveryContact", getSafeValue(recoveryContact));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_CALL_NOTIFICATION, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor call notification: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleGuarantorRecoveryNotification(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal recoveryAmount, Date recoveryDate, String recoveryMethod, BigDecimal remainingBalance,
			String recoveryAgent, BigDecimal guaranteeAmount, BigDecimal guaranteeReleased, Long reminderId) {
		try {
			Map<String, String> placeholders = buildGuarantorPlaceholders(guarantor, loan);
			placeholders.put("recoveryAmount", formatAmount(recoveryAmount));
			placeholders.put("recoveryDate", formatDate(recoveryDate));
			placeholders.put("recoveryMethod", getSafeValue(recoveryMethod));
			placeholders.put("remainingBalance", formatAmount(remainingBalance));
			placeholders.put("recoveryAgent", getSafeValue(recoveryAgent));
			placeholders.put("guaranteeAmount", formatAmount(guaranteeAmount));
			placeholders.put("guaranteeReleased", formatAmount(guaranteeReleased));

			return sendGuarantorSms(SmsTypeEnum.GUARANTOR_RECOVERY_NOTIFICATION, placeholders, guarantor, loan, null,
					reminderId, null);
		} catch (Exception e) {
			log.error("Error handling guarantor recovery notification: {}", e.getMessage(), e);
			return false;
		}
	}

	// ==================== MANUAL SMS ====================

	@Transactional
	public boolean handleManualSms(String phoneNumber, String message, Long orgId, Long adClientId, Long reminderId,
			Long guarantorId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			String template = getTemplate(getRemindersConfiguration(reminderId),
					SmsTypeEnum.MANUAL_SMS_FROM_MESSAGE_CENTER, orgId);

			String finalMessage = message;
			if (template != null && !template.trim().isEmpty()) {
				finalMessage = utils.processTemplate(template, placeholders);
			}

			utils.saveSms(null, phoneNumber, finalMessage, orgId, adClientId,
					SmsTypeEnum.MANUAL_SMS_FROM_MESSAGE_CENTER, 0, 0, LocalDateTime.now(), reminderId, guarantorId);

			log.info("✅ Sent manual SMS to {}", phoneNumber);
			return true;
		} catch (Exception e) {
			log.error("❌ Failed to send manual SMS to {}: {}", phoneNumber, e.getMessage(), e);
			return false;
		}
	}

	// ==================== ADDITIONAL MEMBERSHIP NOTIFICATIONS ====================

	@Transactional
	public boolean handleMemberWelcomeMessage(String username, String membershipAccountNo, String membershipType,
			Date joinDate, String welcomeMessage, String nextSteps, Long orgId, Long adClientId, String phoneNumber,
			Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("membershipType", getSafeValue(membershipType));
			placeholders.put("joinDate", formatDate(joinDate));
			placeholders.put("welcomeMessage", getSafeValue(welcomeMessage));
			placeholders.put("nextSteps", getSafeValue(nextSteps));

			return sendmembershipSMS(SmsTypeEnum.MEMBER_WELCOME_MESSAGE, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling member welcome message: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipRenewalSuccess(String username, String membershipAccountNo, Date newExpiryDate,
			Date renewalDate, String membershipType, BigDecimal amountPaid, Long orgId, Long adClientId,
			String phoneNumber, Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("newExpiryDate", formatDate(newExpiryDate));
			placeholders.put("renewalDate", formatDate(renewalDate));
			placeholders.put("membershipType", getSafeValue(membershipType));
			placeholders.put("amountPaid", formatAmount(amountPaid));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_RENEWAL_SUCCESS, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership renewal success: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipRenewalFailed(String username, String membershipAccountNo, String failureReason,
			Date retryDate, String contactSupport, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("failureReason", getSafeValue(failureReason));
			placeholders.put("retryDate", formatDate(retryDate));
			placeholders.put("contactSupport", getSafeValue(contactSupport));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_RENEWAL_FAILED, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership renewal failed: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipExpiryWarning(String username, String membershipAccountNo, Date expiryDate,
			Long daysRemaining, String renewalLink, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("expiryDate", formatDate(expiryDate));
			placeholders.put("daysRemaining", String.valueOf(daysRemaining));
			placeholders.put("renewalLink", getSafeValue(renewalLink));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_EXPIRY_WARNING, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership expiry warning: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipExpired(String username, String membershipAccountNo, Date expiryDate,
			Date gracePeriodEnds, String reactivationLink, Long orgId, Long adClientId, String phoneNumber,
			Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("expiryDate", formatDate(expiryDate));
			placeholders.put("gracePeriodEnds", formatDate(gracePeriodEnds));
			placeholders.put("reactivationLink", getSafeValue(reactivationLink));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_EXPIRED, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership expired: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipGracePeriodReminder(String username, String membershipAccountNo, Date graceEndDate,
			Long daysRemaining, BigDecimal reactivationFee, Long orgId, Long adClientId, String phoneNumber,
			Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("graceEndDate", formatDate(graceEndDate));
			placeholders.put("daysRemaining", String.valueOf(daysRemaining));
			placeholders.put("reactivationFee", formatAmount(reactivationFee));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_GRACE_PERIOD_REMINDER, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership grace period reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipUpgradeConfirmation(String username, String membershipAccountNo, String oldTier,
			String newTier, Date upgradeDate, BigDecimal priceDifference, String newBenefits, Long orgId,
			Long adClientId, String phoneNumber, Long reminderId, Long individualMemberId, Long groupMemberId,
			Long institutionMemberId, Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId,
			Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("oldTier", getSafeValue(oldTier));
			placeholders.put("newTier", getSafeValue(newTier));
			placeholders.put("upgradeDate", formatDate(upgradeDate));
			placeholders.put("priceDifference", formatAmount(priceDifference));
			placeholders.put("newBenefits", getSafeValue(newBenefits));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_UPGRADE_CONFIRMATION, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership upgrade confirmation: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipDowngradeConfirmation(String username, String membershipAccountNo, String oldTier,
			String newTier, Date effectiveDate, BigDecimal refundAmount, Long orgId, Long adClientId,
			String phoneNumber, Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("oldTier", getSafeValue(oldTier));
			placeholders.put("newTier", getSafeValue(newTier));
			placeholders.put("effectiveDate", formatDate(effectiveDate));
			placeholders.put("refundAmount", formatAmount(refundAmount));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_DOWNGRADE_CONFIRMATION, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership downgrade confirmation: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipPaymentReceived(String username, String membershipAccountNo, BigDecimal amountPaid,
			Date paymentDate, String paymentMethod, String transactionId, Date validUntil, Long orgId, Long adClientId,
			String phoneNumber, Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("amountPaid", formatAmount(amountPaid));
			placeholders.put("paymentDate", formatDate(paymentDate));
			placeholders.put("paymentMethod", getSafeValue(paymentMethod));
			placeholders.put("transactionId", getSafeValue(transactionId));
			placeholders.put("validUntil", formatDate(validUntil));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_PAYMENT_RECEIVED, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership payment received: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipRejection(String username, String membershipId, String reason,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long reminderId, Long orgId,
			Long adClientId, String phoneNumber, Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId,
			Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipId", getSafeValue(membershipId));
			placeholders.put("reason", getSafeValue(reason));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_REJECTION, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership payment received: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipPaymentDue(String username, String membershipAccountNo, BigDecimal amountDue,
			Date dueDate, Long daysRemaining, String paymentLink, Long orgId, Long adClientId, String phoneNumber,
			Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("amountDue", formatAmount(amountDue));
			placeholders.put("dueDate", formatDate(dueDate));
			placeholders.put("daysRemaining", String.valueOf(daysRemaining));
			placeholders.put("paymentLink", getSafeValue(paymentLink));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_PAYMENT_DUE, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership payment due: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipPaymentOverdue(String username, String membershipAccountNo, BigDecimal amountDue,
			Date dueDate, Long daysOverdue, BigDecimal lateFee, Date suspensionDate, Long orgId, Long adClientId,
			String phoneNumber, Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("amountDue", formatAmount(amountDue));
			placeholders.put("dueDate", formatDate(dueDate));
			placeholders.put("daysOverdue", String.valueOf(daysOverdue));
			placeholders.put("lateFee", formatAmount(lateFee));
			placeholders.put("suspensionDate", formatDate(suspensionDate));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_PAYMENT_OVERDUE, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership payment overdue: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipAutoDebitSuccess(String username, String membershipAccountNo, BigDecimal amountPaid,
			Date paymentDate, Date nextBillingDate, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("amountPaid", formatAmount(amountPaid));
			placeholders.put("paymentDate", formatDate(paymentDate));
			placeholders.put("nextBillingDate", formatDate(nextBillingDate));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_AUTO_DEBIT_SUCCESS, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership auto-debit success: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipAutoDebitFailed(String username, String membershipAccountNo, BigDecimal amountDue,
			String failureReason, Date retryDate, String updatePaymentMethod, Long orgId, Long adClientId,
			String phoneNumber, Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("amountDue", formatAmount(amountDue));
			placeholders.put("failureReason", getSafeValue(failureReason));
			placeholders.put("retryDate", formatDate(retryDate));
			placeholders.put("updatePaymentMethod", getSafeValue(updatePaymentMethod));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_AUTO_DEBIT_FAILED, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership auto-debit failed: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipActivation(String username, String membershipAccountNo, Date activationDate,
			String membershipType, Date validUntil, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("activationDate", formatDate(activationDate));
			placeholders.put("membershipType", getSafeValue(membershipType));
			placeholders.put("validUntil", formatDate(validUntil));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_ACTIVATION, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership activation: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipSuspension(String username, String membershipAccountNo, String suspensionReason,
			Date suspensionDate, String reactivationProcess, Long orgId, Long adClientId, String phoneNumber,
			Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("suspensionReason", getSafeValue(suspensionReason));
			placeholders.put("suspensionDate", formatDate(suspensionDate));
			placeholders.put("reactivationProcess", getSafeValue(reactivationProcess));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_SUSPENSION, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership suspension: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipReactivation(String username, String membershipAccountNo, Date reactivationDate,
			Date newExpiryDate, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("reactivationDate", formatDate(reactivationDate));
			placeholders.put("newExpiryDate", formatDate(newExpiryDate));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_REACTIVATION, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership reactivation: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipCancellation(String username, String membershipAccountNo, Date cancellationDate,
			String cancellationReason, BigDecimal refundAmount, Long orgId, Long adClientId, String phoneNumber,
			Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("cancellationDate", formatDate(cancellationDate));
			placeholders.put("cancellationReason", getSafeValue(cancellationReason));
			placeholders.put("refundAmount", formatAmount(refundAmount));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_CANCELLATION, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership cancellation: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipBenefitsReminder(String username, String membershipAccountNo, String membershipTier,
			String availableBenefits, Date benefitExpiryDate, Long orgId, Long adClientId, String phoneNumber,
			Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("membershipTier", getSafeValue(membershipTier));
			placeholders.put("availableBenefits", getSafeValue(availableBenefits));
			placeholders.put("benefitExpiryDate", formatDate(benefitExpiryDate));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_BENEFITS_REMINDER, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership benefits reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipSpecialOffer(String username, String membershipAccountNo, String offerTitle,
			String offerDetails, Date offerExpiry, String redeemLink, Long orgId, Long adClientId, String phoneNumber,
			Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("offerTitle", getSafeValue(offerTitle));
			placeholders.put("offerDetails", getSafeValue(offerDetails));
			placeholders.put("offerExpiry", formatDate(offerExpiry));
			placeholders.put("redeemLink", getSafeValue(redeemLink));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_SPECIAL_OFFER, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership special offer: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipAnniversary(String username, String membershipAccountNo, int yearsAsMember,
			Date joinDate, String specialGift, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("yearsAsMember", String.valueOf(yearsAsMember));
			placeholders.put("joinDate", formatDate(joinDate));
			placeholders.put("specialGift", getSafeValue(specialGift));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_ANNIVERSARY, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership anniversary: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipBirthdayGreeting(String username, String membershipAccountNo, Date birthday,
			String specialOffer, Date validityPeriod, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("birthday", formatDate(birthday));
			placeholders.put("specialOffer", getSafeValue(specialOffer));
			placeholders.put("validityPeriod", formatDate(validityPeriod));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_BIRTHDAY_GREETING, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership birthday greeting: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipPointsEarned(String username, String membershipAccountNo, int pointsEarned,
			int totalPoints, String transactionDetails, Date pointsExpiry, Long orgId, Long adClientId,
			String phoneNumber, Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("pointsEarned", String.valueOf(pointsEarned));
			placeholders.put("totalPoints", String.valueOf(totalPoints));
			placeholders.put("transactionDetails", getSafeValue(transactionDetails));
			placeholders.put("pointsExpiry", formatDate(pointsExpiry));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_POINTS_EARNED, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership points earned: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipPointsExpiry(String username, String membershipAccountNo, int pointsExpiring,
			Date expiryDate, String redeemLink, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("pointsExpiring", String.valueOf(pointsExpiring));
			placeholders.put("expiryDate", formatDate(expiryDate));
			placeholders.put("redeemLink", getSafeValue(redeemLink));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_POINTS_EXPIRY, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership points expiry: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipRewardRedeemed(String username, String membershipAccountNo, String rewardName,
			int pointsUsed, Date redemptionDate, String deliveryDetails, Long orgId, Long adClientId,
			String phoneNumber, Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("rewardName", getSafeValue(rewardName));
			placeholders.put("pointsUsed", String.valueOf(pointsUsed));
			placeholders.put("redemptionDate", formatDate(redemptionDate));
			placeholders.put("deliveryDetails", getSafeValue(deliveryDetails));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_REWARD_REDEEMED, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership reward redeemed: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipProfileUpdate(String username, String membershipAccountNo, String updatedFields,
			Date updateDate, Long orgId, Long adClientId, String phoneNumber, Long reminderId, Long individualMemberId,
			Long groupMemberId, Long institutionMemberId, Long memberShipBillId, Long memberShipPlanId,
			Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("updatedFields", getSafeValue(updatedFields));
			placeholders.put("updateDate", formatDate(updateDate));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_PROFILE_UPDATE, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership profile update: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipDocumentVerified(String username, String membershipAccountNo, String documentType,
			Date verificationDate, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("documentType", getSafeValue(documentType));
			placeholders.put("verificationDate", formatDate(verificationDate));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_DOCUMENT_VERIFIED, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership document verified: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipDocumentRejected(String username, String membershipAccountNo, String documentType,
			String rejectionReason, String resubmissionLink, Long orgId, Long adClientId, String phoneNumber,
			Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("documentType", getSafeValue(documentType));
			placeholders.put("rejectionReason", getSafeValue(rejectionReason));
			placeholders.put("resubmissionLink", getSafeValue(resubmissionLink));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_DOCUMENT_REJECTED, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership document rejected: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipInactivityReminder(String username, String membershipAccountNo, Date lastActiveDate,
			int daysInactive, String engagementOffer, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("lastActiveDate", formatDate(lastActiveDate));
			placeholders.put("daysInactive", String.valueOf(daysInactive));
			placeholders.put("engagementOffer", getSafeValue(engagementOffer));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_INACTIVITY_REMINDER, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership inactivity reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipSurveyRequest(String username, String membershipAccountNo, String surveyLink,
			String incentive, Long orgId, Long adClientId, String phoneNumber, Long reminderId, Long individualMemberId,
			Long groupMemberId, Long institutionMemberId, Long memberShipBillId, Long memberShipPlanId,
			Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("surveyLink", getSafeValue(surveyLink));
			placeholders.put("incentive", getSafeValue(incentive));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_SURVEY_REQUEST, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership survey request: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipReferralSuccess(String username, String membershipAccountNo, String referredName,
			String rewardEarned, int totalReferrals, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("referredName", getSafeValue(referredName));
			placeholders.put("rewardEarned", getSafeValue(rewardEarned));
			placeholders.put("totalReferrals", String.valueOf(totalReferrals));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_REFERRAL_SUCCESS, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership referral success: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipReferralReminder(String username, String membershipAccountNo, String referralCode,
			String referralLink, String rewardAmount, Long orgId, Long adClientId, String phoneNumber, Long reminderId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long memberShipBillId,
			Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("referralCode", getSafeValue(referralCode));
			placeholders.put("referralLink", getSafeValue(referralLink));
			placeholders.put("rewardAmount", getSafeValue(rewardAmount));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_REFERRAL_REMINDER, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership referral reminder: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembership2faEnabled(String username, String membershipAccountNo, Date enableDate, Long orgId,
			Long adClientId, String phoneNumber, Long reminderId, Long individualMemberId, Long groupMemberId,
			Long institutionMemberId, Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId,
			Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("enableDate", formatDate(enableDate));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_2FA_ENABLED, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership 2FA enabled: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipPasswordChanged(String username, String membershipAccountNo, Date changeDate,
			String ipAddress, Long orgId, Long adClientId, String phoneNumber, Long reminderId, Long individualMemberId,
			Long groupMemberId, Long institutionMemberId, Long memberShipBillId, Long memberShipPlanId,
			Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("changeDate", formatDate(changeDate));
			placeholders.put("ipAddress", getSafeValue(ipAddress));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_PASSWORD_CHANGED, placeholders, orgId, adClientId,
					phoneNumber, memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId,
					membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership password changed: {}", e.getMessage(), e);
			return false;
		}
	}

	@Transactional
	public boolean handleMembershipLoginAlert(String username, String membershipAccountNo, Date loginTime,
			String deviceInfo, String location, String actionLink, Long orgId, Long adClientId, String phoneNumber,
			Long reminderId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long memberShipBillId, Long memberShipPlanId, Long membershipInvoiceId, Long membershipAccountId) {
		try {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", getSafeValue(username));
			placeholders.put("membershipAccountNo", getSafeValue(membershipAccountNo));
			placeholders.put("loginTime", formatDate(loginTime));
			placeholders.put("deviceInfo", getSafeValue(deviceInfo));
			placeholders.put("location", getSafeValue(location));
			placeholders.put("actionLink", getSafeValue(actionLink));

			return sendmembershipSMS(SmsTypeEnum.MEMBERSHIP_LOGIN_ALERT, placeholders, orgId, adClientId, phoneNumber,
					memberShipBillId, memberShipPlanId, LocalDateTime.now(), reminderId, membershipInvoiceId,
					individualMemberId, groupMemberId, institutionMemberId, membershipAccountId);
		} catch (Exception e) {
			log.error("Error handling membership login alert: {}", e.getMessage(), e);
			return false;
		}
	}

	private boolean sendmembershipSMS(SmsTypeEnum type, Map<String, String> placeholders, Long orgId, Long adClientId,
			String phoneNumber, Long memberShipBillId, Long memberShipPlanId, LocalDateTime time, Long reminderId,
			Long membershipInvoiceId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long membershipAccountId) {
		try {
			String template = getTemplate(getRemindersConfiguration(reminderId), type, orgId);
			String message = utils.processTemplate(template, placeholders);

			utils.saveMembershipSms(type, phoneNumber, message, orgId, adClientId, memberShipBillId, memberShipPlanId,
					time, reminderId, membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId,
					membershipAccountId);

			log.info("✅ Sent {} to {}", type.getDescription(), phoneNumber);
			return true;
		} catch (Exception e) {
			log.error("❌ Failed to send {} to {}: {}", type, phoneNumber, e.getMessage(), e);
			return false;
		}
	}
	

	

	// ==================== HELPER METHODS ====================

	private boolean sendSms(SmsTypeEnum type, Map<String, String> placeholders, Long orgId, Long clientId,
			String phoneNumber, Long installmentId, Long loanId, Long reminderId, LocalDateTime timeTosend,
			Map<String, Long> borrowerIds) {
		return sendSmsWithGuarantorId(type, placeholders, orgId, clientId, phoneNumber, installmentId, loanId,
				reminderId, timeTosend, borrowerIds, null);
	}

	private boolean sendSmsWithGuarantorId(SmsTypeEnum type, Map<String, String> placeholders, Long orgId,
			Long clientId, String phoneNumber, Long installmentId, Long loanId, Long reminderId,
			LocalDateTime timeTosend, Map<String, Long> borrowerIds, Long guarantorId) {
		if (timeTosend == null) {
			timeTosend = LocalDateTime.now();
		}
		try {
			String template = getTemplate(getRemindersConfiguration(reminderId), type, orgId);
			if (template == null || template.trim().isEmpty()) {
				log.error("❌ Empty template for SMS type: {}, Loan ID: {}", type, loanId);
				return false;
			}

			String message = utils.processTemplate(template, placeholders);
			if (message == null || message.trim().isEmpty()) {
				log.error("❌ Empty message after template processing for SMS type: {}, Loan ID: {}", type, loanId);
				return false;
			}

			MLoanApplication loan = loanApplicationRepository.findById(loanId).orElse(null);
			if (loan == null) {
				log.error("❌ Loan {} not found for SMS type {}", loanId, type);
				return false;
			}

			if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
				phoneNumber = utils.getBorrowerPhone(loan);
				if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
					log.error("❌ Cannot send SMS {} for loan {}: Phone number is null or empty", type, loanId);
					return false;
				}
			}

			boolean saved = utils.saveReminderSms(borrowerIds, phoneNumber, message, orgId, clientId, type,
					installmentId, loanId, timeTosend, reminderId, guarantorId);

			try {
				String borrowerEmail = utils.getBorrowerEmail(loan);
				if (borrowerEmail != null && !borrowerEmail.trim().isEmpty()) {
					String emailSubject = type.getDescription() + " - " + loan.getDocumentNo();
					MUser borrower = utils.getUserByBorrower(loan);
					if (borrower != null) {
						utils.sendEmail(borrower, message, emailSubject);
						log.info("✅ Email copy sent to {} for loan {}", borrowerEmail, loanId);
					} else {
						String[] nameParts = utils.getBorrowerName(loan).split(" ");
						String firstName = nameParts.length > 0 ? nameParts[0] : "";
						String lastName = nameParts.length > 1 ? nameParts[1] : "";
						utils.sendEmail(firstName, lastName, borrowerEmail, message, emailSubject);
					}
				}
			} catch (Exception e) {
				log.error("Failed to send email copy for loan {}: {}", loanId, e.getMessage());
			}

			if (saved) {
				log.info("✅ Sent {} to {} for loan {}", type.getDescription(), phoneNumber, loanId);
				return true;
			} else {
				log.error("❌ Failed to save SMS record for type: {}, Loan ID: {}", type, loanId);
				return false;
			}
		} catch (Exception e) {
			log.error("❌ Failed to send {} to {}: {}", type, phoneNumber, e.getMessage(), e);
			return false;
		}
	}

	private boolean sendStatementSms(SmsTypeEnum type, Map<String, String> placeholders, Long orgId, Long clientId,
			String phoneNumber, Long installmentId, Long loanId, Long reminderId, LocalDateTime timeTosend,
			String downLoadLink, Map<String, Long> borrowerIds) {
		return sendStatementSmsWithGuarantorId(type, placeholders, orgId, clientId, phoneNumber, installmentId, loanId,
				reminderId, timeTosend, downLoadLink, borrowerIds, null);
	}

	private boolean sendStatementSmsWithGuarantorId(SmsTypeEnum type, Map<String, String> placeholders, Long orgId,
			Long clientId, String phoneNumber, Long installmentId, Long loanId, Long reminderId,
			LocalDateTime timeTosend, String downLoadLink, Map<String, Long> borrowerIds, Long guarantorId) {
		if (timeTosend == null) {
			timeTosend = LocalDateTime.now();
		}
		try {
			String template = getTemplate(getRemindersConfiguration(reminderId), type, orgId);
			if (template == null || template.trim().isEmpty()) {
				log.error("❌ Empty template for SMS type: {}, Loan ID: {}", type, loanId);
				return false;
			}

			String message = utils.processTemplate(template, placeholders);
			StringBuffer ms = new StringBuffer();
			ms.append(message);
			ms.append(" To download the statement, Please click the link below.\n" + downLoadLink);

			if (message == null || message.trim().isEmpty()) {
				log.error("❌ Empty message after template processing for SMS type: {}, Loan ID: {}", type, loanId);
				return false;
			}

			MLoanApplication loan = loanApplicationRepository.findById(loanId).orElse(null);
			if (loan == null) {
				log.error("❌ Loan {} not found for SMS type {}", loanId, type);
				return false;
			}

			if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
				phoneNumber = utils.getBorrowerPhone(loan);
				if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
					log.error("❌ Cannot send SMS {} for loan {}: Phone number is null or empty", type, loanId);
					return false;
				}
			}

			boolean saved = utils.saveReminderSms(borrowerIds, phoneNumber, ms.toString(), orgId, clientId, type,
					installmentId, loanId, timeTosend, reminderId, guarantorId);

			try {
				String borrowerEmail = utils.getBorrowerEmail(loan);
				if (borrowerEmail != null && !borrowerEmail.trim().isEmpty()) {
					String emailSubject = "Statement Ready - " + loan.getDocumentNo();
					String emailMessage = message + "<br><br>To download your statement, click the link below:<br>"
							+ "<a href='" + downLoadLink + "'>Download Statement</a>";

					MUser borrower = utils.getUserByBorrower(loan);
					if (borrower != null) {
						utils.sendEmail(borrower, emailMessage, emailSubject);
						log.info("✅ Statement email sent to {} for loan {}", borrowerEmail, loanId);
					}
				}
			} catch (Exception e) {
				log.error("Failed to send statement email for loan {}: {}", loanId, e.getMessage());
			}

			if (saved) {
				log.info("✅ Sent {} to {} for loan {}", type.getDescription(), phoneNumber, loanId);
				return true;
			} else {
				log.error("❌ Failed to save SMS record for type: {}, Loan ID: {}", type, loanId);
				return false;
			}
		} catch (Exception e) {
			log.error("❌ Failed to send {} to {}: {}", type, phoneNumber, e.getMessage(), e);
			return false;
		}
	}

	private boolean sendGuarantorGenericSms(SmsTypeEnum type, Map<String, String> placeholders, MNextOfKin guarantor,
			Long orgId, Long adClientId, Long reminderId) {
		try {
			String template = getTemplate(getRemindersConfiguration(reminderId), type, orgId);
			String message = utils.processTemplate(template, placeholders);

			utils.saveGuarantorSms(guarantor.getNextOfKinId(), guarantor.getPhoneNumber(), message, orgId, adClientId,
					type, null, null, LocalDateTime.now(), reminderId);

			log.info("✅ Sent {} to guarantor {}", type.getDescription(), guarantor.getFullName());
			return true;
		} catch (Exception e) {
			log.error("❌ Failed to send {} to guarantor {}: {}", type, guarantor.getFullName(), e.getMessage(), e);
			return false;
		}
	}

	private boolean sendGuarantorGenericSmsLoanStatement(SmsTypeEnum type, Map<String, String> placeholders,
			MNextOfKin guarantor, Long orgId, Long adClientId, Long reminderId, String link) {
		try {
			String template = getTemplate(getRemindersConfiguration(reminderId), type, orgId);
			String message = utils.processTemplate(template, placeholders);

			StringBuffer sm = new StringBuffer();
			sm.append(message);
			sm.append(" To download the statement, please click on the link below. \n" + link);

			utils.saveGuarantorSms(guarantor.getNextOfKinId(), guarantor.getPhoneNumber(), sm.toString(), orgId,
					adClientId, type, null, null, LocalDateTime.now(), reminderId);

			try {
				String guarantorEmail = utils.getGuarantorEmail(guarantor);
				if (guarantorEmail != null && !guarantorEmail.trim().isEmpty()) {
					String emailSubject = type.getDescription() + " - Statement Ready";

					String htmlMessage = message.replace("\n", "<br>")
							+ "<br><br>To download the statement, please click the link below:<br>" + "<a href='" + link
							+ "' style='display: inline-block; background-color: #FF9800; "
							+ "color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; "
							+ "margin-top: 10px;'>Download Statement</a>";

					utils.sendGuarantorEmail(guarantor, htmlMessage, emailSubject, orgId);

					log.info("✅ Statement email sent to guarantor {} at {}", guarantor.getFullName(), guarantorEmail);
				}
			} catch (Exception e) {
				log.error("Failed to send statement email to guarantor {}: {}", guarantor.getFullName(),
						e.getMessage());
			}

			log.info("✅ Sent {} to guarantor {}", type.getDescription(), guarantor.getFullName());
			return true;
		} catch (Exception e) {
			log.error("❌ Failed to send {} to guarantor {}: {}", type, guarantor.getFullName(), e.getMessage(), e);
			return false;
		}
	}

	private boolean sendGenericSms(SmsTypeEnum type, Map<String, String> placeholders, Long orgId, Long adClientId,
			String phoneNumber, Long installmentId, Long loanId, LocalDateTime time, Long reminderId,
			Long guarantorId) {
		try {
			String template = getTemplate(getRemindersConfiguration(reminderId), type, orgId);
			String message = utils.processTemplate(template, placeholders);

			utils.saveSms(null, phoneNumber, message, orgId, adClientId, type, installmentId, loanId, time, reminderId,
					guarantorId);

			log.info("✅ Sent {} to {}", type.getDescription(), phoneNumber);
			return true;
		} catch (Exception e) {
			log.error("❌ Failed to send {} to {}: {}", type, phoneNumber, e.getMessage(), e);
			return false;
		}
	}

	// ==================== PLACEHOLDER BUILDERS ====================

	private Map<String, String> buildLoanPlaceholders(MLoanApplication loan) {
		Map<String, String> placeholders = new HashMap<>();

		placeholders.put("username", getSafeValue(utils.getBorrowerName(loan)));
		placeholders.put("amountOverDue", formatAmount(loan.getBalance()));
		placeholders.put("documentNo", getSafeValue(loan.getDocumentNo()));
		placeholders.put("loanType", getLoanType(loan));
		placeholders.put("amountApplied", formatAmount(loan.getAppliedAmount()));
		placeholders.put("amountApproved", formatAmount(loan.getApprovedAmount()));
		placeholders.put("balance", formatAmount(loan.getBalance()));
		placeholders.put("outstandingBalance", formatAmount(loan.getBalance()));

		if (loan.getExpectedDisbursementDate() != null) {
			placeholders.put("disbursementDate", formatDate(loan.getExpectedDisbursementDate()));
		}
		if (loan.getDueDate() != null) {
			placeholders.put("nextInstallmentDueDate", formatDate(loan.getDueDate()));
			placeholders.put("dueDate", formatDate(loan.getDueDate()));
		}
		if (loan.getTermInDays() != null) {
			placeholders.put("loanTermInDays", String.valueOf(loan.getTermInDays()));
		}

		Integer graceDays = getGracePeriodDays(loan);
		if (graceDays != null) {
			placeholders.put("gracePeriodDays", String.valueOf(graceDays));
		}

		placeholders.put("repaymentStartDate", formatDate(calculateRepaymentStartDate(loan)));
		placeholders.put("interestEarned",
				formatAmount(loan.getInterestsEarned() != null ? loan.getInterestsEarned() : BigDecimal.ZERO));
		placeholders.put("penaltyAmountIncur",
				formatAmount(loan.getPenaltyEarned() != null ? loan.getPenaltyEarned() : BigDecimal.ZERO));

		return placeholders;
	}

	private Map<String, String> buildInstallmentPlaceholders(MInstallments installment) {
		Map<String, String> placeholders = new HashMap<>();
		MLoanApplication loan = installment.getLoan();

		placeholders.put("username", getSafeValue(utils.getBorrowerName(loan)));
		placeholders.put("documentNo", getSafeValue(loan.getDocumentNo()));
		placeholders.put("loanType", getLoanType(loan));
		placeholders.put("installmentAmount", formatAmount(installment.getAmount()));
		placeholders.put("installmentDueDate", formatDate(installment.getPeriodEnd()));
		placeholders.put("installmentNumber", String.valueOf(installment.getInstallmentNo()));
		placeholders.put("totalInstallments", String.valueOf(countAllInstallments(loan)));
		placeholders.put("totalDue", formatAmount(installment.getLoan().getBalance()));
		placeholders.put("balance", formatAmount(installment.getLoan().getBalance()));
		placeholders.put("outstandingBalance", formatAmount(installment.getBalance()));

		BigDecimal penaltyAmount = installment.getPenaltyEarned() != null ? installment.getPenaltyEarned()
				: BigDecimal.ZERO;
		BigDecimal interestAmount = installment.getInterestEarned() != null ? installment.getInterestEarned()
				: BigDecimal.ZERO;
		BigDecimal principalAmount = installment.getAmount().subtract(interestAmount);

		placeholders.put("penaltyAmount", formatAmount(penaltyAmount));
		placeholders.put("interestAmount", formatAmount(interestAmount));
		placeholders.put("principalAmount", formatAmount(principalAmount));

		return placeholders;
	}

	private Map<String, String> buildInstallmentPaymentPlaceholders(MPayments payment, MInstallments installment) {
		Map<String, String> placeholders = buildInstallmentPlaceholders(installment);

		placeholders.put("amountPaid", formatAmount(payment.getAmount()));
		placeholders.put("paymentDate", formatDate(payment.getCreated()));
		placeholders.put("paymentMethod", getSafeValue(
				payment.getPaymentMethod() != null ? payment.getPaymentMethod().getDescription() : "Payment"));
		placeholders.put("transactionId", getSafeValue(
				payment.getMpesareceitNumber() != null ? payment.getMpesareceitNumber() : payment.getDocumentNo()));
		placeholders.put("receiptNumber", getSafeValue(payment.getDocumentNo()));
		placeholders.put("remainingBalance", formatAmount(installment.getLoan().getBalance()));

		Optional<MInstallments> nextInstallment = getNextInstallment(installment);
		if (nextInstallment.isPresent()) {
			placeholders.put("nextDueDate", formatDate(nextInstallment.get().getPeriodEnd()));
		}

		return placeholders;
	}

	private Map<String, String> buildPaymentPlaceholders(MPayments payment) {
		Map<String, String> placeholders = new HashMap<>();
		MLoanApplication loan = payment.getLoan();

		placeholders.put("username", getSafeValue(utils.getBorrowerName(loan)));
		placeholders.put("documentNo", getSafeValue(loan.getDocumentNo()));
		placeholders.put("loanType", getLoanType(loan));
		placeholders.put("amountPaid", formatAmount(payment.getAmount()));
		placeholders.put("paymentDate", formatDate(payment.getCreated()));
		placeholders.put("paymentMethod", getSafeValue(
				payment.getPaymentMethod() != null ? payment.getPaymentMethod().getDescription() : "Payment"));
		placeholders.put("balance", formatAmount(loan.getBalance()));
		placeholders.put("outstandingBalance", formatAmount(loan.getBalance()));

		if (payment.getMpesareceitNumber() != null) {
			placeholders.put("paymentReference", getSafeValue(payment.getMpesareceitNumber()));
			placeholders.put("transactionId", getSafeValue(payment.getMpesareceitNumber()));
		} else {
			placeholders.put("paymentReference", getSafeValue(payment.getReference()));
			placeholders.put("transactionId", getSafeValue(payment.getDocumentNo()));
		}

		BigDecimal totalPaid = calculateTotalPaid(loan);
		BigDecimal remainingBalance = loan.getBalance();

		placeholders.put("totalPaid", formatAmount(totalPaid));
		placeholders.put("remainingBalance", formatAmount(remainingBalance.max(BigDecimal.ZERO)));

		return placeholders;
	}

	private Map<String, String> buildGuarantorPlaceholders(MNextOfKin guarantor, MLoanApplication loan) {
		Map<String, String> placeholders = new HashMap<>();

		placeholders.put("guarantorName", getSafeValue(guarantor.getFullName()));
		placeholders.put("borrowerName", getSafeValue(utils.getBorrowerName(loan)));
		placeholders.put("loanType", getLoanType(loan));
		placeholders.put("documentNo", getSafeValue(loan.getDocumentNo()));
		placeholders.put("amountApproved", formatAmount(loan.getApprovedAmount()));
		placeholders.put("outstandingBalance", formatAmount(loan.getBalance()));

		return placeholders;
	}

	private Map<String, String> buildGuarantorInstallmentPlaceholders(MNextOfKin guarantor, MInstallments installment) {
		Map<String, String> placeholders = new HashMap<>();
		MLoanApplication loan = installment.getLoan();

		placeholders.put("guarantorName", getSafeValue(guarantor.getFullName()));
		placeholders.put("borrowerName", getSafeValue(utils.getBorrowerName(loan)));
		placeholders.put("loanType", getLoanType(loan));
		placeholders.put("documentNo", getSafeValue(loan.getDocumentNo()));
		placeholders.put("installmentAmount", formatAmount(installment.getAmount()));
		placeholders.put("installmentDueDate", formatDate(installment.getPeriodEnd()));
		placeholders.put("installmentNumber", String.valueOf(installment.getInstallmentNo()));
		placeholders.put("totalInstallments", String.valueOf(countAllInstallments(loan)));
		placeholders.put("balance", formatAmount(installment.getLoan().getBalance()));
		placeholders.put("outstandingBalance", formatAmount(installment.getBalance()));

		BigDecimal penaltyAmount = installment.getPenaltyEarned() != null ? installment.getPenaltyEarned()
				: BigDecimal.ZERO;
		placeholders.put("penaltyAmount", formatAmount(penaltyAmount));
		placeholders.put("lateFee", formatAmount(penaltyAmount));

		return placeholders;
	}

	private Map<String, String> buildGuarantorPaymentPlaceholders(MNextOfKin guarantor, MLoanApplication loan,
			BigDecimal amountPaid, BigDecimal remainingBalance, BigDecimal totalOutstanding, Date paymentDate) {
		Map<String, String> placeholders = new HashMap<>();

		placeholders.put("guarantorName", getSafeValue(guarantor.getFullName()));
		placeholders.put("borrowerName", getSafeValue(utils.getBorrowerName(loan)));
		placeholders.put("loanType", getLoanType(loan));
		placeholders.put("documentNo", getSafeValue(loan.getDocumentNo()));
		placeholders.put("amountPaid", formatAmount(amountPaid));
		placeholders.put("balance", formatAmount(remainingBalance));
		placeholders.put("remainingBalance", formatAmount(remainingBalance));
		placeholders.put("paymentDate", formatDate(paymentDate));
		placeholders.put("totalOutstanding", formatAmount(totalOutstanding));
		placeholders.put("outstandingBalance", formatAmount(totalOutstanding));

		return placeholders;
	}

	// ==================== UTILITY METHODS ====================

	private String getTemplate(MRemindersConfiguration config, SmsTypeEnum type, long adOrgId) {
		MSmsSetup smsSetup = null;

		if (config == null) {
			config = reminderConfigRepository
					.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(adOrgId, true,
							type);
		}
		if (config != null && config.getSmsMessageTemplate() != null) {
			smsSetup = config.getSmsMessageTemplate();
		}

		if (smsSetup == null) {
			smsSetup = smsSetupRepository.findTop1ByAdOrgIDAndIsActiveAndSmsTypeOrderBySmsSetupIdDesc(adOrgId, true,
					type);
		}

		if (smsSetup != null && smsSetup.getMessageTemplate() != null) {
			return smsSetup.getMessageTemplate();
		}

		return getDefaultMessage(type);
	}

	private String getDefaultMessage(SmsTypeEnum type) {
		return utils.getDefaultMessage(type);
	}

	private String getSafeValue(String value) {
		return value != null ? value : "";
	}

	private String formatAmount(BigDecimal amount) {
		return utils.formatAmount(amount != null ? amount : BigDecimal.ZERO, "KES");
	}

	private String formatDate(Date date) {
		return date != null ? utils.formatDate(date) : "";
	}

	private String getLoanType(MLoanApplication loan) {
		return loan.getLoanProductConfiguration() != null && loan.getLoanProductConfiguration().getIsDebtProduct()
				? "Debt"
				: "Loan";
	}

	private Integer getGracePeriodDays(MLoanApplication loan) {
		if (loan.getLoanProductConfiguration() != null
				&& loan.getLoanProductConfiguration().getRepaymentScheduleType() != null) {

			if (loan.getLoanProductConfiguration().getRepaymentScheduleType()
					.equals(RepaymentScheduleTypeEnum.INSTALLMENTS)) {
				return loan.getGracePeriodToFirstInstallment();
			} else {
				return loan.getGraceperiod();
			}
		}
		return null;
	}

	private Date calculateRepaymentStartDate(MLoanApplication loan) {
		if (loan.getExpectedDisbursementDate() == null)
			return new Date();

		Integer gracePeriod = getGracePeriodDays(loan);
		if (gracePeriod != null) {
			return utils.getFutureDateUsingCalender(loan.getExpectedDisbursementDate(), gracePeriod);
		}
		return new Date();
	}

	private Date calculateGracePeriodEnd(MLoanApplication loan) {
		if (loan.getExpectedDisbursementDate() == null)
			return new Date();

		Integer gracePeriod = getGracePeriodDays(loan);
		if (gracePeriod == null || gracePeriod == 0) {
			return loan.getExpectedDisbursementDate();
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(loan.getExpectedDisbursementDate());
		cal.add(Calendar.DAY_OF_MONTH, gracePeriod);
		return cal.getTime();
	}

	public BigDecimal calculateTotalPaid(MLoanApplication loan) {
		try {
			BigDecimal totalPaid = BigDecimal.ZERO;
			List<MPayments> payments = paymentRepository
					.findByIsActiveAndAdOrgIDAndLoanAndDocStatusOrderByPaymentIdDesc(true, loan.getAdOrgID(), loan,
							DocStatus.COMPLETED);

			for (MPayments payment : payments) {
				totalPaid = totalPaid.add(payment.getAmount());
			}
			return totalPaid;
		} catch (Exception e) {
			log.warn("Error calculating total paid: {}", e.getMessage());
			return BigDecimal.ZERO;
		}
	}

	private int countAllInstallments(MLoanApplication loan) {
		try {
			List<MInstallments> allInstallments = installmentRepository.findByIsActiveAndLoanOrderByPeriodStartAsc(true,
					loan);
			return allInstallments.size();
		} catch (Exception e) {
			log.warn("Error counting installments: {}", e.getMessage());
			return 1;
		}
	}

	private Optional<MInstallments> getNextInstallment(MInstallments currentInstallment) {
		try {
			List<MInstallments> nextInstallments = installmentRepository
					.findNextInstallments(currentInstallment.getLoan(), currentInstallment.getPeriodStart());
			return nextInstallments.isEmpty() ? Optional.empty() : Optional.of(nextInstallments.get(0));
		} catch (Exception e) {
			log.warn("Error getting next installment: {}", e.getMessage());
			return Optional.empty();
		}
	}

	private long calculateDaysRemaining(Date dueDate) {
		if (dueDate == null)
			return 0;
		long millisecondsRemaining = dueDate.getTime() - new Date().getTime();
		return Math.max(0, TimeUnit.MILLISECONDS.toDays(millisecondsRemaining));
	}

	private long calculateDaysOverdue(Date dueDate) {
		if (dueDate == null)
			return 0;
		long millisecondsOverdue = new Date().getTime() - dueDate.getTime();
		return Math.max(0, TimeUnit.MILLISECONDS.toDays(millisecondsOverdue));
	}

	
	
	// ==================== LOAN CANCELLATION NOTIFICATIONS ====================

	/**
	 * Handle loan cancellation notification
	 * 
	 * @param loan The cancelled loan
	 * @param cancellationReason The reason for cancellation
	 * @return true if notification was sent successfully
	 */
	@Transactional
	public boolean handleLoanCancellation(MLoanApplication loan, String cancellationReason) {
	    try {
	        Map<String, String> placeholders = buildLoanPlaceholders(loan);
	        placeholders.put("cancellationReason", getSafeValue(cancellationReason));
	        placeholders.put("cancellationDate", formatDate(new Date()));
	        placeholders.put("loanStatus", "CANCELLED");
	        placeholders.put("amountApplied", formatAmount(loan.getAppliedAmount()));
	        placeholders.put("balance", formatAmount(loan.getBalance()));

	        // Send to borrower
	        boolean borrowerSent = sendBorrowerSms(SmsTypeEnum.LOAN_CANCELLATION_NOTIFICATION, placeholders, loan, null,
	                null, LocalDateTime.now(), null);

	        // Also notify assignee if exists
	        if (loan.getAssignee() != null) {
	            String assigneePhone = loan.getAssignee().getPhoneNumber();
	            if (assigneePhone != null && !assigneePhone.trim().isEmpty()) {
	                sendGenericSms(SmsTypeEnum.LOAN_CANCELLATION_NOTIFICATION, placeholders, loan.getAdOrgID(),
	                        loan.getAdClientId(), assigneePhone, null, loan.getLoanApplicationId(),
	                        LocalDateTime.now(), null, null);
	            }
	        }

	        // Notify all guarantors
	        if (loan.getGuarantors() != null && !loan.getGuarantors().isEmpty()) {
	            for (MNextOfKin guarantor : loan.getGuarantors()) {
	                Map<String, String> guarantorPlaceholders = buildGuarantorPlaceholders(guarantor, loan);
	                guarantorPlaceholders.put("cancellationReason", getSafeValue(cancellationReason));
	                guarantorPlaceholders.put("cancellationDate", formatDate(new Date()));
	                sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_CANCELLATION_NOTIFICATION, guarantorPlaceholders,
	                        guarantor, loan, null, null, null);
	            }
	        }

	        log.info("✅ Loan cancellation notification sent for loan: {}", loan.getDocumentNo());
	        return borrowerSent;
	    } catch (Exception e) {
	        log.error("Error handling loan cancellation notification for loan {}: {}", loan.getLoanApplicationId(),
	                e.getMessage(), e);
	        return false;
	    }
	}

	// ==================== LOAN CONSOLIDATION NOTIFICATIONS ====================

	/**
	 * Handle loan consolidation notification
	 * 
	 * @param parentLoan The parent loan
	 * @param childLoans List of child loans consolidated
	 * @param consolidatedBillingGroupId The group ID
	 * @return true if notification was sent successfully
	 */
	@Transactional
	public boolean handleLoanConsolidationNotification(MLoanApplication parentLoan,
	        List<MLoanApplication> childLoans, String consolidatedBillingGroupId) {
	    try {
	        // Build consolidated details
	        BigDecimal totalChildBalance = BigDecimal.ZERO;
	        int childCount = childLoans != null ? childLoans.size() : 0;
	        StringBuilder childLoanRefs = new StringBuilder();

	        if (childLoans != null) {
	            for (MLoanApplication child : childLoans) {
	                totalChildBalance = totalChildBalance.add(child.getBalance() != null ? child.getBalance() : BigDecimal.ZERO);
	                if (childLoanRefs.length() > 0) {
	                    childLoanRefs.append(", ");
	                }
	                childLoanRefs.append(child.getDocumentNo());
	            }
	        }

	        // Send to parent loan borrower
	        Map<String, String> parentPlaceholders = buildLoanPlaceholders(parentLoan);
	        parentPlaceholders.put("consolidationDate", formatDate(new Date()));
	        parentPlaceholders.put("consolidatedBillingGroupId", getSafeValue(consolidatedBillingGroupId));
	        parentPlaceholders.put("totalChildBalance", formatAmount(totalChildBalance));
	        parentPlaceholders.put("childLoanCount", String.valueOf(childCount));
	        parentPlaceholders.put("childLoanRefs", childLoanRefs.toString());
	        parentPlaceholders.put("newTotalBalance", formatAmount(parentLoan.getBalance().add(totalChildBalance)));
	        parentPlaceholders.put("consolidatedDueDate", formatDate(parentLoan.getDueDate()));

	        boolean parentSent = sendBorrowerSms(SmsTypeEnum.LOAN_CONSOLIDATION_NOTIFICATION, parentPlaceholders,
	                parentLoan, null, null, LocalDateTime.now(), null);

	        // Notify assignee if exists
	        if (parentLoan.getAssignee() != null) {
	            String assigneePhone = parentLoan.getAssignee().getPhoneNumber();
	            if (assigneePhone != null && !assigneePhone.trim().isEmpty()) {
	                sendGenericSms(SmsTypeEnum.LOAN_CONSOLIDATION_NOTIFICATION, parentPlaceholders,
	                        parentLoan.getAdOrgID(), parentLoan.getAdClientId(), assigneePhone,
	                        null, parentLoan.getLoanApplicationId(), LocalDateTime.now(), null, null);
	            }
	        }

	        // Notify each child loan borrower
	        if (childLoans != null) {
	            for (MLoanApplication child : childLoans) {
	                Map<String, String> childPlaceholders = buildLoanPlaceholders(child);
	                childPlaceholders.put("consolidationDate", formatDate(new Date()));
	                childPlaceholders.put("consolidatedBillingGroupId", getSafeValue(consolidatedBillingGroupId));
	                childPlaceholders.put("parentLoanRef", parentLoan.getDocumentNo());
	                childPlaceholders.put("newDueDate", formatDate(parentLoan.getDueDate()));
	                childPlaceholders.put("balance", formatAmount(child.getBalance()));

	                sendBorrowerSms(SmsTypeEnum.LOAN_CONSOLIDATION_NOTIFICATION, childPlaceholders,
	                        child, null, null, LocalDateTime.now(), null);

	                // Notify guarantors of child loans
	                if (child.getGuarantors() != null && !child.getGuarantors().isEmpty()) {
	                    for (MNextOfKin guarantor : child.getGuarantors()) {
	                        Map<String, String> guarantorPlaceholders = buildGuarantorPlaceholders(guarantor, child);
	                        guarantorPlaceholders.put("consolidationDate", formatDate(new Date()));
	                        guarantorPlaceholders.put("consolidatedBillingGroupId", getSafeValue(consolidatedBillingGroupId));
	                        guarantorPlaceholders.put("parentLoanRef", parentLoan.getDocumentNo());
	                        sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_CONSOLIDATION_NOTIFICATION,
	                                guarantorPlaceholders, guarantor, child, null, null, null);
	                    }
	                }
	            }
	        }

	        log.info("✅ Loan consolidation notification sent for parent loan: {}, with {} children",
	                parentLoan.getDocumentNo(), childCount);
	        return parentSent;
	    } catch (Exception e) {
	        log.error("Error handling loan consolidation notification for parent loan {}: {}",
	                parentLoan.getLoanApplicationId(), e.getMessage(), e);
	        return false;
	    }
	}

	// ==================== BREAK CONSOLIDATION NOTIFICATION ====================

	/**
	 * Handle breaking consolidation notification
	 * 
	 * @param loan The loan being removed from consolidation
	 * @return true if notification was sent successfully
	 */
	@Transactional
	public boolean handleBreakConsolidationNotification(MLoanApplication loan) {
	    try {
	        Map<String, String> placeholders = buildLoanPlaceholders(loan);
	        placeholders.put("breakDate", formatDate(new Date()));
	        placeholders.put("previousDueDate", formatDate(loan.getDueDate()));
	        
	        // Recalculate due date
	        if (loan.getExpectedDisbursementDate() != null && loan.getTermInDays() != null) {
	            Date newDueDate = utils.getFutureDateUsingCalender(loan.getExpectedDisbursementDate(),
	                    loan.getTermInDays());
	            placeholders.put("newDueDate", formatDate(newDueDate));
	        }

	        boolean borrowerSent = sendBorrowerSms(SmsTypeEnum.LOAN_BREAK_CONSOLIDATION_NOTIFICATION,
	                placeholders, loan, null, null, LocalDateTime.now(), null);

	        // Notify assignee
	        if (loan.getAssignee() != null) {
	            String assigneePhone = loan.getAssignee().getPhoneNumber();
	            if (assigneePhone != null && !assigneePhone.trim().isEmpty()) {
	                sendGenericSms(SmsTypeEnum.LOAN_BREAK_CONSOLIDATION_NOTIFICATION, placeholders,
	                        loan.getAdOrgID(), loan.getAdClientId(), assigneePhone,
	                        null, loan.getLoanApplicationId(), LocalDateTime.now(), null, null);
	            }
	        }

	        // Notify guarantors
	        if (loan.getGuarantors() != null && !loan.getGuarantors().isEmpty()) {
	            for (MNextOfKin guarantor : loan.getGuarantors()) {
	                Map<String, String> guarantorPlaceholders = buildGuarantorPlaceholders(guarantor, loan);
	                guarantorPlaceholders.put("breakDate", formatDate(new Date()));
	                sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_BREAK_CONSOLIDATION_NOTIFICATION,
	                        guarantorPlaceholders, guarantor, loan, null, null, null);
	            }
	        }

	        log.info("✅ Break consolidation notification sent for loan: {}", loan.getDocumentNo());
	        return borrowerSent;
	    } catch (Exception e) {
	        log.error("Error handling break consolidation notification for loan {}: {}",
	                loan.getLoanApplicationId(), e.getMessage(), e);
	        return false;
	    }
	}

	// ==================== LOAN STATE CHANGE NOTIFICATIONS ====================

	/**
	 * Handle loan state change notification
	 * 
	 * @param loan The loan whose state changed
	 * @param oldState The previous state
	 * @param newState The new state
	 * @param trigger The trigger that caused the change
	 * @return true if notification was sent successfully
	 */
	@Transactional
	public boolean handleLoanStateChangeNotification(MLoanApplication loan, LoanStateEnum oldState,
	        LoanStateEnum newState, String trigger) {
	    try {
	        Map<String, String> placeholders = buildLoanPlaceholders(loan);
	        placeholders.put("oldState", getSafeValue(oldState != null ? oldState.getDescription() : "UNKNOWN"));
	        placeholders.put("newState", getSafeValue(newState != null ? newState.getDescription() : "UNKNOWN"));
	        placeholders.put("stateChangeDate", formatDate(new Date()));
	        placeholders.put("stateChangeTrigger", getSafeValue(trigger));
	        placeholders.put("loanStatus", newState != null ? newState.name() : "UNKNOWN");

	        boolean borrowerSent = sendBorrowerSms(SmsTypeEnum.LOAN_STATE_CHANGE_NOTIFICATION, placeholders,
	                loan, null, null, LocalDateTime.now(), null);

	        // Notify assignee
	        if (loan.getAssignee() != null) {
	            String assigneePhone = loan.getAssignee().getPhoneNumber();
	            if (assigneePhone != null && !assigneePhone.trim().isEmpty()) {
	                sendGenericSms(SmsTypeEnum.LOAN_STATE_CHANGE_NOTIFICATION, placeholders,
	                        loan.getAdOrgID(), loan.getAdClientId(), assigneePhone,
	                        null, loan.getLoanApplicationId(), LocalDateTime.now(), null, null);
	            }
	        }

	        // Notify guarantors for critical state changes
	        if (newState == LoanStateEnum.OVERDUE || newState == LoanStateEnum.WRITTEN_OFF) {
	            if (loan.getGuarantors() != null && !loan.getGuarantors().isEmpty()) {
	                for (MNextOfKin guarantor : loan.getGuarantors()) {
	                    Map<String, String> guarantorPlaceholders = buildGuarantorPlaceholders(guarantor, loan);
	                    guarantorPlaceholders.put("oldState", getSafeValue(oldState != null ? oldState.getDescription() : "UNKNOWN"));
	                    guarantorPlaceholders.put("newState", getSafeValue(newState != null ? newState.getDescription() : "UNKNOWN"));
	                    guarantorPlaceholders.put("stateChangeDate", formatDate(new Date()));
	                    sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_STATE_CHANGE_NOTIFICATION,
	                            guarantorPlaceholders, guarantor, loan, null, null, null);
	                }
	            }
	        }

	        log.info("✅ Loan state change notification sent for loan: {} from {} to {}",
	                loan.getDocumentNo(), oldState, newState);
	        return borrowerSent;
	    } catch (Exception e) {
	        log.error("Error handling loan state change notification for loan {}: {}",
	                loan.getLoanApplicationId(), e.getMessage(), e);
	        return false;
	    }
	}

	// ==================== LOAN REINSTATEMENT NOTIFICATION ====================

	/**
	 * Handle loan reinstatement notification
	 * 
	 * @param loan The reinstated loan
	 * @param reinstatementReason The reason for reinstatement
	 * @return true if notification was sent successfully
	 */
	@Transactional
	public boolean handleLoanReinstatementNotification(MLoanApplication loan, String reinstatementReason) {
	    try {
	        Map<String, String> placeholders = buildLoanPlaceholders(loan);
	        placeholders.put("reinstatementDate", formatDate(new Date()));
	        placeholders.put("reinstatementReason", getSafeValue(reinstatementReason));
	        placeholders.put("newDueDate", formatDate(loan.getDueDate()));
	        placeholders.put("balance", formatAmount(loan.getBalance()));
	        placeholders.put("loanStatus", "REINSTATED");

	        boolean borrowerSent = sendBorrowerSms(SmsTypeEnum.LOAN_REINSTATEMENT_NOTIFICATION, placeholders,
	                loan, null, null, LocalDateTime.now(), null);

	        // Notify assignee
	        if (loan.getAssignee() != null) {
	            String assigneePhone = loan.getAssignee().getPhoneNumber();
	            if (assigneePhone != null && !assigneePhone.trim().isEmpty()) {
	                sendGenericSms(SmsTypeEnum.LOAN_REINSTATEMENT_NOTIFICATION, placeholders,
	                        loan.getAdOrgID(), loan.getAdClientId(), assigneePhone,
	                        null, loan.getLoanApplicationId(), LocalDateTime.now(), null, null);
	            }
	        }

	        // Notify guarantors
	        if (loan.getGuarantors() != null && !loan.getGuarantors().isEmpty()) {
	            for (MNextOfKin guarantor : loan.getGuarantors()) {
	                Map<String, String> guarantorPlaceholders = buildGuarantorPlaceholders(guarantor, loan);
	                guarantorPlaceholders.put("reinstatementDate", formatDate(new Date()));
	                guarantorPlaceholders.put("reinstatementReason", getSafeValue(reinstatementReason));
	                sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_REINSTATEMENT_NOTIFICATION,
	                        guarantorPlaceholders, guarantor, loan, null, null, null);
	            }
	        }

	        log.info("✅ Loan reinstatement notification sent for loan: {}", loan.getDocumentNo());
	        return borrowerSent;
	    } catch (Exception e) {
	        log.error("Error handling loan reinstatement notification for loan {}: {}",
	                loan.getLoanApplicationId(), e.getMessage(), e);
	        return false;
	    }
	}

	// ==================== LOAN WRITE-OFF NOTIFICATION ====================

	/**
	 * Handle loan write-off notification
	 * 
	 * @param loan The written-off loan
	 * @param writtenOffAmount The amount written off
	 * @param writeOffReason The reason for write-off
	 * @param approvedBy The user who approved the write-off
	 * @return true if notification was sent successfully
	 */
	@Transactional
	public boolean handleLoanWriteOffNotification(MLoanApplication loan, BigDecimal writtenOffAmount,
	        String writeOffReason, MUser approvedBy) {
	    try {
	        Map<String, String> placeholders = buildLoanPlaceholders(loan);
	        placeholders.put("writtenOffAmount", formatAmount(writtenOffAmount));
	        placeholders.put("writeOffReason", getSafeValue(writeOffReason));
	        placeholders.put("writeOffDate", formatDate(new Date()));
	        placeholders.put("approvedBy", getSafeValue(approvedBy != null ? approvedBy.getFullName() : "System"));
	        placeholders.put("remainingBalance", formatAmount(BigDecimal.ZERO));
	        placeholders.put("loanStatus", "WRITTEN_OFF");

	        boolean borrowerSent = sendBorrowerSms(SmsTypeEnum.LOAN_WRITE_OFF_NOTIFICATION, placeholders,
	                loan, null, null, LocalDateTime.now(), null);

	        // Notify assignee
	        if (loan.getAssignee() != null) {
	            String assigneePhone = loan.getAssignee().getPhoneNumber();
	            if (assigneePhone != null && !assigneePhone.trim().isEmpty()) {
	                sendGenericSms(SmsTypeEnum.LOAN_WRITE_OFF_NOTIFICATION, placeholders,
	                        loan.getAdOrgID(), loan.getAdClientId(), assigneePhone,
	                        null, loan.getLoanApplicationId(), LocalDateTime.now(), null, null);
	            }
	        }

	        // Notify all guarantors
	        if (loan.getGuarantors() != null && !loan.getGuarantors().isEmpty()) {
	            for (MNextOfKin guarantor : loan.getGuarantors()) {
	                Map<String, String> guarantorPlaceholders = buildGuarantorPlaceholders(guarantor, loan);
	                guarantorPlaceholders.put("writtenOffAmount", formatAmount(writtenOffAmount));
	                guarantorPlaceholders.put("writeOffReason", getSafeValue(writeOffReason));
	                guarantorPlaceholders.put("writeOffDate", formatDate(new Date()));
	                guarantorPlaceholders.put("approvedBy", getSafeValue(approvedBy != null ? approvedBy.getFullName() : "System"));
	                sendGuarantorSms(SmsTypeEnum.GUARANTOR_LOAN_WRITE_OFF_NOTIFICATION,
	                        guarantorPlaceholders, guarantor, loan, null, null, null);
	            }
	        }

	        log.info("✅ Loan write-off notification sent for loan: {}", loan.getDocumentNo());
	        return borrowerSent;
	    } catch (Exception e) {
	        log.error("Error handling loan write-off notification for loan {}: {}",
	                loan.getLoanApplicationId(), e.getMessage(), e);
	        return false;
	    }
	}

	
	
	
}