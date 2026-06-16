package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ReferralrewardTypeEnum {
    FLAT_AMOUNT("FLAT_AMOUNT", "Flat Amount"),
    PERCENTAGE("PERCENTAGE", "Percentage %");
    

    private final String value;
    private final String description;

    ReferralrewardTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static ReferralrewardTypeEnum fromValue(String value) {
        for (ReferralrewardTypeEnum type : ReferralrewardTypeEnum.values()) {
            if (type.value.equalsIgnoreCase(value) || type.description.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown borrower type value: " + value);
    }

    @JsonCreator
    public static ReferralrewardTypeEnum forValues(@JsonProperty("value") String value,
                                             @JsonProperty("description") String description) {
        for (ReferralrewardTypeEnum type : ReferralrewardTypeEnum.values()) {
            if (type.value.equalsIgnoreCase(value) || type.description.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
