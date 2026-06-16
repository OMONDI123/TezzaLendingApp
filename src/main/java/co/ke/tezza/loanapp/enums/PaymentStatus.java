package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PaymentStatus {
	
	PENDING("PENDING", "Pending"),
	PROCESSING("PROCESSING", "Processing"),
	SUCCESS("SUCCESS", "Success"),
	FAILED("FAILED", "Failed"),
	CANCELLED("CANCELLED", "Cancelled"),
	REFUNDED("REFUNDED", "Refunded"),
	PARTIALLY_PAID("PARTIALLY_PAID", "Partially Paid"),
	OVERPAID("OVERPAID", "Overpaid");

	private final String value;
	private final String description;

	PaymentStatus(String value, String description) {
		this.value = value;
		this.description = description;
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public static PaymentStatus fromValue(String value) {
		for (PaymentStatus status : PaymentStatus.values()) {
			if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown payment status: " + value);
	}

	@JsonCreator
	public static PaymentStatus forValues(
			@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (PaymentStatus status : PaymentStatus.values()) {
			if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
				return status;
			}
		}
		return null;
	}
}
