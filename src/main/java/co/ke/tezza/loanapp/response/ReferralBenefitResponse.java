package co.ke.tezza.loanapp.response;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ReferralBenefitResponse {
	private long refferalBenefitId;

	private User downLine;
	private BigDecimal amount;

	private boolean claimed;
}
