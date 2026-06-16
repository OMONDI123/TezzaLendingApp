package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ApprovalStage {
	DRAFT("DRAFT", "Draft"), SUBMITTED("SUBMITTED", "Submitted"), APPROVED("APPROVED", "Approved"),
	REJECTED("REJECTED", "Rejected"), DISBURSED("DISBURSED", "Disbursed"), CANCELLED("CANCELLED", "Cancelled"),
	PENDING_ALLOCATION("PENDING_ALLOCATION","Pending Allocation"),
	COMPLETED("COMPLETED","Completed"),
	AMENDED("AMENDED","Amended"),
	OPEN("OPEN", "Open - Active and in good standing"),
    OVERDUE("OVERDUE", "Overdue - Has past due payments"),
    CLOSED("CLOSED", "Closed - Fully repaid"),
    WRITTEN_OFF("WRITTEN_OFF", "Written Off - Considered uncollectable"),
    REINSTATED("REINSTATED", "Reinstated - Reactivated after being written off"),
	INITIATED("INITIATED", "initiated");

	private final String value;
	private final String description;

	ApprovalStage(String value, String description) {
		this.value = value;
		this.description = description;
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public static ApprovalStage fromValue(String value) {
		for (ApprovalStage status : ApprovalStage.values()) {
			if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown loan application status: " + value);
	}

	@JsonCreator
	public static ApprovalStage forValues(@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (ApprovalStage status : ApprovalStage.values()) {
			if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(value)) {
				return status;
			}
		}
		return null;
	}
}
