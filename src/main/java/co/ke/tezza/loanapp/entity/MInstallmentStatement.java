package co.ke.tezza.loanapp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

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
import co.ke.tezza.loanapp.enums.InstallmentTransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Installment_Statement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MInstallmentStatement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Installment_Statement_ID")
	private Long statementId;

	// Borrower-related info
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AD_Installment_ID")
	private MInstallments installment;
	
	@Column(name = "payment_id", nullable = true)
	private Long paymentId;

	// Transaction Details
	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false, length = 50)
	private InstallmentTransactionType transactionType;
	// e.g. LOAN_DISBURSED, INTEREST_ACCRUED, PENALTY, REPAYMENT, ADJUSTMENT

	@Column(name = "transaction_ref", length = 100)
	private String transactionRef; // e.g. Payment ID or system reference

	@Column(name = "transaction_date", nullable = false)
	private LocalDateTime transactionDate;

	@Column(name = "actual_payment_date", nullable = true)
	private LocalDateTime actualPaymentDate;
	@Column(name = "actual_date", nullable = true)
	private LocalDateTime actualDate;

	@Column(name = "description", length = 255)
	private String description;

	// Financials
	@Column(name = "debit_amount", precision = 19, scale = 2)
	private BigDecimal debitAmount = BigDecimal.ZERO; // Increases balance

	@Column(name = "credit_amount", precision = 19, scale = 2)
	private BigDecimal creditAmount = BigDecimal.ZERO; // Decreases balance

	@Column(name = "balance", precision = 19, scale = 2)
	private BigDecimal balance = BigDecimal.ZERO; // Running balance after transaction

	// Optional details
	@Column(name = "is_reversed")
	private Boolean isReversed = false;

	@Column(name = "notes", length = 255)
	private String notes;

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

	@Column(name = "name", nullable = true, updatable = true, columnDefinition = "TEXT DEFAULT NULL")
	private String name;

}
