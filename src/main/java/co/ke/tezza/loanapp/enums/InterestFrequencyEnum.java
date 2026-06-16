package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum InterestFrequencyEnum {
    DAILY("DAILY", "Daily"),
    WEEKLY("WEEKLY", "Weekly"),
    MONTHLY("MONTHLY", "Monthly"),
	YEARLY("YEARLY","Yearly");

    private final String value;
    private final String description;

    InterestFrequencyEnum(String value, String description) {
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
    public static InterestFrequencyEnum forValues(@JsonProperty("value") String value,
                                                  @JsonProperty("description") String description) {
        for (InterestFrequencyEnum freq : InterestFrequencyEnum.values()) {
            if (freq.value.equalsIgnoreCase(value) || freq.description.equalsIgnoreCase(value)) {
                return freq;
            }
        }
        return null;
    }
}
