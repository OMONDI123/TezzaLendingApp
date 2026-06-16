package co.ke.tezza.loanapp.entity;

import java.util.UUID;

import javax.persistence.*;

import co.ke.tezza.loanapp.enums.SupportedProviders;
import lombok.*;

@Entity
@Table(name = "AD_Sms_Config")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MSMSConfig extends AuditModel {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Sms_Config_ID")
	private long smsConfigId;
	private String senderId;

	private String partnerId;
	private String apiKey;
	private String callBackUrl;
	private String smsBaseUrl;
	@Enumerated(EnumType.STRING)
	@Column(columnDefinition = "VARCHAR(255) DEFAULT 'ADVANTA'")
	private SupportedProviders smsProvider = SupportedProviders.ADVANTA;
	private String username;
	private String apiSecret;

	private String AD_Sms_Config_UU = UUID.randomUUID().toString();

}
