package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the type of debt configuration and how interest behaves.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DebtTypeEnum {

    FLAT_RATE("FLAT_RATE", "No interest, even when overdue"),
    INTEREST_WHEN_OVERDUE("INTEREST_WHEN_OVERDUE", "Accrues interest only if overdue"),
    INTERESTED("INTERESTED", "Has normal interest throughout the term");

    private final String value;
    private final String description;

    DebtTypeEnum(String value, String description) {
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
    public static DebtTypeEnum fromValue(@JsonProperty("value") String value,
                                         @JsonProperty("description") String description) {
        for (DebtTypeEnum type : DebtTypeEnum.values()) {
            if (type.value.equalsIgnoreCase(value) || type.description.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown debt type value: " + value);
    }

    public static DebtTypeEnum fromValue(String value) {
        for (DebtTypeEnum type : DebtTypeEnum.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown debt type value: " + value);
    }
}
