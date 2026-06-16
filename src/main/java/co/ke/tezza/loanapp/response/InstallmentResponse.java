package co.ke.tezza.loanapp.response;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstallmentResponse {
	private long installmentId;
	private String documentNo;
	private BigDecimal amount;
	private BigDecimal balance;
	private BigDecimal interestEarned;
	private BigDecimal penaltyEarned;
	private Integer noOfRemindersSent;
	private LoanApplicationResponse loan;
	private Date periodStart;
	private Date periodEnd;
	private Integer gracePeriod;
	private Date lastReminderSent;
	private Date penaltyStartDate;

}
