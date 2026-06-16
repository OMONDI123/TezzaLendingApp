package co.ke.tezza.loanapp.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.entity.MPaymentMethod;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
	
	private long paymentId;
	private BigDecimal amount;
	private LoanApplicationResponse loan;
	private Set<InstallmentResponse> installments=new HashSet<>();
	private boolean isActive;
	private String paymentDate;
	private LocalDateTime paymentDateTime;
	private String reference;
	@Enumerated(EnumType.STRING)
	private PaymentType paymentMethod;
	private DocStatus docStatus;
	private ApprovalStage approvalStage;
	private Date created;
	private LocalDateTime expectedAllocationDate;
	private boolean securityPayment;
	private MPaymentMethod paymentMode;
	private User approvedBy;
	private User rejectedBy;
	private String reasonForRejection;
	private UserResponse receiptedBy;
	private String writeOffWaiverReason;

}
