package co.ke.tezza.loanapp.entity;

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

import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import co.ke.tezza.loanapp.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AD_Message_center")
public class MMessagingCenter extends AuditModel {
	@Column(name = "AD_Message_center_ID")
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long messagingId;

	@Column(columnDefinition = "TEXT")
	private String message;
	private String phoneNumber;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private long individualBorrowerId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private long groupBorrowerId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private long institutionBorrowerId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private long membershipAccountId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private long individualMemberId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private long institutionMemberId;
	@Column(columnDefinition = "BIGINT DEFAULT 0")
	private long groupMemberShipId;
	
	private String receiverName;
	private String receiverEmail;
	@Enumerated(EnumType.STRING)
	private BorrowerTypeEnum borrowerType;
	
	private Date messagingTime;
	@Enumerated(EnumType.STRING)
	private MessageStatus messageStatus;
	private long receiverId;
	private String filepath;
	private String AD_Message_center_UU=UUID.randomUUID().toString();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
