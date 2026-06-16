package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SettingCategoriesEnum {
	SECURITY_SETTINGS("SECURITY_SETTINGS", "Security Settings"),
	GENERAL_SETTINGS("GENERAL_SETTINGS", "General Settings"),
	BIDING_CONFIGURATION("BIDING_CONFIGURATION", "Biding Configurations"),
	CONFIGURATIONS_SETTINGS("CONFIGURATIONS_SETTINGS", "Configurations Settings");
	

	private String value;

	private String description;

	public static SettingCategoriesEnum formValues(String value) {
		for (SettingCategoriesEnum setting : SettingCategoriesEnum.values()) {
			if (setting.value.equals(value)) {
				return setting;
			}
		}
		throw new IllegalArgumentException(value);
	}

	SettingCategoriesEnum(String value, String description) {
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
	public static SettingCategoriesEnum forValues(@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (SettingCategoriesEnum setting : SettingCategoriesEnum.values()) {
			if (setting.value.equalsIgnoreCase(value)) {
				return setting;
			}

			if (setting.description.equalsIgnoreCase(value)) {
				return setting;
			}

			if (description != null) {
				if (setting.description.equalsIgnoreCase(description)) {
					return setting;
				}
				if (setting.description.equalsIgnoreCase(value)) {
					return setting;
				}
			}
		}

		return null;
	}
}
