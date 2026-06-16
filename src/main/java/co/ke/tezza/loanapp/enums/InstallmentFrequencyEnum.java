package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum InstallmentFrequencyEnum {
	DAILY("DAILY","Daily"),
    WEEKLY("WEEKLY", "Weekly"),
    BIWEEKLY("BIWEEKLY", "Bi-Weekly"),
    MONTHLY("MONTHLY", "Monthly"),
    QUARTERLY("QUARTERLY","Quarterly"),
    YEARLY("YEARLY","Yearly"),
	UNKNOWN("UNKNOWN","Unknown");

    private final String value;
    private final String description;

    InstallmentFrequencyEnum(String value, String description) {
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
    public static InstallmentFrequencyEnum forValues(@JsonProperty("value") String value,
                                                     @JsonProperty("description") String description) {
        for (InstallmentFrequencyEnum freq : InstallmentFrequencyEnum.values()) {
            if (freq.value.equalsIgnoreCase(value) || freq.description.equalsIgnoreCase(value)) {
                return freq;
            }
        }
        return null;
    }
}
