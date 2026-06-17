package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MDebtor;
import co.ke.tezza.loanapp.entity.MGroupDebtors;
import co.ke.tezza.loanapp.entity.MInstitutionBorrower;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import co.ke.tezza.loanapp.enums.CustomerEligibilityStatus;
import co.ke.tezza.loanapp.enums.LoanStateEnum;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.CreditEligible;
import co.ke.tezza.loanapp.repository.GroupBorrowersRepository;
import co.ke.tezza.loanapp.repository.IndividualBorrowersRepository;
import co.ke.tezza.loanapp.repository.InstitutionBorrowersRepository;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;

@Service
public class CustomerEligibilityService {

	@Autowired
	private LoanApplicationRepository loanApplicationRepository;
	@Autowired
	private IndividualBorrowersRepository individualBorrowersRepository;
	@Autowired
	private GroupBorrowersRepository groupBorrowersRepository;
	@Autowired
	private InstitutionBorrowersRepository institutionBorrowersRepository;

	private static final BigDecimal MAX_SCORE = new BigDecimal("10000");
	private static final BigDecimal MIN_AUTO_APPROVE_SCORE = new BigDecimal("100");

	/**
	 * Entry point to call before a new loan application is created. Throws
	 * SetUpExceptions if the customer should not receive new credit right now.
	 */
	@Transactional
	public void validateLoanEligibility(BorrowerTypeEnum borrowerType, long borrowerId, BigDecimal requestedAmount) {
		CreditEligible profile = resolveCreditEligible(borrowerType, borrowerId);
		String borrowerLabel = borrowerType.name() + " borrower #" + borrowerId;

		if (profile.getEligibilityStatus() == CustomerEligibilityStatus.BLACKLISTED) {
			throw new SetUpExceptions(borrowerLabel + " is blacklisted and cannot be issued new credit. Reason: "
					+ safe(profile.getEligibilityReason()));
		}

		if (profile.getEligibilityStatus() == CustomerEligibilityStatus.RESTRICTED) {
			throw new SetUpExceptions(borrowerLabel + " is currently restricted from new credit. Reason: "
					+ safe(profile.getEligibilityReason()));
		}

		RepaymentHistorySummary history = getRepaymentHistory(borrowerType, borrowerId);

		if (history.getOpenOverdueLoans() > 0) {
			throw new SetUpExceptions(borrowerLabel + " has " + history.getOpenOverdueLoans()
					+ " overdue loan(s). Arrears must be cleared before a new loan can be issued.");
		}

		BigDecimal creditLimit = profile.getCreditLimit();
		if (creditLimit != null && creditLimit.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal available = creditLimit.subtract(history.getCurrentOutstandingBalance());
			if (requestedAmount.compareTo(available) > 0) {
				throw new SetUpExceptions(String.format(
						"%s's available credit limit is %s (limit %s, current exposure %s). Requested amount %s exceeds this.",
						borrowerLabel, available, creditLimit, history.getCurrentOutstandingBalance(),
						requestedAmount));
			}
		}
	}

	/**
	 * Recomputes the 0-1000 creditworthiness score from repayment history and
	 * persists the resulting score, status, and reason onto the borrower record.
	 * Call this after a loan closes, is written off, or goes overdue, and
	 * periodically via a scheduled job for active customers.
	 */
	@Transactional
	public BigDecimal recalculateCreditworthiness(BorrowerTypeEnum borrowerType, long borrowerId) {
		CreditEligible profile = resolveCreditEligible(borrowerType, borrowerId);
		RepaymentHistorySummary history = getRepaymentHistory(borrowerType, borrowerId);

		BigDecimal score = computeScore(history);
		profile.setCreditScore(score);
		profile.setLastEligibilityReviewDate(new Date());

		CustomerEligibilityStatus status;
		String reason;

		if (history.getWrittenOffLoans() > 0) {
			status = CustomerEligibilityStatus.BLACKLISTED;
			reason = history.getWrittenOffLoans() + " loan(s) written off as uncollectable.";
		} else if (history.getOpenOverdueLoans() > 0) {
			status = CustomerEligibilityStatus.RESTRICTED;
			reason = history.getOpenOverdueLoans() + " loan(s) currently overdue.";
		} else if (score.compareTo(MIN_AUTO_APPROVE_SCORE) < 0) {
			status = CustomerEligibilityStatus.UNDER_REVIEW;
			reason = "Credit score of " + score + " is below the auto-approval threshold of " + MIN_AUTO_APPROVE_SCORE
					+ ".";
		} else {
			status = CustomerEligibilityStatus.ELIGIBLE;
			reason = null;
		}

		profile.setEligibilityStatus(status);
		profile.setEligibilityReason(reason);
		persist(borrowerType, profile);
		return score;
	}

