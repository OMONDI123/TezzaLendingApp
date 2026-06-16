package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AmendmentFeeType {
    NONE("NONE", "No Fee", "No amendment fee charged"),
    FLAT_AMOUNT("FLAT_AMOUNT", "Flat Amount", "Fixed fee amount"),
    PERCENTAGE("PERCENTAGE", "Percentage", "Percentage of amendment amount"),
    BOTH("BOTH", "Both", "Flat amount plus percentage"),
    TIERED("TIERED", "Tiered", "Tiered fee structure based on amount"),
    VARIABLE("VARIABLE", "Variable", "Variable fee based on conditions");

    private final String value;
    private final String description;
    private final String detailedDescription;

    AmendmentFeeType(String value, String description, String detailedDescription) {
        this.value = value;
        this.description = description;
        this.detailedDescription = detailedDescription;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public static AmendmentFeeType fromValue(String value) {
        for (AmendmentFeeType type : AmendmentFeeType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown amendment fee type: " + value);
    }

    @JsonCreator
    public static AmendmentFeeType forValues(@JsonProperty("value") String value,
                                             @JsonProperty("description") String description) {
        for (AmendmentFeeType type : AmendmentFeeType.values()) {
            if (type.value.equalsIgnoreCase(value) || type.description.equalsIgnoreCase(description)) {
                return type;
            }
        }
        return null;
    }
}