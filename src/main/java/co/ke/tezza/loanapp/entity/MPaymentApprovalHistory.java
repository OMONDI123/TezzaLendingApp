package co.ke.tezza.loanapp.entity;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import co.ke.tezza.loanapp.enums.ApprovalAction;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Payment_Approval_History")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MPaymentApprovalHistory  extends AuditModel{

	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	@Column(name = "AD_Payment_Approval_History_ID")
	private long approvalHistoryId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AD_Payment_ID", nullable = false)
	private MPayments payment;
	// Approval step information
	@Column(nullable = false)
	private Integer stepNumber;
	@Column(name = "AD_Payment_Approval_History_UU", unique = true, nullable = false)
	private String AD_Payment_Approval_History_UU = UUID.randomUUID().toString();

	// Role and user information
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AD_Role_ID", nullable = false)
	private MRoles requiredRole;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actionedBy", nullable = true)
	private MUser actionedBy;

	// Action details
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ApprovalAction action; // APPROVE, REJECT, RETURN, ESCALATE

	@Column(columnDefinition = "TEXT")
	private String comments;

	private Date actionDate;

	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private Boolean isEscalated = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "escalated_to_role_id")
	private MRoles escalatedToRole;

	// Status before and after action
	@Enumerated(EnumType.STRING)
	private DocStatus previousDocStatus;

	@Enumerated(EnumType.STRING)
	private DocStatus newDocStatus;

	@Enumerated(EnumType.STRING)
	private ApprovalStage previousApprovalStage;

	@Enumerated(EnumType.STRING)
	private ApprovalStage newApprovalStage;

	// Time tracking
	private Date receivedDate;
	private Integer processingTimeHours;

	// Digital signature/evidence
	private String digitalSignature;
	private String ipAddress;
	private String userAgent;
}
