package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonFormat(shape = JsonFormat.Shape.OBJECT)

public enum PaymentGateway {
	MPESA("MPESA", "M-Pesa"),
    PAYPAL("PAYPAL", "PayPal"),
    SASA_PAY("SASA_PAY", "SasaPay"),
    STRIPE("STRIPE", "Stripe"),
    KCB_BANK("KCB_BANK", "KCB Bank"),
    EQUITY_BANK("EQUITY_BANK", "Equity Bank"),
    CO_OPERATIVE_BANK("CO_OPERATIVE_BANK", "Co-operative Bank"),
    OTHER_CARD_PAYMENT("GENERAL_CARD_PAYMENT", "Other Card Payment");
    
    

    private final String value;
    private final String description;

    PaymentGateway(String value, String description) {
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
    public static PaymentGateway forValues(@JsonProperty("value") String value,
                                            @JsonProperty("description") String description) {
        for (PaymentGateway base : PaymentGateway.values()) {
            if (base.value.equalsIgnoreCase(value) || base.description.equalsIgnoreCase(value)) {
                return base;
            }
        }
        return null;
    }

}
