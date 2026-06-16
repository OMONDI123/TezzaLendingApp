package co.ke.tezza.loanapp.entity;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AD_WF_Email")
public class MWFMail  {


	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wfmail_generator")
	@SequenceGenerator(name = "wfmail_id_generator", sequenceName = "wfmail_seq", allocationSize = 50)
	@Column(name = "AD_WF_Email_ID", updatable = false, nullable = false)
	private long wfmailId;

	@NotNull
	@Size(max = 30000)
	@Column(name = "mailFrom")
    private String mailFrom;
 
	
	@Size(max = 30000)

	@Column(name = "mailTo")
    private String mailTo;
 
	@Size(max = 30000)

	@Column(name = "mailCc")
    private String mailCc;
	
	@Size(max = 30000)

	@Column(name = "mailBcc")
    private String mailBcc;
 
	@Size(max = 30000)

	@Column(name = "mailSubject")
    private String mailSubject;
	@Size(max = 30000)


	@Column(name = "mailContent")
    private String mailContent;
	@Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime timeToSend;
	@Size(max = 30000)

	@Column(name = "contentType")
    private String contentType;
	@Column(columnDefinition = "TEXT DEFAULT NULL")
	private String attachmentName;
	@Column(columnDefinition = "TEXT DEFAULT NULL")
	private String filePath;
	@Column(columnDefinition = "TEXT DEFAULT NULL")
	private String fileUrl;
	@Column(name = "AD_WF_Email_UU")
	private String AD_WF_Email_UU =UUID.randomUUID().toString();
	
	@Column(name = "isactive", columnDefinition = "BOOLEAN DEFAULT true")
	private boolean isActive = true;

	@Column(name = "processed", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean processed = false;

	@Column(name = "processing", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean processing = false;

	@Column(name = "isapproved", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean isApproved = false;
	@Column(columnDefinition = "NUMERIC DEFAULT 1")
    private Integer currentApprovalLevel=1;

	@Column(name = "ammend", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean ammend = false;
	@Column(columnDefinition = "TEXT")
	private String amendmentReason;

	@Column(name = "reject", columnDefinition = "BOOLEAN DEFAULT false")
	private boolean reject = false;
	@Column(columnDefinition = "TEXT DEFAULT NULL")
	private String rejectReason;

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
	@Column(updatable = false, name = "createdby", columnDefinition = "NUMERIC DEFAULT 0")
	private Long createdBy = 0L;

	@LastModifiedBy
	@Column(name = "updatedby", columnDefinition = "NUMERIC DEFAULT 0")
	private Long updatedBy = 0L;

	@Column(name = "AD_Org_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT 0")
	private Long adOrgID = 0L;

	@Column(name = "AD_Client_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT 0")
	private Long adClientId = 0L;

	@Column(name = "C_BPartner_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT 0")
	private Long c_BPartner_ID = 0L;

	@Column(name = "description", nullable = true, updatable = true, columnDefinition = "TEXT DEFAULT NULL")
	private String description;

	@Column(name = "name", nullable = true, updatable = true, columnDefinition = "TEXT DEFAULT NULL")
	private String name;

}
