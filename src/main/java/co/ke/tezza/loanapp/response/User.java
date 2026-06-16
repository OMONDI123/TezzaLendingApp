package co.ke.tezza.loanapp.response;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
	private Long userId;
	private String firstName;
	private String email;
	private String phoneNumber;
	private String lastName;
	private String gender;
	private String fullName;
	private Date created;
	private String externalRefrenceNo;

}