	/**
	 * Lightweight check the notification layer can call to decide who to message.
	 */
	public boolean isEligibleForDirectCommunication(BorrowerTypeEnum borrowerType, long borrowerId) {
		CreditEligible profile = resolveCreditEligible(borrowerType, borrowerId);
		return !profile.isCommunicationOptOut()
				&& profile.getEligibilityStatus() != CustomerEligibilityStatus.BLACKLISTED;
	}

	/**
	 * Builds a summary of a borrower's full loan history for scoring and limit
	 * checks.
	 */
	public RepaymentHistorySummary getRepaymentHistory(BorrowerTypeEnum borrowerType, long borrowerId) {
		List<MLoanApplication> loans = fetchLoanHistory(borrowerType, borrowerId);
		RepaymentHistorySummary summary = new RepaymentHistorySummary();
		BigDecimal currentOutstanding = BigDecimal.ZERO;

		for (MLoanApplication loan : loans) {
			summary.incrementTotalLoans();
			LoanStateEnum state = loan.getLoanState();
			if (state == null) {
				continue;
			}

			switch (state) {
			case CLOSED:
				summary.incrementClosedLoans();
				if (wasRepaidOnTime(loan)) {
					summary.incrementOnTimeRepayments();
				} else {
					summary.incrementLateRepayments();
				}
				break;
			case WRITTEN_OFF:
				summary.incrementWrittenOffLoans();
				break;
			case OVERDUE:
				summary.incrementOpenOverdueLoans();
				currentOutstanding = currentOutstanding.add(nz(loan.getBalance()));
				break;
			case OPEN:
				currentOutstanding = currentOutstanding.add(nz(loan.getBalance()));
				break;
			case CANCELLED:
				summary.incrementCancelledLoans();
				break;
			default:
				// e.g. PENDING_APPROVAL - not yet meaningful for history scoring
				break;
			}
		}

		summary.setCurrentOutstandingBalance(currentOutstanding);
		return summary;
	}

	// ------------------------------------------------------------------
	// Scoring
	// ------------------------------------------------------------------

	private BigDecimal computeScore(RepaymentHistorySummary history) {
		if (history.getTotalLoans() == 0) {
			return new BigDecimal("500"); // no history yet: neutral starting score
		}

		BigDecimal score = new BigDecimal("500");
		int completed = history.getClosedLoans();

		if (completed > 0) {
			BigDecimal onTimeRatio = new BigDecimal(history.getOnTimeRepayments()).divide(new BigDecimal(completed), 4,
					RoundingMode.HALF_UP);
			score = score.add(onTimeRatio.multiply(new BigDecimal("400"))); // up to +400 for a perfect record
		}

		score = score.subtract(new BigDecimal(history.getWrittenOffLoans()).multiply(new BigDecimal("350")));
		score = score.subtract(new BigDecimal(history.getOpenOverdueLoans()).multiply(new BigDecimal("150")));
		score = score.add(new BigDecimal(Math.min(completed, 10)).multiply(BigDecimal.TEN)); // reward track record

		if (score.compareTo(BigDecimal.ZERO) < 0)
			score = BigDecimal.ZERO;
		if (score.compareTo(MAX_SCORE) > 0)
			score = MAX_SCORE;
		return score.setScale(0, RoundingMode.HALF_UP);
	}

	private boolean wasRepaidOnTime(MLoanApplication loan) {
		if (loan.getDueDate() == null || loan.getClosedDate() == null) {
			return true;
		}
		return !loan.getClosedDate().after(loan.getDueDate());
	}

	// ------------------------------------------------------------------
	// Borrower resolution
	// ------------------------------------------------------------------

