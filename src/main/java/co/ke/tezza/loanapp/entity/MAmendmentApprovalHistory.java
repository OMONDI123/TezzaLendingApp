package co.ke.tezza.loanapp.entity;

import lombok.*;
import javax.persistence.*;

import co.ke.tezza.loanapp.enums.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "AD_Amendment_Approval_History")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MAmendmentApprovalHistory extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Amendment_Approval_History_ID")
    private Long approvalHistoryId;

    @Column(name = "AD_Amendment_Approval_History_UU", unique = true, nullable = false)
    private String AD_Amendment_Approval_History_UU=UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_Loan_Amendment_Detail_ID", nullable = false)
    private MLoanAmendmentDetail amendmentRequest;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_Loan_Application_ID", nullable = false)
    private MLoanApplication loan;
    // Approval step information
    @Column(nullable = false)
    private Integer stepNumber;

    


    // Role and user information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_Role_ID", nullable = false)
    private MRoles requiredRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_User_ID", nullable = false)
    private MUser actionedBy;

    // Action details
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalAction action; // APPROVE, REJECT, RETURN, ESCALATE

    @Column(columnDefinition = "TEXT")
    private String comments;

    private Date actionDate;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isEscalated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalated_to_role_id")
    private MRoles escalatedToRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalated_to_user_id")
    private MUser escalatedToUser;

    // Status before and after action
    @Enumerated(EnumType.STRING)
    private DocStatus previousDocStatus;

    @Enumerated(EnumType.STRING)
    private DocStatus newDocStatus;

    @Enumerated(EnumType.STRING)
    private ApprovalStage previousApprovalStage;

    @Enumerated(EnumType.STRING)
    private ApprovalStage newApprovalStage;

    // Time tracking
    private Date receivedDate;
    private Integer processingTimeHours;

    // Digital signature/evidence
    private String digitalSignature;
    private String ipAddress;
    private String userAgent;

    // For return/rejection
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requiresCorrection = false;

    @Column(columnDefinition = "TEXT")
    private String correctionInstructions;

  
}
