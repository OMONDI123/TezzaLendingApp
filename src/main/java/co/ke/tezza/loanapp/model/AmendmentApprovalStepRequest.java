package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import co.ke.tezza.loanapp.enums.*;
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AmendmentApprovalStepRequest {
	private Long id;
	private Integer stepNumber;
	private Long approvalRoleId;
	private ApprovalStage approvalStage;
	private DocStatus trigureStatus;
    private Boolean requireDocumentReview = false;
    private Boolean requireDigitalSignature = false;
    private Long nextApprovalRoleId;
    private List<Long> responsiblePersonIds= new ArrayList<>();
	
	

   
}