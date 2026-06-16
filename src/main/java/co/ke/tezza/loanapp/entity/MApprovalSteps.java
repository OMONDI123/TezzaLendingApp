package co.ke.tezza.loanapp.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "AD_Approval_Steps")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MApprovalSteps extends AuditModel{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private int step;
	@Enumerated(EnumType.STRING)
	private DocStatus trigureStatus;
	
	@Enumerated(EnumType.STRING)
	private DocStatus rejectiontrigeredStatus;
	@Enumerated(EnumType.STRING)
	private DocStatus previousStatus;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Role_ID")
	private MRoles roleInvolved;
	
	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Next_Role_ID")
	private MRoles nextRoleinvolved;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_LoanProductConfiguration_ID",nullable = true)
	private MLoanProductConfiguration loanConfiguration;
	@Enumerated(EnumType.STRING)
	private ApprovalStage previousApprovalStage;
	
	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinTable(name = "AD_Responsible_Users", 
	           joinColumns = @JoinColumn(name = "AD_Approval_Step_ID"), 
	           inverseJoinColumns = @JoinColumn(name = "AD_User_ID"))
	private Set<MUser> responsiblePersons = new HashSet<>();

}
