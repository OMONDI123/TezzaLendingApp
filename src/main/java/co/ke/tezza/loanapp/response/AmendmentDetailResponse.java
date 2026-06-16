package co.ke.tezza.loanapp.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

import co.ke.tezza.loanapp.enums.AmendmentType;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AmendmentDetailResponse {
	private Long amendmentDetailId;

	// Main amendment data fields
	private BigDecimal newPrincipalAmount;

	private BigDecimal newInterestRate;
	private BigDecimal newFlatRateAmount;

	private LoanProductConfigResponse newLoanProduct;

	private Integer newTermInDays;

	private LocalDateTime effectiveDate;
	private String amendmentReason;
	private AmendmentType amendmentType;
	private Long amendmentConfigId;
	private DocStatus docStatus;
	private ApprovalStage approvalStage;
	private Integer currentApprovalLevel;
	private boolean rejected;
	private Date rejectedDate;
	private String rejectionReason;
	

}
