package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordReset {
	
	
	private int otpVerificationCode;
	private String oldPassword;
	
	private String newPassword;
	private int otp;
	private String email;

}
