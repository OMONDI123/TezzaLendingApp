package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SystemStatus {
    CLOSED("CLOSE", "Closed"),
    OPEN("OPEN", "Open");
    

    private final String value;
    private final String description;

    SystemStatus(String value, String description) {
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
    public static SystemStatus forValues(@JsonProperty("value") String value,
                                         @JsonProperty("description") String description) {
        for (SystemStatus unit : SystemStatus.values()) {
            if (unit.value.equalsIgnoreCase(value) || unit.description.equalsIgnoreCase(value)) {
                return unit;
            }
        }
        return null;
    }
}
