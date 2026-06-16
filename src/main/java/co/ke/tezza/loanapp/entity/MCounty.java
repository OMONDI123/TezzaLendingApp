package co.ke.tezza.loanapp.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AD_County")
public class MCounty extends AuditModel{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_County_ID")
	private long countyId;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@OnDelete( action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "AD_Country_ID")
	private MCountry country;
	@Column(name = "AD_County_UU")
	private String AD_County_UU;
	

}
