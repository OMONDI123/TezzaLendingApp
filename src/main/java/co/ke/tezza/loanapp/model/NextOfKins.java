package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.enums.RelationShipEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NextOfKins {
	private Long nextOfKinId;

	private String fullName;
	@Enumerated(EnumType.STRING)
	private RelationShipEnum relationship;
	private String phoneNumber;
	private String address;
	private BigDecimal guaranteeAmount=BigDecimal.ZERO;
	private BigDecimal guaranteeLimit;
	private boolean primaryGuarantor;
	private String email;
	private String nationalId;
	

}
