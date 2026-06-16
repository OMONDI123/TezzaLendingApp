package co.ke.tezza.loanapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanProduct {

	private Long loanProductId;

	private long loanProductConfigurationId;
	private String name;
	private String description;

	private Boolean isDefaultLoanProduct;

}
