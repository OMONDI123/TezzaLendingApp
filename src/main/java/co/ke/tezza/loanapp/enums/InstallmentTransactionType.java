package co.ke.tezza.loanapp.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum InstallmentTransactionType {

    // Core Loan Operations
    INSTALLMENT_GENERATION("INSTALLMENT_GENERATION", "Installment Generated"),
    INTEREST_ACCRUED("INTEREST_ACCRUED", "Interest Accrued"),
    PENALTY_CHARGED("PENALTY_CHARGED", "Penalty Charged"),
    REPAYMENT_RECEIVED("REPAYMENT_RECEIVED", "Repayment Received"),
    
    // Debt Adjustments/Reductions
    WRITE_OFF("WRITE_OFF", "Write Off"),
    CREDIT_NOTE("CREDIT_NOTE", "Credit Note Issued"),
    WAIVER("WAIVER", "Waiver Applied"),
    SETTLEMENT("SETTLEMENT", "Settlement Payment"),
    RESTRUCTURING("RESTRUCTURING", "Loan Restructured"),
    
    // Refunds and Corrections
    REFUND("REFUND", "Refund Issued"),
    REVERSAL("REVERSAL", "Transaction Reversed"),
    ADJUSTMENT("ADJUSTMENT", "Manual Adjustment"),
    CORRECTION("CORRECTION", "Error Correction"),
    
    // Fee Transactions
    PROCESSING_FEE("PROCESSING_FEE", "Processing Fee Charged"),
    ADMIN_FEE("ADMIN_FEE", "Administration Fee"),
    LATE_FEE("LATE_FEE", "Late Payment Fee"),
    EARLY_REPAYMENT_FEE("EARLY_REPAYMENT_FEE", "Early Repayment Fee"),
    
    // Insurance and Protection
    INSURANCE_PREMIUM("INSURANCE_PREMIUM", "Insurance Premium"),
    LOAN_PROTECTION("LOAN_PROTECTION", "Loan Protection Charge"),
    
    // Tax Related
    TAX_CHARGE("TAX_CHARGE", "Tax Charged"),
    TAX_WAIVER("TAX_WAIVER", "Tax Waived"),
    
    // Interest Specific
    INTEREST_WAIVER("INTEREST_WAIVER", "Interest Waived"),
    INTEREST_CAPITALIZATION("INTEREST_CAPITALIZATION", "Interest Capitalized"),
    
    // Penalty Specific
    PENALTY_WAIVER("PENALTY_WAIVER", "Penalty Waived"),
    PENALTY_REVERSAL("PENALTY_REVERSAL", "Penalty Reversed"),
    
    // Disbursement Related
    DISBURSEMENT("DISBURSEMENT", "Loan Disbursed"),
    PRINCIPLE_REDUCTION("PRINCIPLE_REDUCTION", "Amount corrected by reducing the principle amount"),
    TOP_UP("TOP_UP", "Loan Top-up"),
    
    // Collection Actions
    COLLECTION_FEE("COLLECTION_FEE", "Collection Fee"),
    LEGAL_FEE("LEGAL_FEE", "Legal Fee"),
    
    // Special Transactions
    FORBEARANCE("FORBEARANCE", "Forbearance Granted"),
    MORATORIUM("MORATORIUM", "Moratorium Period"),
    DEBT_RESCHEDULING("DEBT_RESCHEDULING", "Debt Rescheduled"),
    DEBT_CONSOLIDATION("DEBT_CONSOLIDATION", "Debt Consolidated"),
    
    // Partial Transactions
    PARTIAL_WRITE_OFF("PARTIAL_WRITE_OFF", "Partial Write Off"),
    PARTIAL_WAIVER("PARTIAL_WAIVER", "Partial Waiver"),
    PARTIAL_SETTLEMENT("PARTIAL_SETTLEMENT", "Partial Settlement"),
    
    // System and Maintenance
    SYSTEM_ADJUSTMENT("SYSTEM_ADJUSTMENT", "System Adjustment"),
    MIGRATION_ENTRY("MIGRATION_ENTRY", "Migration Entry"),
    
    // Default
    UNKNOWN("UNKNOWN", "Unknown Transaction");

    private final String value;
    private final String description;

    InstallmentTransactionType(String value, String description) {
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
    public static InstallmentTransactionType forValues(@JsonProperty("value") String value,
                                                @JsonProperty("description") String description) {
        for (InstallmentTransactionType type : InstallmentTransactionType.values()) {
            if (type.value.equalsIgnoreCase(value) || type.description.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    // Helper methods for categorization
    public boolean isAdjustmentType() {
        return this == ADJUSTMENT || 
               this == CORRECTION || 
               this == SYSTEM_ADJUSTMENT ||
               this == MIGRATION_ENTRY;
    }
    
    public boolean isReductionType() {
        return this == WRITE_OFF ||
               this == CREDIT_NOTE ||
               this == WAIVER ||
               this == SETTLEMENT ||
               this == REFUND ||
               this == REVERSAL ||
               this == PARTIAL_WRITE_OFF ||
               this == PARTIAL_WAIVER ||
               this == PARTIAL_SETTLEMENT;
    }
    
    public boolean isFeeType() {
        return this == PROCESSING_FEE ||
               this == ADMIN_FEE ||
               this == LATE_FEE ||
               this == EARLY_REPAYMENT_FEE ||
               this == COLLECTION_FEE ||
               this == LEGAL_FEE;
    }
    
    public boolean isInterestRelated() {
        return this == INTEREST_ACCRUED ||
               this == INTEREST_WAIVER ||
               this == INTEREST_CAPITALIZATION;
    }
    
    public boolean isPenaltyRelated() {
        return this == PENALTY_CHARGED ||
               this == PENALTY_WAIVER ||
               this == PENALTY_REVERSAL;
    }
    
    public boolean requiresApproval() {
        // These transaction types typically require managerial approval
        return this == WRITE_OFF ||
               this == WAIVER ||
               this == SETTLEMENT ||
               this == RESTRUCTURING ||
               this == PARTIAL_WRITE_OFF ||
               this == PARTIAL_WAIVER ||
               this == FORBEARANCE ||
               this == DEBT_RESCHEDULING ||
               this == DEBT_CONSOLIDATION ;
    }
    
    public boolean affectsPrincipal() {
        return this == WRITE_OFF ||
               this == SETTLEMENT ||
               this == RESTRUCTURING ||
               this == PARTIAL_WRITE_OFF ||
               this == PARTIAL_SETTLEMENT ||
               this == DEBT_RESCHEDULING;
    }
    
    public boolean affectsInterest() {
        return this == INTEREST_ACCRUED ||
               this == INTEREST_WAIVER ||
               this == INTEREST_CAPITALIZATION ||
               this == WAIVER || 
               this == FORBEARANCE;
    }
    
    public boolean affectsPenalty() {
        return this == PENALTY_CHARGED ||
               this == PENALTY_WAIVER ||
               this == PENALTY_REVERSAL ||
               this == WAIVER; 
    }
    
    // Factory method for common operations
    public static InstallmentTransactionType getDefaultForOperation(String operation) {
        switch (operation.toUpperCase()) {
            case "INTEREST":
                return INTEREST_ACCRUED;
            case "PENALTY":
                return PENALTY_CHARGED;
            case "REPAYMENT":
                return REPAYMENT_RECEIVED;
            case "FEE":
                return PROCESSING_FEE;
            case "ADJUST":
                return ADJUSTMENT;
            case "REVERSE":
                return REVERSAL;
            case "WAIVE":
                return WAIVER;
            case "WRITEOFF":
                return WRITE_OFF;
            case "CREDIT":
                return CREDIT_NOTE;
            default:
                return UNKNOWN;
        }
    }
}