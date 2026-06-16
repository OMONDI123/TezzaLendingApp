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
public class InstitutionBorrowerModel {
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

	// Address
	private long countryId;
	private long countyId;
	private long subCountyId;
	private long wardId;
	private String physicalAddress;
	private String postalAddress;

	// Additional Info
	private String sector;
	private Double annualRevenue;
	private String notes;

	// Attachments & Contacts
	private Set<BorrowerAttachments> attachments = new HashSet<>();
	private Set<NextOfKins> authorizedContacts = new HashSet<>();
}
