package co.ke.tezza.loanapp.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AmendmentsToBeApprovedResponse {
	private AmendmentDetailResponse currentAmendmentDetail;
	private LoanAmendmentRequestResponse amendmentRequest;

}
