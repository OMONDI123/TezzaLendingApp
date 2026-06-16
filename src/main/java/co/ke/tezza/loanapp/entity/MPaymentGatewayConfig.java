package co.ke.tezza.loanapp.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import co.ke.tezza.loanapp.enums.PaymentGateway;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Payment_Gateway_Config")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MPaymentGatewayConfig extends AuditModel{
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Payment_Gateway_Config_ID")
	private long paymentGatewayConfigId;
	private String mpesaApiKey;
	private String mpesaApiSecrete;
	private String mpesaProductionBaseUrl;
	private String mpesaTestBaseUrl;
	private boolean mpesaProductionAllowed;
	private String mpesaOrganizationShortCode;
	private String BusinessShortCode;
	private String TransactionType;
	private String PartyB;
	private String CallBackUrl;
	private String stkCallBackUrl;
	private String validationUrl;
	@Enumerated(EnumType.STRING)
	private PaymentGateway paymentGatway;
	
	
	private String passKey;
	

}
