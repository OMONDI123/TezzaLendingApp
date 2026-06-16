package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ReportType {

	PDF("PDF", "PDF"), EXCEL("EXCEL", "Excel"), CSV("CSV", "CSV");

	private String value;

	private String description;

	public static ReportType formValues(String value) {
		for (ReportType reportType : ReportType.values()) {
			if (reportType.value.equals(value)) {
				return reportType;
			}
		}
		throw new IllegalArgumentException(value);
	}

	ReportType(String value, String description) {
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
	public static ReportType forValues(@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (ReportType reportType : ReportType.values()) {
			if (reportType.value.equalsIgnoreCase(value)) {
				return reportType;
			}

			if (reportType.description.equalsIgnoreCase(value)) {
				return reportType;
			}

			if (description != null) {
				if (reportType.description.equalsIgnoreCase(description)) {
					return reportType;
				}
				if (reportType.description.equalsIgnoreCase(value)) {
					return reportType;
				}
			}
		}

		return null;
	}

}
