package co.ke.tezza.loanapp.model;

import co.ke.tezza.loanapp.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodModel {
	private long paymentMethodId;
	private String name;
	private String description;
	private PaymentType paymentType;

}
