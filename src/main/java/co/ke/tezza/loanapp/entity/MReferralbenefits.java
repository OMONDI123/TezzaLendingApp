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
@Table(name = "AD_Referral_Benefits")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MReferralbenefits extends AuditModel{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Referral_Benefits_ID")
	private long refferalBenefitId;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Upline_ID")
	private MUser upline;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Downline_ID")
	private MUser downLine;
	private BigDecimal amount;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")

	private boolean claimed;
	@Column(name = "AD_Referral_Benefits_UU")
	private String AD_Referral_Benefits_UU=UUID.randomUUID().toString();

}