	private List<MLoanApplication> fetchLoanHistory(BorrowerTypeEnum borrowerType, long borrowerId) {
		switch (borrowerType) {
		case INDIVIDUAL:
			MDebtor debtor = individualBorrowersRepository.findById(borrowerId)
					.orElseThrow(() -> new SetUpExceptions("Individual borrower not found: " + borrowerId));
			return loanApplicationRepository.findByIsActiveAndBorrowerTypeAndIndividualBorrower(true,
					BorrowerTypeEnum.INDIVIDUAL, debtor);
		case GROUP:
			MGroupDebtors group = groupBorrowersRepository.findById(borrowerId)
					.orElseThrow(() -> new SetUpExceptions("Group borrower not found: " + borrowerId));
			return loanApplicationRepository.findByIsActiveAndBorrowerTypeAndGroupBorrower(true, BorrowerTypeEnum.GROUP,
					group);
		case INSTITUTION:
			MInstitutionBorrower institution = institutionBorrowersRepository.findById(borrowerId)
					.orElseThrow(() -> new SetUpExceptions("Institution borrower not found: " + borrowerId));
			return loanApplicationRepository.findByIsActiveAndBorrowerTypeAndInstitutionBorrower(true,
					BorrowerTypeEnum.INSTITUTION, institution);
		default:
			throw new SetUpExceptions("Unsupported borrower type: " + borrowerType);
		}
	}

	public CreditEligible resolveCreditEligible(BorrowerTypeEnum borrowerType, long borrowerId) {
		switch (borrowerType) {
		case INDIVIDUAL:
			return individualBorrowersRepository.findById(borrowerId)
					.orElseThrow(() -> new SetUpExceptions("Individual borrower not found: " + borrowerId));
		case GROUP:
			return groupBorrowersRepository.findById(borrowerId)
					.orElseThrow(() -> new SetUpExceptions("Group borrower not found: " + borrowerId));
		case INSTITUTION:
			return institutionBorrowersRepository.findById(borrowerId)
					.orElseThrow(() -> new SetUpExceptions("Institution borrower not found: " + borrowerId));
		default:
			throw new SetUpExceptions("Unsupported borrower type: " + borrowerType);
		}
	}

	public CreditEligible resolveCreditEligible(MLoanApplication loan) {
		long id;
		switch (loan.getBorrowerType()) {
		case INDIVIDUAL:
			id = loan.getIndividualBorrower().getIndividualBorrowerId();
			break;
		case GROUP:
			id = loan.getGroupBorrower().getGroupBorrowerId();
			break;
		case INSTITUTION:
			id = loan.getInstitutionBorrower().getInstitutionBorrowerId();
			break;
		default:
			throw new SetUpExceptions("Unsupported borrower type: " + loan.getBorrowerType());
		}
		return resolveCreditEligible(loan.getBorrowerType(), id);
	}

	private void persist(BorrowerTypeEnum borrowerType, CreditEligible profile) {
		switch (borrowerType) {
		case INDIVIDUAL:
			individualBorrowersRepository.save((MDebtor) profile);
			break;
		case GROUP:
			groupBorrowersRepository.save((MGroupDebtors) profile);
			break;
		case INSTITUTION:
			institutionBorrowersRepository.save((MInstitutionBorrower) profile);
			break;
		default:
			throw new SetUpExceptions("Unsupported borrower type: " + borrowerType);
		}
	}

	private BigDecimal nz(BigDecimal value) {
		return value != null ? value : BigDecimal.ZERO;
	}

	private String safe(String value) {
		return value != null ? value : "Not specified";
	}

	public static class RepaymentHistorySummary {
		private int totalLoans;
		private int closedLoans;
		private int writtenOffLoans;
		private int openOverdueLoans;
		private int cancelledLoans;
		private int onTimeRepayments;
		private int lateRepayments;
		private BigDecimal currentOutstandingBalance = BigDecimal.ZERO;

		public void incrementTotalLoans() {
			totalLoans++;
		}

		public void incrementClosedLoans() {
			closedLoans++;
		}

		public void incrementWrittenOffLoans() {
			writtenOffLoans++;
		}

		public void incrementOpenOverdueLoans() {
			openOverdueLoans++;
		}

		public void incrementCancelledLoans() {
			cancelledLoans++;
		}

		public void incrementOnTimeRepayments() {
			onTimeRepayments++;
		}

		public void incrementLateRepayments() {
			lateRepayments++;
		}

		public int getTotalLoans() {
			return totalLoans;
		}

		public int getClosedLoans() {
			return closedLoans;
		}

		public int getWrittenOffLoans() {
			return writtenOffLoans;
		}

		public int getOpenOverdueLoans() {
			return openOverdueLoans;
		}

		public int getCancelledLoans() {
			return cancelledLoans;
		}

		public int getOnTimeRepayments() {
			return onTimeRepayments;
		}

		public int getLateRepayments() {
			return lateRepayments;
		}

		public BigDecimal getCurrentOutstandingBalance() {
			return currentOutstandingBalance;
		}

		public void setCurrentOutstandingBalance(BigDecimal v) {
			this.currentOutstandingBalance = v;
		}
	}
}