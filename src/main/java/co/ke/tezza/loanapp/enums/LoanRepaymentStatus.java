package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LoanRepaymentStatus {
	
	PENDING("PENDING", "Pending"),
	PARTIALLY_PAID("PARTIALLY_PAID", "Partially Paid"),
	PAID("PAID", "Paid"),
	OVERPAID("OVERPAID", "Overpaid"),
	LATE("LATE", "Late Payment"),
	DEFAULTED("DEFAULTED", "Defaulted"),
	CANCELLED("CANCELLED", "Cancelled"),
	REFUNDED("REFUNDED", "Refunded");

	private final String value;
	private final String description;

	LoanRepaymentStatus(String value, String description) {
		this.value = value;
		this.description = description;
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public static LoanRepaymentStatus fromValue(String value) {
		for (LoanRepaymentStatus status : LoanRepaymentStatus.values()) {
			if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown loan repayment status: " + value);
	}

	@JsonCreator
	public static LoanRepaymentStatus forValues(
			@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (LoanRepaymentStatus status : LoanRepaymentStatus.values()) {
			if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
				return status;
			}
		}
		return null;
	}
}
