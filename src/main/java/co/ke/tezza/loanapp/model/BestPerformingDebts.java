package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BestPerformingDebts {
	private String debtReferenceNo;
	private String debtorName;
	private BigDecimal originalAmount;
	private BigDecimal penaltiesAndInterests;
	private BigDecimal payments;
	private BigDecimal outstandingBalance;
	private BigDecimal individualPerformance;
	private Integer noOfDaysSinceDisbursement;
	private BigDecimal total;

}
