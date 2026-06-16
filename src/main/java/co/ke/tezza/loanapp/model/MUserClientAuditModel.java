package co.ke.tezza.loanapp.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MUserClientAuditModel {
	
	private long id;
	private long aD_Client_ID;
	private long aD_Org_ID;
	private long roleId;

}
