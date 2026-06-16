package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PriorityEnum {
	
	LOW("LOW", "Low"),
	MEDIUM("MEDIUM", "Medium"),
	HIGH("HIGH", "High");

	private String value;

	private String description;

	public static PriorityEnum formValues(String value) {
		for (PriorityEnum priority : PriorityEnum.values()) {
			if (priority.value.equals(value)) {
				return priority;
			}
		}
		throw new IllegalArgumentException(value);
	}

	PriorityEnum(String value, String description) {
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
	public static PriorityEnum forValues(@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (PriorityEnum priority : PriorityEnum.values()) {
			if (priority.value.equalsIgnoreCase(value)) {
				return priority;
			}

			if (priority.description.equalsIgnoreCase(value)) {
				return priority;
			}

			if (description != null) {
				if (priority.description.equalsIgnoreCase(description)) {
					return priority;
				}
				if (priority.description.equalsIgnoreCase(value)) {
					return priority;
				}
			}
		}

		return null;
	}

}
