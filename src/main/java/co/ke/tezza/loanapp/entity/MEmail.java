package co.ke.tezza.loanapp.entity;

import java.util.UUID;

import javax.persistence.*;

import lombok.*;

@Entity
@Table(name = "AD_Email_Config")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MEmail extends AuditModel {
	@Id
	@GeneratedValue(strategy =  GenerationType.IDENTITY)
	@Column(name = "AD_Email_Config_ID")
	private long emailId;
	private String host;
	private int port;
	private String username;
	private String password;
	@Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
	private boolean smtpAuth;
	@Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
	private boolean sslEnabled;
	@Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
	private boolean starttlsEnabled;
	private String AD_Email_Config_UU=UUID.randomUUID().toString();

}
