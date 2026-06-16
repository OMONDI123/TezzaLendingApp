package co.ke.tezza.loanapp.model;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import co.ke.tezza.loanapp.enums.MessageForm;
import co.ke.tezza.loanapp.enums.ReceiverCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequest {
	private String message;
	private String file;
	private String subject;
	private MessageForm messageForm;
	private Date dateToSendMessage; 
	private boolean isScheduledMessage=false;
	private ReceiverCategory receiverCategory;
	private List<Long> individualBorrowerId=new ArrayList<>();
	private List<Long> groupBorrowerId=new ArrayList<>();
	private List<Long> institutionBorrowerId=new ArrayList<>();
	private List<Long> membershipAccountIds=new ArrayList<>();
	private boolean SendAlsoGuarantors;
	
	

}
