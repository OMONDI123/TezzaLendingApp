package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonFormat(shape = JsonFormat.Shape.OBJECT)

public enum DocStatus {
	DRAFT("DRAFT", "Draft"), COMPLETED("COMPLETED", "Completed"), SUBMITTED("SU", "Submitted"),
	UNDER_REVIEW("UR", "Under Review"), APPROVED("AP", "Approved"), REJECTED("RE", "Rejected"), VOIDED("VO", "Voided"),
	CANCELLED("CA", "Cancelled"), RETURNED("RT", "Returned for Correction"), REVISED("RV", "Revised Version"),
	IN_PROGRESS("IP", "In Progress"), VERIFIED("VE", "Verified"), ARCHIVED("AR", "Archived"), CLOSED("CL", "Closed"),
	POSTED("PO", "Posted"), ERROR("ER", "Errored"), PENDING("PE", "Pending"), SYSTEM("SY", "System Generated"),
	AMENDED("AM","Amended"),
	PARTIALLY_APPROVED("PA","Partially Approved"),
	UNKNOWN("XX", "Unknown"),DR("DR","DR"),PENDING_ALLOCATION("PENDING_ALLOCATION","Pending Allocation"),
	OPEN("OPEN", "Open - Active and in good standing"),
    OVERDUE("OVERDUE", "Overdue - Has past due payments"),
    WRITTEN_OFF("WRITTEN_OFF", "Written Off - Considered uncollectable"),
    REINSTATED("REINSTATED", "Reinstated - Reactivated after being written off"),
	CO("CO","CO");

	private final String value;
	private final String description;

	DocStatus(String value, String description) {
		this.value = value;
		this.description = description;
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public static DocStatus fromValue(String value) {
		for (DocStatus status : DocStatus.values()) {
			if (status.value.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown document status: " + value);
	}

	@JsonCreator
	public static DocStatus forValues(@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (DocStatus status : DocStatus.values()) {
			if (status.value.equalsIgnoreCase(value) || status.description.equalsIgnoreCase(description)) {
				return status;
			}
		}
		return null;
	}
}
