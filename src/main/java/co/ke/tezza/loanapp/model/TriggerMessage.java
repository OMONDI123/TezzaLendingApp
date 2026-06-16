package co.ke.tezza.loanapp.model;

import java.util.List;

import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class TriggerMessage {
	private List<String> loanReferenceNo;
	private SmsTypeEnum messageType;
	private List<String> membershipAccountNo;
	

}
