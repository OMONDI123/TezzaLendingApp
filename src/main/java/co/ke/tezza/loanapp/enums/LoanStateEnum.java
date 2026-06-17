package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LoanStateEnum {
    PENDING_APPROVAL("PENDING_APPROVAL", "Pending Approval"),
    APPROVED("APPROVED", "Approved"),
    REJECTED("REJECTED", "Rejected"),
    OPEN("OPEN", "Open - Active and in good standing"),
    OVERDUE("OVERDUE", "Overdue - Has past due payments"),
    CLOSED("CLOSED", "Closed - Fully repaid"),
    CANCELLED("CANCELLED", "Cancelled - Cancelled before disbursement or during grace period"),
    WRITTEN_OFF("WRITTEN_OFF", "Written Off - Considered uncollectable"),
    REINSTATED("REINSTATED", "Reinstated - Reactivated after being written off");

    private final String value;
    private final String description;

    LoanStateEnum(String value, String description) {
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
    public static LoanStateEnum forValues(@JsonProperty("value") String value,
                                          @JsonProperty("description") String description) {
        for (LoanStateEnum state : LoanStateEnum.values()) {
            if (state.value.equalsIgnoreCase(value) || state.description.equalsIgnoreCase(value)) {
                return state;
            }
        }
        return null;
    }
}