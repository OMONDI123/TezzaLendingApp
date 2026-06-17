package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;
import java.util.Date;

import co.ke.tezza.loanapp.enums.CustomerEligibilityStatus;

public interface CreditEligible {
	CustomerEligibilityStatus getEligibilityStatus();

	void setEligibilityStatus(CustomerEligibilityStatus status);

	String getEligibilityReason();

	void setEligibilityReason(String reason);

	BigDecimal getCreditLimit();

	void setCreditLimit(BigDecimal limit);

	void setCreditScore(BigDecimal score);

	void setLastEligibilityReviewDate(Date today);

	boolean isCommunicationOptOut();

}
