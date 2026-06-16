package co.ke.tezza.loanapp.response;

import java.util.Date;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.RelationShipEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NextOfKins {
	private Long nextOfKinId;

	private String fullName;
	@Enumerated(EnumType.STRING)
	private RelationShipEnum relationship;

	private String phoneNumber;
	private String address;
    
	private boolean isActive;
	private String documentNo;
	@Enumerated(EnumType.STRING)
	private DocStatus docStatus;
	@Enumerated(EnumType.STRING)
	private ApprovalStage approvalStage;
	private Date created;
	private Date updated;
	private boolean primaryGuarantor;
	private String email;
	private String nationalId;

}
