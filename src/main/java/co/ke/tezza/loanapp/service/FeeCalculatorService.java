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
import co.ke.tezza.loanapp.enums.FeeTimingEnum;
import co.ke.tezza.loanapp.enums.FeeTypeEnum;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;

@Service
public class FeeCalculatorService {

	public BigDecimal calculateServiceFee(MLoanApplication application) {
		MLoanProductConfiguration config = application.getLoanProductConfiguration();
		if (config == null)
			throw new SetUpExceptions("Loan configuration is missing.");

		if (!Boolean.TRUE.equals(config.getEnableServiceFee()))
			return BigDecimal.ZERO;

		BigDecimal principal = application.getApprovedAmount() != null ? application.getApprovedAmount()
				: application.getAppliedAmount();
		if (principal == null)
			throw new SetUpExceptions("Cannot calculate service fee: principal amount is missing.");

		FeeTypeEnum feeType = config.getServiceFeeType();
		if (feeType == null)
			throw new SetUpExceptions("Service fee type is not configured.");

		BigDecimal fee;
		switch (feeType) {
		case FIXED:
			if (config.getServiceFeeAmount() == null)
				throw new SetUpExceptions("Service fee amount is not configured.");
			fee = config.getServiceFeeAmount();
			break;
		case PERCENTAGE:
			if (config.getServiceFeePercentage() == null)
				throw new SetUpExceptions("Service fee percentage is not configured.");
			fee = principal.multiply(config.getServiceFeePercentage()).divide(BigDecimal.valueOf(100), 10,
					RoundingMode.HALF_UP);
			break;
		default:
			throw new SetUpExceptions("Unsupported service fee type.");
		}

		return fee.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * True if the service fee should be charged for the given lifecycle event
	 * (ORIGINATION = at approval/creation, POST_DISBURSEMENT = once funds are
	 * released) and it hasn't already been charged.
	 */
	public boolean shouldChargeServiceFeeNow(MLoanApplication application, FeeTimingEnum currentEvent) {
		MLoanProductConfiguration config = application.getLoanProductConfiguration();
		if (config == null || !Boolean.TRUE.equals(config.getEnableServiceFee()))
			return false;

		boolean alreadyCharged = application.getServiceFeeCharged() != null
				&& application.getServiceFeeCharged().compareTo(BigDecimal.ZERO) > 0;
		if (alreadyCharged)
			return false;

		return config.getServiceFeeTiming() == currentEvent;
	}

	// ========== DAILY FEE (recurring flat amount) ==========

	/**
	 * Total daily fee accrued from disbursement up to asOfDate. Use for a one-off
	 * fresh calculation; for sweep-job style incremental charging use
	 * calculateIncrementalDailyFee instead, so the fee isn't re-applied from
	 * scratch on every run.
	 */
	public BigDecimal calculateTotalDailyFee(MLoanApplication application, Date asOfDate) {
		MLoanProductConfiguration config = application.getLoanProductConfiguration();
		if (config == null)
			throw new SetUpExceptions("Loan configuration is missing.");

		if (!Boolean.TRUE.equals(config.getEnableDailyFee()))
			return BigDecimal.ZERO;

		if (config.getDailyFeeAmount() == null || config.getDailyFeeAmount().compareTo(BigDecimal.ZERO) <= 0)
			return BigDecimal.ZERO;

		Date disbursement = application.getActualDisbursementDate() != null ? application.getActualDisbursementDate()
				: application.getExpectedDisbursementDate();
		if (disbursement == null)
			throw new SetUpExceptions("Disbursement date is required for daily fee calculation.");

		LocalDate disbursementLocal = disbursement.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate asOfLocal = asOfDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		int startDay = config.getDailyFeeStartDay() != null ? config.getDailyFeeStartDay() : 0;
		LocalDate feeStartDate = disbursementLocal.plusDays(startDay);

		if (asOfLocal.isBefore(feeStartDate))
			return BigDecimal.ZERO;

		long daysChargeable = ChronoUnit.DAYS.between(feeStartDate, asOfLocal) + 1; // inclusive

		return config.getDailyFeeAmount().multiply(BigDecimal.valueOf(daysChargeable)).setScale(2,
				RoundingMode.HALF_UP);
	}

	/**
	 * Daily fee accrued for the window (fromDate, toDate], adjusted so it never
	 * starts before dailyFeeStartDay. Intended for sweep-job use: pass the loan's
	 * last-calculated date as fromDate and "today" as toDate, then add the result
	 * to dailyFeeCharged and balance.
	 */
	public BigDecimal calculateIncrementalDailyFee(MLoanApplication application, Date fromDate, Date toDate) {
		MLoanProductConfiguration config = application.getLoanProductConfiguration();
		if (config == null)
			throw new SetUpExceptions("Loan configuration is missing.");

		if (!Boolean.TRUE.equals(config.getEnableDailyFee()))
			return BigDecimal.ZERO;

		if (config.getDailyFeeAmount() == null || config.getDailyFeeAmount().compareTo(BigDecimal.ZERO) <= 0)
			return BigDecimal.ZERO;

		if (fromDate == null || toDate == null || fromDate.after(toDate))
			return BigDecimal.ZERO;

		Date disbursement = application.getActualDisbursementDate() != null ? application.getActualDisbursementDate()
				: application.getExpectedDisbursementDate();
		if (disbursement == null)
			throw new SetUpExceptions("Disbursement date is required for daily fee calculation.");

		LocalDate disbursementLocal = disbursement.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		int startDay = config.getDailyFeeStartDay() != null ? config.getDailyFeeStartDay() : 0;
		LocalDate feeStartDate = disbursementLocal.plusDays(startDay);

		LocalDate from = fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate to = toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		LocalDate effectiveFrom = from.isBefore(feeStartDate) ? feeStartDate : from;
		if (effectiveFrom.isAfter(to))
			return BigDecimal.ZERO;

		long days = ChronoUnit.DAYS.between(effectiveFrom, to) + 1; // inclusive of toDate

		return config.getDailyFeeAmount().multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
	}
}