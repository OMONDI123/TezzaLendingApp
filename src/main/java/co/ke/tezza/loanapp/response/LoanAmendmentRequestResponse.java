package co.ke.tezza.loanapp.response;

import java.util.ArrayList;
import java.util.List;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoanAmendmentRequestResponse {
	private Long amendmentRequestId;
	    private LoanApplicationResponse loanToAmend;
	     
	    private String requestReason;
	    private User requestedBy;
	    private User processedBy;
	    private DocStatus docStatus;
	    private ApprovalStage approvalStage;
	    
	    private List<AmendmentDetailResponse> amendments = new ArrayList<>();

}
