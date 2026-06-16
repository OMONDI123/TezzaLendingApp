package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SmsTypeEnum {
	
	// ========== LOAN APPROVAL/REGISTRATION (Debtor & Guarantor) ==========
	LOAN_APPLICATION_OR_DEBT_REGISTRATION("LOAN_APPLICATION_OR_DEBT_REGISTRATION",
			"Loan Application / Debt Registration Notification",
			new String[] { "username", "documentNo", "amountApplied", "amountApproved", "disbursementDate", "status",
					"loanType", "noOfDaysRemaining", "loanApplicationDate", "loanTermInDays", "reason", "balance",
					"nextInstallmentStartDate", "nextInstallmentDueDate", "interestEarned", "penaltyAmountIncur",
					"daysOverDue", "amountOverDue", "gracePeriodDays", "outstandingBalance" }),

	LOAN_APPROVAL_DEBT_APPROVAL("LOAN_APPROVAL_DEBT_APPROVAL", "Loan / Debt Approval Notification",
			new String[] { "username", "documentNo", "amountApplied", "amountApproved", "disbursementDate", "status",
					"loanType", "loanTermInDays", "reason", "balance", "interestEarned", "penaltyAmountIncur",
					"repaymentStartDate", "gracePeriodDays", "outstandingBalance" }),

	LOAN_REJECTION_DEBT_REJECTION("LOAN_REJECTION_DEBT_REJECTION", "Loan / Debt Rejection Notification",
			new String[] { "username", "documentNo", "amountApplied", "reason", "status", "loanType" }),

	GUARANTOR_APPROVAL_REQUEST("GUARANTOR_APPROVAL_REQUEST", "Guarantor Approval Request",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "amountApproved",
					"disbursementDate", "repaymentStartDate", "loanTermInDays", "gracePeriodDays",
					"outstandingBalance" }),

	GUARANTOR_APPROVAL_CONFIRMATION("GUARANTOR_APPROVAL_CONFIRMATION", "Guarantor Approval Confirmation",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "amountApproved", "approvalDate",
					"guaranteeAmount", "guaranteeLimit", "outstandingBalance" }),

	GUARANTOR_APPROVAL_REJECTION("GUARANTOR_APPROVAL_REJECTION", "Guarantor Approval Rejection",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "amountApproved", "rejectionDate",
					"rejectionReason" }),

	GUARANTOR_LOAN_ASSIGNMENT_NOTIFICATION("GUARANTOR_LOAN_ASSIGNMENT_NOTIFICATION",
			"Guarantor Loan Assignment Notification",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "amountApproved",
					"disbursementDate", "repaymentStartDate", "loanTermInDays", "gracePeriodDays",
					"guaranteeAmount", "guaranteeLimit", "approvalDate", "outstandingBalance" }),

	// ========== LOAN DUE REMINDERS FOR GUARANTORS ==========
	GUARANTOR_LOAN_DUE_REMINDER("GUARANTOR_LOAN_DUE_REMINDER", "Guarantor Loan Due Reminder",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "loanDueDate", 
					"daysRemaining", "totalAmountDue", "principalAmount", "interestAmount", "penaltyAmount", 
					"gracePeriodEndDate", "currentGuaranteeUsed", "guaranteeRemaining", "outstandingBalance" }),


	// ========== INSTALLMENT DUE REMINDERS FOR GUARANTORS ==========
	GUARANTOR_INSTALLMENT_DUE_REMINDER("GUARANTOR_INSTALLMENT_DUE_REMINDER", "Guarantor Installment Due Reminder",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "installmentDueDate",
					"installmentAmount", "daysRemaining", "totalDue", "balance", "installmentNumber", "totalInstallments",
					"guaranteeAmount", "guaranteeLimit", "currentGuaranteeUsed", "outstandingBalance" }),


	// ========== LOAN PAYMENTS (Debtor & Guarantor) ==========
	PAYMENT_RECEIPT_CONFIRMATION("PAYMENT_RECEIPT_CONFIRMATION", "Payment Receipt Confirmation",
			new String[] { "username", "documentNo", "amountPaid", "paymentDate", "paymentMethod", "balance",
					"loanType", "paymentReference", "transactionId", "paymentStatus", "paymentType", "totalPaid",
					"remainingBalance" }),

	PARTIAL_REPAYMENT_NOTIFICATION("PARTIAL_REPAYMENT_NOTIFICATION", "Partial Repayment Notification",
			new String[] { "username", "documentNo", "loanType", "amountPaid", "balance", "paymentDate",
					"paymentMethod", "paymentReference", "transactionId", "partialAmount", "remainingBalance",
					"totalPaid", "outstandingBalance" }),

	FULL_REPAYMENT_NOTIFICATION("FULL_REPAYMENT_NOTIFICATION", "Full Repayment Notification",
			new String[] { "username", "documentNo", "loanType", "totalRepaid", "completionDate", "amountPaid",
					"paymentDate", "paymentMethod", "paymentReference", "transactionId", "outstandingBalance" }),

	EARLY_REPAYMENT_CONFIRMATION("EARLY_REPAYMENT_CONFIRMATION", "Early Repayment Confirmation",
			new String[] { "username", "documentNo", "loanType", "amountPaid", "discountAmount", "balance",
					"paymentDate", "paymentMethod", "earlyRepaymentAmount", "remainingBalance",
					"outstandingBalance" }),

	GUARANTOR_PARTIAL_PAYMENT_NOTIFICATION("GUARANTOR_PARTIAL_PAYMENT_NOTIFICATION",
			"Guarantor Partial Payment Notification",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "amountPaid", "balance",
					"remainingBalance", "paymentDate", "totalOutstanding", "outstandingBalance" }),

	GUARANTOR_FULL_REPAYMENT_NOTIFICATION("GUARANTOR_FULL_REPAYMENT_NOTIFICATION",
			"Guarantor Full Repayment Notification",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "totalRepaid", 
					"completionDate", "guaranteeReleased", "guaranteeAmount", "outstandingBalance" }),

	// ========== INSTALLMENT PAYMENTS (Debtor & Guarantor) ==========
	INSTALLMENT_PAYMENT_CONFIRMATION("INSTALLMENT_PAYMENT_CONFIRMATION", "Installment Payment Confirmation",
			new String[] { "username", "documentNo", "loanType", "installmentAmount", "amountPaid", "paymentDate",
					"paymentMethod", "transactionId", "remainingBalance", "installmentNumber", "totalInstallments",
					"nextDueDate", "receiptNumber", "outstandingBalance" }),

	INSTALLMENT_PARTIAL_PAYMENT("INSTALLMENT_PARTIAL_PAYMENT", "Installment Partial Payment Notification",
			new String[] { "username", "documentNo", "loanType", "installmentAmount", "amountPaid", "remainingAmount",
					"paymentDate", "paymentMethod", "transactionId", "installmentNumber", "totalInstallments",
					"nextDueDate", "lateFee", "outstandingBalance" }),

	GUARANTOR_INSTALLMENT_PAYMENT_RECEIVED("GUARANTOR_INSTALLMENT_PAYMENT_RECEIVED",
			"Guarantor Installment Payment Received Notification",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "installmentAmount", "amountPaid",
					"paymentDate", "remainingBalance", "installmentNumber", "totalInstallments", "nextDueDate",
					"guaranteeAmount", "guaranteeUtilization", "outstandingBalance" }),

	GUARANTOR_INSTALLMENT_PARTIAL_PAYMENT("GUARANTOR_INSTALLMENT_PARTIAL_PAYMENT",
			"Guarantor Installment Partial Payment Notification",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "installmentAmount", "amountPaid",
					"remainingAmount", "paymentDate", "installmentNumber", "totalInstallments", "lateFee",
					"nextDueDate", "guaranteeAmount", "guaranteeRiskLevel", "outstandingBalance" }),

	// ========== GUARANTOR STATUS & LIMIT UPDATES ==========
	GUARANTOR_LIMIT_UPDATE_NOTIFICATION("GUARANTOR_LIMIT_UPDATE_NOTIFICATION", "Guarantor Limit Update Notification",
			new String[] { "guarantorName", "oldGuaranteeLimit", "newGuaranteeLimit", "updateDate", 
					"reason", "effectiveDate", "approvedBy", "totalActiveGuarantees", "availableGuarantee" }),

	GUARANTOR_STATUS_CHANGE_NOTIFICATION("GUARANTOR_STATUS_CHANGE_NOTIFICATION", "Guarantor Status Change Notification",
			new String[] { "guarantorName", "oldStatus", "newStatus", "changeDate", "reason", 
					"effectiveDate", "approvedBy", "affectedLoansCount", "outstandingBalance" }),

	GUARANTOR_REPLACEMENT_NOTIFICATION("GUARANTOR_REPLACEMENT_NOTIFICATION", "Guarantor Replacement Notification",
			new String[] { "oldGuarantorName", "newGuarantorName", "borrowerName", "loanType", "documentNo", 
					"replacementDate", "reason", "guaranteeAmount", "approvedBy", "outstandingBalance" }),

	// ========== LOAN INTEREST ACCRUALS (Debtor & Guarantor) ==========
	INTEREST_CALCULATION_NOTIFICATION("INTEREST_CALCULATION_NOTIFICATION", "Interest Calculation Notification",
			new String[] { "username", "documentNo", "loanType", "interestAmount", "totalInterest", "calculationDate",
					"interestRate", "balance", "nextInstallmentDueDate", "outstandingBalance" }),

	GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION("GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION",
			"Guarantor Interest Accrual Notification",
			new String[] { "guarantorName", "borrowerName", "documentNo", "loanType", "interestAmount",
					"totalInterestAccrued", "accrualDate", "dailyInterestRate", "weeklyInterestRate",
					"monthlyInterestRate", "annualInterestRate", "currentBalance", "interestCalculationMethod",
					"interestFrequency", "nextAccrualDate", "interestPeriod", "outstandingBalance" }),

	GUARANTOR_INTEREST_WAIVER_NOTIFICATION("GUARANTOR_INTEREST_WAIVER_NOTIFICATION",
			"Guarantor Interest Waiver Notification",
			new String[] { "guarantorName", "borrowerName", "documentNo", "loanType", "waivedInterestAmount",
					"interestWaiverReason", "waiverDate", "previousInterestAmount", "approvedBy",
					"outstandingBalance" }),

	// ========== LOAN PENALTY ACCRUALS (Debtor & Guarantor) ==========
	PENALTY_APPLIED_NOTIFICATION("PENALTY_APPLIED_NOTIFICATION", "Penalty Applied Notification",
			new String[] { "username", "documentNo", "loanType", "penaltyAmount", "totalOutstanding", "penaltyReason",
					"applicationDate", "balance", "amountDue", "daysOverDue", "outstandingBalance" }),

	PENALTY_WAIVER_NOTIFICATION("PENALTY_WAIVER_NOTIFICATION", "Penalty Waiver Notification",
	        new String[] { "username", "documentNo", "loanType", "penaltyAmount", "updatedBalance", "waiverDate",
	                "penaltyWaiverReason", "approvedBy", "outstandingBalance","waivedPenaltyAmount" }),
	
	// General waiver for borrower
	WAIVER_NOTIFICATION("WAIVER_NOTIFICATION", "General Waiver Notification",
	        new String[]{"username", "documentNo", "loanType", "waivedAmount", "waiverReason",
	                "waiverDate", "approvedBy", "outstandingBalance"}),

	// General write-off for borrower
	WRITE_OFF_NOTIFICATION("WRITE_OFF_NOTIFICATION", "General Write-off Notification",
	        new String[]{"username", "documentNo", "loanType", "writtenOffAmount", "writeOffReason",
	                "writeOffDate", "approvedBy", "outstandingBalance"}),

	// General waiver for guarantor
	GUARANTOR_WAIVER_NOTIFICATION("GUARANTOR_WAIVER_NOTIFICATION", "General Waiver Notification for Guarantor",
	        new String[]{"guarantorName", "borrowerName", "documentNo", "loanType", "waivedAmount",
	                "waiverReason", "waiverDate", "approvedBy", "outstandingBalance"}),

	// General write-off for guarantor
	GUARANTOR_WRITE_OFF_NOTIFICATION("GUARANTOR_WRITE_OFF_NOTIFICATION", "General Write-off Notification for Guarantor",
	        new String[]{"guarantorName", "borrowerName", "documentNo", "loanType", "writtenOffAmount",
	                "writeOffReason", "writeOffDate", "approvedBy", "outstandingBalance"}),

	GUARANTOR_PENALTY_CALCULATION_NOTIFICATION("GUARANTOR_PENALTY_CALCULATION_NOTIFICATION",
			"Guarantor Penalty Calculation Notification",
			new String[] { "guarantorName", "borrowerName", "documentNo", "loanType", "penaltyAmount", "penaltyRate",
					"calculationDate", "penaltyReason", "overdueDays", "overdueAmount", "totalOutstanding",
					"penaltyType", "gracePeriodUsed", "penaltyFrequency", "outstandingBalance" }),

	GUARANTOR_PENALTY_WAIVER_NOTIFICATION("GUARANTOR_PENALTY_WAIVER_NOTIFICATION",
			"Guarantor Penalty Waiver Notification",
			new String[] { "guarantorName", "borrowerName", "documentNo", "loanType", "waivedPenaltyAmount",
					"penaltyWaiverReason", "waiverDate", "previousPenaltyAmount", "approvedBy",
					"outstandingBalance" }),

	// ========== INSTALLMENT INTEREST ACCRUALS ==========
	INSTALLMENT_GENERATION_NOTIFICATION("INSTALLMENT_GENERATION_NOTIFICATION",
			"Installment Schedule Generation Notification",
			new String[] { "username", "documentNo", "loanType", "noOfInstallments", "installmentAmount",
					"nextInstallmentStartDate", "balance", "interestEarned", "nextInstallmentDueDate", "amountDue", "outstandingBalance" }),

	// ========== INSTALLMENT PENALTY ACCRUALS ==========
	INSTALLMENT_ADJUSTMENT_NOTIFICATION("INSTALLMENT_ADJUSTMENT_NOTIFICATION", "Installment Adjustment Notification",
			new String[] { "username", "documentNo", "loanType", "oldInstallmentAmount", "newInstallmentAmount",
					"adjustmentDate", "reason", "installmentNumber", "totalInstallments", "nextDueDate",
					"remainingBalance", "adjustmentType", "outstandingBalance" }),

	GUARANTOR_INSTALLMENT_ADJUSTMENT("GUARANTOR_INSTALLMENT_ADJUSTMENT",
			"Guarantor Installment Adjustment Notification",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "oldInstallmentAmount",
					"newInstallmentAmount", "adjustmentDate", "reason", "installmentNumber", "totalInstallments",
					"nextDueDate", "guaranteeAmount", "adjustmentType", "outstandingBalance" }),

	// ========== LOAN OVERDUE REMINDERS (Debtor & Guarantor) ==========
	LOAN_OR_DEBT_OVERDUE_REMINDER("LOAN_OR_DEBT_OVERDUE_REMINDER", "Loan / Debt Overdue Reminder",
			new String[] { "username", "documentNo", "loanType", "daysOverDue", "amountOverDue", "penaltyAmountIncur",
					"nextInstallmentDueDate", "noOfDaysRemaining", "installmentAmount", "nextInstallmentStartDate", "outstandingBalance" }),

	MISSED_REPAYMENT_ALERT("MISSED_REPAYMENT_ALERT", "Missed Repayment Alert", new String[] { "username", "documentNo",
			"loanType", "amountDue", "dueDate", "daysOverdue", "nextInstallmentDueDate", "balance", "penaltyAmountIncur", "outstandingBalance" }),
	GUARANTOR_MISSED_REPAYMENT_ALERT("GUARANTOR_MISSED_REPAYMENT_ALERT", "Guarantor Missed Repayment Alert",
		    new String[] { "guarantorName", "borrowerName", "documentNo", "loanType", "amountDue", "dueDate", 
		        "daysOverdue", "nextInstallmentDueDate", "balance", "penaltyAmountIncur", "outstandingBalance" }),

	GUARANTOR_LOAN_OVERDUE_ALERT("GUARANTOR_LOAN_OVERDUE_ALERT", "Guarantor Loan Overdue Alert",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "amountOverDue", "daysOverDue",
					"penaltyAmountIncur", "totalOutstanding", "nextInstallmentDueDate",
					"guaranteeAmount", "defaultDate", "outstandingBalance" }),

	GUARANTOR_LOAN_DEFAULT_NOTIFICATION("GUARANTOR_LOAN_DEFAULT_NOTIFICATION", "Guarantor Loan Default Notification",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "amountOverDue",
					"daysOverDue", "totalOutstanding", "defaultDate", "guaranteeAmount",
					"outstandingBalance" }),

	// ========== INSTALLMENT OVERDUE REMINDERS (Debtor & Guarantor) ==========
	INSTALLMENT_OVERDUE_REMINDER("INSTALLMENT_OVERDUE_REMINDER", "Installment Overdue Reminder",
			new String[] { "username", "documentNo", "loanType", "installmentDueDate", "installmentAmount",
					"daysOverdue", "totalDue", "balance", "installmentNumber", "totalInstallments",
					"penaltyAmount", "interestAmount", "principalAmount", "latePaymentFee", "outstandingBalance" }),

	GUARANTOR_INSTALLMENT_OVERDUE_ALERT("GUARANTOR_INSTALLMENT_OVERDUE_ALERT", "Guarantor Installment Overdue Alert",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "installmentDueDate",
					"installmentAmount", "daysOverdue", "totalDue", "balance", "installmentNumber", "totalInstallments",
					"penaltyAmount", "lateFee", "guaranteeAmount", "currentGuaranteeUsed", "guaranteeRemaining",
					"outstandingBalance" }),

	// ========== LOAN MISSED PAYMENTS REMINDERS (Debtor & Guarantor) ==========
	INSTALLMENT_MISSED_PAYMENT("INSTALLMENT_MISSED_PAYMENT", "Installment Missed Payment Alert",
			new String[] { "username", "documentNo", "loanType", "installmentDueDate", "installmentAmount",
					"daysMissed", "totalDue", "penaltyAmount", "installmentNumber", "totalInstallments",
					"lateFee", "gracePeriodEnd", "currentBalance", "outstandingBalance" }),

	GUARANTOR_INSTALLMENT_MISSED_PAYMENT("GUARANTOR_INSTALLMENT_MISSED_PAYMENT",
			"Guarantor Installment Missed Payment Alert",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "installmentDueDate",
					"installmentAmount", "daysMissed", "totalDue", "installmentNumber", "totalInstallments",
					"lateFee", "gracePeriodEnd", "guaranteeAmount", "potentialGuaranteeCall", "guarantorLiability",
					"outstandingBalance" }),

	// ========== INSTALLMENT MISSED PAYMENTS REMINDERS ==========
	INSTALLMENT_PAYMENT_REMINDER("INSTALLMENT_PAYMENT_REMINDER", "Installment Payment Reminder",
			new String[] { "username", "documentNo", "loanType", "installmentDueDate", "installmentAmount",
					"daysRemaining", "totalDue", "balance", "installmentNumber", "totalInstallments", "paymentMethod",
					"paymentDeadline", "minimumPayment", "outstandingBalance" }),

	// ========== LOAN APPROACHING DUE/LOAN DUE REMINDERS (Debtor & Guarantor) ==========
	LOAN_OR_DEBT_DUE_REMINDER("LOAN_OR_DEBT_DUE_REMINDER", "Loan / Debt Due Reminder",
			new String[] { "username", "documentNo", "loanType", "nextInstallmentDueDate", "noOfDaysRemaining",
					"amountDue", "balance", "interestEarned", "penaltyAmountIncur", "installmentAmount",
					"nextInstallmentStartDate", "outstandingBalance" }),

	GRACE_PERIOD_EXPIRY_ALERT("GRACE_PERIOD_EXPIRY_ALERT", "Grace Period Expiry Alert",
			new String[] { "username", "documentNo", "loanType", "gracePeriodDays", "daysRemaining", "amountDue",
					"balance", "interestEarned", "nextInstallmentStartDate", "nextInstallmentDueDate", "outstandingBalance" }),

	GUARANTOR_PAYMENT_REMINDER("GUARANTOR_PAYMENT_REMINDER", "Guarantor Payment Reminder",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "amountDue",
					"nextInstallmentDueDate", "noOfDaysRemaining", "balance", "totalOutstanding", "outstandingBalance" }),

	// ========== INSTALLMENT APPROACHING DUE/DUE REMINDERS (Debtor & Guarantor) ==========
	INSTALLMENT_DUE_REMINDER("INSTALLMENT_DUE_REMINDER", "Installment Due Reminder",
			new String[] { "username", "documentNo", "loanType", "installmentDueDate", "installmentAmount",
					"daysRemaining", "totalDue", "balance", "installmentNumber", "totalInstallments", "penaltyAmount",
					"interestAmount", "principalAmount", "outstandingBalance" }),

	// ========== SCHEDULE UPDATES & RESCHEDULES (Debtor & Guarantor) ==========
	INSTALLMENT_RESCHEDULE_NOTIFICATION("INSTALLMENT_RESCHEDULE_NOTIFICATION", "Installment Reschedule Notification",
			new String[] { "username", "documentNo", "loanType", "oldDueDate", "newDueDate", "installmentAmount",
					"rescheduleDate", "reason", "installmentNumber", "totalInstallments", "rescheduleFee",
					"newPaymentPlan", "outstandingBalance" }),

	REPAYMENT_SCHEDULE_UPDATE("REPAYMENT_SCHEDULE_UPDATE", "Repayment Schedule Update",
			new String[] { "username", "documentNo", "loanType", "newInstallmentAmount", "nextDueDate",
					"remainingInstallments", "reason", "balance", "totalPaid", "remainingBalance", "outstandingBalance" }),

	GUARANTOR_INSTALLMENT_RESCHEDULE("GUARANTOR_INSTALLMENT_RESCHEDULE",
			"Guarantor Installment Reschedule Notification",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "oldDueDate", "newDueDate",
					"installmentAmount", "rescheduleDate", "reason", "installmentNumber", "totalInstallments",
					"guaranteeAmount", "newPaymentPlan", "outstandingBalance" }),

	// ========== GUARANTOR CALL & RECOVERY NOTIFICATIONS ==========
	GUARANTOR_CALL_NOTIFICATION("GUARANTOR_CALL_NOTIFICATION", "Guarantor Call Notification",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "callAmount", 
					"totalOutstanding", "callDate", "reason", "gracePeriodForPayment", "paymentDeadline", 
					"guaranteeAmount", "guaranteeUtilized", "recoveryContact", "outstandingBalance" }),

	GUARANTOR_RECOVERY_NOTIFICATION("GUARANTOR_RECOVERY_NOTIFICATION", "Guarantor Recovery Notification",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "recoveryAmount", 
					"recoveryDate", "recoveryMethod", "remainingBalance", "recoveryAgent", 
					"guaranteeAmount", "guaranteeReleased", "outstandingBalance" }),

	// ========== RESCHEDULE REQUESTS ==========
	REPAYMENT_RESCHEDULE_REQUEST("REPAYMENT_RESCHEDULE_REQUEST", "Repayment Reschedule Request",
			new String[] { "username", "documentNo", "loanType", "requestDate", "status", "balance", "amountDue", "outstandingBalance" }),

	REPAYMENT_RESCHEDULE_APPROVAL("REPAYMENT_RESCHEDULE_APPROVAL", "Repayment Reschedule Approval",
			new String[] { "username", "documentNo", "loanType", "newInstallmentAmount", "remainingInstallments",
					"newTerm", "approvalDate", "newPrincipal", "balance", "nextInstallmentDueDate", "outstandingBalance" }),

	REPAYMENT_RESCHEDULE_REJECTION("REPAYMENT_RESCHEDULE_REJECTION", "Repayment Reschedule Rejection",
			new String[] { "username", "documentNo", "loanType", "rejectionReason", "rejectionDate", "balance",
					"amountDue", "nextInstallmentDueDate", "outstandingBalance" }),

	// ========== LOAN RESTRUCTURING (Debtor & Guarantor) ==========
	LOAN_RESTRUCTURING_NOTIFICATION("LOAN_RESTRUCTURING_NOTIFICATION", "Loan Restructuring Notification",
			new String[] { "username", "documentNo", "loanType", "newPrincipal", "newTerm", "newInstallment",
					"remainingBalance", "effectiveDate", "balance", "nextInstallmentDueDate", "noOfInstallments", "outstandingBalance" }),

	GUARANTOR_LOAN_RESTRUCTURING("GUARANTOR_LOAN_RESTRUCTURING", "Guarantor Loan Restructuring",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "newInstallmentAmount", "newTerm",
					"newPrincipal", "effectiveDate", "remainingBalance", "outstandingBalance" }),

	// ========== TOP-UP & DISBURSEMENT ==========
	TOP_UP_LOAN_DISBURSEMENT("TOP_UP_LOAN_DISBURSEMENT", "Top-up Loan Disbursement",
			new String[] { "username", "documentNo", "loanType", "topUpAmount", "totalOutstanding",
					"repaymentStartDate", "disbursementDate", "balance", "newPrincipal", "newInstallment", "outstandingBalance" }),
	// Borrower interest waiver
	INTEREST_WAIVER_NOTIFICATION("INTEREST_WAIVER_NOTIFICATION", "Interest Waiver Notification",
	        new String[]{"username", "documentNo", "loanType", "waivedInterestAmount",
	                "interestWaiverReason", "waiverDate", "approvedBy", "outstandingBalance"}),

	// Borrower interest write-off
	INTEREST_WRITE_OFF_NOTIFICATION("INTEREST_WRITE_OFF_NOTIFICATION", "Interest Write-off Notification",
	        new String[]{"username", "documentNo", "loanType", "writtenOffInterestAmount",
	                "writeOffReason", "writeOffDate", "approvedBy", "outstandingBalance"}),

	// Borrower penalty write-off
	PENALTY_WRITE_OFF_NOTIFICATION("PENALTY_WRITE_OFF_NOTIFICATION", "Penalty Write-off Notification",
	        new String[]{"username", "documentNo", "loanType", "writtenOffPenaltyAmount",
	                "writeOffReason", "writeOffDate", "approvedBy", "outstandingBalance"}),

	// Guarantor interest write-off
	GUARANTOR_INTEREST_WRITE_OFF_NOTIFICATION("GUARANTOR_INTEREST_WRITE_OFF_NOTIFICATION",
	        "Guarantor Interest Write-off Notification",
	        new String[]{"guarantorName", "borrowerName", "documentNo", "loanType",
	                "writtenOffInterestAmount", "writeOffReason", "writeOffDate", "approvedBy",
	                "outstandingBalance"}),

	// Guarantor penalty write-off
	GUARANTOR_PENALTY_WRITE_OFF_NOTIFICATION("GUARANTOR_PENALTY_WRITE_OFF_NOTIFICATION",
	        "Guarantor Penalty Write-off Notification",
	        new String[]{"guarantorName", "borrowerName", "documentNo", "loanType",
	                "writtenOffPenaltyAmount", "writeOffReason", "writeOffDate", "approvedBy",
	                "outstandingBalance"}),

	// ========== LOAN CLOSURE (Debtor & Guarantor) ==========
	LOAN_CLOSURE_NOTIFICATION("LOAN_CLOSURE_NOTIFICATION", "Loan Closure Notification",
			new String[] { "username", "documentNo", "loanType", "settlementAmount", "closureDate", "totalRepaid",
					"balance", "amountPaid", "paymentDate", "outstandingBalance" }),

	GUARANTOR_LOAN_SETTLEMENT("GUARANTOR_LOAN_SETTLEMENT", "Guarantor Loan Settlement",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "settlementAmount", "closureDate",
					"totalRepaid", "guaranteeReleasedDate", "outstandingBalance" }),

	GUARANTOR_LOAN_CLOSURE("GUARANTOR_LOAN_CLOSURE", "Guarantor Loan Closure",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "closureDate", "totalRepaid",
					"settlementAmount", "guaranteeCompletionDate", "loanStatus", "outstandingBalance" }),

	GUARANTOR_RELEASE_NOTIFICATION("GUARANTOR_RELEASE_NOTIFICATION", "Guarantor Release Notification",
			new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "releaseDate", 
					"guaranteeAmount", "releaseReason", "loanStatus", "totalRepaid", "outstandingBalance" }),

	// ========== STATEMENTS ==========
	STATEMENT_READY_NOTIFICATION("STATEMENT_READY_NOTIFICATION", "Statement Ready Notification",
			new String[] { "username", "documentNo", "loanType", "period", "amountDue", "dueDate", "outstandingBalance",
					"statementDate", "balance", "interestEarned", "penaltyAmountIncur", "totalPaid" }),

	GUARANTOR_STATEMENT_NOTIFICATION("GUARANTOR_STATEMENT_NOTIFICATION", "Guarantor Statement Notification",
			new String[] { "guarantorName", "statementPeriod", "statementDate", "totalGuaranteedAmount", 
					"activeGuarantees", "guaranteeUtilization", "availableGuarantee", "highestRiskLoan", 
					"totalOutstandingExposure", "outstandingBalance" }),

	// ========== AUTO DEBIT ==========
	AUTO_DEBIT_SUCCESS("AUTO_DEBIT_SUCCESS", "Auto Debit Success Notification",
			new String[] { "username", "documentNo", "loanType", "amountPaid", "balance",
					"paymentDate", "paymentMethod", "paymentReference", "transactionId", "remainingBalance", "totalPaid", "outstandingBalance" }),

	AUTO_DEBIT_FAILURE("AUTO_DEBIT_FAILURE", "Auto Debit Failure Notification",
			new String[] { "username", "documentNo", "loanType", "amountDue", "failureReason", "retryDate",
					"paymentMethod", "balance", "nextInstallmentDueDate", "daysOverDue", "outstandingBalance" }),

	// ========== ACCOUNT TYPES ==========
	ACCOUNT_ACTIVATION_NOTIFICATION("ACCOUNT_ACTIVATION_NOTIFICATION", "Account Activation Notification",
			new String[] { "username", "activationDate" }),

	ACCOUNT_SUSPENSION_NOTIFICATION("ACCOUNT_SUSPENSION_NOTIFICATION", "Account Suspension Notification",
			new String[] { "username", "suspensionReason", "suspensionDate" }),

	ACCOUNT_REACTIVATION_NOTIFICATION("ACCOUNT_REACTIVATION_NOTIFICATION", "Account Reactivation Notification",
			new String[] { "username", "reactivationDate" }),

	// ========== MEMBERSHIP ==========
	MEMBER_REGISTRATION_SUCCESS("MEMBER_REGISTRATION_SUCCESS", "Member Registration Success Notification",
			new String[] { "username", "membershipAccountNo", "registrationDate" }),

	MEMBERSHIP_RENEWAL_REMINDER("MEMBERSHIP_RENEWAL_REMINDER", "Membership Renewal Reminder",
			new String[] { "username", "membershipAccountNo", "expiryDate", "daysRemaining" }),

	// ========== SAVINGS ==========
	SAVINGS_DEPOSIT_NOTIFICATION("SAVINGS_DEPOSIT_NOTIFICATION", "Savings Deposit Notification",
			new String[] { "username", "depositAmount", "depositDate", "totalBalance" }),

	SAVINGS_WITHDRAWAL_NOTIFICATION("SAVINGS_WITHDRAWAL_NOTIFICATION", "Savings Withdrawal Notification",
			new String[] { "username", "withdrawalAmount", "withdrawalDate", "remainingBalance" }),

	// ========== MANUAL SMS ==========
	MANUAL_SMS_FROM_MESSAGE_CENTER("MANUAL_SMS_FROM_MESSAGE_CENTER", "Manual SMS Sent from Messaging Center", new String[] {}),
	// ========== MEMBERSHIP (additional) ==========
	MEMBER_WELCOME_MESSAGE("MEMBER_WELCOME_MESSAGE", "Welcome Message for New Members",
	        new String[] { "username", "membershipAccountNo", "membershipType", "joinDate", "welcomeMessage", "nextSteps" }),

	MEMBERSHIP_RENEWAL_SUCCESS("MEMBERSHIP_RENEWAL_SUCCESS", "Membership Renewal Success Notification",
	        new String[] { "username", "membershipAccountNo", "newExpiryDate", "renewalDate", "membershipType", "amountPaid" }),

	MEMBERSHIP_RENEWAL_FAILED("MEMBERSHIP_RENEWAL_FAILED", "Membership Renewal Failed Notification",
	        new String[] { "username", "membershipAccountNo", "failureReason", "retryDate", "contactSupport" }),

	MEMBERSHIP_EXPIRY_WARNING("MEMBERSHIP_EXPIRY_WARNING", "Membership Expiry Warning",
	        new String[] { "username", "membershipAccountNo", "expiryDate", "daysRemaining", "renewalLink" }),

	MEMBERSHIP_EXPIRED("MEMBERSHIP_EXPIRED", "Membership Expired Notification",
	        new String[] { "username", "membershipAccountNo", "expiryDate", "gracePeriodEnds", "reactivationLink" }),

	MEMBERSHIP_GRACE_PERIOD_REMINDER("MEMBERSHIP_GRACE_PERIOD_REMINDER", "Membership Grace Period Reminder",
	        new String[] { "username", "membershipAccountNo", "graceEndDate", "daysRemaining", "reactivationFee" }),

	MEMBERSHIP_UPGRADE_CONFIRMATION("MEMBERSHIP_UPGRADE_CONFIRMATION", "Membership Upgrade Confirmation",
	        new String[] { "username", "membershipAccountNo", "oldTier", "newTier", "upgradeDate", "priceDifference", "newBenefits" }),

	MEMBERSHIP_DOWNGRADE_CONFIRMATION("MEMBERSHIP_DOWNGRADE_CONFIRMATION", "Membership Downgrade Confirmation",
	        new String[] { "username", "membershipAccountNo", "oldTier", "newTier", "effectiveDate", "refundAmount" }),

	MEMBERSHIP_PAYMENT_RECEIVED("MEMBERSHIP_PAYMENT_RECEIVED", "Membership Payment Received",
	        new String[] { "username", "membershipAccountNo", "amountPaid", "paymentDate", "paymentMethod", "transactionId", "validUntil" }),

	MEMBERSHIP_PAYMENT_DUE("MEMBERSHIP_PAYMENT_DUE", "Membership Payment Due Reminder",
	        new String[] { "username", "membershipAccountNo", "amountDue", "dueDate", "daysRemaining", "paymentLink" }),

	MEMBERSHIP_PAYMENT_OVERDUE("MEMBERSHIP_PAYMENT_OVERDUE", "Membership Payment Overdue Alert",
	        new String[] { "username", "membershipAccountNo", "amountDue", "dueDate", "daysOverdue", "lateFee", "suspensionDate" }),

	MEMBERSHIP_AUTO_DEBIT_SUCCESS("MEMBERSHIP_AUTO_DEBIT_SUCCESS", "Membership Auto-Debit Success",
	        new String[] { "username", "membershipAccountNo", "amountPaid", "paymentDate", "nextBillingDate" }),

	MEMBERSHIP_AUTO_DEBIT_FAILED("MEMBERSHIP_AUTO_DEBIT_FAILED", "Membership Auto-Debit Failed",
	        new String[] { "username", "membershipAccountNo", "amountDue", "failureReason", "retryDate", "updatePaymentMethod" }),

	MEMBERSHIP_ACTIVATION("MEMBERSHIP_ACTIVATION", "Membership Activation Notification",
	        new String[] { "username", "membershipAccountNo", "activationDate", "membershipType", "validUntil" }),

	MEMBERSHIP_SUSPENSION("MEMBERSHIP_SUSPENSION", "Membership Suspension Notification",
	        new String[] { "username", "membershipAccountNo", "suspensionReason", "suspensionDate", "reactivationProcess" }),

	MEMBERSHIP_REACTIVATION("MEMBERSHIP_REACTIVATION", "Membership Reactivation Notification",
	        new String[] { "username", "membershipAccountNo", "reactivationDate", "newExpiryDate" }),

	MEMBERSHIP_CANCELLATION("MEMBERSHIP_CANCELLATION", "Membership Cancellation Confirmation",
	        new String[] { "username", "membershipAccountNo", "cancellationDate", "cancellationReason", "refundAmount" }),

	MEMBERSHIP_BENEFITS_REMINDER("MEMBERSHIP_BENEFITS_REMINDER", "Membership Benefits Reminder",
	        new String[] { "username", "membershipAccountNo", "membershipTier", "availableBenefits", "benefitExpiryDate" }),
	MEMBERSHIP_REJECTION("MEMBERSHIP_REJECTION", "Membership Application Rejected",
		    new String[] { "username", "membershipAccountNo", "reason" }),

	MEMBERSHIP_SPECIAL_OFFER("MEMBERSHIP_SPECIAL_OFFER", "Membership Special Offer Notification",
	        new String[] { "username", "membershipAccountNo", "offerTitle", "offerDetails", "offerExpiry", "redeemLink" }),

	MEMBERSHIP_ANNIVERSARY("MEMBERSHIP_ANNIVERSARY", "Membership Anniversary Greeting",
	        new String[] { "username", "membershipAccountNo", "yearsAsMember", "joinDate", "specialGift" }),

	MEMBERSHIP_BIRTHDAY_GREETING("MEMBERSHIP_BIRTHDAY_GREETING", "Membership Birthday Greeting",
	        new String[] { "username", "membershipAccountNo", "birthday", "specialOffer", "validityPeriod" }),

	MEMBERSHIP_POINTS_EARNED("MEMBERSHIP_POINTS_EARNED", "Membership Points Earned Notification",
	        new String[] { "username", "membershipAccountNo", "pointsEarned", "totalPoints", "transactionDetails", "pointsExpiry" }),

	MEMBERSHIP_POINTS_EXPIRY("MEMBERSHIP_POINTS_EXPIRY", "Membership Points Expiry Warning",
	        new String[] { "username", "membershipAccountNo", "pointsExpiring", "expiryDate", "redeemLink" }),

	MEMBERSHIP_REWARD_REDEEMED("MEMBERSHIP_REWARD_REDEEMED", "Membership Reward Redeemed",
	        new String[] { "username", "membershipAccountNo", "rewardName", "pointsUsed", "redemptionDate", "deliveryDetails" }),

	MEMBERSHIP_PROFILE_UPDATE("MEMBERSHIP_PROFILE_UPDATE", "Membership Profile Update Confirmation",
	        new String[] { "username", "membershipAccountNo", "updatedFields", "updateDate" }),

	MEMBERSHIP_DOCUMENT_VERIFIED("MEMBERSHIP_DOCUMENT_VERIFIED", "Membership Document Verified",
	        new String[] { "username", "membershipAccountNo", "documentType", "verificationDate" }),

	MEMBERSHIP_DOCUMENT_REJECTED("MEMBERSHIP_DOCUMENT_REJECTED", "Membership Document Rejected",
	        new String[] { "username", "membershipAccountNo", "documentType", "rejectionReason", "resubmissionLink" }),

	MEMBERSHIP_INACTIVITY_REMINDER("MEMBERSHIP_INACTIVITY_REMINDER", "Membership Inactivity Reminder",
	        new String[] { "username", "membershipAccountNo", "lastActiveDate", "daysInactive", "engagementOffer" }),

	MEMBERSHIP_SURVEY_REQUEST("MEMBERSHIP_SURVEY_REQUEST", "Membership Satisfaction Survey",
	        new String[] { "username", "membershipAccountNo", "surveyLink", "incentive" }),

	MEMBERSHIP_REFERRAL_SUCCESS("MEMBERSHIP_REFERRAL_SUCCESS", "Membership Referral Success",
	        new String[] { "username", "membershipAccountNo", "referredName", "rewardEarned", "totalReferrals" }),

	MEMBERSHIP_REFERRAL_REMINDER("MEMBERSHIP_REFERRAL_REMINDER", "Membership Referral Program Reminder",
	        new String[] { "username", "membershipAccountNo", "referralCode", "referralLink", "rewardAmount" }),

	MEMBERSHIP_2FA_ENABLED("MEMBERSHIP_2FA_ENABLED", "Two-Factor Authentication Enabled",
	        new String[] { "username", "membershipAccountNo", "enableDate" }),

	MEMBERSHIP_PASSWORD_CHANGED("MEMBERSHIP_PASSWORD_CHANGED", "Membership Password Changed",
	        new String[] { "username", "membershipAccountNo", "changeDate", "ipAddress" }),

	MEMBERSHIP_LOGIN_ALERT("MEMBERSHIP_LOGIN_ALERT", "Suspicious Login Alert",
	        new String[] { "username", "membershipAccountNo", "loginTime", "deviceInfo", "location", "actionLink" }),
	BILL_SUBMISSION_NOTIFICATION("BILL_SUBMISSION_NOTIFICATION", "Bill/Invoice Submission Notification",
	        new String[] { "username", "billNo", "submissionDate", "billType", "dueDate", "billingPeriodStart","billingPeriodEnd","amount","attachMentDownloadUrl" }),
	PROFORMA_INVOICE_SUBMISSION_NOTIFICATION("PROFORMA_INVOICE_SUBMISSION_NOTIFICATION", "Proforma Invoice Submission Notification",
		    new String[] { "username", "proformaInvoiceNo", "submissionDate", "validUntil", "amount","billType",
		                   "itemsCount", "totalAmount", "currency","attachMentDownloadUrl" }),
	// ========== INVOICE NOTIFICATIONS ==========
	INVOICE_GENERATED_NOTIFICATION("INVOICE_GENERATED_NOTIFICATION", "Invoice Generated Notification",
	        new String[] { "username", "invoiceNo", "invoiceDate", "amount", "dueDate", "billType", "currency", "attachMentDownloadUrl" }),

	INVOICE_DUE_REMINDER("INVOICE_DUE_REMINDER", "Invoice Due Reminder",
	        new String[] { "username", "invoiceNo", "dueDate", "amount", "daysRemaining", "balance", "billType", "outstandingBalance" }),

	INVOICE_OVERDUE_REMINDER("INVOICE_OVERDUE_REMINDER", "Invoice Overdue Reminder",
	        new String[] { "username", "invoiceNo", "dueDate", "daysOverdue", "amount", "penaltyAmount", "balance", "billType", "outstandingBalance" }),

	INVOICE_PARTIAL_PAYMENT("INVOICE_PARTIAL_PAYMENT", "Invoice Partial Payment Notification",
	        new String[] { "username", "invoiceNo", "amountPaid", "remainingBalance", "paymentDate", "paymentMethod", "transactionId", "billType", "outstandingBalance" }),

	INVOICE_FULL_PAYMENT("INVOICE_FULL_PAYMENT", "Invoice Full Payment Notification",
	        new String[] { "username", "invoiceNo", "amountPaid", "paymentDate", "paymentMethod", "transactionId", "billType", "outstandingBalance" }),
	// ========== ADDITIONAL INVOICE NOTIFICATIONS ==========
	INVOICE_PAYMENT_REMINDER("INVOICE_PAYMENT_REMINDER", "Invoice Payment Reminder",
	        new String[] { "username", "invoiceNo", "dueDate", "amount", "daysRemaining", "billType", "outstandingBalance" }),

	INVOICE_GRACE_PERIOD_EXPIRY_ALERT("INVOICE_GRACE_PERIOD_EXPIRY_ALERT", "Invoice Grace Period Expiry Alert",
	        new String[] { "username", "invoiceNo", "graceEndDate", "daysRemaining", "amount", "penaltyRate", "billType", "outstandingBalance" }),

	INVOICE_ADJUSTMENT_NOTIFICATION("INVOICE_ADJUSTMENT_NOTIFICATION", "Invoice Adjustment Notification",
	        new String[] { "username", "invoiceNo", "oldAmount", "newAmount", "adjustmentDate", "reason", "billType", "outstandingBalance" }),

	INVOICE_CREDIT_NOTE_NOTIFICATION("INVOICE_CREDIT_NOTE_NOTIFICATION", "Credit Note Issued Notification",
	        new String[] { "username", "invoiceNo", "creditNoteNo", "creditAmount", "creditDate", "reason", "billType", "remainingBalance" }),

	INVOICE_CLOSURE_NOTIFICATION("INVOICE_CLOSURE_NOTIFICATION", "Invoice Closure Notification",
	        new String[] { "username", "invoiceNo", "closureDate", "totalPaid", "settlementAmount", "billType", "outstandingBalance" }),

	INVOICE_WRITE_OFF_NOTIFICATION("INVOICE_WRITE_OFF_NOTIFICATION", "Invoice Write-Off Notification",
	        new String[] { "username", "invoiceNo", "writeOffAmount", "writeOffReason", "writeOffDate", "approvedBy", "billType", "remainingBalance" }),
	// ========== ANNOUNCEMENT ==========
	// ========== WALLET DEPOSITS & WITHDRAWALS ==========
	WALLET_DEPOSIT_NOTIFICATION("WALLET_DEPOSIT_NOTIFICATION", "Wallet Deposit Notification",
	        new String[] { "username", "amount", "depositDate", "paymentMethod", "transactionId", 
	                       "walletBalance", "reference", "phoneNumber", "status", "membershipAccountNo" }),

	WALLET_WITHDRAWAL_NOTIFICATION("WALLET_WITHDRAWAL_NOTIFICATION", "Wallet Withdrawal Notification",
	        new String[] { "username", "amount", "withdrawalDate", "withdrawalMethod", "transactionId", 
	                       "walletBalance", "reference", "phoneNumber", "status", "membershipAccountNo", 
	                       "reason", "destinationAccount" }),

	WALLET_DEPOSIT_SUCCESS("WALLET_DEPOSIT_SUCCESS", "Wallet Deposit Success Notification",
	        new String[] { "username", "amount", "depositDate", "paymentMethod", "transactionId", 
	                       "newBalance", "previousBalance", "reference", "phoneNumber", "membershipAccountNo" }),

	WALLET_WITHDRAWAL_SUCCESS("WALLET_WITHDRAWAL_SUCCESS", "Wallet Withdrawal Success Notification",
	        new String[] { "username", "amount", "withdrawalDate", "withdrawalMethod", "transactionId", 
	                       "newBalance", "previousBalance", "reference", "phoneNumber", "membershipAccountNo", 
	                       "destinationAccount" }),

	WALLET_DEPOSIT_FAILED("WALLET_DEPOSIT_FAILED", "Wallet Deposit Failed Notification",
	        new String[] { "username", "amount", "attemptDate", "paymentMethod", "failureReason", 
	                       "reference", "phoneNumber", "retryAction", "supportContact" }),

	WALLET_WITHDRAWAL_FAILED("WALLET_WITHDRAWAL_FAILED", "Wallet Withdrawal Failed Notification",
	        new String[] { "username", "amount", "attemptDate", "withdrawalMethod", "failureReason", 
	                       "reference", "phoneNumber", "retryAction", "supportContact" }),

	WALLET_LOW_BALANCE_ALERT("WALLET_LOW_BALANCE_ALERT", "Low Wallet Balance Alert",
	        new String[] { "username", "currentBalance", "minimumBalance", "shortfallAmount", 
	                       "alertDate", "topUpLink", "membershipAccountNo", "phoneNumber" }),

	WALLET_BALANCE_STATEMENT("WALLET_BALANCE_STATEMENT", "Wallet Balance Statement",
	        new String[] { "username", "currentBalance", "statementDate", "totalDeposits", 
	                       "totalWithdrawals", "availableBalance", "pendingTransactions", 
	                       "membershipAccountNo", "phoneNumber" }),

	WALLET_AUTO_DEBIT_SETUP("WALLET_AUTO_DEBIT_SETUP", "Wallet Auto-Debit Setup Confirmation",
	        new String[] { "username", "autoDebitAmount", "billingDate", "paymentMethod", 
	                       "walletBalance", "effectiveDate", "membershipAccountNo", "phoneNumber" }),

	WALLET_AUTO_DEBIT_SUCCESS("WALLET_AUTO_DEBIT_SUCCESS", "Wallet Auto-Debit Success Notification",
	        new String[] { "username", "amount", "debitDate", "purpose", "transactionId", 
	                       "walletBalance", "reference", "membershipAccountNo", "phoneNumber" }),

	WALLET_AUTO_DEBIT_FAILED("WALLET_AUTO_DEBIT_FAILED", "Wallet Auto-Debit Failed Notification",
	        new String[] { "username", "amount", "attemptDate", "purpose", "failureReason", 
	                       "currentBalance", "requiredBalance", "retryDate", "membershipAccountNo", 
	                       "phoneNumber", "actionRequired" }),

	WALLET_TRANSACTION_REVERSAL("WALLET_TRANSACTION_REVERSAL", "Wallet Transaction Reversal Notification",
	        new String[] { "username", "originalTransactionId", "reversalAmount", "reversalDate", 
	                       "reversalReason", "currentBalance", "previousBalance", "membershipAccountNo", 
	                       "phoneNumber", "reference" }),

	WALLET_REFUND_NOTIFICATION("WALLET_REFUND_NOTIFICATION", "Wallet Refund Notification",
	        new String[] { "username", "refundAmount", "refundDate", "originalTransactionId", 
	                       "refundReason", "walletBalance", "reference", "membershipAccountNo", 
	                       "phoneNumber", "expectedDate" }),

	WALLET_DEPOSIT_VERIFICATION("WALLET_DEPOSIT_VERIFICATION", "Wallet Deposit Verification Required",
	        new String[] { "username", "amount", "depositDate", "paymentMethod", "transactionId", 
	                       "verificationCode", "verificationLink", "expiryTime", "membershipAccountNo", 
	                       "phoneNumber" }),

	WALLET_WITHDRAWAL_VERIFICATION("WALLET_WITHDRAWAL_VERIFICATION", "Wallet Withdrawal Verification Required",
	        new String[] { "username", "amount", "requestDate", "withdrawalMethod", "verificationCode", 
	                       "verificationLink", "expiryTime", "destinationAccount", "membershipAccountNo", 
	                       "phoneNumber" }),

	WALLET_DAILY_SUMMARY("WALLET_DAILY_SUMMARY", "Wallet Daily Summary",
	        new String[] { "username", "summaryDate", "totalCredits", "totalDebits", "openingBalance", 
	                       "closingBalance", "transactionCount", "membershipAccountNo", "phoneNumber" }),

	WALLET_WEEKLY_SUMMARY("WALLET_WEEKLY_SUMMARY", "Wallet Weekly Summary",
	        new String[] { "username", "weekStartDate", "weekEndDate", "totalDeposits", "totalWithdrawals", 
	                       "averageBalance", "closingBalance", "transactionCount", "membershipAccountNo", 
	                       "phoneNumber" }),

	WALLET_MONTHLY_SUMMARY("WALLET_MONTHLY_SUMMARY", "Wallet Monthly Summary",
	        new String[] { "username", "month", "totalDeposits", "totalWithdrawals", "highestBalance", 
	                       "lowestBalance", "averageBalance", "closingBalance", "transactionCount", 
	                       "interestEarned", "membershipAccountNo", "phoneNumber" }),

	WALLET_LIMIT_EXCEEDED("WALLET_LIMIT_EXCEEDED", "Wallet Transaction Limit Exceeded",
	        new String[] { "username", "attemptedAmount", "dailyLimit", "transactionType", "currentDailyTotal", 
	                       "remainingLimit", "resetTime", "membershipAccountNo", "phoneNumber" }),

	WALLET_FRAUD_ALERT("WALLET_FRAUD_ALERT", "Suspicious Wallet Activity Alert",
	        new String[] { "username", "alertType", "transactionAmount", "transactionTime", "location", 
	                       "deviceInfo", "actionRequired", "supportContact", "membershipAccountNo", 
	                       "phoneNumber" }),

	WALLET_PIN_CHANGE_CONFIRMATION("WALLET_PIN_CHANGE_CONFIRMATION", "Wallet PIN Change Confirmation",
	        new String[] { "username", "changeDate", "deviceInfo", "ipAddress", "actionRequired", 
	                       "membershipAccountNo", "phoneNumber", "supportContact" }),

	WALLET_KYC_REMINDER("WALLET_KYC_REMINDER", "Wallet KYC Update Reminder",
	        new String[] { "username", "dueDate", "daysRemaining", "requiredDocuments", "verificationLink", 
	                       "consequencesOfDelay", "membershipAccountNo", "phoneNumber" }),

	WALLET_ACCOUNT_UPGRADE("WALLET_ACCOUNT_UPGRADE", "Wallet Account Tier Upgrade",
	        new String[] { "username", "oldTier", "newTier", "upgradeDate", "newBenefits", "newLimits", 
	                       "membershipAccountNo", "phoneNumber" }),

	WALLET_FEE_CHARGE_NOTIFICATION("WALLET_FEE_CHARGE_NOTIFICATION", "Wallet Fee Charge Notification",
	        new String[] { "username", "feeType", "amount", "chargeDate", "description", "walletBalance", 
	                       "transactionId", "membershipAccountNo", "phoneNumber" }),

	WALLET_INTEREST_CREDITED("WALLET_INTEREST_CREDITED", "Wallet Interest Credited Notification",
	        new String[] { "username", "interestAmount", "period", "interestRate", "creditDate", 
	                       "newBalance", "previousBalance", "membershipAccountNo", "phoneNumber" }),

	WALLET_PROMOTIONAL_CREDIT("WALLET_PROMOTIONAL_CREDIT", "Wallet Promotional Credit Notification",
	        new String[] { "username", "creditAmount", "promotionName", "creditDate", "expiryDate", 
	                       "terms", "walletBalance", "membershipAccountNo", "phoneNumber" }),

	WALLET_SCHEDULED_TRANSACTION("WALLET_SCHEDULED_TRANSACTION", "Scheduled Wallet Transaction Notification",
	        new String[] { "username", "transactionType", "amount", "scheduledDate", "frequency", 
	                       "reference", "status", "walletBalance", "membershipAccountNo", "phoneNumber" }),

	WALLET_SCHEDULED_TRANSACTION_SUCCESS("WALLET_SCHEDULED_TRANSACTION_SUCCESS", "Scheduled Transaction Success",
	        new String[] { "username", "transactionType", "amount", "executionDate", "transactionId", 
	                       "walletBalance", "reference", "membershipAccountNo", "phoneNumber" }),

	WALLET_SCHEDULED_TRANSACTION_FAILED("WALLET_SCHEDULED_TRANSACTION_FAILED", "Scheduled Transaction Failed",
	        new String[] { "username", "transactionType", "amount", "scheduledDate", "failureReason", 
	                       "currentBalance", "requiredBalance", "nextAttemptDate", "membershipAccountNo", 
	                       "phoneNumber", "actionRequired" }),

	WALLET_BONUS_CREDITED("WALLET_BONUS_CREDITED", "Wallet Bonus Credited Notification",
	        new String[] { "username", "bonusAmount", "bonusType", "creditDate", "reason", 
	                       "validUntil", "walletBalance", "membershipAccountNo", "phoneNumber" }),

	WALLET_CASHBACK_CREDITED("WALLET_CASHBACK_CREDITED", "Wallet Cashback Credited Notification",
	        new String[] { "username", "cashbackAmount", "originalTransactionId", "cashbackRate", 
	                       "creditDate", "walletBalance", "membershipAccountNo", "phoneNumber" }),

	WALLET_PENDING_TRANSACTION_REMINDER("WALLET_PENDING_TRANSACTION_REMINDER", "Pending Wallet Transaction Reminder",
	        new String[] { "username", "transactionType", "amount", "initiatedDate", "pendingReason", 
	                       "completionLink", "expiryDate", "membershipAccountNo", "phoneNumber" }),

	WALLET_INACTIVITY_ALERT("WALLET_INACTIVITY_ALERT", "Wallet Inactivity Alert",
	        new String[] { "username", "lastActivityDate", "daysInactive", "inactivityFee", 
	                       "reactivationOffer", "dormancyDate", "membershipAccountNo", "phoneNumber" }),

	WALLET_STATEMENT_READY("WALLET_STATEMENT_READY", "Wallet Statement Ready",
	        new String[] { "username", "period", "statementDate", "downloadLink", "openingBalance", 
	                       "closingBalance", "membershipAccountNo", "phoneNumber" }),

	WALLET_SECURITY_ALERT("WALLET_SECURITY_ALERT", "Wallet Security Alert",
	        new String[] { "username", "alertType", "alertTime", "affectedAmount", "recommendedAction", 
	                       "supportContact", "reference", "membershipAccountNo", "phoneNumber" }),

	WALLET_DEVICE_REGISTERED("WALLET_DEVICE_REGISTERED", "New Device Registered on Wallet",
	        new String[] { "username", "deviceName", "registrationDate", "deviceInfo", "location", 
	                       "actionRequired", "membershipAccountNo", "phoneNumber" }),

	WALLET_DEVICE_REMOVED("WALLET_DEVICE_REMOVED", "Wallet Device Removal Notification",
	        new String[] { "username", "deviceName", "removalDate", "reason", "membershipAccountNo", 
	                       "phoneNumber", "supportContact" }),

	WALLET_MAINTENANCE_NOTICE("WALLET_MAINTENANCE_NOTICE", "Wallet Maintenance Notice",
	        new String[] { "username", "startTime", "endTime", "impactedServices", "alternativeChannels", 
	                       "membershipAccountNo", "phoneNumber", "supportContact" }),

	WALLET_SYSTEM_UPDATE("WALLET_SYSTEM_UPDATE", "Wallet System Update Notification",
	        new String[] { "username", "updateDate", "newFeatures", "improvements", "actionRequired", 
	                       "releaseNotes", "membershipAccountNo", "phoneNumber" }),
	

	// ========== LOAN CANCELLATION NOTIFICATIONS ==========
	LOAN_CANCELLATION_NOTIFICATION("LOAN_CANCELLATION_NOTIFICATION", "Loan Cancellation Notification",
	        new String[] { "username", "documentNo", "loanType", "cancellationReason", "cancellationDate", 
	                       "amountApplied", "balance", "loanStatus", "outstandingBalance" }),

	GUARANTOR_LOAN_CANCELLATION_NOTIFICATION("GUARANTOR_LOAN_CANCELLATION_NOTIFICATION", 
	        "Guarantor Loan Cancellation Notification",
	        new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "cancellationReason", 
	                       "cancellationDate", "outstandingBalance" }),

	// ========== LOAN CONSOLIDATION NOTIFICATIONS ==========
	LOAN_CONSOLIDATION_NOTIFICATION("LOAN_CONSOLIDATION_NOTIFICATION", "Loan Consolidation Notification",
	        new String[] { "username", "documentNo", "loanType", "consolidationDate", "consolidatedBillingGroupId",
	                       "totalChildBalance", "childLoanCount", "childLoanRefs", "newTotalBalance",
	                       "consolidatedDueDate", "balance", "outstandingBalance" }),

	GUARANTOR_LOAN_CONSOLIDATION_NOTIFICATION("GUARANTOR_LOAN_CONSOLIDATION_NOTIFICATION", 
	        "Guarantor Loan Consolidation Notification",
	        new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "consolidationDate",
	                       "consolidatedBillingGroupId", "parentLoanRef", "outstandingBalance" }),

	// ========== BREAK CONSOLIDATION NOTIFICATIONS ==========
	LOAN_BREAK_CONSOLIDATION_NOTIFICATION("LOAN_BREAK_CONSOLIDATION_NOTIFICATION", 
	        "Loan Break Consolidation Notification",
	        new String[] { "username", "documentNo", "loanType", "breakDate", "previousDueDate", "newDueDate",
	                       "balance", "outstandingBalance" }),

	GUARANTOR_LOAN_BREAK_CONSOLIDATION_NOTIFICATION("GUARANTOR_LOAN_BREAK_CONSOLIDATION_NOTIFICATION", 
	        "Guarantor Loan Break Consolidation Notification",
	        new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "breakDate",
	                       "outstandingBalance" }),

	// ========== LOAN STATE CHANGE NOTIFICATIONS ==========
	LOAN_STATE_CHANGE_NOTIFICATION("LOAN_STATE_CHANGE_NOTIFICATION", "Loan State Change Notification",
	        new String[] { "username", "documentNo", "loanType", "oldState", "newState", "stateChangeDate",
	                       "stateChangeTrigger", "balance", "loanStatus", "outstandingBalance" }),

	GUARANTOR_LOAN_STATE_CHANGE_NOTIFICATION("GUARANTOR_LOAN_STATE_CHANGE_NOTIFICATION", 
	        "Guarantor Loan State Change Notification",
	        new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "oldState", "newState",
	                       "stateChangeDate", "outstandingBalance" }),

	// ========== LOAN REINSTATEMENT NOTIFICATIONS ==========
	LOAN_REINSTATEMENT_NOTIFICATION("LOAN_REINSTATEMENT_NOTIFICATION", "Loan Reinstatement Notification",
	        new String[] { "username", "documentNo", "loanType", "reinstatementDate", "reinstatementReason",
	                       "newDueDate", "balance", "loanStatus", "outstandingBalance" }),

	GUARANTOR_LOAN_REINSTATEMENT_NOTIFICATION("GUARANTOR_LOAN_REINSTATEMENT_NOTIFICATION", 
	        "Guarantor Loan Reinstatement Notification",
	        new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "reinstatementDate",
	                       "reinstatementReason", "outstandingBalance" }),

	// ========== LOAN WRITE-OFF NOTIFICATIONS ==========
	LOAN_WRITE_OFF_NOTIFICATION("LOAN_WRITE_OFF_NOTIFICATION", "Loan Write-Off Notification",
	        new String[] { "username", "documentNo", "loanType", "writtenOffAmount", "writeOffReason",
	                       "writeOffDate", "approvedBy", "remainingBalance", "loanStatus", "outstandingBalance" }),

	GUARANTOR_LOAN_WRITE_OFF_NOTIFICATION("GUARANTOR_LOAN_WRITE_OFF_NOTIFICATION", 
	        "Guarantor Loan Write-Off Notification",
	        new String[] { "guarantorName", "borrowerName", "loanType", "documentNo", "writtenOffAmount",
	                       "writeOffReason", "writeOffDate", "approvedBy", "outstandingBalance" }),

	// ========== ANNOUNCEMENT ==========
	ANNOUNCEMENT_NOTIFICATION("ANNOUNCEMENT_NOTIFICATION", "Announcement Notification",
	        new String[] { "announcementDate" });

	private final String value;
	private final String description;
	private final String[] allowedParameters;

	SmsTypeEnum(String value, String description, String[] allowedParameters) {
		this.value = value;
		this.description = description;
		this.allowedParameters = allowedParameters;
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public String[] getAllowedParameters() {
		return allowedParameters;
	}

	@JsonCreator
	public static SmsTypeEnum forValues(@JsonProperty("value") String value) {
		for (SmsTypeEnum type : SmsTypeEnum.values()) {
			if (type.value.equalsIgnoreCase(value)) {
				return type;
			}
		}
		return null;
	}
}