package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the day of debt configuration and how interest behaves.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Days {

    MONDAY("MONDAY", "Monday"),
    TUESDAY("TUESDAY", "Tuesday"),
    WEDNESDAY("WEDNESDAY", "Wednesday"),
    THURSDAY("THURSDAY", "Thursday"),
    FRIDAY("FRIDAY", "Friday"),
    SATURDAY("SATURDAY", "Saturday"),
    SUNDAY("SUNDAY", "Sunday");

    private final String value;
    private final String description;

    Days(String value, String description) {
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
    public static Days fromValue(@JsonProperty("value") String value,
                                         @JsonProperty("description") String description) {
        for (Days day : Days.values()) {
            if (day.value.equalsIgnoreCase(value) || day.description.equalsIgnoreCase(value)) {
                return day;
            }
        }
        throw new IllegalArgumentException("Unknown debt day value: " + value);
    }

    public static Days fromValue(String value) {
        for (Days day : Days.values()) {
            if (day.value.equalsIgnoreCase(value)) {
                return day;
            }
        }
        throw new IllegalArgumentException("Unknown debt day value: " + value);
    }
}
