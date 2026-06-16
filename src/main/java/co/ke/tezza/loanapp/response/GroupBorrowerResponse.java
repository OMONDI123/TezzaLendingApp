package co.ke.tezza.loanapp.response;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupBorrowerResponse {
	private long groupBorrowerId;

	// Group Info
	private String groupName;
	private String registrationNumber;
	private Date formationDate;
	private String groupType;
	private String externalRefrenceNo;

	// Contact Info
	private String contactPhone;
	private String contactEmail;

	// Location Info
	private String country;
	private String county;
	private String subCounty;
	private String ward;
	private String physicalAddress;
	private String postalAddress;
	
	private long countryId;
	private long countyId;
	private long subCountyId;
	private long wardId;

	// Meetings & Internal
	private String meetingFrequency;
	private String meetingPlace;
	private String loanOfficer;
	private String notes;

	// Audit/Metadata
	private boolean isActive;
	private String documentNo;
	@Enumerated(EnumType.STRING)
	private DocStatus docStatus;
	@Enumerated(EnumType.STRING)
	private ApprovalStage approvalStage;
	private Date created;
	private Date updated;

	// Group Members & Attachments
	private Set<GroupMembersResponse> members = new HashSet<>();
	private Set<BorrowerAttachments> attachments = new HashSet<>();
}
