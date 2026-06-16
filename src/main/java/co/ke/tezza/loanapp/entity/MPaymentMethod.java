package co.ke.tezza.loanapp.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import co.ke.tezza.loanapp.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Payment_Method")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MPaymentMethod extends AuditModel {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Payment_Method_ID")
	private Long paymentMethodId;
	@Enumerated(EnumType.STRING)
	private PaymentType paymentType;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean usedForWaiversAndWriteOffs;

}
