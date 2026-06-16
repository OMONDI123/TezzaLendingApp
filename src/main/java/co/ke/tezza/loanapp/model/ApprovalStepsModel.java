package co.ke.tezza.loanapp.model;

import java.util.ArrayList;
import java.util.List;

import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalStepsModel {
	private long id;
	private int steps;
	private long approvalRoleInvolvedId;
	private long nextApprovalRoleId;
	private DocStatus trigureStatus;
	private DocStatus rejectiontrigeredStatus;
	private ApprovalStage approvalStage;
	private List<Long> responsiblePersonIds= new ArrayList<>();
}
