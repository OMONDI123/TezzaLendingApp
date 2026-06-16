package co.ke.tezza.loanapp.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardexBorrowers {
    private String borrowerName;
    private long borrowerId;
    private long individualBorrowerId;
    private long groupBorrowerId;
    private long institutionBorrowerId;
    private String borrowerType;
    private String phoneNumber;
    private int totalActiveLoans;
    private BigDecimal totalOutstandingBalance;
    private BigDecimal totalApprovedAmount;
    private BigDecimal totalPaidAmount;
    private int totalCardexEntries;
    private String lastCardexDate;
    private String latestFeedback;
    private String borrowerNo;
    private String lastCardexRecordedBy;
}