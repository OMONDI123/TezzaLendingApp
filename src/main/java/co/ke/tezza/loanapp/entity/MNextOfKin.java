package co.ke.tezza.loanapp.entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import co.ke.tezza.loanapp.enums.RelationShipEnum;
import lombok.*;

@Entity
@Table(name = "AD_NextOfKin")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MNextOfKin extends AuditModel{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_NextOfKin_ID")
	private Long nextOfKinId;

	private String fullName;
	@Enumerated(EnumType.STRING)
	private RelationShipEnum relationship;

	private String phoneNumber;
	private String address;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean primaryGuarantor;
	private String email;
	private String nationalId;
	
	
	

}
