package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FeeTimingEnum {
    ORIGINATION("ORIGINATION", "At Origination"),
    POST_DISBURSEMENT("POST_DISBURSEMENT", "Post Disbursement");

    private final String value;
    private final String description;

    FeeTimingEnum(String value, String description) {
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
    public static FeeTimingEnum forValues(@JsonProperty("value") String value,
                                          @JsonProperty("description") String description) {
        for (FeeTimingEnum timing : FeeTimingEnum.values()) {
            if (timing.value.equalsIgnoreCase(value) || timing.description.equalsIgnoreCase(value)) {
                return timing;
            }
        }
        return null;
    }
}