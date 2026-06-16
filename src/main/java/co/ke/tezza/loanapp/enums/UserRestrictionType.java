package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum UserRestrictionType {
    NO_BIDDING("NO_BIDDING", "Restricted from bidding"),
    ACCOUNT_HOLD("ACCOUNT_HOLD", "Account hold");

    private final String value;
    private final String description;

    UserRestrictionType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }

    @JsonCreator
    public static UserRestrictionType forValues(@JsonProperty("value") String value,
                                                @JsonProperty("description") String description) {
        for (UserRestrictionType type : values()) {
            if (type.value.equalsIgnoreCase(value)) return type;
        }
        return null;
    }
}
