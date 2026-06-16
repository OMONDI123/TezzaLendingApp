package co.ke.tezza.loanapp.response;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmsSetupResponse {
	private Long smsSetupId;

	private String messageTemplate;

	@Enumerated(EnumType.STRING)
	private SmsTypeEnum smsType;

	private boolean isDebt;
	private boolean isActive;
	

}
