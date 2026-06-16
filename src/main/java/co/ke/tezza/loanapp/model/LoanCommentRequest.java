package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import co.ke.tezza.loanapp.enums.CardexStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanCommentRequest {

    private Long commentId;

    // Loan reference (foreign key)
    private Long loanId;
    
    private Long membershipAccountId;

    // Notes text
    private String notes;

    // Status (FOLLOW_UP, COMPLETED, PENDING, etc.)
    private CardexStatusEnum status;

    // Action date
    private LocalDate actionDate;

    // Call date/time
    private LocalDateTime callDateTime;

    // Next talk date/time
    private LocalDateTime nextCallDate;

    // Installment reference (foreign key)
    private Long installmentId;

    // Priority level (e.g., LOW, MEDIUM, HIGH)
    private String priority;

    // Contact method (e.g., PHONE, EMAIL, SMS)
    private String contactMethod;

    // Call duration in minutes (nullable)
    private Integer callDuration;

    // Promise amount (nullable)
    private BigDecimal promiseAmount;
}
