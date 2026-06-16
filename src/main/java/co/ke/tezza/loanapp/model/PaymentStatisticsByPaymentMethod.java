package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStatisticsByPaymentMethod {
	private String paymentMethod;
	private BigDecimal amount;
	

}
