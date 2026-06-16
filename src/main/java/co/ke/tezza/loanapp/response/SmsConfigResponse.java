package co.ke.tezza.loanapp.response;

import co.ke.tezza.loanapp.enums.SupportedProviders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmsConfigResponse {
	private long smsConfigId;
	private String senderId;
	
	private String partnerId;
	private String apiKey;
	private String callBackUrl;
	private String smsBaseUrl;
	private SupportedProviders smsProvider;
	private String username;
	private String apiSecret;
	private boolean isActive;

}
