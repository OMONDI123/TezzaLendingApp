package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MNextOfKin;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import co.ke.tezza.loanapp.model.TriggerMessage;
import co.ke.tezza.loanapp.repository.GuarantorLoanRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.schedulers.RemindersScheduler;
import co.ke.tezza.loanapp.util.ResponseEntity;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ForceSendSmsService {

	@Autowired
	private LoanApplicationRepository loanApplicationRepository;
	@Autowired
	private RemindersScheduler remindersScheduler;
	

	public ResponseEntity<TriggerMessage> forceSms(TriggerMessage m) {
		int noOfSmsSent = 0;
		int guarantorsSentSms = 0;

		if (!m.getLoanReferenceNo().isEmpty()) {
			for (String docNo : m.getLoanReferenceNo()) {
				MLoanApplication application = loanApplicationRepository
						.findTop1ByBalanceGreaterThanAndApprovalStageAndDocumentNoAndIsActive(BigDecimal.ZERO,
								ApprovalStage.APPROVED, docNo, true);

				if (application != null) {
					SmsTypeEnum smsType = m.getMessageType();

					// Determine if this SMS type should be sent to borrower
					if (shouldSendToBorrower(smsType)) {
						// Send to borrower
						boolean borrowerSent = remindersScheduler.sendGenericSmsByType(smsType, application, null);
						if (borrowerSent) {
							noOfSmsSent++;
							log.info("✅ Sent {} to borrower for loan {}", smsType, application.getDocumentNo());
						}
					}

					// Determine if this SMS type should be sent to guarantors
					if (shouldSendToGuarantors(smsType)) {
						// Send to guarantors if they exist
						if (application.getGuarantors() != null && !application.getGuarantors().isEmpty()) {
							for (MNextOfKin guarantor : application.getGuarantors()) {
								boolean guarantorSent = remindersScheduler.sendGenericSmsByType(smsType, application,
										guarantor);
								if (guarantorSent) {
									guarantorsSentSms++;
									log.info("✅ Sent {} to guarantor {} for loan {}", smsType, guarantor.getFullName(),
											application.getDocumentNo());
								}
							}
						} else {
							log.warn("⚠️ No guarantors found for loan {} for SMS type {}", application.getDocumentNo(),
									smsType);
						}
					}
				}
			}
		}

		String message = String.format("%d Debtor SMS and %d Guarantor SMS have been sent successfully.", noOfSmsSent,
				guarantorsSentSms);
		return new ResponseEntity<TriggerMessage>(message, 200, m);
	}

	/**
	 * Determine if an SMS type should be sent to the borrower
	 */
	private boolean shouldSendToBorrower(SmsTypeEnum smsType) {
		// All non-guarantor specific SMS types should be sent to borrower
		return !smsType.toString().startsWith("GUARANTOR_") && smsType != SmsTypeEnum.GUARANTOR_PAYMENT_REMINDER
				&& smsType != SmsTypeEnum.GUARANTOR_LOAN_OVERDUE_ALERT
				&& smsType != SmsTypeEnum.GUARANTOR_LOAN_DUE_REMINDER
				&& smsType != SmsTypeEnum.GUARANTOR_LOAN_DEFAULT_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_INSTALLMENT_DUE_REMINDER
				&& smsType != SmsTypeEnum.GUARANTOR_INSTALLMENT_OVERDUE_ALERT
				&& smsType != SmsTypeEnum.GUARANTOR_INSTALLMENT_MISSED_PAYMENT
				&& smsType != SmsTypeEnum.GUARANTOR_PARTIAL_PAYMENT_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_FULL_REPAYMENT_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_INSTALLMENT_PAYMENT_RECEIVED
				&& smsType != SmsTypeEnum.GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_INTEREST_WAIVER_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_PENALTY_CALCULATION_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_PENALTY_WAIVER_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_INSTALLMENT_ADJUSTMENT
				&& smsType != SmsTypeEnum.GUARANTOR_INSTALLMENT_RESCHEDULE
				&& smsType != SmsTypeEnum.GUARANTOR_LOAN_RESTRUCTURING
				&& smsType != SmsTypeEnum.GUARANTOR_LOAN_SETTLEMENT && smsType != SmsTypeEnum.GUARANTOR_LOAN_CLOSURE
				&& smsType != SmsTypeEnum.GUARANTOR_RELEASE_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_STATEMENT_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_LIMIT_UPDATE_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_STATUS_CHANGE_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_REPLACEMENT_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_CALL_NOTIFICATION
				&& smsType != SmsTypeEnum.GUARANTOR_RECOVERY_NOTIFICATION;
	}

	/**
	 * Determine if an SMS type should be sent to guarantors
	 */
	private boolean shouldSendToGuarantors(SmsTypeEnum smsType) {
		// Only guarantor-specific SMS types should be sent to guarantors
		return smsType.toString().startsWith("GUARANTOR_") || smsType == SmsTypeEnum.GUARANTOR_PAYMENT_REMINDER
				|| smsType == SmsTypeEnum.GUARANTOR_LOAN_OVERDUE_ALERT
				|| smsType == SmsTypeEnum.GUARANTOR_LOAN_DUE_REMINDER
				|| smsType == SmsTypeEnum.GUARANTOR_LOAN_DEFAULT_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_INSTALLMENT_DUE_REMINDER
				|| smsType == SmsTypeEnum.GUARANTOR_INSTALLMENT_OVERDUE_ALERT
				|| smsType == SmsTypeEnum.GUARANTOR_INSTALLMENT_MISSED_PAYMENT
				|| smsType == SmsTypeEnum.GUARANTOR_PARTIAL_PAYMENT_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_FULL_REPAYMENT_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_INSTALLMENT_PAYMENT_RECEIVED
				|| smsType == SmsTypeEnum.GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_INTEREST_WAIVER_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_PENALTY_CALCULATION_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_PENALTY_WAIVER_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_INSTALLMENT_ADJUSTMENT
				|| smsType == SmsTypeEnum.GUARANTOR_INSTALLMENT_RESCHEDULE
				|| smsType == SmsTypeEnum.GUARANTOR_LOAN_RESTRUCTURING
				|| smsType == SmsTypeEnum.GUARANTOR_LOAN_SETTLEMENT || smsType == SmsTypeEnum.GUARANTOR_LOAN_CLOSURE
				|| smsType == SmsTypeEnum.GUARANTOR_RELEASE_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_STATEMENT_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_LIMIT_UPDATE_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_STATUS_CHANGE_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_REPLACEMENT_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_CALL_NOTIFICATION
				|| smsType == SmsTypeEnum.GUARANTOR_RECOVERY_NOTIFICATION;
	}

}