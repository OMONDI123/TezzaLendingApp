package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ForgotPassword {
	private int otp;
	private String newPassword;
	private String confirmPassword;

}
