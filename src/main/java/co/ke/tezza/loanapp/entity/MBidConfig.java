package co.ke.tezza.loanapp.entity;

import java.math.BigDecimal;
import java.util.UUID;
import javax.persistence.*;

import org.hibernate.envers.Audited;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AD_Bid_Config")
@Audited
public class MBidConfig extends AuditModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Bid_Config_ID")
	private long id;

	@Column(name = "bid_interest_rates")
	private double bidInterestRates;

	@Column(name = "minimum_bid_amount")
	private BigDecimal minimumBidAmount;

	@Column(name = "maximum_bid_amount")
	private BigDecimal maximumBidAmount;

	@Column(name = "default_cycle_duration_weeks")
	private int defaultCycleDurationWeeks;

	@Column(name = "interest_compounding_enabled", columnDefinition = "BOOLEAN DEFAULT TRUE")
	private boolean interestCompoundingEnabled;

	@Column(name = "ad_bid_config_UU")
	private String aD_Bid_Config_UU = UUID.randomUUID().toString();
}
