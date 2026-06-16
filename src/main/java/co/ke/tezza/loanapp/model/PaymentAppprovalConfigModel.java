package co.ke.tezza.loanapp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentAppprovalConfigModel {
	
	private long paymentApprovalConfigId;
	
	private long paymentMethodId;
	private List<ApprovalStepsModel> approvalLevels=new ArrayList<>();
	private int requiredAprrovalSteps; 

}
