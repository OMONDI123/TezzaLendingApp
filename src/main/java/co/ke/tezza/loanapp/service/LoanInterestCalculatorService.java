package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;
import co.ke.tezza.loanapp.enums.DebtTypeEnum;
import co.ke.tezza.loanapp.enums.FlatRateType;
import co.ke.tezza.loanapp.enums.InterestFrequencyEnum;
import co.ke.tezza.loanapp.enums.RepaymentScheduleTypeEnum;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;

@Service
public class LoanInterestCalculatorService {

    // ========== PUBLIC METHODS ==========

    public BigDecimal calculateTotalInterest(MLoanApplication application, int daysElapsedSinceApproval) {
        MLoanProductConfiguration config = application.getLoanProductConfiguration();
        if (config == null)
            throw new SetUpExceptions("Loan configuration is missing.");

        BigDecimal principal = application.getApprovedAmount();
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0)
            throw new SetUpExceptions("Principal amount must be greater than zero.");

        int gracePeriod = getEffectiveGracePeriod(application);
        int effectiveDays = Math.max(0, daysElapsedSinceApproval - gracePeriod);

        if (config.getIsDebtProduct() && config.getDebtType() == DebtTypeEnum.FLAT_RATE)
            return BigDecimal.ZERO;

        // Convert days elapsed to actual date range using disbursement date
        Date disbursement = application.getExpectedDisbursementDate();
        if (disbursement == null)
            throw new SetUpExceptions("Disbursement date is required for interest calculation.");

