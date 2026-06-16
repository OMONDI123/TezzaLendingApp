package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum SupportedProviders {
	ADVANTA("ADVANTA", "Advanta"),
	AFRICAS_TALKING("AFRICAS_TALKING", "Africas Talking"),
	CELCOM_AFRICA("CELCOM_AFRICA", "Celcom Africa"),
	ESMS_AFRICA("ESMS_AFRICA", "eSMS Africa"),
	SEND_EXA("SEND_EXA", "Sendexa"),
	LUMIERE("LUMIERE", "Lumiere Africa"),
	MOBITECH("MOBITECH", "Mobitech (MoveSMS)"),
	ORAMOBILE("ORAMOBILE", "Oramobile"),

	TWILIO("TWILIO", "Twilio"),
	VONAGE("VONAGE", "Vonage (Nexmo)"),
	INFOBIP("INFOBIP", "Infobip"),
	MESSAGEBIRD("MESSAGEBIRD", "MessageBird"),
	PLIVO("PLIVO", "Plivo"),
	SINCH("SINCH", "Sinch"),
	CLICKATELL("CLICKATELL", "Clickatell"),
	TERMII("TERMII", "Termii"),
	HUBTEL("HUBTEL", "Hubtel"),
	BULKSMS("BULKSMS", "BulkSMS"),
	ROUTE_MOBILE("ROUTE_MOBILE", "Route Mobile"),
	TEXTLOCAL("TEXTLOCAL", "Textlocal");

	private String value;

	private String description;

	public static SupportedProviders formValues(String value) {
		for (SupportedProviders provider : SupportedProviders.values()) {
			if (provider.value.equals(value)) {
				return provider;
			}
		}
		throw new IllegalArgumentException(value);
	}

	SupportedProviders(String value, String description) {
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
	public static SupportedProviders forValues(@JsonProperty("value") String value,
			@JsonProperty("description") String description) {
		for (SupportedProviders provider : SupportedProviders.values()) {
			if (provider.value.equalsIgnoreCase(value)) {
				return provider;
			}

			if (provider.description.equalsIgnoreCase(value)) {
				return provider;
			}

			if (description != null) {
				if (provider.description.equalsIgnoreCase(description)) {
					return provider;
				}
				if (provider.description.equalsIgnoreCase(value)) {
					return provider;
				}
			}
		}

		return null;
	}

}
