package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FeeTypeEnum {
    FIXED("FIXED", "Fixed Amount"),
    PERCENTAGE("PERCENTAGE", "Percentage Based");

    private final String value;
    private final String description;

    FeeTypeEnum(String value, String description) {
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
    public static FeeTypeEnum forValues(@JsonProperty("value") String value,
                                        @JsonProperty("description") String description) {
        for (FeeTypeEnum type : FeeTypeEnum.values()) {
            if (type.value.equalsIgnoreCase(value) || type.description.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}