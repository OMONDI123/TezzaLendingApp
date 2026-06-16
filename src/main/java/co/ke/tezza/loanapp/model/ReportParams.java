package co.ke.tezza.loanapp.model;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import co.ke.tezza.loanapp.enums.ReportType;
import co.ke.tezza.loanapp.util.FlexibleDateDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportParams {
	private long ad_Org_ID;
	@JsonDeserialize(using = FlexibleDateDeserializer.class)
	private Date dateFrom;
	@JsonDeserialize(using = FlexibleDateDeserializer.class)
	private Date dateTo;
	private long individualBorrowerId;
	private long groupBorrowerId;
	private long institutionBorrowerId;
	private long institutionMemberId;
	private long individualMemberId;
	private long groupMemberId;

	private long borrowerId;
	private long memberId;
	private long membershipAccountId;
	private long loanId;
	private ReportType reportType;

}
