package co.ke.tezza.loanapp.entity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AD_Group_Borrower")
public class MGroupDebtors extends AuditModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Group_Borrower_ID")
    private long groupBorrowerId;

    private String groupName;
    private String kraPin;
    private String registrationNumber;
    private String externalRefrenceNo;
    private Date formationDate;
    private String groupType; // e.g. Self-help, Welfare, Chama
    private String contactPhone;
    private String contactEmail;

    private long countryId;
    private long countyId;
    private long subCountyId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "AD_Ward_ID")
    private MWard ward;
    
    @ManyToOne
    @JoinColumn(name = "AD_User_ID", nullable = false)
    private MUser groupRepresentative;

    private String meetingFrequency;
    private String meetingPlace;

    private String postalAddress;
    private String physicalAddress;

    // Group Members
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "AD_Group_Members",
        joinColumns = @JoinColumn(name = "AD_Group_Borrower_ID"),
        inverseJoinColumns = @JoinColumn(name = "AD_Group_Member_ID"))
    private Set<MGroupMembers> members = new HashSet<>();

    // Attachments
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "AD_Group_Attachments",
        joinColumns = @JoinColumn(name = "AD_Group_Borrower_ID"),
        inverseJoinColumns = @JoinColumn(name = "AD_Document_ID"))
    private Set<MDocuments> attachments = new HashSet<>();

    private String loanOfficer;
    private String notes;
}
