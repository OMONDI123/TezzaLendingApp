package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonFormat(shape = JsonFormat.Shape.OBJECT)

public enum MessageStatus {
	PENDING("PENDING", "Pending"),
	PROCESSING("PROCESSING", "Processing"),
	SENT("SENT", "Sent"),
	FAILED("FAILED", "Failed"),
	CANCELLED("CANCELLED", "Cancelled"),
	SCHEDULED("SCHEDULED", "Scheduled");

	private final String value;
	private final String description;

	MessageStatus(String value, String description) {
		this.value = value;
		this.description = description;
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public static MessageStatus fromValue(String value) {
		for (MessageStatus status : MessageStatus.values()) {
			if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown payment status: " + value);
	}

	@JsonCreator
	public static MessageStatus forValues(
			@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (MessageStatus status : MessageStatus.values()) {
			if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
				return status;
			}
		}
		return null;
	}

}
