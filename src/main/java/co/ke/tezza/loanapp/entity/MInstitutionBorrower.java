package co.ke.tezza.loanapp.entity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import co.ke.tezza.loanapp.enums.CustomerEligibilityStatus;
import co.ke.tezza.loanapp.model.CreditEligible;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AD_Institution_Borrower")
public class MInstitutionBorrower extends AuditModel implements CreditEligible {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Institution_Borrower_ID")
    private long institutionBorrowerId;

    private String institutionName;
    private String kraPin;
    private String registrationNumber;
    private Date registrationDate;
    private String taxId;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String externalRefrenceNo;

    private long countryId;
    private long countyId;
    private long subCountyId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "AD_Ward_ID")
    private MWard ward;

    private String physicalAddress;
    private String postalAddress;
    private String sector;
    private Double annualRevenue;

    // Attachments
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "AD_Institution_Attachments",
        joinColumns = @JoinColumn(name = "AD_Institution_Borrower_ID"),
        inverseJoinColumns = @JoinColumn(name = "AD_Document_ID"))
    private Set<MDocuments> attachments = new HashSet<>();

    // Next of kin / authorized signatories
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "AD_Institution_Contacts",
        joinColumns = @JoinColumn(name = "AD_Institution_Borrower_ID"),
        inverseJoinColumns = @JoinColumn(name = "AD_Contact_ID"))
    private Set<MNextOfKin> authorizedContacts = new HashSet<>();

    private String notes;
    
    @ManyToOne
    @JoinColumn(name="AD_User_Representative_ID")
    private MUser representative;
    
    @Enumerated(EnumType.STRING)
    private CustomerEligibilityStatus eligibilityStatus = CustomerEligibilityStatus.ELIGIBLE;
    private String eligibilityReason;
    private Date lastEligibilityReviewDate;
    private BigDecimal creditScore;
    private BigDecimal creditLimit;
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean communicationOptOut;
	
}
