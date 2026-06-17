package co.ke.tezza.loanapp.entity;

import java.math.BigDecimal;
import java.sql.Blob;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import co.ke.tezza.loanapp.enums.CustomerEligibilityStatus;
import co.ke.tezza.loanapp.model.CreditEligible;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AD_Debtor")
public class MDebtor extends AuditModel implements CreditEligible{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Debtor_ID")
	private long individualBorrowerId;

	// Step 1: Personal Information
	private String firstName;
	private String kraPin;
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

	@ManyToOne(fetch = FetchType.EAGER)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "AD_Ward_ID")
	private MWard ward;

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
	@ManyToOne(optional = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "Assignee_ID")
	private MUser loanOfficer;
	private String referralSource;
	private String riskRating;
	@Column(columnDefinition = "TEXT")
	private String notes;

	@OneToOne
	@JoinColumn(name = "AD_User_ID")
	private MUser user;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean groupRepresentative;

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(name = "AD_Borrower_NextOfKins", joinColumns = @JoinColumn(name = "AD_Individual_Borrower_ID"), inverseJoinColumns = @JoinColumn(name = "AD_NextOfKin_ID"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<MNextOfKin> borrowerNextOfKins = new HashSet<>();

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(name = "AD_Borrower_Attachments", joinColumns = @JoinColumn(name = "AD_Individual_Borrower_ID"), inverseJoinColumns = @JoinColumn(name = "AD_Document_ID"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<MDocuments> borrowerAttachments = new HashSet<>();
	@Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
	private boolean eligibleToPay;

	@Enumerated(EnumType.STRING)
	private CustomerEligibilityStatus eligibilityStatus = CustomerEligibilityStatus.ELIGIBLE;
	private String eligibilityReason;
	private Date lastEligibilityReviewDate;
	private BigDecimal creditScore;
	private BigDecimal creditLimit;
	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean communicationOptOut;

}
