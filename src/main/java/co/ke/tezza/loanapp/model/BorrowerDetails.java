package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;
import java.util.Date;

import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BorrowerDetails {
	private long borrowerId;
	private long individualBorrowerId;
	private long groupBorrowerId;
	private long institutionBorrowerId;
	private String borrowerName;
	private String phoneNumber;
	private String email;
	private BorrowerTypeEnum borrowerType;
	private String loanDocumentNo;
	private BigDecimal amountApplied;
	private BigDecimal amountApproved;
	private BigDecimal balance;
	private BigDecimal interestAccrued;
	private BigDecimal penaltyCharged;
	private Date dueDate;
	private int totalActiveloans;
	

}
