package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentGateWayConfigRequest {
	private long paymentGatewayConfigId;
	private String mpesaApiKey;
	private String mpesaApiSecret;
	private String mpesaProductionBaseUrl;
	private String mpesaTestBaseUrl;
	private boolean mpesaProductionAllowed;
	private String mpesaOrganizationShortCode;
	private String businessShortCode;
	private String transactionType;
	private String partyB;
	private String callBackUrl;
	private String stkCallBackUrl;
	private String validationUrl;
	
	private String passKey;
	

}