        LocalDate start = disbursement.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = start.plusDays(effectiveDays);
        Date fromDate = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant());

        BigDecimal totalInterest;
        switch (config.getInterestCalculationMethod()) {
            case FLAT:
                totalInterest = calculateFlatInterest(principal, application);
                break;
            case DECLINING_BALANCE:
                totalInterest = calculateDecliningBalanceEqualPrincipal(principal, application, fromDate, toDate);
                break;
            case DECLINING_BALANCE_EMI:
                totalInterest = calculateDecliningBalanceEMI(principal, application, fromDate, toDate);
                break;
            case COMPOUND:
                totalInterest = calculateCompoundInterest(principal, application, fromDate, toDate);
                break;
            case CYCLE_BASED:
                totalInterest = calculateCycleBasedInterest(application, fromDate, toDate);
                break;
            case SIMPLE_INTEREST:
                totalInterest = calculateSimpleInterest(principal, application, fromDate, toDate);
                break;
            default:
                throw new SetUpExceptions("Unsupported interest calculation method.");
        }

        totalInterest = applyEarlyRepaymentDiscount(totalInterest, config, daysElapsedSinceApproval);
        return totalInterest.setScale(0, RoundingMode.UP);
    }

    public BigDecimal calculateIncrementalInterest(MLoanApplication application, Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null || fromDate.after(toDate))
            return BigDecimal.ZERO;

        MLoanProductConfiguration config = application.getLoanProductConfiguration();
        if (config == null)
            throw new SetUpExceptions("Loan configuration is missing.");

        if (config.getIsDebtProduct() && config.getDebtType() == DebtTypeEnum.FLAT_RATE)
            return BigDecimal.ZERO;

        // Apply grace period: only days after grace period count
        Date disbursementDate = application.getExpectedDisbursementDate();
        if (disbursementDate == null)
            disbursementDate = fromDate;

        LocalDate disbursement = disbursementDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate from = fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate to = toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        long graceDays = getEffectiveGracePeriod(application);
        LocalDate graceEnd = disbursement.plusDays(graceDays);

        // If the entire period is within grace period → no interest
        if (!to.isAfter(graceEnd))
            return BigDecimal.ZERO;

        // Adjust fromDate to start of grace period if it starts earlier
        LocalDate effectiveFrom = from.isBefore(graceEnd) ? graceEnd : from;
        if (effectiveFrom.isAfter(to))
            return BigDecimal.ZERO;

        long effectiveDays = ChronoUnit.DAYS.between(effectiveFrom, to);
        if (effectiveDays <= 0)
            return BigDecimal.ZERO;

        BigDecimal currentBalance = application.getBalance() != null ? application.getBalance() : application.getApprovedAmount();
        BigDecimal incrementalInterest;

        switch (config.getInterestCalculationMethod()) {
            case COMPOUND:
                incrementalInterest = calculateIncrementalCompoundInterest(currentBalance, application,
                        Date.from(effectiveFrom.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                        Date.from(to.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                break;
            case SIMPLE_INTEREST:
                incrementalInterest = calculateIncrementalSimpleInterest(application, effectiveFrom, to);
                break;
            case DECLINING_BALANCE:
                incrementalInterest = calculateIncrementalDecliningBalance(currentBalance, application, effectiveDays);
                break;
            case DECLINING_BALANCE_EMI:
                incrementalInterest = calculateIncrementalDecliningBalanceEMI(currentBalance, application, effectiveDays);
                break;
            case FLAT:
                incrementalInterest = calculateIncrementalFlatInterest(application);
                break;
            case CYCLE_BASED:
                incrementalInterest = calculateIncrementalCycleBasedInterest(application, fromDate, toDate);
                break;
            default:
                throw new SetUpExceptions("Unsupported interest calculation method.");
        }

        return incrementalInterest.setScale(2, RoundingMode.HALF_UP);
    }

    // ========== FLAT INTEREST ==========

    private BigDecimal calculateFlatInterest(BigDecimal principal, MLoanApplication app) {
        FlatRateType flatRateType = app.getLoanProductConfiguration().getFlatRateType();
        if (flatRateType == null)
            throw new SetUpExceptions("Flat rate type is not configured.");

        switch (flatRateType) {
            case PERCENTAGE_BASED:
                if (app.getInteretsFlatRate() == null)
                    throw new SetUpExceptions("Flat rate percentage is not configured.");
                BigDecimal rate = app.getInteretsFlatRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                return principal.multiply(rate);
            case AMOUNT_BASED:
                if (app.getInteretsFlatRateAmount() == null)
                    throw new SetUpExceptions("Flat rate amount is not configured.");
                return app.getInteretsFlatRateAmount();
            default:
                throw new SetUpExceptions("Unsupported flat rate type.");
        }
    }

    private BigDecimal calculateIncrementalFlatInterest(MLoanApplication app) {
        // Flat interest is charged once at the beginning
        BigDecimal existingInterest = app.getInterestsEarned() != null ? app.getInterestsEarned() : BigDecimal.ZERO;
        if (existingInterest.compareTo(BigDecimal.ZERO) == 0)
            return calculateFlatInterest(app.getApprovedAmount(), app);
        return BigDecimal.ZERO;
    }

    // ========== DECLINING BALANCE (EQUAL PRINCIPAL) ==========

    private BigDecimal calculateDecliningBalanceEqualPrincipal(BigDecimal principal, MLoanApplication app,
                                                               Date fromDate, Date toDate) {
        InterestFrequencyEnum frequency = app.getLoanProductConfiguration().getInterestFrequency();
        if (frequency == null)
            throw new SetUpExceptions("Interest frequency is not configured.");

        // Determine number of full periods elapsed between fromDate and toDate
        long periodsElapsed = getFullPeriodsBetween(fromDate, toDate, frequency);
        if (periodsElapsed <= 0)
            return BigDecimal.ZERO;

        int totalPeriods = getTotalPeriods(app, frequency);
        BigDecimal periodicRate = getPeriodicInterestRate(app, frequency);
        BigDecimal principalPerPeriod = principal.divide(BigDecimal.valueOf(totalPeriods), 10, RoundingMode.HALF_UP);
        BigDecimal remainingPrincipal = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (int i = 0; i < periodsElapsed; i++) {
            BigDecimal periodInterest = remainingPrincipal.multiply(periodicRate);
            totalInterest = totalInterest.add(periodInterest);
            remainingPrincipal = remainingPrincipal.subtract(principalPerPeriod);
            if (remainingPrincipal.compareTo(BigDecimal.ZERO) < 0)
                break;
        }
        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateIncrementalDecliningBalance(BigDecimal currentBalance, MLoanApplication app, long days) {
        if (days <= 0 || currentBalance.compareTo(BigDecimal.ZERO) <= 0)
            return BigDecimal.ZERO;
        BigDecimal dailyRate = getDailyInterestRate(app);
        return currentBalance.multiply(dailyRate).multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }

    // ========== DECLINING BALANCE (EMI) ==========

    private BigDecimal calculateDecliningBalanceEMI(BigDecimal principal, MLoanApplication app,
                                                    Date fromDate, Date toDate) {
        InterestFrequencyEnum frequency = app.getLoanProductConfiguration().getInterestFrequency();
        if (frequency == null)
            throw new SetUpExceptions("Interest frequency is not configured.");

        long periodsElapsed = getFullPeriodsBetween(fromDate, toDate, frequency);
        if (periodsElapsed <= 0)
            return BigDecimal.ZERO;

        int totalPeriods = getTotalPeriods(app, frequency);
        BigDecimal periodicRate = getPeriodicInterestRate(app, frequency);

        // EMI formula: P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal onePlusRatePowN = (BigDecimal.ONE.add(periodicRate)).pow(totalPeriods);
        BigDecimal numerator = principal.multiply(periodicRate).multiply(onePlusRatePowN);
        BigDecimal denominator = onePlusRatePowN.subtract(BigDecimal.ONE);
        BigDecimal emi = numerator.divide(denominator, 10, RoundingMode.HALF_UP);

        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (int i = 0; i < periodsElapsed; i++) {
            BigDecimal interestPart = balance.multiply(periodicRate);
            BigDecimal principalPart = emi.subtract(interestPart);
            balance = balance.subtract(principalPart);
            totalInterest = totalInterest.add(interestPart);
            if (balance.compareTo(BigDecimal.ZERO) <= 0)
                break;
        }
        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateIncrementalDecliningBalanceEMI(BigDecimal currentBalance, MLoanApplication app, long days) {
        if (days <= 0 || currentBalance.compareTo(BigDecimal.ZERO) <= 0)
            return BigDecimal.ZERO;
        BigDecimal dailyRate = getDailyInterestRate(app);
        return currentBalance.multiply(dailyRate).multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }

    // ========== COMPOUND INTEREST ==========

    private BigDecimal calculateCompoundInterest(BigDecimal principal, MLoanApplication app,
                                                 Date fromDate, Date toDate) {
        InterestFrequencyEnum frequency = app.getLoanProductConfiguration().getInterestFrequency();
        if (frequency == null)
            throw new SetUpExceptions("Interest frequency is not configured.");

        switch (frequency) {
            case DAILY:
                return calculateDailyCompound(principal, app, fromDate, toDate);
            case WEEKLY:
                return calculateWeeklyCompound(principal, app, fromDate, toDate);
            case MONTHLY:
                return calculateMonthlyCompound(principal, app, fromDate, toDate);
            case YEARLY:
                return calculateYearlyCompound(principal, app, fromDate, toDate);
            default:
                throw new SetUpExceptions("Unsupported interest frequency for compound interest.");
        }
    }

    private BigDecimal calculateIncrementalCompoundInterest(BigDecimal currentBalance, MLoanApplication app,
                                                            Date fromDate, Date toDate) {
        InterestFrequencyEnum frequency = app.getLoanProductConfiguration().getInterestFrequency();
        if (frequency == null)
            throw new SetUpExceptions("Interest frequency is not configured.");

        switch (frequency) {
            case DAILY:
                return calculateDailyCompoundIncremental(currentBalance, app, fromDate, toDate);
            case WEEKLY:
                return calculateWeeklyCompoundIncremental(currentBalance, app, fromDate, toDate);
            case MONTHLY:
                return calculateMonthlyCompoundIncremental(currentBalance, app, fromDate, toDate);
            case YEARLY:
                return calculateYearlyCompoundIncremental(currentBalance, app, fromDate, toDate);
            default:
                throw new SetUpExceptions("Unsupported interest frequency for compound interest.");
        }
    }

    // Daily compound – exact day count
    private BigDecimal calculateDailyCompound(BigDecimal principal, MLoanApplication app,
                                              Date fromDate, Date toDate) {
        long days = ChronoUnit.DAYS.between(fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        if (days <= 0)
            return BigDecimal.ZERO;

        BigDecimal dailyRate = getDailyInterestRate(app);
        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (int i = 0; i < days; i++) {
            BigDecimal dailyInterest = balance.multiply(dailyRate);
            totalInterest = totalInterest.add(dailyInterest);
            balance = balance.add(dailyInterest);
        }
        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDailyCompoundIncremental(BigDecimal currentBalance, MLoanApplication app,
                                                         Date fromDate, Date toDate) {
        long days = ChronoUnit.DAYS.between(fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        if (days <= 0)
            return BigDecimal.ZERO;

        BigDecimal dailyRate = getDailyInterestRate(app);
        BigDecimal balance = currentBalance;
        BigDecimal incremental = BigDecimal.ZERO;

        for (int i = 0; i < days; i++) {
            BigDecimal dailyInterest = balance.multiply(dailyRate);
            incremental = incremental.add(dailyInterest);
            balance = balance.add(dailyInterest);
        }
        return incremental.setScale(2, RoundingMode.HALF_UP);
    }

    // Weekly compound – full weeks between dates
    private BigDecimal calculateWeeklyCompound(BigDecimal principal, MLoanApplication app,
                                               Date fromDate, Date toDate) {
        long weeks = ChronoUnit.WEEKS.between(fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        if (weeks <= 0)
            return BigDecimal.ZERO;

        BigDecimal weeklyRate = getWeeklyInterestRate(app);
        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (int i = 0; i < weeks; i++) {
            BigDecimal weeklyInterest = balance.multiply(weeklyRate);
            totalInterest = totalInterest.add(weeklyInterest);
            balance = balance.add(weeklyInterest);
        }
        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWeeklyCompoundIncremental(BigDecimal currentBalance, MLoanApplication app,
                                                          Date fromDate, Date toDate) {
        long weeks = ChronoUnit.WEEKS.between(fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        if (weeks <= 0)
            return BigDecimal.ZERO;

        BigDecimal weeklyRate = getWeeklyInterestRate(app);
        BigDecimal balance = currentBalance;
        BigDecimal incremental = BigDecimal.ZERO;

        for (int i = 0; i < weeks; i++) {
            BigDecimal weeklyInterest = balance.multiply(weeklyRate);
            incremental = incremental.add(weeklyInterest);
            balance = balance.add(weeklyInterest);
        }
        return incremental.setScale(2, RoundingMode.HALF_UP);
    }

    // Monthly compound – full calendar months between dates
    private BigDecimal calculateMonthlyCompound(BigDecimal principal, MLoanApplication app,
                                                Date fromDate, Date toDate) {
        long months = ChronoUnit.MONTHS.between(fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        if (months <= 0)
            return BigDecimal.ZERO;

        BigDecimal monthlyRate = getMonthlyInterestRate(app);
        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (int i = 0; i < months; i++) {
            BigDecimal monthlyInterest = balance.multiply(monthlyRate);
            totalInterest = totalInterest.add(monthlyInterest);
            balance = balance.add(monthlyInterest);
        }
        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMonthlyCompoundIncremental(BigDecimal currentBalance, MLoanApplication app,
                                                           Date fromDate, Date toDate) {
        long months = ChronoUnit.MONTHS.between(fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        if (months <= 0)
            return BigDecimal.ZERO;

        BigDecimal monthlyRate = getMonthlyInterestRate(app);
        BigDecimal balance = currentBalance;
        BigDecimal incremental = BigDecimal.ZERO;

        for (int i = 0; i < months; i++) {
            BigDecimal monthlyInterest = balance.multiply(monthlyRate);
            incremental = incremental.add(monthlyInterest);
            balance = balance.add(monthlyInterest);
        }
        return incremental.setScale(2, RoundingMode.HALF_UP);
    }

    // Yearly compound – full calendar years between dates
    private BigDecimal calculateYearlyCompound(BigDecimal principal, MLoanApplication app,
                                               Date fromDate, Date toDate) {
        long years = ChronoUnit.YEARS.between(fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        if (years <= 0)
            return BigDecimal.ZERO;

        BigDecimal annualRate = getAnnualInterestRate(app);
        BigDecimal balance = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (int i = 0; i < years; i++) {
            BigDecimal yearlyInterest = balance.multiply(annualRate);
            totalInterest = totalInterest.add(yearlyInterest);
            balance = balance.add(yearlyInterest);
        }
        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateYearlyCompoundIncremental(BigDecimal currentBalance, MLoanApplication app,
                                                          Date fromDate, Date toDate) {
        long years = ChronoUnit.YEARS.between(fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        if (years <= 0)
            return BigDecimal.ZERO;

        BigDecimal annualRate = getAnnualInterestRate(app);
        BigDecimal balance = currentBalance;
        BigDecimal incremental = BigDecimal.ZERO;

        for (int i = 0; i < years; i++) {
            BigDecimal yearlyInterest = balance.multiply(annualRate);
            incremental = incremental.add(yearlyInterest);
            balance = balance.add(yearlyInterest);
        }
        return incremental.setScale(2, RoundingMode.HALF_UP);
    }

    // ========== SIMPLE INTEREST ==========

    private BigDecimal calculateSimpleInterest(BigDecimal principal, MLoanApplication app,
                                               Date fromDate, Date toDate) {
        long days = ChronoUnit.DAYS.between(fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        if (days <= 0)
            return BigDecimal.ZERO;

        BigDecimal annualRate = getEffectiveAnnualRate(app);
        BigDecimal timeInYears = BigDecimal.valueOf(days).divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
        return principal.multiply(annualRate).multiply(timeInYears).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateIncrementalSimpleInterest(MLoanApplication app, LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to);
        if (days <= 0)
            return BigDecimal.ZERO;

        BigDecimal principal = app.getApprovedAmount();
        BigDecimal annualRate = getEffectiveAnnualRate(app);
        BigDecimal timeInYears = BigDecimal.valueOf(days).divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
        return principal.multiply(annualRate).multiply(timeInYears).setScale(2, RoundingMode.HALF_UP);
    }

    // ========== CYCLE BASED INTEREST ==========

    private BigDecimal calculateCycleBasedInterest(MLoanApplication application, Date fromDate, Date toDate) {
        MLoanProductConfiguration config = application.getLoanProductConfiguration();
        if (config == null)
            throw new SetUpExceptions("Loan configuration is missing.");

        BigDecimal principal = application.getApprovedAmount();
        BigDecimal totalInterest = BigDecimal.ZERO;

        Integer cycle1End = config.getCycle1DurationDays();
        Integer cycle2Start = config.getCycle2StartsAfterDay();
        Integer cycle2End = (cycle2Start != null && config.getCycle2DurationDays() != null)
                ? cycle2Start + config.getCycle2DurationDays() - 1
                : null;
        Integer cycle3Start = config.getCycle3PenaltyStartsAfterDay();

        LocalDate disbursement = application.getExpectedDisbursementDate()
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate start = fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // For simplicity, we use the maximum days elapsed from disbursement to end date
        long daysElapsed = ChronoUnit.DAYS.between(disbursement, end);

        // Cycle 1 (flat/fixed)
        if (daysElapsed > 0 && cycle1End != null && cycle1End > 0 && daysElapsed <= cycle1End) {
            BigDecimal cycle1Rate = application.getCycle1FlatInterestPercent()
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            totalInterest = principal.multiply(cycle1Rate);
        }

        // Cycle 2 (daily interest)
        if (cycle2Start != null && daysElapsed > cycle2Start) {
            int daysInCycle2 = (int) Math.min(daysElapsed, cycle2End != null ? cycle2End : daysElapsed) - cycle2Start;
            if (daysInCycle2 > 0) {
                BigDecimal cycle2DailyRate = application.getCycle2DailyInterestPercent()
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                BigDecimal cycle2Interest = principal.multiply(cycle2DailyRate).multiply(BigDecimal.valueOf(daysInCycle2));
                totalInterest = totalInterest.add(cycle2Interest);
            }
        }

        // Cycle 3 (penalty)
        if (cycle3Start != null && daysElapsed > cycle3Start) {
            int overdueDays = (int) (daysElapsed - cycle3Start);
            Integer penaltyPeriodDays = config.getCycle3PenaltyPeriodDays();
            if (penaltyPeriodDays != null && penaltyPeriodDays > 0) {
                int penaltyPeriods = (int) Math.ceil((double) overdueDays / penaltyPeriodDays);
                BigDecimal currentBalance = application.getBalance() != null ? application.getBalance() : principal.add(totalInterest);
                BigDecimal penaltyRate = application.getCycle3PenaltyPercentPerPeriod()
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                BigDecimal penalty = currentBalance.multiply(penaltyRate).multiply(BigDecimal.valueOf(penaltyPeriods));
                totalInterest = totalInterest.add(penalty);
            }
        }

        return totalInterest;
    }

    private BigDecimal calculateIncrementalCycleBasedInterest(MLoanApplication application, Date fromDate, Date toDate) {
        BigDecimal totalTo = calculateCycleBasedInterest(application, application.getExpectedDisbursementDate(), toDate);
        BigDecimal totalFrom = calculateCycleBasedInterest(application, application.getExpectedDisbursementDate(), fromDate);
        return totalTo.subtract(totalFrom).max(BigDecimal.ZERO);
    }

    // ========== HELPER METHODS FOR PERIOD COUNTING ==========

    /**
     * Returns the number of full interest periods between two dates based on frequency.
     * For MONTHLY and YEARLY, uses ChronoUnit.MONTHS/YEARS.
     * For DAILY and WEEKLY, uses day/week counting.
     */
    private long getFullPeriodsBetween(Date fromDate, Date toDate, InterestFrequencyEnum frequency) {
        LocalDate from = fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate to = toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        switch (frequency) {
            case DAILY:
                return ChronoUnit.DAYS.between(from, to);
            case WEEKLY:
                return ChronoUnit.WEEKS.between(from, to);
            case MONTHLY:
                return ChronoUnit.MONTHS.between(from, to);
            case YEARLY:
                return ChronoUnit.YEARS.between(from, to);
            default:
                return 0;
        }
    }

    /**
     * Total number of periods over the loan term (from disbursement to due date).
     * Uses the same calendar-based logic.
     */
    private int getTotalPeriods(MLoanApplication app, InterestFrequencyEnum frequency) {
        Date disbursement = app.getExpectedDisbursementDate();
        Date dueDate = app.getDueDate();
        if (disbursement == null || dueDate == null)
            throw new SetUpExceptions("Disbursement or due date missing for period calculation.");

        long periods = getFullPeriodsBetween(disbursement, dueDate, frequency);
        if (periods <= 0)
            periods = 1; // at least one period
        return (int) periods;
    }

    // ========== RATE GETTERS (unchanged) ==========

    private BigDecimal getDailyInterestRate(MLoanApplication app) {
        if (app.getDailyInterestRate() == null || app.getDailyInterestRate().compareTo(BigDecimal.ZERO) <= 0)
            throw new SetUpExceptions("Daily interest rate is not configured.");
        return app.getDailyInterestRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal getWeeklyInterestRate(MLoanApplication app) {
        if (app.getWeeklyInterestRate() == null || app.getWeeklyInterestRate().compareTo(BigDecimal.ZERO) <= 0)
            throw new SetUpExceptions("Weekly interest rate is not configured.");
        return app.getWeeklyInterestRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal getMonthlyInterestRate(MLoanApplication app) {
        if (app.getMonthlyInterestRate() == null || app.getMonthlyInterestRate().compareTo(BigDecimal.ZERO) <= 0)
            throw new SetUpExceptions("Monthly interest rate is not configured.");
        return app.getMonthlyInterestRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal getAnnualInterestRate(MLoanApplication app) {
        if (app.getAnnualInterestRate() == null || app.getAnnualInterestRate().compareTo(BigDecimal.ZERO) <= 0)
            throw new SetUpExceptions("Annual interest rate is not configured.");
        return app.getAnnualInterestRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal getPeriodicInterestRate(MLoanApplication app, InterestFrequencyEnum frequency) {
        switch (frequency) {
            case DAILY: return getDailyInterestRate(app);
            case WEEKLY: return getWeeklyInterestRate(app);
            case MONTHLY: return getMonthlyInterestRate(app);
            case YEARLY: return getAnnualInterestRate(app);
            default: return getMonthlyInterestRate(app);
        }
    }

    private BigDecimal getEffectiveAnnualRate(MLoanApplication app) {
        InterestFrequencyEnum frequency = app.getLoanProductConfiguration().getInterestFrequency();
        if (frequency == null)
            throw new SetUpExceptions("Interest frequency is not configured.");

        BigDecimal rate;
        switch (frequency) {
            case DAILY:
                rate = app.getDailyInterestRate().multiply(BigDecimal.valueOf(365));
                break;
            case WEEKLY:
                rate = app.getWeeklyInterestRate().multiply(BigDecimal.valueOf(52));
                break;
            case MONTHLY:
                rate = app.getMonthlyInterestRate().multiply(BigDecimal.valueOf(12));
                break;
            case YEARLY:
                rate = app.getAnnualInterestRate();
                break;
            default:
                throw new SetUpExceptions("Unsupported interest frequency.");
        }
        return rate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }

    private int getEffectiveGracePeriod(MLoanApplication application) {
        if (application.getLoanProductConfiguration().getRepaymentScheduleType() == RepaymentScheduleTypeEnum.INSTALLMENTS) {
            if (application.getGracePeriodToFirstInstallment() != null)
                return application.getGracePeriodToFirstInstallment();
            if (application.getLoanProductConfiguration().getGracePeriodBeforeFirstInstallment() != null)
                return application.getLoanProductConfiguration().getGracePeriodBeforeFirstInstallment();
        } else {
            if (application.getGraceperiod() != null)
                return application.getGraceperiod();
            if (application.getLoanProductConfiguration().getGracePeriodDays() != null)
                return application.getLoanProductConfiguration().getGracePeriodDays();
        }
        return 0;
    }

    public BigDecimal applyEarlyRepaymentDiscount(BigDecimal totalInterest, MLoanProductConfiguration config, int daysElapsed) {
        if (config.getAllowEarlyRepayment() != null && config.getAllowEarlyRepayment()
                && config.getEarlyRepaymentDiscountPercent() != null
                && config.getEarlyRepaymentDiscountPercent().compareTo(BigDecimal.ZERO) > 0
                && daysElapsed <= config.getCycle1DurationDays()) {
            BigDecimal discount = totalInterest.multiply(config.getEarlyRepaymentDiscountPercent())
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            return totalInterest.subtract(discount).max(BigDecimal.ZERO);
        }
        return totalInterest;
    }
}