package co.ke.tezza.loanapp.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;

import co.ke.tezza.loanapp.enums.LoanTransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoanStatementResponse {
	private Long statementId;
	private LoanApplicationResponse loan;
	private InstallmentResponse installment;
	private LoanTransactionType transactionType;
	// e.g. LOAN_DISBURSED, INTEREST_ACCRUED, PENALTY, REPAYMENT, ADJUSTMENT

	private String transactionRef; // e.g. Payment ID or system reference

	private LocalDateTime transactionDate;

	private String description;

	// Financials
	private BigDecimal debitAmount = BigDecimal.ZERO; // Increases balance

	private BigDecimal creditAmount = BigDecimal.ZERO; // Decreases balance

	private BigDecimal balance = BigDecimal.ZERO; // Running balance after transaction

	// Optional details
	private Boolean isReversed = false;

	private String notes;

}
