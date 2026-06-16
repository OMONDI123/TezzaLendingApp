package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CardexStatusEnum {

    FOLLOW_UP("FOLLOW_UP", "Follow Up"),
    COMPLETED("COMPLETED", "Completed"),
    PENDING("PENDING", "Pending"),
    MISSED_CALL("MISSED_CALL", "Missed Call"),
    PROMISE_TO_PAY("PROMISE_TO_PAY", "Promise To Pay"),
    CLOSED("CLOSED", "Closed"),
    UNKNOWN("UNKNOWN", "Unknown");

    private final String value;
    private final String description;

    CardexStatusEnum(String value, String description) {
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
    public static CardexStatusEnum forValues(@JsonProperty("value") String value,
                                             @JsonProperty("description") String description) {
        for (CardexStatusEnum status : CardexStatusEnum.values()) {
            if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
