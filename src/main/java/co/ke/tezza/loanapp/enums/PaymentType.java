package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PaymentType {
	MPESA("MPESA", "M-Pesa"),
	SASA_PAY("SASA_PAY", "Sasa Pay"),
	WRITE_OFF("WRITE_OFF","Write Off"),
	WAIVER("WAIVER","Waiver"),               // <-- added
    BANK_TRANSFER("BANK_TRANSFER", "Bank Transfer"),
    CASH("CASH", "Cash"),
    CHEQUE("CHEQUE", "Cheque"),
    MOBILE_BANKING("MOBILE_BANKING", "Mobile Banking"),
    CREDIT_CARD("CREDIT_CARD", "Credit Card"),
    DEBIT_CARD("DEBIT_CARD", "Debit Card"),
    CREDIT_NOTE("CREDIT_NOTE", "Credit Note"),
    WALLET_PAYMENT("WALLET_PAYMENT","Wallet Payment"),
    OTHER("OTHER", "Other");

    private final String value;
    private final String description;

    PaymentType(String value, String description) {
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
    public static PaymentType forValues(@JsonProperty("value") String value,
                                            @JsonProperty("description") String description) {
        for (PaymentType base : PaymentType.values()) {
            if (base.value.equalsIgnoreCase(value) || base.description.equalsIgnoreCase(value)) {
                return base;
            }
        }
        return null;
    }
}