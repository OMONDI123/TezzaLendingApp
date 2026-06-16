package co.ke.tezza.loanapp.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Client_Org_Audit")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MUserClientAudit implements Serializable {

    private static final long serialVersionUID = 1L;
	@Column(name = "AD_Client_Org_Audit_ID")
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private long aD_Client_ID;
	private long aD_Org_ID;
	
	@OneToOne
	@JoinColumn(name = "AD_User_ID")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private MUser user;
	
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "ad_user_role_audit", joinColumns = @JoinColumn(referencedColumnName = "ad_user_id"), inverseJoinColumns = @JoinColumn(referencedColumnName = "ad_role_id"))
	private Set<MRoles> roles = new HashSet<>();

}
