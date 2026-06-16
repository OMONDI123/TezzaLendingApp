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
public class IndividualBorrowerResponse {
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
	private String county;
	private String subCounty;
	private String ward;
	private long wardId;
	private long subCountyId;
	private long countyId;
	private long countryId;
	private String location;
	private String physicalAddress;
	private String postalAddress;

	// Step 3: Employment / Income Info
	private String employmentStatus;
	private String occupation;
	private String employer;
	private Double monthlyIncome;
	private String otherIncome;

	// Step 6: Internal Details
	private User loanOfficer;
	private String referralSource;
	private String riskRating;
	private String notes;
	private boolean isActive;
	private String documentNo;
	@Enumerated(EnumType.STRING)
	private DocStatus docStatus;
	@Enumerated(EnumType.STRING)
	private ApprovalStage approvalStage;
	private Date created;
	private Date updated;
	private Set<NextOfKins> borrowerNextOfKins =new HashSet<>();
	private Set<BorrowerAttachments> borrowerAttachments =new HashSet<>();
	private UserResponse user;
	private boolean groupRepresentative;
	private boolean eligibleToPay;
		
	

}
