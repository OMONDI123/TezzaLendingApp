package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TenureTypeEnum {
    FIXED("FIXED", "Fixed Tenure"),
    FLEXIBLE("FLEXIBLE", "Flexible Tenure");

    private final String value;
    private final String description;

    TenureTypeEnum(String value, String description) {
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
    public static TenureTypeEnum forValues(@JsonProperty("value") String value,
                                           @JsonProperty("description") String description) {
        for (TenureTypeEnum type : TenureTypeEnum.values()) {
            if (type.value.equalsIgnoreCase(value) || type.description.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}