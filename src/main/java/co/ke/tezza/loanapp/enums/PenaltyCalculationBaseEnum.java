package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PenaltyCalculationBaseEnum {

    PER_DAY("PER_DAY", "Penalty applied per day overdue"),
    PER_WEEK("PER_WEEK", "Penalty applied per week overdue"),
    PER_MONTH("PER_MONTH", "Penalty applied per month overdue"),
    PER_CYCLE("PER_CYCLE", "Penalty applied per cycle missed"),
	ONCE("ONCE", "Once");

    private final String value;
    private final String description;

    PenaltyCalculationBaseEnum(String value, String description) {
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
    public static PenaltyCalculationBaseEnum forValues(@JsonProperty("value") String value,
                                                       @JsonProperty("description") String description) {
        for (PenaltyCalculationBaseEnum base : PenaltyCalculationBaseEnum.values()) {
            if (base.value.equalsIgnoreCase(value) || base.description.equalsIgnoreCase(value)) {
                return base;
            }
        }
        return null;
    }
}
