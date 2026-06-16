package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TopOverdueDebtors {
	private int noOfDaysOverdue;
	private BigDecimal amountOverdue;
	private String debtorName;

}
