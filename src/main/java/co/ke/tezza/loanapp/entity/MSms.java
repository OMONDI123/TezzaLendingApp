package co.ke.tezza.loanapp.entity;

import java.math.BigDecimal;
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
import co.ke.tezza.loanapp.enums.MessageStatus;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Sms")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MSms {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Sms_ID")
	private Long smsId;
	@Column(columnDefinition = "TEXT DEFAULT NULL")
	private String message;
	private String phoneNo;
	private String timeToSend;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long individualBorrowerId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long groupBorrowerId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long institutionBorrowerId;
	
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long individualMemberId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long groupMemberId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long institutionMemberId;	
	
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long memberPlanId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long billId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long proformaInvoiceId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long invoiceId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long membershipAccountId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long customerId;
	
	
	private Long reminderId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long installmentId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long loanId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private Long paymentId;
	@Column(name = "guarantor_id",columnDefinition = "BIGINT DEFAULT 0")
	private Long guarantorId;
	@Enumerated(EnumType.STRING)
	private SmsTypeEnum smsType;
	@Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime timesTosend;

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
	@Column(columnDefinition = "BIGINT DEFAULT NULL")
	private Long messageCenterId;
	private String responseCode;

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
	private Long createdBy;

	@LastModifiedBy
	@Column(name = "updatedby")
	private Long updatedBy;

	@Column(name = "AD_Org_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT NULL")
	private Long adOrgID;

	@Column(name = "AD_Client_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT NULL")
	private Long adClientId;

	@Column(name = "C_BPartner_ID", nullable = true, updatable = false, columnDefinition = "NUMERIC DEFAULT NULL")
	private Long c_BPartner_ID;

	@Column(name = "description", nullable = true, updatable = true, columnDefinition = "TEXT DEFAULT NULL")
	private String description;

	@Column(name = "name", nullable = true, updatable = true, columnDefinition = "TEXT DEFAULT NULL")
	private String name;

	private String AD_Sms_UU = UUID.randomUUID().toString();
	@Enumerated(EnumType.STRING)
	private MessageStatus messageStatus; 
	private String reason;
	private BigDecimal totalCost=BigDecimal.ZERO;

}
