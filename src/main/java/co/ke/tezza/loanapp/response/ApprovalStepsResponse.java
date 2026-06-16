package co.ke.tezza.loanapp.response;

import java.util.HashSet;
import java.util.Set;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ApprovalStepsResponse {
	private long id;
	private int steps;
	private RoleResponse approvalRoleInvolved;
	private RoleResponse nextRoleinvolved;
	private DocStatus trigureStatus;
	private DocStatus rejectiontrigeredStatus;
	private DocStatus previousStatus;
	private ApprovalStage approvalStage;
	private ApprovalStage previousApprovalStage;
	
	private Set<User> responsiblePersons=new HashSet<>();

}
