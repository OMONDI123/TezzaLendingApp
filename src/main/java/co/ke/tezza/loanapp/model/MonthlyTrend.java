package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MonthlyTrend {
	private String month;
	private BigDecimal amount;

}
