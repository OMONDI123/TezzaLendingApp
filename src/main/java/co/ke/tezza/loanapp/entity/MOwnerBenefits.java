package co.ke.tezza.loanapp.entity;

import java.math.BigDecimal;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "AD_Owner_Benefits")
public class MOwnerBenefits extends AuditModel{
	@Column(name = "AD_Owner_Benefits_ID")
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long ownerBenefitId;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Bidder_ID")
	private MUser bidder;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean claimed;
	private BigDecimal amount;
	private String AD_Owner_Benefits_UU=UUID.randomUUID().toString();
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Owner_ID")
	private MUser owner;

}
