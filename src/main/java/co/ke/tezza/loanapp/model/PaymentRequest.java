package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
	private long paymentId;
	private BigDecimal amount;
	private long loanId;
	private long billId;
	private String phoneNo;
	private boolean useMpesaPrompt=false;
	private boolean useOtherPhone=false;
	private String paymentDate;
	private LocalDateTime paymentDateTime;
	private String reference;
	private String paymentMethod;
	private boolean securityPayment;
	private LocalDateTime expectedAllocationDate; 
	private long paymentModeId;
	private long paymentReceivedBy;
	private boolean interestOnly=false;
	private boolean penaltiesOnly=false;
	private String writeOffWaiverReason;
	private boolean walletDeposit=false;
	private Long individualBorrowerId;
	private Long institutionBorrowerId;
	private Long groupBorrowerId;
	private Long customerId;
	private Long membershipAccountId;
}
