package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CustomerEligibilityStatus {
    ELIGIBLE("ELIGIBLE", "Eligible", "Customer is eligible for loans and services"),
    UNDER_REVIEW("UNDER_REVIEW", "Under Review", "Customer is under review for eligibility"),
    RESTRICTED("RESTRICTED", "Restricted", "Customer has restricted access to services"),
    BLACKLISTED("BLACKLISTED", "Blacklisted", "Customer is blacklisted and cannot access services");

    private final String value;
    private final String description;
    private final String detailedDescription;

    CustomerEligibilityStatus(String value, String description, String detailedDescription) {
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

    public static CustomerEligibilityStatus fromValue(String value) {
        for (CustomerEligibilityStatus status : CustomerEligibilityStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown customer eligibility status: " + value);
    }

    @JsonCreator
    public static CustomerEligibilityStatus forValues(@JsonProperty("value") String value,
                                                      @JsonProperty("description") String description) {
        for (CustomerEligibilityStatus status : CustomerEligibilityStatus.values()) {
            if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(description)) {
                return status;
            }
        }
        return null;
    }
}