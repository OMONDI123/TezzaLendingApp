package co.ke.tezza.loanapp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

import co.ke.tezza.loanapp.enums.CardexStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AD_Loan_Comment")
public class MLoanComment extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Loan_Comment_ID")
    private Long commentId;

    /** Loan reference */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_LoanApplication_ID", nullable = true)
    private MLoanApplication loan;
    
    

    /** Notes or remarks */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Status (FOLLOW_UP, COMPLETED, PENDING, etc.) */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CardexStatusEnum status;

    /** Date the action occurred */
    @Column(name = "ActionDate")
    private LocalDate actionDate;

    /** Call date/time */
    @Column(name = "CallDateTime")
    private LocalDateTime callDateTime;

    /** Next follow-up date/time */
    @Column(name = "NextCallDate")
    private LocalDateTime nextCallDate;

    /** Officer or staff who took the notes */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NotesTakenBy", nullable = false)
    private MUser notesTakenBy;

    /** Related installment (if applicable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AD_Installment_ID")
    private MInstallments installment;
    
    private String priority;
    private String contactMethod;
    private Integer callDuration;
    private BigDecimal promiseAmount;

}
