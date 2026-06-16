package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfigRequest {
	private long emailId;
    private String host;
    private int port;
    private String username;
    private String password;
    private boolean smtpAuth = true;
    private boolean sslEnabled = true;
    private boolean starttlsEnabled = true;
    private String organisationName;
}
