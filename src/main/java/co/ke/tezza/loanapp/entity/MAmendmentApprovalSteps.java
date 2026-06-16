package co.ke.tezza.loanapp.entity;

import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import co.ke.tezza.loanapp.enums.*;

@Entity
@Table(name = "AD_Amendment_Approval_Steps")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MAmendmentApprovalSteps extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    
    private Integer stepNumber;
    
    // Role required for this approval step
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "AD_Role_ID", nullable = false)
    private MRoles requiredRole;
    
    // Next role in sequence (optional)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "AD_Next_Role_ID")
    private MRoles nextRole;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    
    private Boolean requireDigitalSignature = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireDocumentReview = false;
    
    @Enumerated(EnumType.STRING)
    private DocStatus trigureStatus;
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinTable(name = "AD_Responsible_Users", joinColumns = @JoinColumn(name = "AD_Approval_Step_ID"), inverseJoinColumns = @JoinColumn(name = "AD_User_ID"))
	private Set<MUser> responsiblePersons=new HashSet<>();
    
   
    
   
}
