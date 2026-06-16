package co.ke.tezza.loanapp.response;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;

import co.ke.tezza.loanapp.enums.DocStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanAmendmentApprovalStepsResponse {
	private Long id;

	@Column(nullable = false)

	private Integer stepNumber;

	private RoleResponse requiredRole;

	private RoleResponse nextRole;

	private Boolean requireDigitalSignature = false;

	private Boolean requireDocumentReview = false;

	private DocStatus trigureStatus;
	
	private Set<User> responsiblePersons=new HashSet<>();
}
