package co.ke.tezza.loanapp.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Document")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MDocuments extends AuditModel{
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="AD_Document_ID")
	private long attachmentDocumentId;
	@Column(name = "filepath",nullable = true)
	private String filepath;
	@Column(nullable = true)
	private String actualFilePath;
	@Column(nullable = true)
	private String mimeType;
	@Column(nullable = true)
	private Long fileSize;
	
	@Column(name = "fileName",nullable = true)
	private String fileName;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AD_Attachment_ID")
	private MAttachment attachment;
	
	
	private String colleteralOwner;
	private BigDecimal colleteralValue;
	private Date expiryDate;
	private Integer storageDurationDaysOnLoanCompletion;
	private String colleteralNo;
	
	@Column(name = "AD_Document_UU",nullable = true)
	private String AD_Document_UU;

}
