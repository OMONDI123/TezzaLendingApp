package co.ke.tezza.loanapp.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "AD_Loan_Amendment_Request")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MLoanAmendmentRequest extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Loan_Amendment_Request_ID")
    private Long amendmentRequestId;

    @Column(name = "AD_Loan_Amendment_Request_UU",  nullable = false)
    private String AD_Loan_Amendment_Request_UU=UUID.randomUUID().toString();

    // ------------------------------------------
    // 1. Basic Information & Tracking
    // ------------------------------------------
   
    @Column(columnDefinition = "TEXT", name = "request_reason")
    private String requestReason;
    
   
    // ------------------------------------------
    // 2. References
    // ------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_LoanApplication_ID", nullable = false)
    private MLoanApplication loanToAmend;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private MUser requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id")
    private MUser processedBy;
    

    // ------------------------------------------
    // 3. Amendments (One-to-Many relationship)
    // ------------------------------------------
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "AD_Request_Amendment_Details",
        joinColumns = @JoinColumn(name = "AD_Loan_Amendment_Request_ID"),
        inverseJoinColumns = @JoinColumn(name = "AD_Loan_Amendment_Detail_ID")
    )
    private List<MLoanAmendmentDetail> amendmentDetails = new ArrayList<>();
}
