package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum InterestCalculationMethodEnum {
    FLAT("FLAT", "Flat Rate"),
    DECLINING_BALANCE("DECLINING_BALANCE", "Declining Balance (Equal Principal-Payments reduce)"),
    DECLINING_BALANCE_EMI("DECLINING_BALANCE_EMI", "Declining Balance (EMI-Payments are equal all through)"),
    CYCLE_BASED("CYCLE_BASED","Cycle Based"),
    SIMPLE_INTEREST("SIMPLE_INTEREST","Simple Interest"),
    COMPOUND("COMPOUND", "Compound");

    private final String value;
    private final String description;

    InterestCalculationMethodEnum(String value, String description) {
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
    public static InterestCalculationMethodEnum forValues(@JsonProperty("value") String value,
                                                          @JsonProperty("description") String description) {
        for (InterestCalculationMethodEnum method : InterestCalculationMethodEnum.values()) {
            if (method.value.equalsIgnoreCase(value) || method.description.equalsIgnoreCase(value)) {
                return method;
            }
        }
        return null;
    }
}
