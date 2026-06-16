package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum MessageForm {

	SMS("SMS", "Sms"), EMAIL("EMAIL", "Email"), BOTH("BOTH", "Both");

	private final String value;
	private final String description;

	MessageForm(String value, String description) {
		this.value = value;
		this.description = description;
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	@JsonCreator
	public static MessageForm forValues(@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (MessageForm base : MessageForm.values()) {
			if (base.value.equalsIgnoreCase(value) || base.description.equalsIgnoreCase(value)) {
				return base;
			}
		}
		return null;
	}
}
