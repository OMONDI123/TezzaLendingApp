package co.ke.tezza.loanapp.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import co.ke.tezza.loanapp.enums.CardexStatusEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanCommentResponse {

	  private Long commentId;

	    // Loan reference (foreign key)
	    private LoanApplicationResponse loan;

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
	    private InstallmentResponse installment;
	    
	    private User notesTakenBy;
	    
	    private String priority;
	    private String contactMethod;
	    private Integer callDuration;
	    private BigDecimal promiseAmount;
}
