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
public class IndividualBorrowerModel {
	private long individualBorrowerId;

	// Step 1: Personal Information
	private String firstName;
	private String middleName;
	private String lastName;
	private String gender;
	private Date dob;
	private String maritalStatus;
	private String educationLevel;
	private String nationalId;
	private String externalRefrenceNo;

	// Step 2: Contact Information
	private String phone;
	private String email;
	private long countryId;
	private long countyId;
	private long subCountyId;
	private long wardId;
	private String location;
	private String physicalAddress;
	private String postalAddress;

	// Step 3: Employment / Income Info
	private String employmentStatus;
	private String occupation;
	private String employer;
	private Double monthlyIncome;
	private String otherIncome;
	private boolean groupRepresentative;

	// Step 6: Internal Details
	private long loanOfficerId;
	private String referralSource;
	private String riskRating;
	private String notes;
	private Set<NextOfKins> borrowerNextOfKins =new HashSet<>();
	private Set<BorrowerAttachments> borrowerAttachments =new HashSet<>();
	private boolean eligibleToPay;
	
		
	

}
