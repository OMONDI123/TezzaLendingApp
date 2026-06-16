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
public class InstitutionBorrowerResponse {
	private long institutionBorrowerId;

	// Institution Details
	private String institutionName;
	private String registrationNumber;
	private Date registrationDate;
	private String taxId;
	private String externalRefrenceNo;

	// Contact Person
	private String contactPerson;
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
	

	// Business Info
	private String sector;
	private Double annualRevenue;
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

	// Attachments and Contacts
	private Set<NextOfKins> authorizedContacts = new HashSet<>();
	private Set<BorrowerAttachments> attachments = new HashSet<>();
}
