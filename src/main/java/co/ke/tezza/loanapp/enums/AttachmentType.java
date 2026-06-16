package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Getter
public enum AttachmentType {

    COLLATERAL("COLLATERAL", "Collateral Ownership Document"),
    KYC("KYC", "KYC Document (e.g., National ID, KRA PIN)"),
    GUARANTOR("GUARANTOR", "Guarantor Supporting Document"),
    PHOTO("PHOTO", "Borrower Passport Photo"),
    LOAN_AGREEMENT("LOAN_AGREEMENT", "Signed Loan Agreement"),
    BANK_STATEMENT("BANK_STATEMENT", "Bank Statement"),
    PAYSLIP("PAYSLIP", "Latest Payslip"),
    BUSINESS_PERMIT("BUSINESS_PERMIT", "Business Permit or License"),
    OTHER("OTHER", "Other Supporting Document");

    private final String value;
    private final String description;

    AttachmentType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonCreator
    public static AttachmentType forValue(String value) {
        for (AttachmentType type : AttachmentType.values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid AttachmentType: " + value);
    }
}
