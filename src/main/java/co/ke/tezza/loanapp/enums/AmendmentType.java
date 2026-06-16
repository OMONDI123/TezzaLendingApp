package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AmendmentType {
    
    // Principal/Amount Amendments
    TOP_UP("TOP_UP", "Top-Up", "Increase loan principal amount"),
    PRINCIPAL_REDUCTION("PRINCIPAL_REDUCTION", "Principal Reduction", "Reduce loan principal amount"),
    PRINCIPAL_RESTRUCTURING("PRINCIPAL_RESTRUCTURING", "Principal Restructuring", "Restructure principal payment terms"),
    
    // Term Amendments
    TERM_EXTENSION("TERM_EXTENSION", "Term Extension", "Extend loan repayment period"),
    TERM_REDUCTION("TERM_REDUCTION", "Term Reduction", "Reduce loan repayment period"),
    GRACE_PERIOD_EXTENSION("GRACE_PERIOD_EXTENSION", "Grace Period Extension", "Extend grace period"),
    
    // Interest Rate Amendments
    INTEREST_RATE_CHANGE("INTEREST_RATE_CHANGE", "Interest Rate Change", "Modify interest rates"),
    FLAT_RATE_CHANGE("FLAT_RATE_CHANGE", "Flat Rate Change", "Change flat interest rate"),
    INTEREST_CALCULATION_CHANGE("INTEREST_CALCULATION_CHANGE", "Interest Calculation Change", "Change calculation method"),
    
    // Borrower Amendments
    BORROWER_CHANGE("BORROWER_CHANGE", "Borrower Change", "Change borrower details/transfer"),
    BORROWER_TYPE_CHANGE("BORROWER_TYPE_CHANGE", "Borrower Type Change", "Change borrower type"),
    
    // Product/Contract Amendments
    PRODUCT_CHANGE("PRODUCT_CHANGE", "Product Change", "Change loan product"),
    CONTRACT_TERMS_CHANGE("CONTRACT_TERMS_CHANGE", "Contract Terms Change", "Modify contract terms"),
    
    // Security Amendments
    COLLATERAL_CHANGE("COLLATERAL_CHANGE", "Collateral Change", "Add/remove/modify collateral"),
    GUARANTOR_CHANGE("GUARANTOR_CHANGE", "Guarantor Change", "Add/remove/modify guarantors"),
    SECURITY_ENHANCEMENT("SECURITY_ENHANCEMENT", "Security Enhancement", "Enhance security arrangements"),
    
    // Penalty/Fee Amendments
    PENALTY_WAIVER("PENALTY_WAIVER", "Penalty Waiver", "Waive penalties"),
    FEE_WAIVER("FEE_WAIVER", "Fee Waiver", "Waive fees"),
    PENALTY_RATE_CHANGE("PENALTY_RATE_CHANGE", "Penalty Rate Change", "Modify penalty rates"),
    
    // Repayment Amendments
    REPAYMENT_SCHEDULE_CHANGE("REPAYMENT_SCHEDULE_CHANGE", "Repayment Schedule Change", "Change repayment schedule"),
    INSTALLMENT_RESCHEDULING("INSTALLMENT_RESCHEDULING", "Installment Rescheduling", "Reschedule installments"),
    PAYMENT_HOLIDAY("PAYMENT_HOLIDAY", "Payment Holiday", "Temporary payment suspension"),
    
    // Miscellaneous
    DOCUMENT_UPDATE("DOCUMENT_UPDATE", "Document Update", "Update loan documents"),
    ADMINISTRATIVE_CORRECTION("ADMINISTRATIVE_CORRECTION", "Administrative Correction", "Correct administrative errors"),
    EMERGENCY_AMENDMENT("EMERGENCY_AMENDMENT", "Emergency Amendment", "Emergency modification"),
    
    // Loan Status Amendments
    LOAN_RENEWAL("LOAN_RENEWAL", "Loan Renewal", "Renew existing loan"),
    LOAN_RESCHEDULING("LOAN_RESCHEDULING", "Loan Rescheduling", "Complete loan rescheduling"),
    FORBEARANCE("FORBEARANCE", "Forbearance", "Temporary relief arrangement");

    private final String value;
    private final String description;
    private final String detailedDescription;

    AmendmentType(String value, String description, String detailedDescription) {
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

    public static AmendmentType fromValue(String value) {
        for (AmendmentType type : AmendmentType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown amendment type: " + value);
    }

    @JsonCreator
    public static AmendmentType forValues(@JsonProperty("value") String value,
                                          @JsonProperty("description") String description) {
        for (AmendmentType type : AmendmentType.values()) {
            if (type.value.equalsIgnoreCase(value) || type.description.equalsIgnoreCase(description)) {
                return type;
            }
        }
        return null;
    }
}