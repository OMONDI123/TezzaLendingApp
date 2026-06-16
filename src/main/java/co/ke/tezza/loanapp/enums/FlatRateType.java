package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FlatRateType {
    PERCENTAGE_BASED("PERCENTAGE_BASED", "Flate Rate Percentage(%) Based"),
    AMOUNT_BASED("AMOUNT_BASED", "Flat Rate Amount Based");

    private final String value;
    private final String description;

    FlatRateType(String value, String description) {
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
    public static FlatRateType forValues(@JsonProperty("value") String value,
                                                      @JsonProperty("description") String description) {
        for (FlatRateType type : FlatRateType.values()) {
            if (type.value.equalsIgnoreCase(value) || type.description.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
