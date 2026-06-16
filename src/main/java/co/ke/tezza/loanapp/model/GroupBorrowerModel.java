package co.ke.tezza.loanapp.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupBorrowerModel {
	private long groupBorrowerId;

	// Group Details
	private String groupName;
	private String registrationNumber;
	private Date formationDate;
	private String groupType;
	private String externalRefrenceNo;

	// Contact Info
	private String contactPhone;
	private String contactEmail;

	// Address
	private long countryId;
	private long countyId;
	private long subCountyId;
	private long wardId;
	private String physicalAddress;
	private String postalAddress;

	// Meeting Info
	private String meetingFrequency;
	private String meetingPlace;

	// Internal Info
	private String loanOfficer;
	private String notes;

	// Members & Attachments
	private Set<GroupMembers> members = new HashSet<>();
	private Set<BorrowerAttachments> attachments = new HashSet<>();
}
