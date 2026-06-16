package co.ke.tezza.loanapp.entity;

import lombok.*;
import javax.persistence.*;

import co.ke.tezza.loanapp.enums.UserRestrictionType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AD_User_Restriction")
public class MUserRestriction extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_User_Restriction_ID")
    private Long restrictionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "AD_Restricted_User_ID")
    private MUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "Restriction_Type")
    private UserRestrictionType restrictionType;

    @Column(name = "Reason")
    private String reason;

    @Column(name = "Imposed_At")
    private String imposedAt;

    @Column(name = "Lifted_At")
    private String liftedAt;

   
}
