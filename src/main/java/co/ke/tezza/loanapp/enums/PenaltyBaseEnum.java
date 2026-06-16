package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PenaltyBaseEnum {

    PRINCIPAL(
        "PRINCIPAL",
        "Penalty is calculated only on the  principal amount, excluding any interest or fees."
    ),

    FULL_LOAN_BALANCE(
        "FULL_LOAN_BALANCE",
        "Penalty is calculated on the entire outstanding loan balance, including both principal and accrued interest."
    ),

    CURRENT_INSTALLMENT_OVERDUE(
        "CURRENT_INSTALLMENT_OVERDUE",
        "Penalty is applied based on the total overdue amount for the current installment, including principal and interest due."
    );

    private final String value;
    private final String description;

    PenaltyBaseEnum(String value, String description) {
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
    public static PenaltyBaseEnum forValues(@JsonProperty("value") String value,
                                            @JsonProperty("description") String description) {
        for (PenaltyBaseEnum base : PenaltyBaseEnum.values()) {
            if (base.value.equalsIgnoreCase(value) || base.description.equalsIgnoreCase(value)) {
                return base;
            }
        }
        return null;
    }
}
