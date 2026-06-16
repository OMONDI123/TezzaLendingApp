package co.ke.tezza.loanapp.response;

import co.ke.tezza.loanapp.entity.MPaymentApprovalConfiguration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentApprovalResponse {
	
	private PaymentResponse payment;
	private MPaymentApprovalConfiguration paymentApprovalConfig;

}
