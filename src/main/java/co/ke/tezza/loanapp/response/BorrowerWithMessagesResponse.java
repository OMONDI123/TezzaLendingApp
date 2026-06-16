package co.ke.tezza.loanapp.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorrowerWithMessagesResponse {
	private Long borrowerId;
	private String borrowerType;
	private Integer messagesSent;
	private Long individualBorrowerId;
	private Long institutionBorrowerId;
	private Long groupBorrowerId;
	private String borrowerName;
	private String email;
	private String phone;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime lastMessageTime;

	private String lastMessage;
	private Double balance;

}
