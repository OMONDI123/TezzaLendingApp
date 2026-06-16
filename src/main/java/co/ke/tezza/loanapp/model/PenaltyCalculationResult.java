package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Result DTO returned by PenaltyCalculatorService methods.
 */
public class PenaltyCalculationResult {
    private final BigDecimal penaltyAmount;           // total penalty to apply now
    private final BigDecimal totalPenaltyAfterApply;  // (if you want to sum with existing penalty)
    private final int periodsApplied;                 // number of periods/units penalty applied for
    private final boolean capApplied;                 // whether cap was hit
    private final BigDecimal cappedAmount;            // cap value used (null if none)
    private final Date nextCalculationDate;           // suggested next calculation date (nullable)
    private final Date lastCalculationDateToPersist;  // value to persist as lastPenaltyCalculationDate

    public PenaltyCalculationResult(BigDecimal penaltyAmount,
                                    BigDecimal totalPenaltyAfterApply,
                                    int periodsApplied,
                                    boolean capApplied,
                                    BigDecimal cappedAmount,
                                    Date nextCalculationDate,
                                    Date lastCalculationDateToPersist) {
        this.penaltyAmount = penaltyAmount;
        this.totalPenaltyAfterApply = totalPenaltyAfterApply;
        this.periodsApplied = periodsApplied;
        this.capApplied = capApplied;
        this.cappedAmount = cappedAmount;
        this.nextCalculationDate = nextCalculationDate;
        this.lastCalculationDateToPersist = lastCalculationDateToPersist;
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount;
    }

    public BigDecimal getTotalPenaltyAfterApply() {
        return totalPenaltyAfterApply;
    }

    public int getPeriodsApplied() {
        return periodsApplied;
    }

    public boolean isCapApplied() {
        return capApplied;
    }

    public BigDecimal getCappedAmount() {
        return cappedAmount;
    }

    public Date getNextCalculationDate() {
        return nextCalculationDate;
    }

    public Date getLastCalculationDateToPersist() {
        return lastCalculationDateToPersist;
    }

    @Override
    public String toString() {
        return "PenaltyCalculationResult{" +
                "penaltyAmount=" + penaltyAmount +
                ", totalPenaltyAfterApply=" + totalPenaltyAfterApply +
                ", periodsApplied=" + periodsApplied +
                ", capApplied=" + capApplied +
                ", cappedAmount=" + cappedAmount +
                ", nextCalculationDate=" + nextCalculationDate +
                ", lastCalculationDateToPersist=" + lastCalculationDateToPersist +
                '}';
    }
}