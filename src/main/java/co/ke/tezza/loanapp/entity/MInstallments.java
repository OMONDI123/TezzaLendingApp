package co.ke.tezza.loanapp.entity;

import java.math.BigDecimal;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Installment")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MInstallments {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Installment_ID")
	private long installmentId;
	private BigDecimal amount;
	private BigDecimal balance;
	private BigDecimal interestEarned;
	private BigDecimal penaltyEarned;
	private Integer noOfRemindersSent;
	private Integer installmentNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_LoanApplication_ID", nullable = false)
	private MLoanApplication loan;
	private Date periodStart;
	private Date periodEnd;
	private Integer gracePeriod;
	private Date lastReminderSent;
	private BigDecimal paidAmount;
	private Date penaltyStartDate;
	private Date lastPenaltyCalculationDate;
	private Date nextPenaltyCalculationDate;
	private BigDecimal cummulatedAmount;
	private BigDecimal exemptedAmount=BigDecimal.ZERO;
    private BigDecimal exemptedInterests=BigDecimal.ZERO;
    private BigDecimal exemptedPenalties=BigDecimal.ZERO;
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean exempted;
	
	
	
	
	
	

	
	@Column(name = "isactive", columnDefinition = "BOOLEAN DEFAULT true")
	private boolean isActive = true;

	@Column(name = "processed", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean processed = false;

	@Column(name = "processing", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean processing = false;

	@Column(name = "isapproved", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean isApproved = false;

	@Column(name = "ammend", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean ammend = false;

	@Column(name = "reject", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean reject = false;

	@Column(name = "documentno")
	private String documentNo;

	@Column(name = "docstatus")
	@Enumerated(EnumType.STRING)
	private DocStatus docStatus = DocStatus.DRAFT;

	@Column(name = "approvalstage")
	@Enumerated(EnumType.STRING)
	private ApprovalStage approvalStage = ApprovalStage.DRAFT;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "approval_date", nullable = true, updatable = true, columnDefinition = "TIMESTAMP DEFAULT NULL")
	private Date approvalDate;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "rejected_date", nullable = true, updatable = true, columnDefinition = "TIMESTAMP DEFAULT NULL")
	private Date rejectedDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	@CreatedDate
	@CreationTimestamp
	private Date created;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "updated", nullable = false, updatable = true, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	@LastModifiedDate
	@UpdateTimestamp
	private Date updated;

	@CreatedBy
	@Column(updatable = false, name = "createdby")
	private long createdBy;

	@LastModifiedBy
	@Column(name = "updatedby")
	private long updatedBy;

	@Column(name = "AD_Org_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT NULL")
	private long adOrgID;

	@Column(name = "AD_Client_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT NULL")
	private long adClientId;

	@Column(name = "C_BPartner_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT NULL")
	private long c_BPartner_ID;

	@Column(name = "description", nullable = true, updatable = true, columnDefinition = "TEXT DEFAULT NULL")
	private String description;

	@Column(name = "name", nullable = true, updatable = true, columnDefinition = "TEXT DEFAULT NULL")
	private String name;
	
	private String AD_Installment_UU=UUID.randomUUID().toString();
	

}
