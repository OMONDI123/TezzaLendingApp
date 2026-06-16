package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum BorrowerTypeEnum {
    INDIVIDUAL("INDIVIDUAL", "Individual Borrower"),
    GROUP("GROUP", "Group Borrower"),
    INSTITUTION("INSTITUTION", "Institution Borrower");

    private final String value;
    private final String description;

    BorrowerTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static BorrowerTypeEnum fromValue(String value) {
        for (BorrowerTypeEnum type : BorrowerTypeEnum.values()) {
            if (type.value.equalsIgnoreCase(value) || type.description.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown borrower type value: " + value);
    }

    @JsonCreator
    public static BorrowerTypeEnum forValues(@JsonProperty("value") String value,
                                             @JsonProperty("description") String description) {
        for (BorrowerTypeEnum type : BorrowerTypeEnum.values()) {
            if (type.value.equalsIgnoreCase(value) || type.description.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
