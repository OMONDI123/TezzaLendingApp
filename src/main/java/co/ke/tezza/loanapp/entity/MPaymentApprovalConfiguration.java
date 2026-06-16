package co.ke.tezza.loanapp.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "AD_Payment_Approval_Config")
@Entity
public class MPaymentApprovalConfiguration extends AuditModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Payment_Approval_Config_ID")
	private long paymentApprovalConfigId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Payment_Method_ID")
	@OnDelete(action = OnDeleteAction.NO_ACTION)
	private MPaymentMethod paymentMethod;

	private int requiredAprrovalSteps;

	private String AD_Payment_Approval_Config_UU = UUID.randomUUID().toString();

	@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinTable(name = "AD_Payment_Approval_Steps", joinColumns = @JoinColumn(name = "AD_Payment_Approval_Config_ID"), inverseJoinColumns = @JoinColumn(name = "AD_Approval_Step_ID"))
	private Set<MApprovalSteps> approvalLevels = new HashSet<>();

}
