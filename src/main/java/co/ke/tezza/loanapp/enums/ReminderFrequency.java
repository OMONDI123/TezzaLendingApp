package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonFormat(shape = JsonFormat.Shape.OBJECT)

public enum ReminderFrequency {
	ONCE("ONCE","Once"),
	DAILY("DAILY", "Daily"), WEEKLY("WEEKLY", "Weekly"),
	SPECIFIC_DAYS("SPECIFIC_DAYS","Specific Day(s)"),
	MONTHLY("MONTHLY", "Monthly"), QUARTERLY("QUARTERLY", "Quarterly"), YEARLY("YEARLY", "Yearly");

	private final String value;
	private final String description;

	ReminderFrequency(String value, String description) {
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
	public static ReminderFrequency forValues(@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (ReminderFrequency freq : ReminderFrequency.values()) {
			if (freq.value.equalsIgnoreCase(value) || freq.description.equalsIgnoreCase(value)) {
				return freq;
			}
		}
		return null;
	}

}
