package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum MessageChannel {
	
	SMS("SMS", "Sms"),
	EMAIL("EMAIL", "Email"),
	BOTH("BOTH", "Both");
	

	private final String value;
	private final String description;

	MessageChannel(String value, String description) {
		this.value = value;
		this.description = description;
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public static MessageChannel fromValue(String value) {
		for (MessageChannel status : MessageChannel.values()) {
			if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown payment status: " + value);
	}

	@JsonCreator
	public static MessageChannel forValues(
			@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (MessageChannel status : MessageChannel.values()) {
			if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
				return status;
			}
		}
		return null;
	}

}
