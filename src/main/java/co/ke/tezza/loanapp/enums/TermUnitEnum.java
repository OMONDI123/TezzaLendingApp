package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TermUnitEnum {
    DAYS("DAYS", "Days"),
    WEEKS("WEEKS", "Weeks"),
    MONTHS("MONTHS", "Months"),
    YEARS("YEARS","Years");

    private final String value;
    private final String description;

    TermUnitEnum(String value, String description) {
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
    public static TermUnitEnum forValues(@JsonProperty("value") String value,
                                         @JsonProperty("description") String description) {
        for (TermUnitEnum unit : TermUnitEnum.values()) {
            if (unit.value.equalsIgnoreCase(value) || unit.description.equalsIgnoreCase(value)) {
                return unit;
            }
        }
        return null;
    }
}
