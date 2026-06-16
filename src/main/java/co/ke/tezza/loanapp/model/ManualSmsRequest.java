package co.ke.tezza.loanapp.model;

import co.ke.tezza.loanapp.enums.MessageChannel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ManualSmsRequest {
	private long userId;
	private String phoneNo;
	private String email;
	private String message;
	private String file;
	private MessageChannel messageChannel;

}
