package co.ke.tezza.loanapp.model;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmsSetupRequest {
	private Long smsSetupId;

	private String messageTemplate;

	@Enumerated(EnumType.STRING)
	private SmsTypeEnum smsType;

	private boolean debt;

}
