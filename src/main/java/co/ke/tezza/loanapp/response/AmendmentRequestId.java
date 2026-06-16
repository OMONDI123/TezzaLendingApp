package co.ke.tezza.loanapp.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AmendmentRequestId {
	private long amendmentRequestId;
	private long amendmentDetailId;

}
