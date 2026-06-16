package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MInstallmentStatement;
import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanStatement;
import co.ke.tezza.loanapp.enums.InstallmentTransactionType;
import co.ke.tezza.loanapp.enums.LoanTransactionType;
import co.ke.tezza.loanapp.repository.InstallmentRepository;
import co.ke.tezza.loanapp.repository.InstallmentStatementRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.repository.LoanStatementRepository;
import co.ke.tezza.loanapp.response.InstallmentResponse;
import co.ke.tezza.loanapp.response.LoanStatementResponse;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.Utils;

@Service
@Transactional
public class LoanStatementService {

	@Autowired
	private LoanStatementRepository loanStatementRepository;

	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	@Autowired
	private InstallmentRepository installmentRepository;

	@Autowired
	private Utils utils;

	@Autowired
	private ObjectsMapper objectsMapper;

	@Autowired
	private InstallmentStatementRepository installmentStatementRepository;

	// ===============================================
	// === MAIN TRANSACTION RECORDING METHODS ===
	// ===============================================

	
	/** Record Loan Disbursement (Loan given to borrower) */
	public void recordDisbursement(Long loanId, Long installmentId, BigDecimal amount, String ref,
			LocalDateTime expectedDisursementDate) {
		String defaultReason = "Loan disbursement processed";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.INSTALLMENT_GENERATION, amount,
					BigDecimal.ZERO, ref, expectedDisursementDate, null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.LOAN_DISBURSED, amount, BigDecimal.ZERO, ref,
					expectedDisursementDate, null, defaultReason);
		}
	}

	public void recordPrincipleReduction(Long loanId, Long installmentId, BigDecimal amount, String ref,
			LocalDateTime expectedDisursementDate) {
		String defaultReason = "Principle amount reduced due to overstated amount.";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.PRINCIPLE_REDUCTION, BigDecimal.ZERO,
					amount, ref, expectedDisursementDate, null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.LOAN_DISBURSED, amount, BigDecimal.ZERO, ref,
					expectedDisursementDate, null, defaultReason);
		}
	}

	/** Record Interest Accrued */
	public void recordInterest(Long loanId, Long installmentId, BigDecimal interest) {
		String defaultReason = "Interest accrued as per loan terms";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.INTEREST_ACCRUED, interest,
					BigDecimal.ZERO, null, null, null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.INTEREST_ACCRUED, interest, BigDecimal.ZERO, null, null,
					null, defaultReason);
		}
	}

	/** Record Penalty Charged */
	public void recordPenalty(Long loanId, Long installmentId, BigDecimal penalty) {
		String defaultReason = "Penalty charged for overdue payment";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.PENALTY_CHARGED, penalty,
					BigDecimal.ZERO, null, null, null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.PENALTY_CHARGED, penalty, BigDecimal.ZERO, null, null, null,
					defaultReason);
		}
	}

	/** Record Repayment Received */
	public void recordRepayment(Long loanId, Long installmentId, BigDecimal amount, String ref,
			LocalDateTime actualPaymentDate, Long paymentId) {
		String defaultReason = "Loan repayment received from borrower";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.REPAYMENT_RECEIVED, BigDecimal.ZERO,
					amount, ref, actualPaymentDate, paymentId, defaultReason);
		} else {
			createLoanStatement(loanId, LoanTransactionType.REPAYMENT_RECEIVED, BigDecimal.ZERO, amount, ref,
					actualPaymentDate, paymentId, defaultReason);
		}
	}

	/** Record Adjustment (manual correction) */
	public void recordAdjustment(Long loanId, Long installmentId, BigDecimal debit, BigDecimal credit) {
		String defaultReason = "Manual account adjustment";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.ADJUSTMENT, debit, credit, null, null,
					null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.ADJUSTMENT, debit, credit, null, null, null, defaultReason);
		}
	}

	// ===============================================
	// === DEBT REDUCTION TRANSACTIONS ===
	// ===============================================

	/** Record Partial Write-off */
	public void recordPartialWriteOff(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Partial loan amount written off as uncollectible";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.PARTIAL_WRITE_OFF, BigDecimal.ZERO,
					amount, ref, null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.PARTIAL_WRITE_OFF, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Credit Note (billing correction) */
	public void recordCreditNote(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref,
			LocalDateTime actualPaymentDate, Long paymentId) {
		String defaultReason = "Credit note issued for billing correction";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.CREDIT_NOTE, BigDecimal.ZERO, amount,
					ref, null, paymentId, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.CREDIT_NOTE, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Credit Note (billing correction) */
	public void recordWriteOffs(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref,
			LocalDateTime actualPaymentDate, Long paymentId) {
		String defaultReason = "Loan amount written off as uncollectible debt";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.WRITE_OFF, BigDecimal.ZERO, amount,
					ref, null, paymentId, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.WRITE_OFF, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Waiver (voluntary forgiveness) */
	public void recordWaiver(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Loan amount waived as per company policy";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.WAIVER, BigDecimal.ZERO, amount, ref,
					null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.WAIVER, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Partial Waiver */
	public void recordPartialWaiver(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Partial loan amount waived as per company policy";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.PARTIAL_WAIVER, BigDecimal.ZERO,
					amount, ref, null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.PARTIAL_WAIVER, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Specific Interest Waiver */
	public void recordInterestWaiver(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Interest amount waived as per company policy";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.INTEREST_WAIVER, BigDecimal.ZERO,
					amount, ref, null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.INTEREST_WAIVER, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Specific Penalty Waiver */
	public void recordPenaltyWaiver(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Penalty amount waived as per company policy";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.PENALTY_WAIVER, BigDecimal.ZERO,
					amount, ref, null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.PENALTY_WAIVER, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Settlement Payment */
	public void recordSettlement(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Settlement payment received for loan";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.SETTLEMENT, BigDecimal.ZERO, amount,
					ref, null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.SETTLEMENT, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Partial Settlement */
	public void recordPartialSettlement(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Partial settlement payment received";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.PARTIAL_SETTLEMENT, BigDecimal.ZERO,
					amount, ref, null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.PARTIAL_SETTLEMENT, BigDecimal.ZERO, amount, ref, null,
					null, reason != null ? reason : defaultReason);
		}
	}

	// ===============================================
	// === FEE TRANSACTIONS ===
	// ===============================================

	/** Record Processing Fee */
	public void recordProcessingFee(Long loanId, Long installmentId, BigDecimal amount, String ref) {
		String defaultReason = "Loan processing fee charged";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.PROCESSING_FEE, amount,
					BigDecimal.ZERO, ref, null, null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.PROCESSING_FEE, amount, BigDecimal.ZERO, ref, null, null,
					defaultReason);
		}
	}

	/** Record Late Fee */
	public void recordLateFee(Long loanId, Long installmentId, BigDecimal amount, String ref) {
		String defaultReason = "Late payment fee charged";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.LATE_FEE, amount, BigDecimal.ZERO, ref,
					null, null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.LATE_FEE, amount, BigDecimal.ZERO, ref, null, null,
					defaultReason);
		}
	}

	/** Record Admin Fee */
	public void recordAdminFee(Long loanId, Long installmentId, BigDecimal amount, String ref) {
		String defaultReason = "Administration fee charged";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.ADMIN_FEE, amount, BigDecimal.ZERO,
					ref, null, null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.ADMIN_FEE, amount, BigDecimal.ZERO, ref, null, null,
					defaultReason);
		}
	}

	/** Record Early Repayment Fee */
	public void recordEarlyRepaymentFee(Long loanId, Long installmentId, BigDecimal amount, String ref) {
		String defaultReason = "Early repayment fee charged";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.EARLY_REPAYMENT_FEE, amount,
					BigDecimal.ZERO, ref, null, null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.EARLY_REPAYMENT_FEE, amount, BigDecimal.ZERO, ref, null,
					null, defaultReason);
		}
	}

	// ===============================================
	// === REFUNDS AND REVERSALS ===
	// ===============================================

	/** Record Refund */
	public void recordRefund(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Refund issued to borrower";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.REFUND, BigDecimal.ZERO, amount, ref,
					null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.REFUND, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Reversal */
	public void recordReversal(Long loanId, Long installmentId, BigDecimal debit, BigDecimal credit, String reason,
			String ref) {
		String defaultReason = "Transaction reversed due to error";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.REVERSAL, debit, credit, ref, null,
					null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.REVERSAL, debit, credit, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Penalty Reversal */
	public void recordPenaltyReversal(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Penalty charge reversed";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.PENALTY_REVERSAL, BigDecimal.ZERO,
					amount, ref, null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.PENALTY_REVERSAL, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	// ===============================================
	// === SPECIAL TRANSACTIONS ===
	// ===============================================

	/** Record Interest Capitalization */
	public void recordInterestCapitalization(Long loanId, Long installmentId, BigDecimal amount, String ref) {
		String defaultReason = "Interest capitalized to principal balance";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.INTEREST_CAPITALIZATION, amount,
					BigDecimal.ZERO, ref, null, null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.INTEREST_CAPITALIZATION, amount, BigDecimal.ZERO, ref, null,
					null, defaultReason);
		}
	}

	/** Record Forbearance */
	public void recordForbearance(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Forbearance granted on loan payments";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.FORBEARANCE, BigDecimal.ZERO, amount,
					ref, null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.FORBEARANCE, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Debt Rescheduling */
	public void recordDebtRescheduling(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Debt payment schedule rescheduled";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.DEBT_RESCHEDULING, BigDecimal.ZERO,
					amount, ref, null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.DEBT_RESCHEDULING, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record Debt Consolidation */
	public void recordDebtConsolidation(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Debt consolidated with other obligations";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.DEBT_CONSOLIDATION, BigDecimal.ZERO,
					amount, ref, null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.DEBT_CONSOLIDATION, BigDecimal.ZERO, amount, ref, null,
					null, reason != null ? reason : defaultReason);
		}
	}

	// ===============================================
	// === CORRECTION TRANSACTIONS ===
	// ===============================================

	/** Record Correction */
	public void recordCorrection(Long loanId, Long installmentId, BigDecimal debit, BigDecimal credit, String reason,
			String ref) {
		String defaultReason = "Error correction applied to account";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.CORRECTION, debit, credit, ref, null,
					null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.CORRECTION, debit, credit, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	/** Record System Adjustment */
	public void recordSystemAdjustment(Long loanId, Long installmentId, BigDecimal debit, BigDecimal credit,
			String reason, String ref) {
		String defaultReason = "System-initiated adjustment";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.SYSTEM_ADJUSTMENT, debit, credit, ref,
					null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.SYSTEM_ADJUSTMENT, debit, credit, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	// ===============================================
	// === INSURANCE AND TAX TRANSACTIONS ===
	// ===============================================

	/** Record Insurance Premium */
	public void recordInsurancePremium(Long loanId, Long installmentId, BigDecimal amount, String ref) {
		String defaultReason = "Loan insurance premium charged";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.INSURANCE_PREMIUM, amount,
					BigDecimal.ZERO, ref, null, null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.INSURANCE_PREMIUM, amount, BigDecimal.ZERO, ref, null, null,
					defaultReason);
		}
	}

	/** Record Tax Charge */
	public void recordTaxCharge(Long loanId, Long installmentId, BigDecimal amount, String ref) {
		String defaultReason = "Tax charged on loan transaction";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.TAX_CHARGE, amount, BigDecimal.ZERO,
					ref, null, null, defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.TAX_CHARGE, amount, BigDecimal.ZERO, ref, null, null,
					defaultReason);
		}
	}

	/** Record Tax Waiver */
	public void recordTaxWaiver(Long loanId, Long installmentId, BigDecimal amount, String reason, String ref) {
		String defaultReason = "Tax amount waived as per policy";
		if (installmentId != null) {
			createInstallmentStatement(installmentId, InstallmentTransactionType.TAX_WAIVER, BigDecimal.ZERO, amount,
					ref, null, null, reason != null ? reason : defaultReason);
		}
		if (loanId != null) {
			createLoanStatement(loanId, LoanTransactionType.TAX_WAIVER, BigDecimal.ZERO, amount, ref, null, null,
					reason != null ? reason : defaultReason);
		}
	}

	// ===============================================
	// === LOAN STATEMENT CREATION (CORE LOGIC) ===
	// ===============================================

	@Transactional(propagation = Propagation.MANDATORY)
	private void createLoanStatement(Long loanId, LoanTransactionType type, BigDecimal debit, BigDecimal credit,
			String ref, LocalDateTime actualPaymentDate, Long paymentId, String reason) {
		Optional<MLoanApplication> optionalLoan = loanApplicationRepository.findById(loanId);
		if (!optionalLoan.isPresent()) {
			return;
		}

		MLoanApplication loan = optionalLoan.get();

		BigDecimal lastBalance = BigDecimal.ZERO;
		MLoanStatement lastEntry = loanStatementRepository
				.findTopByIsActiveAndAdOrgIDAndLoanOrderByStatementIdDesc(true, loan.getAdOrgID(), loan);
		if (lastEntry != null) {
			lastBalance = lastEntry.getBalance();
		}

		BigDecimal newBalance = lastBalance.add(debit).subtract(credit);
		String borrowerName = utils.getBorrowerName(loan);

		MLoanStatement entry = new MLoanStatement();
		entry.setLoan(loan);
		entry.setTransactionType(type);
		entry.setTransactionRef(ref);
		entry.setTransactionDate(LocalDateTime.now());
		entry.setActualPaymentDate(actualPaymentDate != null ? actualPaymentDate : LocalDateTime.now());
		entry.setPaymentId(paymentId);
		entry.setDebitAmount(debit);
		entry.setActualDate(entry.getActualPaymentDate());
		entry.setCreditAmount(credit);
		entry.setBalance(newBalance);
		entry.setAdOrgID(loan.getAdOrgID());
		entry.setAdClientId(loan.getAdClientId());
		entry.setIsReversed(false);

		// Get default reason if none provided
		String finalReason = reason != null ? reason : getDefaultReasonForTransaction(type);

		// Description and Notes
		entry.setDescription(getDescription(type, debit, credit, borrowerName, null, finalReason));
		entry.setNotes(getNotes(type, debit, credit, borrowerName, null, finalReason));

		loanStatementRepository.save(entry);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	private void createInstallmentStatement(Long installmentId, InstallmentTransactionType type, BigDecimal debit,
			BigDecimal credit, String ref, LocalDateTime actualPaymentDate, Long paymentId, String reason) {
		Optional<MInstallments> optionalInstallment = installmentRepository.findById(installmentId);
		if (!optionalInstallment.isPresent()) {
			return;
		}

		MInstallments installment = optionalInstallment.get();

		BigDecimal lastBalance = BigDecimal.ZERO;
		MInstallmentStatement lastEntry = installmentStatementRepository
				.findTopByIsActiveAndAdOrgIDAndInstallmentOrderByStatementIdDesc(true, installment.getAdOrgID(),
						installment);
		if (lastEntry != null) {
			lastBalance = lastEntry.getBalance();
		}

		BigDecimal newBalance = lastBalance.add(debit).subtract(credit);
		String borrowerName = utils.getBorrowerName(installment.getLoan());

		MInstallmentStatement entry = new MInstallmentStatement();
		entry.setInstallment(installment);
		entry.setTransactionType(type);
		entry.setTransactionRef(ref);
		entry.setTransactionDate(LocalDateTime.now());
		entry.setActualPaymentDate(actualPaymentDate != null ? actualPaymentDate : LocalDateTime.now());
		entry.setActualDate(entry.getActualPaymentDate());
		entry.setPaymentId(paymentId);
		entry.setDebitAmount(debit);
		entry.setCreditAmount(credit);
		entry.setBalance(newBalance);
		entry.setAdOrgID(installment.getAdOrgID());
		entry.setAdClientId(installment.getAdClientId());
		entry.setIsReversed(false);

		// Get default reason if none provided
		String finalReason = reason != null ? reason : getDefaultReasonForTransaction(type);

		// Description and Notes
		entry.setDescription(
				getInstallmentStatementDescription(type, debit, credit, borrowerName, installment, finalReason));
		entry.setNotes(getInstallmentStatementsNotes(type, debit, credit, borrowerName, installment, finalReason));

		installmentStatementRepository.save(entry);
	}

	

	// ===============================================
	// === DEFAULT REASON GENERATORS ===
	// ===============================================

	/**
	 * Get default reason for LoanTransactionType
	 */
	private String getDefaultReasonForTransaction(LoanTransactionType type) {
		switch (type) {
		case LOAN_DISBURSED:
			return "Amount approved";
		case INTEREST_ACCRUED:
			return "Interest accrued as per loan terms";
		case PENALTY_CHARGED:
			return "Penalty charged for overdue payment";
		case REPAYMENT_RECEIVED:
			return "Loan repayment received from borrower";
		case ADJUSTMENT:
			return "Manual account adjustment";
		case WRITE_OFF:
			return "Loan amount written off as uncollectible debt";
		case PARTIAL_WRITE_OFF:
			return "Partial loan amount written off as uncollectible";
		case CREDIT_NOTE:
			return "Credit note issued for billing correction";
		case WAIVER:
			return "Loan amount waived as per company policy";
		case PARTIAL_WAIVER:
			return "Partial loan amount waived as per company policy";
		case INTEREST_WAIVER:
			return "Interest amount waived as per company policy";
		case PENALTY_WAIVER:
			return "Penalty amount waived as per company policy";
		case SETTLEMENT:
			return "Settlement payment received for loan";
		case PARTIAL_SETTLEMENT:
			return "Partial settlement payment received";
		case PROCESSING_FEE:
			return "Loan processing fee charged";
		case LATE_FEE:
			return "Late payment fee charged";
		case ADMIN_FEE:
			return "Administration fee charged";
		case EARLY_REPAYMENT_FEE:
			return "Early repayment fee charged";
		case REFUND:
			return "Refund issued to borrower";
		case REVERSAL:
			return "Transaction reversed due to error";
		case PENALTY_REVERSAL:
			return "Penalty charge reversed";
		case INTEREST_CAPITALIZATION:
			return "Interest capitalized to principal balance";
		case FORBEARANCE:
			return "Forbearance granted on loan payments";
		case DEBT_RESCHEDULING:
			return "Debt payment schedule rescheduled";
		case DEBT_CONSOLIDATION:
			return "Debt consolidated with other obligations";
		case CORRECTION:
			return "Error correction applied to account";
		case SYSTEM_ADJUSTMENT:
			return "System-initiated adjustment";
		case INSURANCE_PREMIUM:
			return "Loan insurance premium charged";
		case TAX_CHARGE:
			return "Tax charged on loan transaction";
		case TAX_WAIVER:
			return "Tax amount waived as per policy";
		case BILL_OR_INVOICE_GENERATED:
			return "New bill/invoice issued";
		case BILL_INTEREST_ACCRUED:
			return "Interest calculated on bill";
		case BILL_INTEREST_WAIVER:
			return "Interest waived on bill";
		case BILL_PENALTY_CHARGED:
			return "Penalty applied to bill";
		case BILL_PENALTY_WAIVER:
			return "Penalty waived on bill";
		case BILL_PENALTY_REVERSAL:
			return "Penalty reversed on bill";
		case WALLET_DEPOSIT:
		case WALLET_WITHDRAWAL:
			return "Wallet transaction (should not be here)";
		default:
			return "Transaction processed";
		}
	}

	/**
	 * Get default reason for InstallmentTransactionType
	 */
	private String getDefaultReasonForTransaction(InstallmentTransactionType type) {
		switch (type) {
		case INSTALLMENT_GENERATION:
			return "Installment amount generated";
		case INTEREST_ACCRUED:
			return "Interest accrued on installment";
		case PENALTY_CHARGED:
			return "Penalty charged on installment";
		case REPAYMENT_RECEIVED:
			return "Installment repayment received";
		case ADJUSTMENT:
			return "Manual adjustment on installment";
		case WRITE_OFF:
			return "Installment amount written off as uncollectible";
		case PARTIAL_WRITE_OFF:
			return "Partial installment amount written off";
		case CREDIT_NOTE:
			return "Credit note for installment correction";
		case WAIVER:
			return "Installment amount waived as per policy";
		case PARTIAL_WAIVER:
			return "Partial installment amount waived";
		case INTEREST_WAIVER:
			return "Installment interest waived as per policy";
		case PENALTY_WAIVER:
			return "Installment penalty waived as per policy";
		case SETTLEMENT:
			return "Installment settlement payment received";
		case PARTIAL_SETTLEMENT:
			return "Partial installment settlement";
		case PROCESSING_FEE:
			return "Installment processing fee charged";
		case LATE_FEE:
			return "Installment late fee charged";
		case ADMIN_FEE:
			return "Installment administration fee charged";
		case EARLY_REPAYMENT_FEE:
			return "Early repayment fee for installment";
		case REFUND:
			return "Installment refund issued";
		case REVERSAL:
			return "Installment transaction reversed";
		case PENALTY_REVERSAL:
			return "Installment penalty reversed";
		case INTEREST_CAPITALIZATION:
			return "Installment interest capitalized";
		case FORBEARANCE:
			return "Installment forbearance granted";
		case DEBT_RESCHEDULING:
			return "Installment payment schedule rescheduled";
		case DEBT_CONSOLIDATION:
			return "Installment consolidated with other debts";
		case CORRECTION:
			return "Installment error correction applied";
		case SYSTEM_ADJUSTMENT:
			return "System adjustment on installment";
		case INSURANCE_PREMIUM:
			return "Installment insurance premium charged";
		case TAX_CHARGE:
			return "Tax charged on installment";
		case TAX_WAIVER:
			return "Installment tax waived as per policy";
		default:
			return "Installment transaction processed";
		}
	}

	// ===============================================
	// === DESCRIPTION AND NOTES HELPERS ===
	// ===============================================

	private String getDescription(LoanTransactionType type, BigDecimal debit, BigDecimal credit, String borrowerName,
			MInstallments installment, String reason) {
		switch (type) {
		case LOAN_DISBURSED:
			return "Amount approved";
		case INTEREST_ACCRUED:
			return "Interest accrued";
		case PENALTY_CHARGED:
			return "Penalty charged for overdue amount";
		case REPAYMENT_RECEIVED:
			return "Repayment received from borrower";
		case ADJUSTMENT:
			return "Manual account adjustment";
		case WRITE_OFF:
			return "Loan amount written off as bad debt";
		case PARTIAL_WRITE_OFF:
			return "Partial loan amount written off";
		case CREDIT_NOTE:
			return "Credit note issued for billing correction";
		case WAIVER:
			return "Loan amount waived";
		case PARTIAL_WAIVER:
			return "Partial loan amount waived";
		case INTEREST_WAIVER:
			return "Interest amount waived";
		case PENALTY_WAIVER:
			return "Penalty amount waived";
		case SETTLEMENT:
			return "Settlement payment received";
		case PARTIAL_SETTLEMENT:
			return "Partial settlement payment received";
		case PROCESSING_FEE:
			return "Processing fee charged";
		case LATE_FEE:
			return "Late payment fee charged";
		case ADMIN_FEE:
			return "Administration fee charged";
		case EARLY_REPAYMENT_FEE:
			return "Early repayment fee charged";
		case REFUND:
			return "Refund issued to borrower";
		case REVERSAL:
			return "Transaction reversed";
		case PENALTY_REVERSAL:
			return "Penalty charge reversed";
		case INTEREST_CAPITALIZATION:
			return "Interest capitalized to principal";
		case FORBEARANCE:
			return "Forbearance granted on loan payments";
		case DEBT_RESCHEDULING:
			return "Debt rescheduled";
		case DEBT_CONSOLIDATION:
			return "Debt consolidated with other loans";
		case CORRECTION:
			return "Error correction applied";
		case SYSTEM_ADJUSTMENT:
			return "System adjustment applied";
		case INSURANCE_PREMIUM:
			return "Insurance premium charged";
		case TAX_CHARGE:
			return "Tax charged on loan";
		case TAX_WAIVER:
			return "Tax amount waived";
		default:
			return "Loan transaction recorded";
		}
	}

	private String getInstallmentStatementDescription(InstallmentTransactionType type, BigDecimal debit,
			BigDecimal credit, String borrowerName, MInstallments installment, String reason) {
		switch (type) {
		case INSTALLMENT_GENERATION:
			return "Installment amount generated";
		case INTEREST_ACCRUED:
			return "Interest accrued on installment";
		case PENALTY_CHARGED:
			return "Penalty charged on installment";
		case REPAYMENT_RECEIVED:
			return "Repayment received for installment";
		case ADJUSTMENT:
			return "Manual adjustment on installment";
		case WRITE_OFF:
			return "Installment amount written off";
		case PARTIAL_WRITE_OFF:
			return "Partial installment amount written off";
		case CREDIT_NOTE:
			return "Credit note for installment correction";
		case WAIVER:
			return "Installment amount waived";
		case PARTIAL_WAIVER:
			return "Partial installment amount waived";
		case INTEREST_WAIVER:
			return "Installment interest waived";
		case PENALTY_WAIVER:
			return "Installment penalty waived";
		case SETTLEMENT:
			return "Installment settlement payment";
		case PARTIAL_SETTLEMENT:
			return "Partial installment settlement";
		case PROCESSING_FEE:
			return "Installment processing fee";
		case LATE_FEE:
			return "Installment late fee";
		case ADMIN_FEE:
			return "Installment admin fee";
		case EARLY_REPAYMENT_FEE:
			return "Early repayment fee for installment";
		case REFUND:
			return "Installment refund issued";
		case REVERSAL:
			return "Installment transaction reversed";
		case PENALTY_REVERSAL:
			return "Installment penalty reversed";
		case INTEREST_CAPITALIZATION:
			return "Installment interest capitalized";
		case FORBEARANCE:
			return "Installment forbearance granted";
		case DEBT_RESCHEDULING:
			return "Installment rescheduled";
		case DEBT_CONSOLIDATION:
			return "Installment consolidated";
		case CORRECTION:
			return "Installment error correction";
		case SYSTEM_ADJUSTMENT:
			return "System adjustment on installment";
		case INSURANCE_PREMIUM:
			return "Installment insurance premium";
		case TAX_CHARGE:
			return "Tax charged on installment";
		case TAX_WAIVER:
			return "Installment tax waived";
		default:
			return "Installment transaction recorded";
		}
	}

	private String getNotes(LoanTransactionType type, BigDecimal debit, BigDecimal credit, String borrowerName,
			MInstallments installment, String reason) {
		StringBuilder notes = new StringBuilder();

		switch (type) {
		case LOAN_DISBURSED:
			notes.append("Principal amount of ").append(debit).append(" approved for ").append(borrowerName);
			break;
		case INTEREST_ACCRUED:
			notes.append("Interest of ").append(debit).append(" accrued");
			break;
		case PENALTY_CHARGED:
			notes.append("Penalty of ").append(debit).append(" charged");
			break;
		case REPAYMENT_RECEIVED:
			notes.append("Payment of ").append(credit).append(" received from ").append(borrowerName);
			break;
		case ADJUSTMENT:
			notes.append("Adjustment made — debit: ").append(debit).append(", credit: ").append(credit);
			break;
		case WRITE_OFF:
		case PARTIAL_WRITE_OFF:
			notes.append("Amount of ").append(credit).append(" written off for ").append(borrowerName);
			break;
		case CREDIT_NOTE:
			notes.append("Credit note of ").append(credit).append(" issued to ").append(borrowerName);
			break;
		case WAIVER:
		case PARTIAL_WAIVER:
			notes.append("Amount of ").append(credit).append(" waived for ").append(borrowerName);
			break;
		case INTEREST_WAIVER:
			notes.append("Interest amount of ").append(credit).append(" waived");
			break;
		case PENALTY_WAIVER:
			notes.append("Penalty amount of ").append(credit).append(" waived");
			break;
		default:
			notes.append(type.getDescription()).append(" — debit: ").append(debit).append(", credit: ").append(credit)
					.append(" for ").append(borrowerName);
			break;
		}

		if (reason != null && !reason.trim().isEmpty()) {
			notes.append(". Reason: ").append(reason);
		}

		return notes.toString();
	}

	private String getInstallmentStatementsNotes(InstallmentTransactionType type, BigDecimal debit, BigDecimal credit,
			String borrowerName, MInstallments installment, String reason) {
		StringBuilder notes = new StringBuilder();

		switch (type) {
		case INSTALLMENT_GENERATION:
			notes.append("Installment amount of ").append(debit).append(" generated for ").append(borrowerName);
			break;
		case INTEREST_ACCRUED:
			notes.append("Interest of ").append(debit).append(" accrued on installment");
			break;
		case PENALTY_CHARGED:
			notes.append("Penalty of ").append(debit).append(" charged on installment");
			break;
		case REPAYMENT_RECEIVED:
			notes.append("Payment of ").append(credit).append(" received for installment from ").append(borrowerName);
			break;
		case ADJUSTMENT:
			notes.append("Adjustment made — debit: ").append(debit).append(", credit: ").append(credit);
			break;
		case WRITE_OFF:
		case PARTIAL_WRITE_OFF:
			notes.append("Installment amount of ").append(credit).append(" written off");
			break;
		case CREDIT_NOTE:
			notes.append("Credit note of ").append(credit).append(" issued for installment");
			break;
		case WAIVER:
		case PARTIAL_WAIVER:
			notes.append("Installment amount of ").append(credit).append(" waived");
			break;
		case INTEREST_WAIVER:
			notes.append("Installment interest of ").append(credit).append(" waived");
			break;
		case PENALTY_WAIVER:
			notes.append("Installment penalty of ").append(credit).append(" waived");
			break;
		default:
			notes.append(type.getDescription()).append(" — debit: ").append(debit).append(", credit: ").append(credit)
					.append(" for installment");
			break;
		}

		if (installment != null) {
			notes.append(" (").append(installment.getDocumentNo()).append(")");
		}

		if (reason != null && !reason.trim().isEmpty()) {
			notes.append(". Reason: ").append(reason);
		}

		return notes.toString();
	}

	// ===============================================
	// === RETRIEVAL METHODS (NO CHANGES NEEDED) ===
	// ===============================================

	@Transactional(readOnly = true)
	public List<LoanStatementResponse> getLoanStatement(Long loanId) {
		Optional<MLoanApplication> optionalLoan = loanApplicationRepository.findById(loanId);
		if (!optionalLoan.isPresent())
			return new ArrayList<>();

		return loanStatementRepository.findByIsActiveAndLoanOrderByStatementIdAsc(true, optionalLoan.get()).stream()
				.map(objectsMapper::mapLoanStatement).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public Page<LoanStatementResponse> getAllLoanStatement(int size, int page, String searchTerm,
			String transactionType) {
		String term = (searchTerm == null || searchTerm.trim().isEmpty()) ? "" : searchTerm.trim();
		LoanTransactionType transactionType1 = LoanTransactionType.UNKNOWN;
		if (transactionType != null && !transactionType.isEmpty()) {
			transactionType1 = LoanTransactionType.valueOf(transactionType);
		}

		Page<MLoanStatement> loanStatements = loanStatementRepository
				.findByIsActiveAndAdOrgIDOrderByStatementIdAsc(true, utils.getAD_Org_ID(), PageRequest.of(page, size));

		return loanStatements.map(objectsMapper::mapLoanStatement);
	}

	@Transactional(readOnly = true)
	public List<LoanStatementResponse> getLoanStatementOnly(Long loanId) {
		Optional<MLoanApplication> optionalLoan = loanApplicationRepository.findById(loanId);
		if (!optionalLoan.isPresent())
			return new ArrayList<>();

		return loanStatementRepository.findByIsActiveAndLoanOrderByStatementIdAsc(true, optionalLoan.get()).stream()
				.map(objectsMapper::mapLoanStatement).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<InstallmentResponse> getInstallmentsByLoan(long loanId) {
		MLoanApplication loan = loanApplicationRepository.findById(loanId).orElse(null);
		if (loan == null)
			return new ArrayList<>();

		return installmentRepository.findByIsActiveAndLoanOrderByInstallmentIdAsc(true, loan).stream()
				.map(objectsMapper::mapInstallments).collect(Collectors.toList());
	}
}