
package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ApprovalAction {
    APPROVE("APPROVE", "Approved", "The request was approved"),
    REJECT("REJECT", "Rejected", "The request was rejected"),
    RETURN("RETURN", "Returned", "The request was returned for correction"),
    ESCALATE("ESCALATE", "Escalated", "The request was escalated"),
    REVIEW("REVIEW", "Under Review", "The request is under review"),
    HOLD("HOLD", "On Hold", "The request is on hold"),
    DELEGATE("DELEGATE", "Delegated", "The request was delegated"),
    WITHDRAW("WITHDRAW", "Withdrawn", "The request was withdrawn");

    private final String value;
    private final String description;
    private final String detailedDescription;

    ApprovalAction(String value, String description, String detailedDescription) {
        this.value = value;
        this.description = description;
        this.detailedDescription = detailedDescription;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public static ApprovalAction fromValue(String value) {
        for (ApprovalAction action : ApprovalAction.values()) {
            if (action.value.equalsIgnoreCase(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown approval action: " + value);
    }

    @JsonCreator
    public static ApprovalAction forValues(@JsonProperty("value") String value,
                                           @JsonProperty("description") String description) {
        for (ApprovalAction action : ApprovalAction.values()) {
            if (action.value.equalsIgnoreCase(value) || action.description.equalsIgnoreCase(description)) {
                return action;
            }
        }
        return null;
    }
}