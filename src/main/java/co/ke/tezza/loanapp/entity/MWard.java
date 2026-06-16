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
@Table(name = "AD_Ward")
public class MWard extends AuditModel{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Ward_ID")
	private long wardId;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@OnDelete( action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "AD_Sub_County_ID")
	private MSubCounty subCounty;
	@Column(name = "AD_Ward_UU")
	private String AD_Ward_UU;


}
