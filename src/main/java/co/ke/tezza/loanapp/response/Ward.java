package co.ke.tezza.loanapp.response;

import java.util.Date;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ward {
	private long wardId;
	private String name;
	private SubCounty subCounty;
	private boolean isActive;
	private String documentNo;
	@Enumerated(EnumType.STRING)
	private DocStatus docStatus;
	@Enumerated(EnumType.STRING)
	private ApprovalStage approvalStage;
	private Date created;
	private Date updated;
}
