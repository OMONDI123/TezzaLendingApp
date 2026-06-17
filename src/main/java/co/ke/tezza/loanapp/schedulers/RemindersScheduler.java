package co.ke.tezza.loanapp.schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.*;
import co.ke.tezza.loanapp.enums.*;
import co.ke.tezza.loanapp.repository.*;
import co.ke.tezza.loanapp.service.SmsHandlersService;
import co.ke.tezza.loanapp.util.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class RemindersScheduler {

	@Autowired
	private LoanApplicationRepository loanApplicationRepository;
	@Autowired
	private InstallmentRepository installmentRepository;
	@Autowired
	private ReminderConfigRepository reminderConfigRepository;
	@Autowired
	private SmsRepository smsRepository;
	@Autowired
	private Utils utils;
	@Autowired
	private GuarantorLoanRepository guarantorLoanRepository;
	@Autowired
	private SmsHandlersService smsHandlersService;

	@Scheduled(cron = "0 0/1 6-23 * * *")
	@Transactional
	public void executeComprehensiveReminderScheduler() {
		LocalDateTime now = LocalDateTime.now();

		List<MLoanApplication> activeLoans = loanApplicationRepository
				.findByBalanceGreaterThanAndApprovalStageAndIsActive(BigDecimal.ZERO, ApprovalStage.APPROVED, true);

		if (activeLoans.isEmpty()) {
			return;
		}

		for (MLoanApplication loan : activeLoans) {
			try {
				MInstallments inst = installmentRepository
						.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(true, BigDecimal.ZERO,
								loan);
				processRemindersForLoan(loan, inst, now);
			} catch (Exception e) {
				// Exception handled silently
				e.printStackTrace();
			}
		}
	}

	// -------------------------------------------------------------------------
	// Main processing methods
	// -------------------------------------------------------------------------

	public boolean processRemindersForLoan(MLoanApplication loan, MInstallments inst, LocalDateTime currentTime) {
		try {
			MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
					SettingCategoriesEnum.GENERAL_SETTINGS, loan.getAdOrgID());
			if (sys == null || !sys.isAllowSystemNotifications()) {
				return false;
			}

			boolean remindersSent = false;

			if (processConfiguredSmsTypesForLoan(loan, inst, currentTime)) {
				remindersSent = true;
			}

			boolean allowDefaultSms = sys.isAllowDefaultSms();
			if (allowDefaultSms && !remindersSent) {
				if (processDefaultSmsTypesForLoan(loan, inst, currentTime)) {
					remindersSent = true;
				}
			}

			return remindersSent;
		} catch (Exception e) {
			return false;
		}
	}

	// -------------------------------------------------------------------------
	// Configured reminders (from AD_Reminder_Config)
	// -------------------------------------------------------------------------

	public boolean processConfiguredSmsTypesForLoan(MLoanApplication loan, MInstallments inst,
			LocalDateTime currentTime) {
		List<MRemindersConfiguration> configs = reminderConfigRepository
				.findByIsActiveAndAdOrgIDOrderByReminderIdDesc(true, loan.getAdOrgID());

		if (configs.isEmpty()) {
			return false;
		}

		boolean anyReminderSent = false;
		Set<SmsTypeEnum> sentTypes = new HashSet<>();

		for (MRemindersConfiguration config : configs) {
			try {
				MSmsSetup smsSetup = config.getSmsMessageTemplate();
				if (smsSetup == null || !smsSetup.isActive()) {
					continue;
				}

				SmsTypeEnum smsType = smsSetup.getSmsType();

				if (sentTypes.contains(smsType)) {
					continue;
				}

				if (!isWithinMaxRemindersLimit(config, smsType, loan, inst)) {
					continue;
				}

				if (processReminderConfig(config, loan, inst, currentTime)) {
					sentTypes.add(smsType);
					anyReminderSent = true;
				}
			} catch (Exception e) {
				// Exception handled silently
			}
		}

		return anyReminderSent;
	}

	public boolean processReminderConfig(MRemindersConfiguration config, MLoanApplication loan, MInstallments inst,
			LocalDateTime currentTime) {
		MSmsSetup smsSetup = config.getSmsMessageTemplate();
		SmsTypeEnum smsType = smsSetup.getSmsType();

		if (!isDayAllowedForReminder(config, currentTime)) {
			return false;
		}

		if (!isTimeToSend(config, currentTime)) {
			return false;
		}

		if (!shouldProcessReminderByType(config, smsType, loan, inst)) {
			return false;
		}

		if (!shouldSendReminder(config, smsType, loan, inst, currentTime)) {
			return false;
		}

		return sendReminderByType(smsType, loan, inst, config.getReminderId());
	}

	// -------------------------------------------------------------------------
	// Time and day helpers
	// -------------------------------------------------------------------------

	public boolean isTimeToSend(MRemindersConfiguration config, LocalDateTime currentTime) {
		Boolean sendTimeEnabled = config.getSendTimeEnabled();
		if (sendTimeEnabled == null || !sendTimeEnabled) {
			return true;
		}

		LocalTime currentLocalTime = currentTime.toLocalTime();

		if (Boolean.TRUE.equals(config.getUseMultipleTimes()) && config.getSendTimes() != null) {
			for (String timeStr : config.getSendTimes()) {
				try {
					LocalTime scheduledTime = LocalTime.parse(timeStr);
					if (currentLocalTime.getHour() == scheduledTime.getHour()
							&& currentLocalTime.getMinute() == scheduledTime.getMinute()) {
						return true;
					}
				} catch (Exception e) {
					// Invalid time format, skip
				}
			}
		} else if (config.getSendTime() != null) {
			try {
				LocalTime scheduledTime = LocalTime.parse(config.getSendTime());
				if (currentLocalTime.getHour() == scheduledTime.getHour()
						&& currentLocalTime.getMinute() == scheduledTime.getMinute()) {
					return true;
				}
			} catch (Exception e) {
				// Invalid time format, skip
			}
		}

		return false;
	}

	public boolean isDayAllowedForReminder(MRemindersConfiguration config, LocalDateTime currentTime) {
		ReminderFrequency frequency = config.getReminderFrequency();

		if (frequency != ReminderFrequency.SPECIFIC_DAYS) {
			return true;
		}

		List<Days> selectedDays = config.getSpecificDays();
		if (selectedDays == null || selectedDays.isEmpty()) {
			return false;
		}

		DayOfWeek currentDayOfWeek = currentTime.getDayOfWeek();
		Days currentDay = convertToDaysEnum(currentDayOfWeek);

		return selectedDays.contains(currentDay);
	}

	public Days convertToDaysEnum(DayOfWeek dayOfWeek) {
		switch (dayOfWeek) {
		case MONDAY:
			return Days.MONDAY;
		case TUESDAY:
			return Days.TUESDAY;
		case WEDNESDAY:
			return Days.WEDNESDAY;
		case THURSDAY:
			return Days.THURSDAY;
		case FRIDAY:
			return Days.FRIDAY;
		case SATURDAY:
			return Days.SATURDAY;
		case SUNDAY:
			return Days.SUNDAY;
		default:
			throw new IllegalArgumentException("Unknown day: " + dayOfWeek);
		}
	}

	// -------------------------------------------------------------------------
	// Frequency and last‑sent logic (core improvements)
	// -------------------------------------------------------------------------

	public boolean shouldSendReminder(MRemindersConfiguration config, SmsTypeEnum smsType, MLoanApplication loan,
			MInstallments installment, LocalDateTime currentTime) {
		// For guarantor SMS, we defer to per‑guarantor checks inside sending methods
		if (isGuarantorSmsType(smsType)) {
			return true;
		}

		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Optional<MSms> lastSms = getLastApprovedSmsForRecipientType(config, smsType, loan, installment,
				borrowerEligible);
		LocalDateTime lastSent = lastSms.map(sms -> {
			LocalDateTime ts = sms.getTimesTosend();
			return ts != null ? ts : convertToLocalDateTime(sms.getCreated());
		}).orElse(null);

		return isEligibleToSend(config, lastSent, currentTime);
	}

	public boolean isEligibleToSend(MRemindersConfiguration config, LocalDateTime lastSent,
			LocalDateTime currentTime) {
		ReminderFrequency freq = config.getReminderFrequency();

		if (freq == ReminderFrequency.ONCE) {
			return lastSent == null;
		}

		if (lastSent == null) {
			return true;
		}

		boolean useMultipleTimes = Boolean.TRUE.equals(config.getUseMultipleTimes());

		switch (freq) {
		case DAILY:
			if (useMultipleTimes) {
				// Allow multiple times per day but not the same minute
				if (isSameDay(lastSent, currentTime) && isSameHourMinute(lastSent, currentTime)) {
					return false;
				}
				return true;
			} else {
				return !isSameDay(lastSent, currentTime);
			}

		case WEEKLY:
			long daysDiff = ChronoUnit.DAYS.between(lastSent.toLocalDate(), currentTime.toLocalDate());
			if (daysDiff >= 7)
				return true;
			if (daysDiff == 0 && useMultipleTimes && !isSameHourMinute(lastSent, currentTime))
				return true;
			return false;

		case MONTHLY:
			int monthsDiff = monthsBetween(lastSent, currentTime);
			if (monthsDiff >= 1)
				return true;
			if (monthsDiff == 0 && useMultipleTimes && !isSameHourMinute(lastSent, currentTime))
				return true;
			return false;

		case QUARTERLY:
			int quartersDiff = monthsBetween(lastSent, currentTime);
			if (quartersDiff >= 3)
				return true;
			if (quartersDiff == 0 && useMultipleTimes && !isSameHourMinute(lastSent, currentTime))
				return true;
			return false;

		case YEARLY:
			int yearsDiff = (int) ChronoUnit.YEARS.between(lastSent.toLocalDate(), currentTime.toLocalDate());
			if (yearsDiff >= 1)
				return true;
			if (yearsDiff == 0 && useMultipleTimes && !isSameHourMinute(lastSent, currentTime))
				return true;
			return false;

		case SPECIFIC_DAYS:
			// Day-of-week already checked; now handle multiple times
			if (useMultipleTimes) {
				if (isSameDay(lastSent, currentTime) && isSameHourMinute(lastSent, currentTime)) {
					return false;
				}
				return true;
			} else {
				return !isSameDay(lastSent, currentTime);
			}

		default:
			return true;
		}
	}

	public int monthsBetween(LocalDateTime start, LocalDateTime end) {
		Period p = Period.between(start.toLocalDate(), end.toLocalDate());
		return p.getYears() * 12 + p.getMonths();
	}

	public boolean isSameHourMinute(LocalDateTime dt1, LocalDateTime dt2) {
		return dt1.getHour() == dt2.getHour() && dt1.getMinute() == dt2.getMinute();
	}

	// -------------------------------------------------------------------------
	// Maximum reminders check
	// -------------------------------------------------------------------------

	public boolean isWithinMaxRemindersLimit(MRemindersConfiguration config, SmsTypeEnum smsType,
			MLoanApplication loan, MInstallments installment) {
		Integer maxReminders = config.getMaxReminders();
		if (maxReminders == null || maxReminders <= 0) {
			return true;
		}

		Long loanId = loan.getLoanApplicationId();
		Long installmentId = installment != null ? installment.getInstallmentId() : null;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);

		if (isBorrowerSmsType(smsType)) {
			long sentCount = countBorrowerSmsSent(config, smsType, loan, installment, borrowerEligible);
			return sentCount < maxReminders;
		} else if (isGuarantorSmsType(smsType)) {
			Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
			if (targetGuarantors.isEmpty()) {
				return false;
			}
			// At least one guarantor has not reached the limit
			for (MNextOfKin guarantor : targetGuarantors) {
				long guarantorCount = countGuarantorSmsSent(config, smsType, loan, installment, guarantor);
				if (guarantorCount < maxReminders) {
					return true;
				}
			}
			return false;
		} else {
			long sentCount;
			if (installmentId != null && isInstallmentLevelSms(smsType)) {
				sentCount = smsRepository.countBySmsTypeAndReminderIdAndLoanIdAndInstallmentIdAndDocStatus(smsType,
						config.getReminderId(), loanId, installmentId, DocStatus.APPROVED);
			} else {
				sentCount = smsRepository.countBySmsTypeAndReminderIdAndLoanIdAndDocStatus(smsType,
						config.getReminderId(), loanId, DocStatus.APPROVED);
			}
			return sentCount < maxReminders;
		}
	}

	// -------------------------------------------------------------------------
	// Guarantor‑specific frequency check
	// -------------------------------------------------------------------------

	public boolean shouldSendReminderForGuarantor(MRemindersConfiguration config, SmsTypeEnum smsType,
			MLoanApplication loan, MInstallments installment, MNextOfKin guarantor, LocalDateTime currentTime) {
		Optional<MSms> lastSms = getLastApprovedSmsForGuarantor(config, smsType, loan, installment, guarantor);
		LocalDateTime lastSent = lastSms.map(sms -> {
			LocalDateTime ts = sms.getTimesTosend();
			return ts != null ? ts : convertToLocalDateTime(sms.getCreated());
		}).orElse(null);
		return isEligibleToSend(config, lastSent, currentTime);
	}

	// -------------------------------------------------------------------------
	// SMS type classification (unchanged)
	// -------------------------------------------------------------------------

	public boolean isBorrowerSmsType(SmsTypeEnum smsType) {
		switch (smsType) {
		case LOAN_OR_DEBT_DUE_REMINDER:
		case LOAN_OR_DEBT_OVERDUE_REMINDER:
		case INSTALLMENT_DUE_REMINDER:
		case INSTALLMENT_OVERDUE_REMINDER:
		case INSTALLMENT_MISSED_PAYMENT:
		case INSTALLMENT_PAYMENT_REMINDER:
		case MISSED_REPAYMENT_ALERT:
		case GRACE_PERIOD_EXPIRY_ALERT:
		case STATEMENT_READY_NOTIFICATION:
		case INSTALLMENT_GENERATION_NOTIFICATION:
		case INSTALLMENT_ADJUSTMENT_NOTIFICATION:
		case INSTALLMENT_RESCHEDULE_NOTIFICATION:
		case REPAYMENT_SCHEDULE_UPDATE:
		case REPAYMENT_RESCHEDULE_REQUEST:
		case REPAYMENT_RESCHEDULE_APPROVAL:
		case REPAYMENT_RESCHEDULE_REJECTION:
		case LOAN_RESTRUCTURING_NOTIFICATION:
		case TOP_UP_LOAN_DISBURSEMENT:
		case LOAN_CLOSURE_NOTIFICATION:
		case AUTO_DEBIT_FAILURE:
		case LOAN_APPLICATION_OR_DEBT_REGISTRATION:
		case LOAN_APPROVAL_DEBT_APPROVAL:
		case LOAN_REJECTION_DEBT_REJECTION:
			return true;
		default:
			return false;
		}
	}

	public boolean isGuarantorSmsType(SmsTypeEnum smsType) {
		switch (smsType) {
		case GUARANTOR_PAYMENT_REMINDER:
		case GUARANTOR_LOAN_DUE_REMINDER:
		case GUARANTOR_LOAN_OVERDUE_ALERT:
		case GUARANTOR_LOAN_DEFAULT_NOTIFICATION:
		case GUARANTOR_INSTALLMENT_DUE_REMINDER:
		case GUARANTOR_INSTALLMENT_OVERDUE_ALERT:
		case GUARANTOR_INSTALLMENT_MISSED_PAYMENT:
		case GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION:
		case GUARANTOR_PENALTY_CALCULATION_NOTIFICATION:
		case GUARANTOR_STATEMENT_NOTIFICATION:
		case GUARANTOR_PARTIAL_PAYMENT_NOTIFICATION:
		case GUARANTOR_FULL_REPAYMENT_NOTIFICATION:
		case GUARANTOR_INSTALLMENT_PAYMENT_RECEIVED:
		case GUARANTOR_INSTALLMENT_PARTIAL_PAYMENT:
		case GUARANTOR_INTEREST_WAIVER_NOTIFICATION:
		case GUARANTOR_PENALTY_WAIVER_NOTIFICATION:
		case GUARANTOR_INSTALLMENT_ADJUSTMENT:
		case GUARANTOR_INSTALLMENT_RESCHEDULE:
		case GUARANTOR_LOAN_RESTRUCTURING:
		case GUARANTOR_RELEASE_NOTIFICATION:
		case GUARANTOR_CALL_NOTIFICATION:
		case GUARANTOR_RECOVERY_NOTIFICATION:
		case GUARANTOR_LIMIT_UPDATE_NOTIFICATION:
		case GUARANTOR_STATUS_CHANGE_NOTIFICATION:
		case GUARANTOR_APPROVAL_REQUEST:
		case GUARANTOR_APPROVAL_CONFIRMATION:
		case GUARANTOR_APPROVAL_REJECTION:
		case GUARANTOR_LOAN_ASSIGNMENT_NOTIFICATION:
		case GUARANTOR_MISSED_REPAYMENT_ALERT:
			return true;
		default:
			return false;
		}
	}

	public boolean isInstallmentLevelSms(SmsTypeEnum smsType) {
		switch (smsType) {
		case INSTALLMENT_DUE_REMINDER:
		case INSTALLMENT_OVERDUE_REMINDER:
		case INSTALLMENT_MISSED_PAYMENT:
		case INSTALLMENT_PAYMENT_REMINDER:
		case INSTALLMENT_GENERATION_NOTIFICATION:
		case INSTALLMENT_ADJUSTMENT_NOTIFICATION:
		case INSTALLMENT_RESCHEDULE_NOTIFICATION:
		case GUARANTOR_INSTALLMENT_DUE_REMINDER:
		case GUARANTOR_INSTALLMENT_OVERDUE_ALERT:
		case GUARANTOR_INSTALLMENT_MISSED_PAYMENT:
		case GUARANTOR_INSTALLMENT_PAYMENT_RECEIVED:
		case GUARANTOR_INSTALLMENT_PARTIAL_PAYMENT:
		case GUARANTOR_INSTALLMENT_ADJUSTMENT:
		case GUARANTOR_INSTALLMENT_RESCHEDULE:
			return true;
		default:
			return false;
		}
	}

	// -------------------------------------------------------------------------
	// Database query methods (unchanged)
	// -------------------------------------------------------------------------

	public Optional<MSms> getLastApprovedSmsForRecipientType(MRemindersConfiguration config, SmsTypeEnum smsType,
			MLoanApplication loan, MInstallments installment, boolean borrowerEligible) {
		Long loanId = loan.getLoanApplicationId();
		Long installmentId = installment != null ? installment.getInstallmentId() : null;

		if (isBorrowerSmsType(smsType)) {
			if (!borrowerEligible)
				return Optional.empty();
			return getLastApprovedSmsForBorrower(config, smsType, loan, installmentId, borrowerEligible);
		} else if (isGuarantorSmsType(smsType)) {
			Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
			Optional<MSms> latestSms = Optional.empty();
			LocalDateTime latestTime = null;
			for (MNextOfKin guarantor : targetGuarantors) {
				Optional<MSms> sms = getLastApprovedSmsForGuarantor(config, smsType, loan, installment, guarantor);
				if (sms.isPresent()) {
					LocalDateTime sentTime = sms.get().getTimesTosend();
					if (latestTime == null || (sentTime != null && sentTime.isAfter(latestTime))) {
						latestTime = sentTime;
						latestSms = sms;
					}
				}
			}
			return latestSms;
		} else {
			if (installmentId != null) {
				return smsRepository.findTopByReminderIdAndSmsTypeAndInstallmentIdAndDocStatusOrderByTimesTosendDesc(
						config.getReminderId(), smsType, installmentId, DocStatus.APPROVED);
			} else {
				return smsRepository.findTopByReminderIdAndSmsTypeAndLoanIdAndDocStatusOrderByTimesTosendDesc(
						config.getReminderId(), smsType, loanId, DocStatus.APPROVED);
			}
		}
	}

	public Optional<MSms> getLastApprovedSmsForGuarantor(MRemindersConfiguration config, SmsTypeEnum smsType,
			MLoanApplication loan, MInstallments installment, MNextOfKin guarantor) {
		Long loanId = loan.getLoanApplicationId();
		Long installmentId = installment != null ? installment.getInstallmentId() : null;
		Long guarantorId = guarantor.getNextOfKinId();

		if (installmentId != null && isInstallmentLevelSms(smsType)) {
			return smsRepository
					.findTopByReminderIdAndSmsTypeAndInstallmentIdAndGuarantorIdAndDocStatusOrderByTimesTosendDesc(
							config.getReminderId(), smsType, installmentId, guarantorId, DocStatus.APPROVED);
		} else {
			return smsRepository.findTopByReminderIdAndSmsTypeAndLoanIdAndGuarantorIdAndDocStatusOrderByTimesTosendDesc(
					config.getReminderId(), smsType, loanId, guarantorId, DocStatus.APPROVED);
		}
	}

	public Optional<MSms> getLastApprovedSmsForBorrower(MRemindersConfiguration config, SmsTypeEnum smsType,
			MLoanApplication loan, Long installmentId, boolean borrowerEligible) {
		if (!borrowerEligible)
			return Optional.empty();

		Long loanId = loan.getLoanApplicationId();
	

		return getLastApprovedSmsForEachLoanAndBorrower(config, smsType, loanId, installmentId, loan);
	}

	public Optional<MSms> getLastApprovedSmsForEachLoanAndBorrower(MRemindersConfiguration config, SmsTypeEnum smsType,
	        Long loanId, Long installmentId, MLoanApplication loan) {
	    // Determine borrower type and the appropriate ID
	    BorrowerTypeEnum borrowerType = loan.getBorrowerType();
	    Long borrowerId = null;
	    if (borrowerType == BorrowerTypeEnum.INDIVIDUAL && loan.getIndividualBorrower() != null) {
	        borrowerId = loan.getIndividualBorrower().getIndividualBorrowerId();
	    } else if (borrowerType == BorrowerTypeEnum.GROUP && loan.getGroupBorrower() != null) {
	        borrowerId = loan.getGroupBorrower().getGroupBorrowerId();
	    } else if (borrowerType == BorrowerTypeEnum.INSTITUTION && loan.getInstitutionBorrower() != null) {
	        borrowerId = loan.getInstitutionBorrower().getInstitutionBorrowerId();
	    }

	    if (borrowerId == null) {
	        return Optional.empty(); 
	    }

	    if (installmentId != null && isInstallmentLevelSms(smsType)) {
	        switch (borrowerType) {
	            case INDIVIDUAL:
	                return smsRepository.findTopByReminderIdAndSmsTypeAndInstallmentIdAndIndividualBorrowerIdAndDocStatusOrderByTimesTosendDesc(
	                        config.getReminderId(), smsType, installmentId, borrowerId, DocStatus.APPROVED);
	            case GROUP:
	                return smsRepository.findTopByReminderIdAndSmsTypeAndInstallmentIdAndGroupBorrowerIdAndDocStatusOrderByTimesTosendDesc(
	                        config.getReminderId(), smsType, installmentId, borrowerId, DocStatus.APPROVED);
	            case INSTITUTION:
	                return smsRepository.findTopByReminderIdAndSmsTypeAndInstallmentIdAndInstitutionBorrowerIdAndDocStatusOrderByTimesTosendDesc(
	                        config.getReminderId(), smsType, installmentId, borrowerId, DocStatus.APPROVED);
	            default:
	                return Optional.empty();
	        }
	    } else {
	        // Loan‑level SMS
	        switch (borrowerType) {
	            case INDIVIDUAL:
	                return smsRepository.findTopByReminderIdAndSmsTypeAndLoanIdAndIndividualBorrowerIdAndDocStatusOrderByTimesTosendDesc(
	                        config.getReminderId(), smsType, loanId, borrowerId, DocStatus.APPROVED);
	            case GROUP:
	                return smsRepository.findTopByReminderIdAndSmsTypeAndLoanIdAndGroupBorrowerIdAndDocStatusOrderByTimesTosendDesc(
	                        config.getReminderId(), smsType, loanId, borrowerId, DocStatus.APPROVED);
	            case INSTITUTION:
	                return smsRepository.findTopByReminderIdAndSmsTypeAndLoanIdAndInstitutionBorrowerIdAndDocStatusOrderByTimesTosendDesc(
	                        config.getReminderId(), smsType, loanId, borrowerId, DocStatus.APPROVED);
	            default:
	                return Optional.empty();
	        }
	    }
	}

	public long countBorrowerSmsSent(MRemindersConfiguration config, SmsTypeEnum smsType, MLoanApplication loan,
			MInstallments installment, boolean borrowerEligible) {
		if (!borrowerEligible)
			return 0;

		Long loanId = loan.getLoanApplicationId();
		Long installmentId = installment != null ? installment.getInstallmentId() : null;

		if (installmentId != null && isInstallmentLevelSms(smsType)) {
			if (loan.getBorrowerType().equals(BorrowerTypeEnum.INDIVIDUAL)) {
				return smsRepository.countBySmsTypeAndReminderIdAndInstallmentIdAndDocStatusAndIndividualBorrowerId(
						smsType, config.getReminderId(), installmentId, DocStatus.APPROVED,
						loan.getIndividualBorrower().getIndividualBorrowerId());
			}
			if (loan.getBorrowerType().equals(BorrowerTypeEnum.INSTITUTION)) {
				return smsRepository.countBySmsTypeAndReminderIdAndInstallmentIdAndDocStatusAndInstitutionBorrowerId(
						smsType, config.getReminderId(),  installmentId, DocStatus.APPROVED,
						loan.getInstitutionBorrower().getInstitutionBorrowerId());
			} else {
				return smsRepository.countBySmsTypeAndReminderIdAndInstallmentIdAndDocStatusAndGroupBorrowerId(
						smsType, config.getReminderId(),  installmentId, DocStatus.APPROVED,
						loan.getGroupBorrower().getGroupBorrowerId());

			}

		} else {
			if (loan.getBorrowerType().equals(BorrowerTypeEnum.INDIVIDUAL)) {
				return smsRepository.countBySmsTypeAndReminderIdAndLoanIdAndDocStatusAndIndividualBorrowerId(
						smsType, config.getReminderId(), loanId, DocStatus.APPROVED,
						loan.getIndividualBorrower().getIndividualBorrowerId());
			}
			if (loan.getBorrowerType().equals(BorrowerTypeEnum.INSTITUTION)) {
				return smsRepository.countBySmsTypeAndReminderIdAndLoanIdAndDocStatusAndInstitutionBorrowerId(
						smsType, config.getReminderId(), loanId, DocStatus.APPROVED,
						loan.getInstitutionBorrower().getInstitutionBorrowerId());
			} else {
				return smsRepository.countBySmsTypeAndReminderIdAndLoanIdAndDocStatusAndGroupBorrowerId(
						smsType, config.getReminderId(), loanId, DocStatus.APPROVED,
						loan.getGroupBorrower().getGroupBorrowerId());

			}

		}
	}

	public long countGuarantorSmsSent(MRemindersConfiguration config, SmsTypeEnum smsType, MLoanApplication loan,
			MInstallments installment, MNextOfKin guarantor) {
		Long loanId = loan.getLoanApplicationId();
		Long installmentId = installment != null ? installment.getInstallmentId() : null;
		Long guarantorId = guarantor.getNextOfKinId();

		if (installmentId != null && isInstallmentLevelSms(smsType)) {
			return smsRepository.countBySmsTypeAndReminderIdAndLoanIdAndInstallmentIdAndGuarantorIdAndDocStatus(smsType,
					config.getReminderId(), loanId, installmentId, guarantorId, DocStatus.APPROVED);
		} else {
			return smsRepository.countBySmsTypeAndReminderIdAndLoanIdAndGuarantorIdAndDocStatus(smsType,
					config.getReminderId(), loanId, guarantorId, DocStatus.APPROVED);
		}
	}

	// -------------------------------------------------------------------------
	// Due/overdue logic (unchanged)
	// -------------------------------------------------------------------------

	public boolean shouldProcessReminderByType(MRemindersConfiguration config, SmsTypeEnum smsType,
			MLoanApplication loan, MInstallments installment) {
		boolean hasActiveInstallments = hasActiveInstallmentsWithBalance(loan);
		boolean loanOverdue = isLoanOverdue(loan);
		boolean borrowerEligible = utils.isBorrowerEligible(loan);

		if (isBorrowerSmsType(smsType) && !borrowerEligible) {
			return false;
		}

		if (isGuarantorSmsType(smsType)) {
			Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
			if (targetGuarantors.isEmpty()) {
				return false;
			}
		}

		switch (smsType) {
		case LOAN_OR_DEBT_DUE_REMINDER:
			if (loanOverdue)
				return false;
			return isWithinDaysBeforeRange(config, loan.getDueDate());

		case LOAN_OR_DEBT_OVERDUE_REMINDER:
			if (!loanOverdue)
				return false;
			return isWithinDaysAfterRange(config, loan.getDueDate());

		case INSTALLMENT_DUE_REMINDER:
			if (!hasActiveInstallments || loanOverdue || installment == null || isInstallmentOverdue(installment))
				return false;
			return isWithinDaysBeforeRange(config, installment.getPeriodEnd());

		case INSTALLMENT_OVERDUE_REMINDER:
			if (!hasActiveInstallments || loanOverdue || installment == null || !isInstallmentOverdue(installment))
				return false;
			return isWithinDaysAfterRange(config, installment.getPeriodEnd());

		case INSTALLMENT_MISSED_PAYMENT:
			if (!hasActiveInstallments || loanOverdue || installment == null || !isInstallmentOverdue(installment))
				return false;
			long daysMissed = calculateDaysOverdue(installment.getPeriodEnd());
			if (daysMissed <= 1)
				return false;
			return isWithinDaysAfterRange(config, installment.getPeriodEnd());

		case INSTALLMENT_PAYMENT_REMINDER:
			if (!hasActiveInstallments || loanOverdue || installment == null)
				return false;
			return isWithinDaysBeforeRange(config, installment.getPeriodEnd());

		case MISSED_REPAYMENT_ALERT:
			if (!loanOverdue)
				return false;
			return isWithinDaysAfterRange(config, loan.getDueDate());

		case GUARANTOR_PAYMENT_REMINDER:
			if (loanOverdue)
				return false;
			return isWithinDaysBeforeRange(config, loan.getDueDate());

		case GUARANTOR_LOAN_DUE_REMINDER:
			if (loanOverdue)
				return false;
			return isWithinDaysBeforeRange(config, loan.getDueDate());

		case GUARANTOR_LOAN_OVERDUE_ALERT:
			if (!loanOverdue)
				return false;
			return isWithinDaysAfterRange(config, loan.getDueDate());

		case GUARANTOR_LOAN_DEFAULT_NOTIFICATION:
			if (!loanOverdue)
				return false;
			long loanDaysOverdue = loan.getDueDate() != null ? calculateDaysOverdue(loan.getDueDate()) : 0;
			if (loanDaysOverdue <= 30)
				return false;
			return isWithinDaysAfterRange(config, loan.getDueDate());

		case GUARANTOR_MISSED_REPAYMENT_ALERT:
			if (!loanOverdue)
				return false;
			long loanMissedDays = calculateDaysOverdue(loan.getDueDate());
			if (loanMissedDays <= 1)
				return false;
			return isWithinDaysAfterRange(config, loan.getDueDate());

		case GUARANTOR_INSTALLMENT_DUE_REMINDER:
			if (!hasActiveInstallments || loanOverdue || installment == null || isInstallmentOverdue(installment))
				return false;
			return isWithinDaysBeforeRange(config, installment.getPeriodEnd());

		case GUARANTOR_INSTALLMENT_OVERDUE_ALERT:
			if (!hasActiveInstallments || loanOverdue || installment == null || !isInstallmentOverdue(installment))
				return false;
			return isWithinDaysAfterRange(config, installment.getPeriodEnd());

		case GUARANTOR_INSTALLMENT_MISSED_PAYMENT:
			if (!hasActiveInstallments || loanOverdue || installment == null || !isInstallmentOverdue(installment))
				return false;
			long missedDays = calculateDaysOverdue(installment.getPeriodEnd());
			if (missedDays <= 1)
				return false;
			return isWithinDaysAfterRange(config, installment.getPeriodEnd());

		case GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION:
			return isWithinDaysAfterRange(config, calculateInterestAccrualDate(loan));

		case GUARANTOR_PENALTY_CALCULATION_NOTIFICATION:
			if (!loanOverdue)
				return false;
			return isWithinDaysAfterRange(config, loan.getDueDate());

		case GUARANTOR_STATEMENT_NOTIFICATION:
			return isFirstDayOfMonth(LocalDateTime.now());

		case GRACE_PERIOD_EXPIRY_ALERT:
			Date graceEnd = calculateGracePeriodEnd(loan);
			return isWithinDaysBeforeRange(config, graceEnd);

		case STATEMENT_READY_NOTIFICATION:
			return isFirstDayOfMonth(LocalDateTime.now());

		default:
			return false;
		}
	}

	public boolean isWithinDaysBeforeRange(MRemindersConfiguration config, Date targetDate) {
		if (targetDate == null)
			return false;

		long daysRemaining = calculateDaysRemaining(targetDate);
		int daysBefore = config.getStartNoOfDaysBefore();

		if (daysRemaining == 0 && daysBefore == 0)
			return true;
		if (daysRemaining < 0)
			return false;
		if (daysBefore > 0)
			return daysRemaining <= daysBefore && daysRemaining > 0;
		return daysRemaining == 0;
	}

	public boolean isWithinDaysAfterRange(MRemindersConfiguration config, Date targetDate) {
		if (targetDate == null)
			return false;

		long daysOverdue = calculateDaysOverdue(targetDate);
		int daysAfter = config.getStartNoOfDaysAfter();

		if (daysOverdue <= 0)
			return false;
		if (daysAfter > 0)
			return daysOverdue >= daysAfter;
		return true;
	}

	public boolean isWithinDefaultDaysBefore(Date targetDate, int defaultDays) {
		if (targetDate == null)
			return false;
		long daysRemaining = calculateDaysRemaining(targetDate);
		return daysRemaining > 0 && daysRemaining <= defaultDays;
	}

	public boolean isWithinDefaultDaysAfter(Date targetDate, int defaultDays) {
		if (targetDate == null)
			return false;
		long daysOverdue = calculateDaysOverdue(targetDate);
		return daysOverdue > 0 && daysOverdue <= defaultDays;
	}

	public boolean shouldProcessDefaultMissedPaymentAlert(MLoanApplication loan) {
		if (loan.getDueDate() == null)
			return false;
		long daysOverdue = calculateDaysOverdue(loan.getDueDate());
		return daysOverdue > 1;
	}

	public boolean shouldProcessDefaultStatementNotification(MLoanApplication loan, LocalDateTime currentTime) {
		return isFirstDayOfMonth(currentTime);
	}

	// -------------------------------------------------------------------------
	// Default system reminders (unchanged)
	// -------------------------------------------------------------------------

	public boolean processDefaultSmsTypesForLoan(MLoanApplication loan, MInstallments inst,
			LocalDateTime currentTime) {
		boolean anyReminderSent = false;

		boolean hasActiveInstallments = hasActiveInstallmentsWithBalance(loan);
		boolean loanOverdue = isLoanOverdue(loan);
		MInstallments overdueInst = getOverdueInstallment(loan);
		boolean installmentOverdue = overdueInst != null && isInstallmentOverdue(overdueInst);
		boolean borrowerEligible = utils.isBorrowerEligible(loan);

		// Loan due reminder
		if (borrowerEligible && !hasBeenSentToday(SmsTypeEnum.LOAN_OR_DEBT_DUE_REMINDER, loan, null)) {
			if (!hasActiveInstallments || (!loanOverdue && !installmentOverdue)) {
				if (isWithinDefaultDaysBefore(loan.getDueDate(), 7)) {
					if (sendLoanDueReminder(loan, null))
						anyReminderSent = true;
				}
			}
		}

		// Loan overdue reminder
		if (borrowerEligible && !hasBeenSentToday(SmsTypeEnum.LOAN_OR_DEBT_OVERDUE_REMINDER, loan, null)) {
			if (!hasActiveInstallments || loanOverdue) {
				if (isWithinDefaultDaysAfter(loan.getDueDate(), 30)) {
					if (sendLoanOverdueReminder(loan, null))
						anyReminderSent = true;
				}
			}
		}

		// Installment due reminder
		if (borrowerEligible && inst != null && !hasBeenSentToday(SmsTypeEnum.INSTALLMENT_DUE_REMINDER, loan, inst)) {
			if (hasActiveInstallments && !loanOverdue && !installmentOverdue) {
				if (isWithinDefaultDaysBefore(inst.getPeriodEnd(), 7)) {
					if (sendInstallmentDueReminder(inst, null))
						anyReminderSent = true;
				}
			}
		}

		// Installment overdue reminder
		if (borrowerEligible && overdueInst != null
				&& !hasBeenSentToday(SmsTypeEnum.INSTALLMENT_OVERDUE_REMINDER, loan, overdueInst)) {
			if (hasActiveInstallments && !loanOverdue && installmentOverdue) {
				if (isWithinDefaultDaysAfter(overdueInst.getPeriodEnd(), 30)) {
					if (sendInstallmentOverdueReminder(overdueInst, null))
						anyReminderSent = true;
				}
			}
		}

		// Missed repayment alert
		if (borrowerEligible && !hasBeenSentToday(SmsTypeEnum.MISSED_REPAYMENT_ALERT, loan, null)) {
			if (shouldProcessDefaultMissedPaymentAlert(loan)) {
				if (sendMissedRepaymentAlert(loan, null))
					anyReminderSent = true;
			}
		}

		// Statement notification
		if (borrowerEligible && shouldProcessDefaultStatementNotification(loan, currentTime)
				&& !hasBeenSentThisMonth(SmsTypeEnum.STATEMENT_READY_NOTIFICATION, loan, null)) {
			if (sendStatementReadyNotification(loan, null))
				anyReminderSent = true;
		}

		// Grace period expiry alert
		if (borrowerEligible && !hasBeenSentToday(SmsTypeEnum.GRACE_PERIOD_EXPIRY_ALERT, loan, null)) {
			if (isWithinDefaultDaysBefore(calculateGracePeriodEnd(loan), 3)) {
				if (sendGracePeriodExpiryAlert(loan, null))
					anyReminderSent = true;
			}
		}

		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		if (targetGuarantors.isEmpty())
			return anyReminderSent;

		// Guarantor payment reminder
		if ((!hasActiveInstallments || (!loanOverdue && !installmentOverdue))) {
			boolean guarantorSent = false;
			for (MNextOfKin guarantor : targetGuarantors) {
				if (!hasBeenSentTodayForGuarantor(SmsTypeEnum.GUARANTOR_PAYMENT_REMINDER, loan, guarantor)) {
					if (isWithinDefaultDaysBefore(loan.getDueDate(), 7)) {
						if (sendGuarantorPaymentReminder(guarantor, loan, null))
							guarantorSent = true;
					}
				}
			}
			if (guarantorSent)
				anyReminderSent = true;
		}

		// Guarantor loan due reminder
		if (!loanOverdue) {
			boolean guarantorDueSent = false;
			for (MNextOfKin guarantor : targetGuarantors) {
				if (!hasBeenSentTodayForGuarantor(SmsTypeEnum.GUARANTOR_LOAN_DUE_REMINDER, loan, guarantor)) {
					if (isWithinDefaultDaysBefore(loan.getDueDate(), 7)) {
						if (sendGuarantorLoanDueReminderToGuarantor(guarantor, loan, null))
							guarantorDueSent = true;
					}
				}
			}
			if (guarantorDueSent)
				anyReminderSent = true;
		}

		// Guarantor loan overdue alert
		if (loanOverdue) {
			boolean guarantorAlertSent = false;
			for (MNextOfKin guarantor : targetGuarantors) {
				if (!hasBeenSentTodayForGuarantor(SmsTypeEnum.GUARANTOR_LOAN_OVERDUE_ALERT, loan, guarantor)) {
					if (isWithinDefaultDaysAfter(loan.getDueDate(), 30)) {
						if (sendGuarantorLoanOverdueAlertToGuarantor(guarantor, loan, null))
							guarantorAlertSent = true;
					}
				}
			}
			if (guarantorAlertSent)
				anyReminderSent = true;
		}

		// Guarantor installment due reminder
		if (inst != null && !loanOverdue && !installmentOverdue) {
			boolean guarantorInstDueSent = false;
			for (MNextOfKin guarantor : targetGuarantors) {
				if (!hasBeenSentTodayForGuarantor(SmsTypeEnum.GUARANTOR_INSTALLMENT_DUE_REMINDER, loan, guarantor)) {
					if (isWithinDefaultDaysBefore(inst.getPeriodEnd(), 7)) {
						if (sendGuarantorInstallmentDueReminderToGuarantor(guarantor, inst, loan, null))
							guarantorInstDueSent = true;
					}
				}
			}
			if (guarantorInstDueSent)
				anyReminderSent = true;
		}

		// Guarantor installment overdue alert
		if (overdueInst != null && !loanOverdue && installmentOverdue) {
			boolean guarantorInstOverdueSent = false;
			for (MNextOfKin guarantor : targetGuarantors) {
				if (!hasBeenSentTodayForGuarantor(SmsTypeEnum.GUARANTOR_INSTALLMENT_OVERDUE_ALERT, loan, guarantor)) {
					if (isWithinDefaultDaysAfter(overdueInst.getPeriodEnd(), 30)) {
						if (sendGuarantorInstallmentOverdueAlertToGuarantor(guarantor, overdueInst, loan, null))
							guarantorInstOverdueSent = true;
					}
				}
			}
			if (guarantorInstOverdueSent)
				anyReminderSent = true;
		}

		// Guarantor installment missed payment
		if (overdueInst != null && !loanOverdue && installmentOverdue
				&& calculateDaysOverdue(overdueInst.getPeriodEnd()) > 1) {
			boolean guarantorMissedSent = false;
			for (MNextOfKin guarantor : targetGuarantors) {
				if (!hasBeenSentTodayForGuarantor(SmsTypeEnum.GUARANTOR_INSTALLMENT_MISSED_PAYMENT, loan, guarantor)) {
					if (isWithinDefaultDaysAfter(overdueInst.getPeriodEnd(), 30)) {
						if (sendGuarantorInstallmentMissedPaymentToGuarantor(guarantor, overdueInst, loan, null))
							guarantorMissedSent = true;
					}
				}
			}
			if (guarantorMissedSent)
				anyReminderSent = true;
		}

		// Guarantor missed repayment alert
		if (loanOverdue && calculateDaysOverdue(loan.getDueDate()) > 1) {
			boolean guarantorMissedRepaymentSent = false;
			for (MNextOfKin guarantor : targetGuarantors) {
				if (!hasBeenSentTodayForGuarantor(SmsTypeEnum.GUARANTOR_MISSED_REPAYMENT_ALERT, loan, guarantor)) {
					if (isWithinDefaultDaysAfter(loan.getDueDate(), 30)) {
						if (sendGuarantorMissedRepaymentAlertToGuarantor(guarantor, loan, null))
							guarantorMissedRepaymentSent = true;
					}
				}
			}
			if (guarantorMissedRepaymentSent)
				anyReminderSent = true;
		}

		return anyReminderSent;
	}

	// -------------------------------------------------------------------------
	// SMS sending methods (borrower)
	// -------------------------------------------------------------------------

	public boolean sendLoanDueReminder(MLoanApplication loan, Long reminderId) {
		if (!utils.isBorrowerEligible(loan))
			return false;
		return smsHandlersService.handleLoanDueReminder(loan, reminderId);
	}

	public boolean sendLoanOverdueReminder(MLoanApplication loan, Long reminderId) {
		if (!utils.isBorrowerEligible(loan))
			return false;
		return smsHandlersService.handleLoanOverdueReminder(loan, reminderId);
	}

	public boolean sendInstallmentDueReminder(MInstallments installment, Long reminderId) {
		MLoanApplication loan = installment.getLoan();
		if (!utils.isBorrowerEligible(loan))
			return false;
		return smsHandlersService.handleInstallmentDueReminder(installment, reminderId);
	}

	public boolean sendInstallmentOverdueReminder(MInstallments installment, Long reminderId) {
		MLoanApplication loan = installment.getLoan();
		if (!utils.isBorrowerEligible(loan))
			return false;
		return smsHandlersService.handleInstallmentOverdueReminder(installment, reminderId);
	}

	public boolean sendInstallmentMissedPayment(MInstallments installment, Long reminderId) {
		MLoanApplication loan = installment.getLoan();
		if (!utils.isBorrowerEligible(loan))
			return false;
		return smsHandlersService.handleInstallmentMissedPayment(installment, reminderId);
	}

	public boolean sendInstallmentPaymentReminder(MInstallments installment, Long reminderId) {
		MLoanApplication loan = installment.getLoan();
		if (!utils.isBorrowerEligible(loan))
			return false;
		return smsHandlersService.handleInstallmentPaymentReminder(installment, reminderId);
	}

	public boolean sendMissedRepaymentAlert(MLoanApplication loan, Long reminderId) {
		if (!utils.isBorrowerEligible(loan))
			return false;
		return smsHandlersService.handleMissedRepaymentAlert(loan, reminderId);
	}

	public boolean sendGracePeriodExpiryAlert(MLoanApplication loan, Long reminderId) {
		if (!utils.isBorrowerEligible(loan))
			return false;
		return smsHandlersService.handleGracePeriodExpiryAlert(loan, reminderId);
	}

	public boolean sendStatementReadyNotification(MLoanApplication loan, Long reminderId) {
		if (!utils.isBorrowerEligible(loan))
			return false;
		return smsHandlersService.handleStatementReadyNotification(loan, reminderId);
	}

	public boolean sendInstallmentGenerationNotification(MInstallments installment, Long reminderId) {
		return smsHandlersService.handleInstallmentGenerationNotification(installment, reminderId);
	}

	public boolean sendInstallmentAdjustmentNotification(MInstallments installment, Long reminderId) {
		return smsHandlersService.handleInstallmentAdjustmentNotification(installment, installment.getAmount(),
				"Schedule adjustment", "Manual", reminderId);
	}

	public boolean sendInstallmentRescheduleNotification(MInstallments installment, Long reminderId) {
		return smsHandlersService.handleInstallmentRescheduleNotification(installment, installment.getPeriodEnd(),
				installment.getPeriodEnd(), "Rescheduled", BigDecimal.ZERO, reminderId);
	}

	public boolean sendRepaymentScheduleUpdate(MLoanApplication loan, Long reminderId) {
		return smsHandlersService.handleRepaymentScheduleUpdate(loan, loan.getBalance(), loan.getDueDate(), 1,
				"Schedule update", reminderId);
	}

	public boolean sendRepaymentRescheduleRequest(MLoanApplication loan, Long reminderId) {
		return smsHandlersService.handleRepaymentRescheduleRequest(loan, new Date(), "Pending", reminderId);
	}

	public boolean sendRepaymentRescheduleApproval(MLoanApplication loan, Long reminderId) {
		return smsHandlersService.handleRepaymentRescheduleApproval(loan, loan.getBalance(), 1, loan.getTermInDays(),
				new Date(), reminderId);
	}

	public boolean sendRepaymentRescheduleRejection(MLoanApplication loan, Long reminderId) {
		return smsHandlersService.handleRepaymentRescheduleRejection(loan, "Rejected", new Date(), reminderId);
	}

	public boolean sendLoanRestructuringNotification(MLoanApplication loan, Long reminderId) {
		return smsHandlersService.handleLoanRestructuringNotification(loan, loan.getBalance(), loan.getTermInDays(),
				loan.getBalance(), loan.getBalance(), new Date(), reminderId);
	}

	public boolean sendTopUpLoanDisbursement(MLoanApplication loan, Long reminderId) {
		return smsHandlersService.handleTopUpLoanDisbursement(loan, loan.getBalance(), loan.getBalance(), new Date(),
				reminderId);
	}

	public boolean sendLoanClosureNotification(MLoanApplication loan, Long reminderId) {
		return smsHandlersService.handleLoanClosureNotification(loan, loan.getBalance(), reminderId);
	}

	public boolean sendAutoDebitFailure(MLoanApplication loan, Long reminderId) {
		return smsHandlersService.handleAutoDebitFailure(loan, loan.getBalance(), "Insufficient funds", new Date(),
				"Auto Debit", reminderId);
	}

	// -------------------------------------------------------------------------
	// SMS sending methods (guarantor)
	// -------------------------------------------------------------------------

	public boolean sendGuarantorPaymentReminder(MNextOfKin guarantor, MLoanApplication loan, Long reminderId) {
		MRemindersConfiguration config = reminderConfigRepository.findById(reminderId).orElse(null);
		if (config != null && config.getMaxReminders() != null && config.getMaxReminders() > 0) {
			long sentCount = countGuarantorSmsSent(config, SmsTypeEnum.GUARANTOR_PAYMENT_REMINDER, loan, null,
					guarantor);
			if (sentCount >= config.getMaxReminders())
				return false;
		}
		if (config != null && !shouldSendReminderForGuarantor(config, SmsTypeEnum.GUARANTOR_PAYMENT_REMINDER, loan,
				null, guarantor, LocalDateTime.now())) {
			return false;
		}
		return smsHandlersService.handleGuarantorPaymentReminder(guarantor, loan, reminderId);
	}

	public boolean sendGuarantorPaymentReminders(MLoanApplication loan, Long reminderId) {
		boolean success = true;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		for (MNextOfKin guarantor : targetGuarantors) {
			success = success && sendGuarantorPaymentReminder(guarantor, loan, reminderId);
		}
		return success;
	}

	public boolean sendGuarantorLoanDueReminder(MLoanApplication loan, Long reminderId) {
		boolean success = true;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		for (MNextOfKin guarantor : targetGuarantors) {
			success = success && sendGuarantorLoanDueReminderToGuarantor(guarantor, loan, reminderId);
		}
		return success;
	}

	public boolean sendGuarantorLoanDueReminderToGuarantor(MNextOfKin guarantor, MLoanApplication loan,
			Long reminderId) {
		MRemindersConfiguration config = reminderConfigRepository.findById(reminderId).orElse(null);
		if (config != null && config.getMaxReminders() != null && config.getMaxReminders() > 0) {
			long sentCount = countGuarantorSmsSent(config, SmsTypeEnum.GUARANTOR_LOAN_DUE_REMINDER, loan, null,
					guarantor);
			if (sentCount >= config.getMaxReminders())
				return false;
		}
		if (config != null && !shouldSendReminderForGuarantor(config, SmsTypeEnum.GUARANTOR_LOAN_DUE_REMINDER, loan,
				null, guarantor, LocalDateTime.now())) {
			return false;
		}

		try {
			MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan, guarantor, true);
			BigDecimal currentGuaranteeUsed = BigDecimal.ZERO;
			BigDecimal guaranteeRemaining = BigDecimal.ZERO;
			Date gracePeriodEndDate = calculateGracePeriodEnd(loan);
			BigDecimal interestAmount = loan.getInterestsEarned() != null ? loan.getInterestsEarned() : BigDecimal.ZERO;
			BigDecimal penaltyAmount = loan.getPenaltyEarned() != null ? loan.getPenaltyEarned() : BigDecimal.ZERO;
			if (gLoan != null) {
				currentGuaranteeUsed = gLoan.getGuaranteeAmount().subtract(gLoan.getGuaranteeAmountBalance());
				guaranteeRemaining = gLoan.getGuaranteeAmountBalance();
			}
			return smsHandlersService.handleGuarantorLoanDueReminder(guarantor, loan, loan.getDueDate(),
					loan.getBalance(), interestAmount, penaltyAmount, gracePeriodEndDate, currentGuaranteeUsed,
					guaranteeRemaining, reminderId);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean sendGuarantorLoanOverdueAlert(MLoanApplication loan, Long reminderId) {
		boolean success = true;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		for (MNextOfKin guarantor : targetGuarantors) {
			success = success && sendGuarantorLoanOverdueAlertToGuarantor(guarantor, loan, reminderId);
		}
		return success;
	}

	public boolean sendGuarantorLoanOverdueAlertToGuarantor(MNextOfKin guarantor, MLoanApplication loan,
			Long reminderId) {
		MRemindersConfiguration config = reminderConfigRepository.findById(reminderId).orElse(null);
		if (config != null && config.getMaxReminders() != null && config.getMaxReminders() > 0) {
			long sentCount = countGuarantorSmsSent(config, SmsTypeEnum.GUARANTOR_LOAN_OVERDUE_ALERT, loan, null,
					guarantor);
			if (sentCount >= config.getMaxReminders())
				return false;
		}
		if (config != null && !shouldSendReminderForGuarantor(config, SmsTypeEnum.GUARANTOR_LOAN_OVERDUE_ALERT, loan,
				null, guarantor, LocalDateTime.now())) {
			return false;
		}
		return smsHandlersService.handleGuarantorLoanOverdueAlert(guarantor, loan, reminderId);
	}

	public boolean sendGuarantorLoanDefaultNotification(MLoanApplication loan, Long reminderId) {
		boolean success = true;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		for (MNextOfKin guarantor : targetGuarantors) {
			success = success && sendGuarantorLoanDefaultNotificationToGuarantor(guarantor, loan, reminderId);
		}
		return success;
	}

	public boolean sendGuarantorLoanDefaultNotificationToGuarantor(MNextOfKin guarantor, MLoanApplication loan,
			Long reminderId) {
		MRemindersConfiguration config = reminderConfigRepository.findById(reminderId).orElse(null);
		if (config != null && config.getMaxReminders() != null && config.getMaxReminders() > 0) {
			long sentCount = countGuarantorSmsSent(config, SmsTypeEnum.GUARANTOR_LOAN_DEFAULT_NOTIFICATION, loan, null,
					guarantor);
			if (sentCount >= config.getMaxReminders())
				return false;
		}
		if (config != null && !shouldSendReminderForGuarantor(config, SmsTypeEnum.GUARANTOR_LOAN_DEFAULT_NOTIFICATION,
				loan, null, guarantor, LocalDateTime.now())) {
			return false;
		}

		try {
			MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan, guarantor, true);
			BigDecimal guaranteeAmount = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
			BigDecimal amountOverDue = loan.getBalance();
			Long daysOverDue = calculateDaysOverdue(loan.getDueDate());
			BigDecimal totalOutstanding = loan.getBalance();
			Date defaultDate = new Date();
			return smsHandlersService.handleGuarantorLoanDefaultNotification(guarantor, loan, amountOverDue,
					daysOverDue, totalOutstanding, defaultDate, guaranteeAmount, reminderId);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean sendGuarantorInstallmentDueReminder(MLoanApplication loan, Long reminderId) {
		MInstallments installment = getNextDueInstallment(loan);
		if (installment == null)
			return false;

		boolean success = true;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		for (MNextOfKin guarantor : targetGuarantors) {
			success = success
					&& sendGuarantorInstallmentDueReminderToGuarantor(guarantor, installment, loan, reminderId);
		}
		return success;
	}

	public boolean sendGuarantorInstallmentDueReminderToGuarantor(MNextOfKin guarantor, MInstallments installment,
			MLoanApplication loan, Long reminderId) {
		MRemindersConfiguration config = reminderConfigRepository.findById(reminderId).orElse(null);
		if (config != null && config.getMaxReminders() != null && config.getMaxReminders() > 0) {
			long sentCount = countGuarantorSmsSent(config, SmsTypeEnum.GUARANTOR_INSTALLMENT_DUE_REMINDER, loan,
					installment, guarantor);
			if (sentCount >= config.getMaxReminders())
				return false;
		}
		if (config != null && !shouldSendReminderForGuarantor(config, SmsTypeEnum.GUARANTOR_INSTALLMENT_DUE_REMINDER,
				loan, installment, guarantor, LocalDateTime.now())) {
			return false;
		}

		try {
			MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan, guarantor, true);
			BigDecimal guaranteeAmount = BigDecimal.ZERO;
			BigDecimal guaranteeLimit = BigDecimal.ZERO;
			BigDecimal currentGuaranteeUsed = BigDecimal.ZERO;
			if (gLoan != null) {
				guaranteeAmount = gLoan.getGuaranteeAmount() != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
				guaranteeLimit = gLoan.getGuaranteeLimit() != null ? gLoan.getGuaranteeLimit() : guaranteeAmount;
				BigDecimal guaranteeAmountBalance = gLoan.getGuaranteeAmountBalance() != null
						? gLoan.getGuaranteeAmountBalance()
						: BigDecimal.ZERO;
				if (guaranteeAmount.compareTo(BigDecimal.ZERO) > 0) {
					currentGuaranteeUsed = guaranteeAmount.subtract(guaranteeAmountBalance);
				}
			}
			return smsHandlersService.handleGuarantorInstallmentDueReminder(guarantor, installment, guaranteeAmount,
					guaranteeLimit, currentGuaranteeUsed, reminderId);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean sendGuarantorInstallmentOverdueAlert(MLoanApplication loan, Long reminderId) {
		MInstallments installment = getOverdueInstallment(loan);
		if (installment == null)
			return false;

		boolean success = true;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		for (MNextOfKin guarantor : targetGuarantors) {
			success = success
					&& sendGuarantorInstallmentOverdueAlertToGuarantor(guarantor, installment, loan, reminderId);
		}
		return success;
	}

	public boolean sendGuarantorInstallmentOverdueAlertToGuarantor(MNextOfKin guarantor, MInstallments installment,
			MLoanApplication loan, Long reminderId) {
		MRemindersConfiguration config = reminderConfigRepository.findById(reminderId).orElse(null);
		if (config != null && config.getMaxReminders() != null && config.getMaxReminders() > 0) {
			long sentCount = countGuarantorSmsSent(config, SmsTypeEnum.GUARANTOR_INSTALLMENT_OVERDUE_ALERT, loan,
					installment, guarantor);
			if (sentCount >= config.getMaxReminders())
				return false;
		}
		if (config != null && !shouldSendReminderForGuarantor(config, SmsTypeEnum.GUARANTOR_INSTALLMENT_OVERDUE_ALERT,
				loan, installment, guarantor, LocalDateTime.now())) {
			return false;
		}

		try {
			BigDecimal guaranteeAmount = BigDecimal.ZERO;
			BigDecimal currentGuaranteeUsed = BigDecimal.ZERO;
			MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan, guarantor, true);
			if (gLoan != null) {
				guaranteeAmount = gLoan.getGuaranteeAmount();
				currentGuaranteeUsed = gLoan.getGuaranteeAmount().subtract(gLoan.getGuaranteeAmountBalance());
			}
			return smsHandlersService.handleGuarantorInstallmentOverdueAlert(guarantor, installment, guaranteeAmount,
					currentGuaranteeUsed, reminderId);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean sendGuarantorInstallmentMissedPayment(MLoanApplication loan, Long reminderId) {
		MInstallments installment = getMissedInstallment(loan);
		if (installment == null)
			return false;

		boolean success = true;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		for (MNextOfKin guarantor : targetGuarantors) {
			success = success
					&& sendGuarantorInstallmentMissedPaymentToGuarantor(guarantor, installment, loan, reminderId);
		}
		return success;
	}

	public boolean sendGuarantorInstallmentMissedPaymentToGuarantor(MNextOfKin guarantor, MInstallments installment,
			MLoanApplication loan, Long reminderId) {
		MRemindersConfiguration config = reminderConfigRepository.findById(reminderId).orElse(null);
		if (config != null && config.getMaxReminders() != null && config.getMaxReminders() > 0) {
			long sentCount = countGuarantorSmsSent(config, SmsTypeEnum.GUARANTOR_INSTALLMENT_MISSED_PAYMENT, loan,
					installment, guarantor);
			if (sentCount >= config.getMaxReminders())
				return false;
		}
		if (config != null && !shouldSendReminderForGuarantor(config, SmsTypeEnum.GUARANTOR_INSTALLMENT_MISSED_PAYMENT,
				loan, installment, guarantor, LocalDateTime.now())) {
			return false;
		}

		try {
			BigDecimal guaranteeAmount = BigDecimal.ZERO;
			BigDecimal potentialGuaranteeCall = BigDecimal.ZERO;
			MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan, guarantor, true);
			if (gLoan != null) {
				guaranteeAmount = gLoan.getGuaranteeAmount();
				potentialGuaranteeCall = installment.getBalance().min(guaranteeAmount);
			}
			return smsHandlersService.handleGuarantorInstallmentMissedPayment(guarantor, installment, guaranteeAmount,
					potentialGuaranteeCall, reminderId);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean sendGuarantorInterestAccrualNotification(MLoanApplication loan, Long reminderId) {
		boolean success = true;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		for (MNextOfKin guarantor : targetGuarantors) {
			success = success && sendGuarantorInterestAccrualNotificationToGuarantor(guarantor, loan, reminderId);
		}
		return success;
	}

	public boolean sendGuarantorInterestAccrualNotificationToGuarantor(MNextOfKin guarantor, MLoanApplication loan,
			Long reminderId) {
		MRemindersConfiguration config = reminderConfigRepository.findById(reminderId).orElse(null);
		if (config != null && config.getMaxReminders() != null && config.getMaxReminders() > 0) {
			long sentCount = countGuarantorSmsSent(config, SmsTypeEnum.GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION, loan,
					null, guarantor);
			if (sentCount >= config.getMaxReminders())
				return false;
		}
		if (config != null && !shouldSendReminderForGuarantor(config,
				SmsTypeEnum.GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION, loan, null, guarantor, LocalDateTime.now())) {
			return false;
		}

		try {
			BigDecimal interestAmount = loan.getInterestsEarned() != null ? loan.getInterestsEarned() : BigDecimal.ZERO;
			BigDecimal totalInterestAccrued = interestAmount;
			Date accrualDate = new Date();

			BigDecimal dailyInterestRate = loan.getLoanProductConfiguration() != null
					? loan.getLoanProductConfiguration().getDailyInterestRate()
					: BigDecimal.ZERO;
			BigDecimal weeklyInterestRate = loan.getLoanProductConfiguration() != null
					? loan.getLoanProductConfiguration().getWeeklyInterestRate()
					: BigDecimal.ZERO;
			BigDecimal monthlyInterestRate = loan.getLoanProductConfiguration() != null
					? loan.getLoanProductConfiguration().getMonthlyInterestRate()
					: BigDecimal.ZERO;
			BigDecimal annualInterestRate = loan.getLoanProductConfiguration() != null
					? loan.getLoanProductConfiguration().getAnnualInterestRate()
					: BigDecimal.ZERO;

			BigDecimal currentBalance = loan.getBalance();
			String interestCalculationMethod = "Standard";
			String interestFrequency = loan.getLoanProductConfiguration() != null
					? loan.getLoanProductConfiguration().getInterestFrequency().name()
					: "MONTHLY";
			Date nextAccrualDate = new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);
			String interestPeriod = "Current Period";

			return smsHandlersService.handleGuarantorInterestAccrualNotification(guarantor, loan, interestAmount,
					totalInterestAccrued, accrualDate, dailyInterestRate, weeklyInterestRate, monthlyInterestRate,
					annualInterestRate, currentBalance, interestCalculationMethod, interestFrequency, nextAccrualDate,
					interestPeriod, reminderId, null);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean sendGuarantorPenaltyCalculationNotification(MLoanApplication loan, Long reminderId) {
		boolean success = true;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		for (MNextOfKin guarantor : targetGuarantors) {
			success = success && sendGuarantorPenaltyCalculationNotificationToGuarantor(guarantor, loan, reminderId);
		}
		return success;
	}

	public boolean sendGuarantorPenaltyCalculationNotificationToGuarantor(MNextOfKin guarantor, MLoanApplication loan,
			Long reminderId) {
		MRemindersConfiguration config = reminderConfigRepository.findById(reminderId).orElse(null);
		if (config != null && config.getMaxReminders() != null && config.getMaxReminders() > 0) {
			long sentCount = countGuarantorSmsSent(config, SmsTypeEnum.GUARANTOR_PENALTY_CALCULATION_NOTIFICATION, loan,
					null, guarantor);
			if (sentCount >= config.getMaxReminders())
				return false;
		}
		if (config != null && !shouldSendReminderForGuarantor(config,
				SmsTypeEnum.GUARANTOR_PENALTY_CALCULATION_NOTIFICATION, loan, null, guarantor, LocalDateTime.now())) {
			return false;
		}

		try {
			BigDecimal penaltyAmount = loan.getPenaltyEarned() != null ? loan.getPenaltyEarned() : BigDecimal.ZERO;
			BigDecimal penaltyRate = new BigDecimal("5.0");
			Date calculationDate = new Date();
			String penaltyReason = "Overdue payment";
			Integer overdueDays = (int) calculateDaysOverdue(loan.getDueDate());
			BigDecimal overdueAmount = loan.getBalance();
			BigDecimal totalOutstanding = loan.getBalance();
			String penaltyType = "Late Payment Fee";
			Boolean gracePeriodUsed = false;
			String penaltyFrequency = "Daily";

			return smsHandlersService.handleGuarantorPenaltyCalculationNotification(guarantor, loan, penaltyAmount,
					penaltyRate, calculationDate, penaltyReason, overdueDays, overdueAmount, totalOutstanding,
					penaltyType, gracePeriodUsed, penaltyFrequency, reminderId, null);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean sendGuarantorStatementNotification(MLoanApplication loan, Long reminderId) {
		boolean success = true;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		for (MNextOfKin guarantor : targetGuarantors) {
			success = success && sendGuarantorStatementNotificationToGuarantor(guarantor, loan, reminderId);
		}
		return success;
	}

	public boolean sendGuarantorStatementNotificationToGuarantor(MNextOfKin guarantor, MLoanApplication loan,
			Long reminderId) {
		MRemindersConfiguration config = reminderConfigRepository.findById(reminderId).orElse(null);
		if (config != null && config.getMaxReminders() != null && config.getMaxReminders() > 0) {
			long sentCount = countGuarantorSmsSent(config, SmsTypeEnum.GUARANTOR_STATEMENT_NOTIFICATION, loan, null,
					guarantor);
			if (sentCount >= config.getMaxReminders())
				return false;
		}
		if (config != null && !shouldSendReminderForGuarantor(config, SmsTypeEnum.GUARANTOR_STATEMENT_NOTIFICATION,
				loan, null, guarantor, LocalDateTime.now())) {
			return false;
		}

		try {
			Calendar cal = Calendar.getInstance();
			String month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
			int year = cal.get(Calendar.YEAR);
			String statementPeriod = month + " " + year;
			Date statementDate = new Date();

			MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan, guarantor, true);
			BigDecimal totalGuaranteedAmount = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
			Integer activeGuarantees = 1;
			BigDecimal currentGuaranteeUsed = gLoan != null
					? gLoan.getGuaranteeAmount().subtract(gLoan.getGuaranteeAmountBalance())
					: BigDecimal.ZERO;
			BigDecimal guaranteeUtilization = (gLoan != null
					&& gLoan.getGuaranteeAmount().compareTo(BigDecimal.ZERO) > 0)
							? currentGuaranteeUsed.divide(gLoan.getGuaranteeAmount(), 2, RoundingMode.HALF_UP)
							: BigDecimal.ZERO;
			BigDecimal availableGuarantee = gLoan != null ? gLoan.getGuaranteeAmountBalance() : BigDecimal.ZERO;
			String highestRiskLoan = loan.getDocumentNo();
			BigDecimal totalOutstandingExposure = loan.getBalance();

			return smsHandlersService.handleGuarantorStatementNotification(guarantor, statementPeriod, statementDate,
					totalGuaranteedAmount, activeGuarantees, guaranteeUtilization, availableGuarantee, highestRiskLoan,
					totalOutstandingExposure, loan.getAdOrgID(), loan.getAdClientId(), reminderId);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean sendGuarantorMissedRepaymentAlert(MLoanApplication loan, Long reminderId) {
		boolean success = true;
		boolean borrowerEligible = utils.isBorrowerEligible(loan);
		Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
		for (MNextOfKin guarantor : targetGuarantors) {
			success = success && sendGuarantorMissedRepaymentAlertToGuarantor(guarantor, loan, reminderId);
		}
		return success;
	}

	public boolean sendGuarantorMissedRepaymentAlertToGuarantor(MNextOfKin guarantor, MLoanApplication loan,
			Long reminderId) {
		MRemindersConfiguration config = reminderConfigRepository.findById(reminderId).orElse(null);
		if (config != null && config.getMaxReminders() != null && config.getMaxReminders() > 0) {
			long sentCount = countGuarantorSmsSent(config, SmsTypeEnum.GUARANTOR_MISSED_REPAYMENT_ALERT, loan, null,
					guarantor);
			if (sentCount >= config.getMaxReminders())
				return false;
		}
		if (config != null && !shouldSendReminderForGuarantor(config, SmsTypeEnum.GUARANTOR_MISSED_REPAYMENT_ALERT,
				loan, null, guarantor, LocalDateTime.now())) {
			return false;
		}
		return smsHandlersService.handleGuarantorMissedRepaymentAlert(guarantor, loan, reminderId);
	}

	// -------------------------------------------------------------------------
	// sendReminderByType – dispatches to the appropriate sending method
	// -------------------------------------------------------------------------

	public boolean sendReminderByType(SmsTypeEnum smsType, MLoanApplication loan, MInstallments installment,
			Long reminderId) {
		try {
			switch (smsType) {
			case LOAN_OR_DEBT_DUE_REMINDER:
				return sendLoanDueReminder(loan, reminderId);
			case LOAN_OR_DEBT_OVERDUE_REMINDER:
				return sendLoanOverdueReminder(loan, reminderId);
			case INSTALLMENT_DUE_REMINDER:
				return sendInstallmentDueReminder(installment, reminderId);
			case INSTALLMENT_OVERDUE_REMINDER:
				return sendInstallmentOverdueReminder(installment, reminderId);
			case INSTALLMENT_MISSED_PAYMENT:
				return sendInstallmentMissedPayment(installment, reminderId);
			case INSTALLMENT_PAYMENT_REMINDER:
				return sendInstallmentPaymentReminder(installment, reminderId);
			case MISSED_REPAYMENT_ALERT:
				return sendMissedRepaymentAlert(loan, reminderId);
			case GRACE_PERIOD_EXPIRY_ALERT:
				return sendGracePeriodExpiryAlert(loan, reminderId);
			case STATEMENT_READY_NOTIFICATION:
				return sendStatementReadyNotification(loan, reminderId);
			case INSTALLMENT_GENERATION_NOTIFICATION:
				return sendInstallmentGenerationNotification(installment, reminderId);
			case INSTALLMENT_ADJUSTMENT_NOTIFICATION:
				return sendInstallmentAdjustmentNotification(installment, reminderId);
			case INSTALLMENT_RESCHEDULE_NOTIFICATION:
				return sendInstallmentRescheduleNotification(installment, reminderId);
			case REPAYMENT_SCHEDULE_UPDATE:
				return sendRepaymentScheduleUpdate(loan, reminderId);
			case REPAYMENT_RESCHEDULE_REQUEST:
				return sendRepaymentRescheduleRequest(loan, reminderId);
			case REPAYMENT_RESCHEDULE_APPROVAL:
				return sendRepaymentRescheduleApproval(loan, reminderId);
			case REPAYMENT_RESCHEDULE_REJECTION:
				return sendRepaymentRescheduleRejection(loan, reminderId);
			case LOAN_RESTRUCTURING_NOTIFICATION:
				return sendLoanRestructuringNotification(loan, reminderId);
			case TOP_UP_LOAN_DISBURSEMENT:
				return sendTopUpLoanDisbursement(loan, reminderId);
			case LOAN_CLOSURE_NOTIFICATION:
				return sendLoanClosureNotification(loan, reminderId);
			case AUTO_DEBIT_FAILURE:
				return sendAutoDebitFailure(loan, reminderId);
			case GUARANTOR_PAYMENT_REMINDER:
				return sendGuarantorPaymentReminders(loan, reminderId);
			case GUARANTOR_LOAN_DUE_REMINDER:
				return sendGuarantorLoanDueReminder(loan, reminderId);
			case GUARANTOR_LOAN_OVERDUE_ALERT:
				return sendGuarantorLoanOverdueAlert(loan, reminderId);
			case GUARANTOR_LOAN_DEFAULT_NOTIFICATION:
				return sendGuarantorLoanDefaultNotification(loan, reminderId);
			case GUARANTOR_INSTALLMENT_DUE_REMINDER:
				return sendGuarantorInstallmentDueReminder(loan, reminderId);
			case GUARANTOR_INSTALLMENT_OVERDUE_ALERT:
				return sendGuarantorInstallmentOverdueAlert(loan, reminderId);
			case GUARANTOR_INSTALLMENT_MISSED_PAYMENT:
				return sendGuarantorInstallmentMissedPayment(loan, reminderId);
			case GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION:
				return sendGuarantorInterestAccrualNotification(loan, reminderId);
			case GUARANTOR_PENALTY_CALCULATION_NOTIFICATION:
				return sendGuarantorPenaltyCalculationNotification(loan, reminderId);
			case GUARANTOR_STATEMENT_NOTIFICATION:
				return sendGuarantorStatementNotification(loan, reminderId);
			case GUARANTOR_MISSED_REPAYMENT_ALERT:
				return sendGuarantorMissedRepaymentAlert(loan, reminderId);
			default:
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	// -------------------------------------------------------------------------
	// Helper methods (unchanged)
	// -------------------------------------------------------------------------


	public Set<MNextOfKin> getTargetGuarantors(MLoanApplication loan, boolean borrowerEligible) {
		return loan.getGuarantors() != null ? loan.getGuarantors() : new HashSet<>();
	}

	public boolean hasBeenSentToday(SmsTypeEnum smsType, MLoanApplication loan, MInstallments installment) {
		try {
			LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();

			if (isBorrowerSmsType(smsType)) {
				if (!utils.isBorrowerEligible(loan))
					return false;
				return checkIfBorrowerSmsSentAfter(smsType, loan, installment, startOfDay);
			} else if (isGuarantorSmsType(smsType)) {
				boolean borrowerEligible = utils.isBorrowerEligible(loan);
				Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
				for (MNextOfKin guarantor : targetGuarantors) {
					if (hasBeenSentTodayForGuarantor(smsType, loan, guarantor))
						return true;
				}
				return false;
			} else {
				if (installment != null) {
					return smsRepository.existsBySmsTypeAndLoanIdAndInstallmentIdAndTimesTosendAfter(smsType,
							loan.getLoanApplicationId(), installment.getInstallmentId(), startOfDay);
				} else {
					return smsRepository.existsBySmsTypeAndLoanIdAndTimesTosendAfter(smsType,
							loan.getLoanApplicationId(), startOfDay);
				}
			}
		} catch (Exception e) {
			return false;
		}
	}

	public boolean checkIfBorrowerSmsSentAfter(SmsTypeEnum smsType, MLoanApplication loan, MInstallments installment,
			LocalDateTime after) {
		if (!utils.isBorrowerEligible(loan))
			return false;
		Long loanId = loan.getLoanApplicationId();
		Long installmentId = installment != null ? installment.getInstallmentId() : null;
		if (installmentId != null && isInstallmentLevelSms(smsType)) {
			if (loan.getBorrowerType().equals(BorrowerTypeEnum.INDIVIDUAL)) {
				return smsRepository.existsBySmsTypeAndInstallmentIdAndDocStatusAndIndividualBorrowerIdAndTimesTosendAfter(
						smsType, installmentId, DocStatus.APPROVED,
						loan.getIndividualBorrower().getIndividualBorrowerId(),after);
			}
			if (loan.getBorrowerType().equals(BorrowerTypeEnum.INSTITUTION)) {
				return smsRepository.existsBySmsTypeAndInstallmentIdAndDocStatusAndInstitutionBorrowerIdAndTimesTosendAfter(
						smsType,   installmentId, DocStatus.APPROVED,
						loan.getInstitutionBorrower().getInstitutionBorrowerId(),after);
			} else {
				return smsRepository.existsBySmsTypeAndInstallmentIdAndDocStatusAndGroupBorrowerIdAndTimesTosendAfter(
						smsType,   installmentId, DocStatus.APPROVED,
						loan.getGroupBorrower().getGroupBorrowerId(),after);

			}

		} else {
			if (loan.getBorrowerType().equals(BorrowerTypeEnum.INDIVIDUAL)) {
				return smsRepository.existsBySmsTypeAndLoanIdAndDocStatusAndIndividualBorrowerIdAndTimesTosendAfter(
						smsType,  loanId, DocStatus.APPROVED,
						loan.getIndividualBorrower().getIndividualBorrowerId(),after);
			}
			if (loan.getBorrowerType().equals(BorrowerTypeEnum.INSTITUTION)) {
				return smsRepository.existsBySmsTypeAndLoanIdAndDocStatusAndInstitutionBorrowerIdAndTimesTosendAfter(
						smsType, loanId, DocStatus.APPROVED,
						loan.getInstitutionBorrower().getInstitutionBorrowerId(),after);
			} else {
				return smsRepository.existsBySmsTypeAndLoanIdAndDocStatusAndGroupBorrowerIdAndTimesTosendAfter(
						smsType,  loanId, DocStatus.APPROVED,
						loan.getGroupBorrower().getGroupBorrowerId(),after);

			}

		}
	}

	public boolean hasBeenSentTodayForGuarantor(SmsTypeEnum smsType, MLoanApplication loan, MNextOfKin guarantor) {
		try {
			LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
			return smsRepository.existsBySmsTypeAndLoanIdAndGuarantorIdAndTimesTosendAfter(smsType,
					loan.getLoanApplicationId(), guarantor.getNextOfKinId(), startOfDay);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean hasBeenSentThisMonth(SmsTypeEnum smsType, MLoanApplication loan, MInstallments installment) {
		try {
			LocalDateTime firstDayOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();

			if (isBorrowerSmsType(smsType)) {
				if (!utils.isBorrowerEligible(loan))
					return false;
				return checkIfBorrowerSmsSentAfter(smsType, loan, installment, firstDayOfMonth);
			} else if (isGuarantorSmsType(smsType)) {
				boolean borrowerEligible = utils.isBorrowerEligible(loan);
				Set<MNextOfKin> targetGuarantors = getTargetGuarantors(loan, borrowerEligible);
				for (MNextOfKin guarantor : targetGuarantors) {
					if (smsRepository.existsBySmsTypeAndLoanIdAndGuarantorIdAndTimesTosendAfter(smsType,
							loan.getLoanApplicationId(), guarantor.getNextOfKinId(), firstDayOfMonth)) {
						return true;
					}
				}
				return false;
			} else {
				if (installment != null) {
					return smsRepository.existsBySmsTypeAndLoanIdAndInstallmentIdAndTimesTosendAfter(smsType,
							loan.getLoanApplicationId(), installment.getInstallmentId(), firstDayOfMonth);
				} else {
					return smsRepository.existsBySmsTypeAndLoanIdAndTimesTosendAfter(smsType,
							loan.getLoanApplicationId(), firstDayOfMonth);
				}
			}
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isSameDay(LocalDateTime date1, LocalDateTime date2) {
		if (date1 == null || date2 == null)
			return false;
		return date1.toLocalDate().isEqual(date2.toLocalDate());
	}

	public LocalDateTime convertToLocalDateTime(Date date) {
		if (date == null)
			return null;
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	public long calculateDaysRemaining(Date dueDate) {
		if (dueDate == null)
			return 0;
		LocalDate dueLocal = dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate today = LocalDate.now();
		return ChronoUnit.DAYS.between(today, dueLocal);
	}

	public long calculateDaysOverdue(Date dueDate) {
		if (dueDate == null)
			return 0;
		LocalDate dueLocal = dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate today = LocalDate.now();
		long daysOverdue = ChronoUnit.DAYS.between(dueLocal, today);
		return daysOverdue > 0 ? daysOverdue : 0;
	}

	public boolean hasActiveInstallmentsWithBalance(MLoanApplication loan) {
		try {
			List<MInstallments> activeInstallments = installmentRepository
					.findByIsActiveAndLoanAndBalanceGreaterThanOrderByPeriodEndAsc(true, loan, BigDecimal.ZERO);
			return activeInstallments != null && !activeInstallments.isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	public MInstallments getNextDueInstallment(MLoanApplication loan) {
		try {
			Date now = new Date();
			List<MInstallments> dueInstallments = installmentRepository
					.findByIsActiveAndLoanAndBalanceGreaterThanAndPeriodEndAfterOrderByPeriodEndAsc(true, loan,
							BigDecimal.ZERO, now);
			return dueInstallments.isEmpty() ? null : dueInstallments.get(0);
		} catch (Exception e) {
			return null;
		}
	}

	public MInstallments getOverdueInstallment(MLoanApplication loan) {
		try {
			List<MInstallments> overdueInstallments = installmentRepository
					.findByIsActiveAndLoanAndBalanceGreaterThanAndPeriodEndBeforeOrderByPeriodEndDesc(true, loan,
							BigDecimal.ZERO, new Date());
			return overdueInstallments.isEmpty() ? null : overdueInstallments.get(0);
		} catch (Exception e) {
			return null;
		}
	}

	public MInstallments getMissedInstallment(MLoanApplication loan) {
		MInstallments installment = getOverdueInstallment(loan);
		if (installment == null)
			return null;
		long daysOverdue = calculateDaysOverdue(installment.getPeriodEnd());
		return daysOverdue > 1 ? installment : null;
	}

	public Date calculateGracePeriodEnd(MLoanApplication loan) {
		if (loan.getExpectedDisbursementDate() == null)
			return null;

		Integer gracePeriod = loan.getLoanProductConfiguration() != null
				&& loan.getLoanProductConfiguration().getRepaymentScheduleType() != null
				&& loan.getLoanProductConfiguration().getRepaymentScheduleType()
						.equals(RepaymentScheduleTypeEnum.INSTALLMENTS) ? loan.getGracePeriodToFirstInstallment()
								: loan.getGraceperiod();

		if (gracePeriod == null || gracePeriod == 0) {
			return loan.getExpectedDisbursementDate();
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(loan.getExpectedDisbursementDate());
		cal.add(Calendar.DAY_OF_MONTH, gracePeriod);
		return cal.getTime();
	}

	public boolean isLoanOverdue(MLoanApplication loan) {
		if (loan.getDueDate() == null)
			return false;
		return calculateDaysOverdue(loan.getDueDate()) > 0;
	}

	public boolean isInstallmentOverdue(MInstallments installment) {
		if (installment == null || installment.getPeriodEnd() == null)
			return false;
		return calculateDaysOverdue(installment.getPeriodEnd()) > 0;
	}

	public boolean isFirstDayOfMonth(LocalDateTime dateTime) {
		return dateTime.getDayOfMonth() == 1;
	}

	public Date calculateInterestAccrualDate(MLoanApplication loan) {
		if (loan.getDueDate() == null)
			return new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(loan.getDueDate());
		cal.add(Calendar.DAY_OF_MONTH, -1);
		return cal.getTime();
	}

	// -------------------------------------------------------------------------
	// Public generic method (unchanged)
	// -------------------------------------------------------------------------

	public boolean sendGenericSmsByType(SmsTypeEnum smsType, MLoanApplication loan, MNextOfKin guarantor) {
		Long reminderId = 0L;
		try {
			MRemindersConfiguration config = reminderConfigRepository
					.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(
							utils.getAD_Org_ID(), true, smsType);
			if (config != null) {
				reminderId = config.getReminderId();
			}

			MInstallments installment = installmentRepository
					.findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(true, BigDecimal.ZERO, loan);
			if (installment == null && smsType.getValue().contains("INSTALLMENT")) {
				return false;
			}

			MADSysConfig sys = utils.getOrganizationSystemConfiguratinsByDynamicOrganisation(
					SettingCategoriesEnum.GENERAL_SETTINGS, loan.getAdOrgID());
			if (sys == null || !sys.isAllowSystemNotifications()) {
				return false;
			}

			boolean success = false;

			switch (smsType) {
			case LOAN_APPLICATION_OR_DEBT_REGISTRATION:
				success = smsHandlersService.handleLoanApplicationRegistration(loan, reminderId);
				break;
			case LOAN_APPROVAL_DEBT_APPROVAL:
				success = smsHandlersService.handleLoanApproval(loan, reminderId);
				break;
			case LOAN_REJECTION_DEBT_REJECTION:
				String rejectionReason = "Not specified";
				success = smsHandlersService.handleLoanRejection(loan, rejectionReason, reminderId);
				break;
			case GUARANTOR_APPROVAL_REQUEST:
				if (guarantor != null) {
					success = smsHandlersService.handleGuarantorApprovalRequest(guarantor, loan, reminderId);
				}
				break;
			case GUARANTOR_APPROVAL_CONFIRMATION:
				if (guarantor != null) {
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					Date approvalDate = new Date();
					BigDecimal guaranteeAmount = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					BigDecimal guaranteeLimit = gLoan != null && gLoan.getGuaranteeLimit() != null
							? gLoan.getGuaranteeLimit()
							: guaranteeAmount;
					success = smsHandlersService.handleGuarantorApprovalConfirmation(guarantor, loan, approvalDate,
							guaranteeAmount, guaranteeLimit, reminderId);
				}
				break;
			case GUARANTOR_APPROVAL_REJECTION:
				if (guarantor != null) {
					Date rejectionDate = new Date();
					String guarantorRejectionReason = "Not specified";
					success = smsHandlersService.handleGuarantorApprovalRejection(guarantor, loan, rejectionDate,
							guarantorRejectionReason, reminderId);
				}
				break;
			case GUARANTOR_LOAN_ASSIGNMENT_NOTIFICATION:
				if (guarantor != null) {
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					Date approvalDate = new Date();
					BigDecimal guaranteeAmount = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					BigDecimal guaranteeLimit = gLoan != null && gLoan.getGuaranteeLimit() != null
							? gLoan.getGuaranteeLimit()
							: guaranteeAmount;
					success = smsHandlersService.handleGuarantorLoanAssignmentNotification(guarantor, loan,
							approvalDate, guaranteeAmount, guaranteeLimit, reminderId);
				}
				break;
			case LOAN_OR_DEBT_DUE_REMINDER:
				if (utils.isBorrowerEligible(loan)) {
					success = smsHandlersService.handleLoanDueReminder(loan, reminderId);
				}
				break;
			case LOAN_OR_DEBT_OVERDUE_REMINDER:
				if (utils.isBorrowerEligible(loan)) {
					success = smsHandlersService.handleLoanOverdueReminder(loan, reminderId);
				}
				break;
			case MISSED_REPAYMENT_ALERT:
				if (utils.isBorrowerEligible(loan)) {
					success = smsHandlersService.handleMissedRepaymentAlert(loan, reminderId);
				}
				break;
			case GUARANTOR_LOAN_DUE_REMINDER:
				if (guarantor != null) {
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					BigDecimal currentGuaranteeUsed = BigDecimal.ZERO;
					BigDecimal guaranteeRemaining = BigDecimal.ZERO;
					Date gracePeriodEndDate = calculateGracePeriodEnd(loan);
					BigDecimal interestAmount = loan.getInterestsEarned() != null ? loan.getInterestsEarned()
							: BigDecimal.ZERO;
					BigDecimal penaltyAmount = loan.getPenaltyEarned() != null ? loan.getPenaltyEarned()
							: BigDecimal.ZERO;
					if (gLoan != null) {
						currentGuaranteeUsed = gLoan.getGuaranteeAmount().subtract(gLoan.getGuaranteeAmountBalance());
						guaranteeRemaining = gLoan.getGuaranteeAmountBalance();
					}
					success = smsHandlersService.handleGuarantorLoanDueReminder(guarantor, loan, loan.getDueDate(),
							loan.getBalance(), interestAmount, penaltyAmount, gracePeriodEndDate, currentGuaranteeUsed,
							guaranteeRemaining, reminderId);
				}
				break;
			case GUARANTOR_LOAN_OVERDUE_ALERT:
				if (guarantor != null) {
					success = smsHandlersService.handleGuarantorLoanOverdueAlert(guarantor, loan, reminderId);
				}
				break;
			case GUARANTOR_LOAN_DEFAULT_NOTIFICATION:
				if (guarantor != null) {
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					BigDecimal amountOverDue = loan.getBalance();
					Long daysOverDue = calculateDaysOverdue(loan.getDueDate());
					BigDecimal totalOutstanding = loan.getBalance();
					Date defaultDate = new Date();
					BigDecimal guaranteeAmount = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					success = smsHandlersService.handleGuarantorLoanDefaultNotification(guarantor, loan, amountOverDue,
							daysOverDue, totalOutstanding, defaultDate, guaranteeAmount, reminderId);
				}
				break;
			case INSTALLMENT_DUE_REMINDER:
				if (utils.isBorrowerEligible(loan)) {
					MInstallments dueInstallment = installment != null ? installment : getNextDueInstallment(loan);
					if (dueInstallment != null) {
						success = smsHandlersService.handleInstallmentDueReminder(dueInstallment, reminderId);
					}
				}
				break;
			case INSTALLMENT_OVERDUE_REMINDER:
				if (utils.isBorrowerEligible(loan)) {
					MInstallments overdueInstallment = installment != null ? installment : getOverdueInstallment(loan);
					if (overdueInstallment != null) {
						success = smsHandlersService.handleInstallmentOverdueReminder(overdueInstallment, reminderId);
					}
				}
				break;
			case INSTALLMENT_MISSED_PAYMENT:
				if (utils.isBorrowerEligible(loan)) {
					MInstallments missedInstallment = installment != null ? installment : getMissedInstallment(loan);
					if (missedInstallment != null) {
						success = smsHandlersService.handleInstallmentMissedPayment(missedInstallment, reminderId);
					}
				}
				break;
			case INSTALLMENT_PAYMENT_REMINDER:
				if (utils.isBorrowerEligible(loan)) {
					MInstallments paymentReminderInstallment = installment != null ? installment
							: getNextDueInstallment(loan);
					if (paymentReminderInstallment != null) {
						success = smsHandlersService.handleInstallmentPaymentReminder(paymentReminderInstallment,
								reminderId);
					}
				}
				break;
			case GUARANTOR_INSTALLMENT_DUE_REMINDER:
				if (guarantor != null) {
					MInstallments dueInst = installment != null ? installment : getNextDueInstallment(loan);
					if (dueInst != null) {
						MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
								guarantor, true);
						BigDecimal guaranteeAmount = BigDecimal.ZERO;
						BigDecimal guaranteeLimit = BigDecimal.ZERO;
						BigDecimal currentGuaranteeUsed = BigDecimal.ZERO;
						if (gLoan != null) {
							guaranteeAmount = gLoan.getGuaranteeAmount();
							guaranteeLimit = gLoan.getGuaranteeLimit() != null ? gLoan.getGuaranteeLimit()
									: guaranteeAmount;
							currentGuaranteeUsed = gLoan.getGuaranteeAmount()
									.subtract(gLoan.getGuaranteeAmountBalance());
						}
						success = smsHandlersService.handleGuarantorInstallmentDueReminder(guarantor, dueInst,
								guaranteeAmount, guaranteeLimit, currentGuaranteeUsed, reminderId);
					}
				}
				break;
			case GUARANTOR_INSTALLMENT_OVERDUE_ALERT:
				if (guarantor != null) {
					MInstallments overdueInst = installment != null ? installment : getOverdueInstallment(loan);
					if (overdueInst != null) {
						MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
								guarantor, true);
						BigDecimal guaranteeAmount = BigDecimal.ZERO;
						BigDecimal currentGuaranteeUsed = BigDecimal.ZERO;
						if (gLoan != null) {
							guaranteeAmount = gLoan.getGuaranteeAmount();
							currentGuaranteeUsed = gLoan.getGuaranteeAmount()
									.subtract(gLoan.getGuaranteeAmountBalance());
						}
						success = smsHandlersService.handleGuarantorInstallmentOverdueAlert(guarantor, overdueInst,
								guaranteeAmount, currentGuaranteeUsed, reminderId);
					}
				}
				break;
			case GUARANTOR_INSTALLMENT_MISSED_PAYMENT:
				if (guarantor != null) {
					MInstallments missedInst = installment != null ? installment : getMissedInstallment(loan);
					if (missedInst != null) {
						MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
								guarantor, true);
						BigDecimal guaranteeAmount = BigDecimal.ZERO;
						BigDecimal potentialGuaranteeCall = BigDecimal.ZERO;
						if (gLoan != null) {
							guaranteeAmount = gLoan.getGuaranteeAmount();
							potentialGuaranteeCall = missedInst.getBalance().min(guaranteeAmount);
						}
						success = smsHandlersService.handleGuarantorInstallmentMissedPayment(guarantor, missedInst,
								guaranteeAmount, potentialGuaranteeCall, reminderId);
					}
				}
				break;
			case GUARANTOR_PARTIAL_PAYMENT_NOTIFICATION:
				if (guarantor != null) {
					BigDecimal amountPaid = loan.getBalance().multiply(new BigDecimal("0.5"));
					BigDecimal remainingBalance = loan.getBalance().subtract(amountPaid);
					BigDecimal totalOutstanding = remainingBalance;
					Date paymentDate = new Date();
					success = smsHandlersService.handleGuarantorPartialPaymentNotification(guarantor, loan, amountPaid,
							remainingBalance, totalOutstanding, paymentDate, reminderId);
				}
				break;
			case GUARANTOR_FULL_REPAYMENT_NOTIFICATION:
				if (guarantor != null) {
					BigDecimal totalRepaid = smsHandlersService.calculateTotalPaid(loan);
					Date completionDate = new Date();
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					BigDecimal guaranteeReleased = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					BigDecimal guaranteeAmount = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					success = smsHandlersService.handleGuarantorFullRepaymentNotification(guarantor, loan, totalRepaid,
							completionDate, guaranteeReleased, guaranteeAmount, reminderId);
				}
				break;
			case GUARANTOR_INSTALLMENT_PAYMENT_RECEIVED:
				if (guarantor != null && installment != null) {
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					BigDecimal amountPaid = installment.getAmount();
					Date paymentDate = new Date();
					BigDecimal guaranteeAmount = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					BigDecimal currentGuaranteeUsed = gLoan != null
							? gLoan.getGuaranteeAmount().subtract(gLoan.getGuaranteeAmountBalance())
							: BigDecimal.ZERO;
					BigDecimal guaranteeUtilization = gLoan != null
							&& gLoan.getGuaranteeAmount().compareTo(BigDecimal.ZERO) > 0
									? currentGuaranteeUsed.divide(gLoan.getGuaranteeAmount(), 2, RoundingMode.HALF_UP)
									: BigDecimal.ZERO;
					success = smsHandlersService.handleGuarantorInstallmentPaymentReceived(guarantor, installment,
							amountPaid, paymentDate, guaranteeAmount, guaranteeUtilization, reminderId);
				}
				break;
			case INTEREST_CALCULATION_NOTIFICATION:
				BigDecimal interestAmount = loan.getInterestsEarned() != null ? loan.getInterestsEarned()
						: BigDecimal.ZERO;
				BigDecimal totalInterests = interestAmount;
				Date date = new Date();
				String interestRate = loan.getLoanProductConfiguration() != null
						? loan.getLoanProductConfiguration().getAnnualInterestRate() + "%"
						: "0%";
				success = smsHandlersService.handleInterestCalculationNotification(loan, interestAmount, totalInterests,
						date, interestRate, reminderId, null);
				break;
			case GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION:
				if (guarantor != null) {
					BigDecimal guarantorInterestAmount = loan.getInterestsEarned() != null ? loan.getInterestsEarned()
							: BigDecimal.ZERO;
					BigDecimal totalInterestAccrued = guarantorInterestAmount;
					Date accrualDate = new Date();
					BigDecimal dailyInterestRate = loan.getLoanProductConfiguration() != null
							? loan.getLoanProductConfiguration().getDailyInterestRate()
							: BigDecimal.ZERO;
					BigDecimal weeklyInterestRate = loan.getLoanProductConfiguration() != null
							? loan.getLoanProductConfiguration().getWeeklyInterestRate()
							: BigDecimal.ZERO;
					BigDecimal monthlyInterestRate = loan.getLoanProductConfiguration() != null
							? loan.getLoanProductConfiguration().getMonthlyInterestRate()
							: BigDecimal.ZERO;
					BigDecimal annualInterestRate = loan.getLoanProductConfiguration() != null
							? loan.getLoanProductConfiguration().getAnnualInterestRate()
							: BigDecimal.ZERO;
					BigDecimal currentBalance = loan.getBalance();
					String interestCalculationMethod = "Compound";
					String interestFrequency = "Monthly";
					Date nextAccrualDate = new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);
					String interestPeriod = "Current Month";
					success = smsHandlersService.handleGuarantorInterestAccrualNotification(guarantor, loan,
							guarantorInterestAmount, totalInterestAccrued, accrualDate, dailyInterestRate,
							weeklyInterestRate, monthlyInterestRate, annualInterestRate, currentBalance,
							interestCalculationMethod, interestFrequency, nextAccrualDate, interestPeriod, reminderId,
							null);
				}
				break;
			case GUARANTOR_INTEREST_WAIVER_NOTIFICATION:
				if (guarantor != null) {
					BigDecimal waivedInterestAmount = BigDecimal.ZERO;
					String interestWaiverReason = "Good customer relationship";
					Date waiverDate = new Date();
					BigDecimal previousInterestAmount = loan.getInterestsEarned() != null ? loan.getInterestsEarned()
							: BigDecimal.ZERO;
					String approvedBy = "System";
					success = smsHandlersService.handleGuarantorInterestWaiverNotification(guarantor, loan,
							waivedInterestAmount, interestWaiverReason, waiverDate, previousInterestAmount, approvedBy,
							reminderId, null);
				}
				break;
			case PENALTY_APPLIED_NOTIFICATION:
				BigDecimal penaltyAmount = loan.getPenaltyEarned() != null ? loan.getPenaltyEarned() : BigDecimal.ZERO;
				BigDecimal balance = loan.getBalance();
				String penaltyReason = "Late payment";
				Date applicationDate = new Date();
				success = smsHandlersService.handlePenaltyAppliedNotification(loan, penaltyAmount, balance,
						penaltyReason, applicationDate, reminderId, null);
				break;
			case PENALTY_WAIVER_NOTIFICATION:
				BigDecimal waivedPenaltyAmount = BigDecimal.ZERO;
				BigDecimal updatedBalance = loan.getBalance();
				Date waiverDatePen = new Date();
				String approvedByPen = "System";
				String reasonPen = "Penalty waived";
				success = smsHandlersService.handlePenaltyWaiverNotification(loan, waivedPenaltyAmount, updatedBalance,
						waiverDatePen, reasonPen, approvedByPen, reminderId, null);
				break;
			case GUARANTOR_PENALTY_CALCULATION_NOTIFICATION:
				if (guarantor != null) {
					BigDecimal guarantorPenaltyAmount = loan.getPenaltyEarned() != null ? loan.getPenaltyEarned()
							: BigDecimal.ZERO;
					BigDecimal penaltyRateCalc = new BigDecimal("5.0");
					Date calculationDate = new Date();
					String penaltyReasonCalc = "Overdue payment";
					Integer overdueDays = (int) calculateDaysOverdue(loan.getDueDate());
					BigDecimal overdueAmount = loan.getBalance();
					BigDecimal totalOutstandingCalc = loan.getBalance();
					String penaltyType = "Late Payment Fee";
					Boolean gracePeriodUsed = false;
					String penaltyFrequency = "Daily";
					success = smsHandlersService.handleGuarantorPenaltyCalculationNotification(guarantor, loan,
							guarantorPenaltyAmount, penaltyRateCalc, calculationDate, penaltyReasonCalc, overdueDays,
							overdueAmount, totalOutstandingCalc, penaltyType, gracePeriodUsed, penaltyFrequency,
							reminderId, null);
				}
				break;
			case GUARANTOR_PENALTY_WAIVER_NOTIFICATION:
				if (guarantor != null) {
					BigDecimal waivedGuarantorPenalty = BigDecimal.ZERO;
					String penaltyWaiverReason = "Customer loyalty";
					Date penaltyWaiverDate = new Date();
					BigDecimal previousPenaltyAmount = loan.getPenaltyEarned() != null ? loan.getPenaltyEarned()
							: BigDecimal.ZERO;
					String approvedByWaiver = "Manager";
					success = smsHandlersService.handleGuarantorPenaltyWaiverNotification(guarantor, loan,
							waivedGuarantorPenalty, penaltyWaiverReason, penaltyWaiverDate, previousPenaltyAmount,
							approvedByWaiver, reminderId, null);
				}
				break;
			case INSTALLMENT_GENERATION_NOTIFICATION:
				if (installment != null) {
					success = smsHandlersService.handleInstallmentGenerationNotification(installment, reminderId);
				}
				break;
			case INSTALLMENT_ADJUSTMENT_NOTIFICATION:
				if (installment != null) {
					BigDecimal oldAmount = installment.getAmount();
					String reasonAdjust = "Schedule adjustment";
					String adjustmentType = "Manual";
					success = smsHandlersService.handleInstallmentAdjustmentNotification(installment, oldAmount,
							reasonAdjust, adjustmentType, reminderId);
				}
				break;
			case GUARANTOR_INSTALLMENT_ADJUSTMENT:
				if (guarantor != null && installment != null) {
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					BigDecimal oldInstallmentAmount = installment.getAmount();
					BigDecimal newInstallmentAmount = installment.getAmount();
					Date adjustmentDate = new Date();
					String reasonAdjustGuar = "Installment adjustment";
					String adjustmentTypeGuar = "Manual";
					BigDecimal guaranteeAmountAdj = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					success = smsHandlersService.handleGuarantorInstallmentAdjustment(guarantor, installment,
							oldInstallmentAmount, newInstallmentAmount, adjustmentDate, reasonAdjustGuar,
							adjustmentTypeGuar, guaranteeAmountAdj, reminderId);
				}
				break;
			case INSTALLMENT_RESCHEDULE_NOTIFICATION:
				if (installment != null) {
					Date oldDueDate = installment.getPeriodEnd();
					Date newDueDate = new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
					String reasonResched = "Customer request";
					BigDecimal rescheduleFee = BigDecimal.ZERO;
					success = smsHandlersService.handleInstallmentRescheduleNotification(installment, oldDueDate,
							newDueDate, reasonResched, rescheduleFee, reminderId);
				}
				break;
			case GUARANTOR_INSTALLMENT_RESCHEDULE:
				if (guarantor != null && installment != null) {
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					Date oldDueDate = installment.getPeriodEnd();
					Date newDueDate = new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
					Date rescheduleDate = new Date();
					String reasonReschedGuar = "Customer request";
					BigDecimal rescheduleFeeGuar = BigDecimal.ZERO;
					String newPaymentPlan = "Extended payment plan";
					BigDecimal guaranteeAmountResched = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					success = smsHandlersService.handleGuarantorInstallmentReschedule(guarantor, installment,
							oldDueDate, newDueDate, rescheduleDate, reasonReschedGuar, rescheduleFeeGuar,
							newPaymentPlan, guaranteeAmountResched, reminderId);
				}
				break;
			case GRACE_PERIOD_EXPIRY_ALERT:
				if (utils.isBorrowerEligible(loan)) {
					success = smsHandlersService.handleGracePeriodExpiryAlert(loan, reminderId);
				}
				break;
			case REPAYMENT_SCHEDULE_UPDATE:
				BigDecimal newInstallmentAmountSched = loan.getBalance();
				Date nextDueDate = loan.getDueDate();
				Integer remainingInstallments = 1;
				String reasonSched = "Schedule update";
				success = smsHandlersService.handleRepaymentScheduleUpdate(loan, newInstallmentAmountSched, nextDueDate,
						remainingInstallments, reasonSched, reminderId);
				break;
			case REPAYMENT_RESCHEDULE_REQUEST:
				Date requestDate = new Date();
				String status = "Pending";
				success = smsHandlersService.handleRepaymentRescheduleRequest(loan, requestDate, status, reminderId);
				break;
			case REPAYMENT_RESCHEDULE_APPROVAL:
				BigDecimal newRescheduleInstallmentAmount = loan.getBalance();
				Integer rescheduleRemainingInstallments = 1;
				Integer newTerm = loan.getTermInDays();
				Date approvalDate = new Date();
				success = smsHandlersService.handleRepaymentRescheduleApproval(loan, newRescheduleInstallmentAmount,
						rescheduleRemainingInstallments, newTerm, approvalDate, reminderId);
				break;
			case REPAYMENT_RESCHEDULE_REJECTION:
				String rescheduleRejectionReason = "Insufficient documentation";
				Date rescheduleRejectionDate = new Date();
				success = smsHandlersService.handleRepaymentRescheduleRejection(loan, rescheduleRejectionReason,
						rescheduleRejectionDate, reminderId);
				break;
			case LOAN_RESTRUCTURING_NOTIFICATION:
				BigDecimal newPrincipal = loan.getBalance();
				Integer restructuringNewTerm = loan.getTermInDays();
				BigDecimal newInstallmentRestruct = loan.getBalance();
				BigDecimal remainingBalance = loan.getBalance();
				Date effectiveDate = new Date();
				success = smsHandlersService.handleLoanRestructuringNotification(loan, newPrincipal,
						restructuringNewTerm, newInstallmentRestruct, remainingBalance, effectiveDate, reminderId);
				break;
			case GUARANTOR_LOAN_RESTRUCTURING:
				if (guarantor != null) {
					BigDecimal guarantorNewInstallmentAmount = loan.getBalance();
					Integer guarantorNewTerm = loan.getTermInDays();
					BigDecimal guarantorNewPrincipal = loan.getBalance();
					Date guarantorEffectiveDate = new Date();
					BigDecimal guarantorRemainingBalance = loan.getBalance();
					success = smsHandlersService.handleGuarantorLoanRestructuring(guarantor, loan,
							guarantorNewInstallmentAmount, guarantorNewTerm, guarantorNewPrincipal,
							guarantorEffectiveDate, guarantorRemainingBalance, reminderId);
				}
				break;
			case TOP_UP_LOAN_DISBURSEMENT:
				BigDecimal topUpAmount = loan.getBalance();
				BigDecimal topUpTotalOutstanding = loan.getBalance();
				Date repaymentStartDate = new Date();
				success = smsHandlersService.handleTopUpLoanDisbursement(loan, topUpAmount, topUpTotalOutstanding,
						repaymentStartDate, reminderId);
				break;
			case LOAN_CLOSURE_NOTIFICATION:
				BigDecimal settlementAmount = smsHandlersService.calculateTotalPaid(loan);
				success = smsHandlersService.handleLoanClosureNotification(loan, settlementAmount, reminderId);
				break;
			case GUARANTOR_LOAN_SETTLEMENT:
				if (guarantor != null) {
					BigDecimal guarantorSettlementAmount = smsHandlersService.calculateTotalPaid(loan);
					Date closureDate = new Date();
					BigDecimal totalRepaid = smsHandlersService.calculateTotalPaid(loan);
					Date guaranteeReleasedDate = new Date();
					success = smsHandlersService.handleGuarantorLoanSettlement(guarantor, loan,
							guarantorSettlementAmount, closureDate, totalRepaid, guaranteeReleasedDate, reminderId);
				}
				break;
			case GUARANTOR_LOAN_CLOSURE:
				if (guarantor != null) {
					Date guarantorClosureDate = new Date();
					BigDecimal guarantorTotalRepaid = smsHandlersService.calculateTotalPaid(loan);
					BigDecimal guarantorSettlementAmount = smsHandlersService.calculateTotalPaid(loan);
					Date guaranteeCompletionDate = new Date();
					success = smsHandlersService.handleGuarantorLoanClosure(guarantor, loan, guarantorClosureDate,
							guarantorTotalRepaid, guarantorSettlementAmount, guaranteeCompletionDate, reminderId);
				}
				break;
			case GUARANTOR_RELEASE_NOTIFICATION:
				if (guarantor != null) {
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					Date releaseDate = new Date();
					BigDecimal guaranteeAmount = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					String releaseReason = "Loan fully settled";
					String loanStatus = "CLOSED";
					BigDecimal totalRepaidRel = smsHandlersService.calculateTotalPaid(loan);
					success = smsHandlersService.handleGuarantorReleaseNotification(guarantor, loan, releaseDate,
							guaranteeAmount, releaseReason, loanStatus, totalRepaidRel, reminderId);
				}
				break;
			case STATEMENT_READY_NOTIFICATION:
				if (utils.isBorrowerEligible(loan)) {
					success = smsHandlersService.handleStatementReadyNotification(loan, reminderId);
				}
				break;
			case GUARANTOR_STATEMENT_NOTIFICATION:
				if (guarantor != null) {
					Calendar cal = Calendar.getInstance();
					String month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
					int year = cal.get(Calendar.YEAR);
					String statementPeriod = month + " " + year;
					Date statementDate = new Date();
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					BigDecimal totalGuaranteedAmount = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					Integer activeGuarantees = 1;
					BigDecimal currentGuaranteeUsed = gLoan != null
							? gLoan.getGuaranteeAmount().subtract(gLoan.getGuaranteeAmountBalance())
							: BigDecimal.ZERO;
					BigDecimal guaranteeUtilization = gLoan != null
							&& gLoan.getGuaranteeAmount().compareTo(BigDecimal.ZERO) > 0
									? currentGuaranteeUsed.divide(gLoan.getGuaranteeAmount(), 2, RoundingMode.HALF_UP)
									: BigDecimal.ZERO;
					BigDecimal availableGuarantee = gLoan != null ? gLoan.getGuaranteeAmountBalance() : BigDecimal.ZERO;
					String highestRiskLoan = loan.getDocumentNo();
					BigDecimal totalOutstandingExposure = loan.getBalance();
					success = smsHandlersService.handleGuarantorStatementNotification(guarantor, statementPeriod,
							statementDate, totalGuaranteedAmount, activeGuarantees, guaranteeUtilization,
							availableGuarantee, highestRiskLoan, totalOutstandingExposure, loan.getAdOrgID(),
							loan.getAdClientId(), reminderId);
				}
				break;
			case AUTO_DEBIT_FAILURE:
				BigDecimal amountDue = loan.getBalance();
				String failureReason = "Insufficient funds";
				Date retryDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
				String paymentMethod = "Auto Debit";
				success = smsHandlersService.handleAutoDebitFailure(loan, amountDue, failureReason, retryDate,
						paymentMethod, reminderId);
				break;
			case GUARANTOR_LIMIT_UPDATE_NOTIFICATION:
				if (guarantor != null) {
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					BigDecimal oldGuaranteeLimit = gLoan != null && gLoan.getGuaranteeLimit() != null
							? gLoan.getGuaranteeLimit()
							: BigDecimal.ZERO;
					BigDecimal newGuaranteeLimit = oldGuaranteeLimit.multiply(new BigDecimal("1.1"));
					Date updateDate = new Date();
					String limitUpdateReason = "Credit limit increase";
					Date effectiveDateLimit = new Date();
					String approvedByLimit = "System";
					BigDecimal totalActiveGuarantees = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					BigDecimal availableGuarantee = gLoan != null ? gLoan.getGuaranteeAmountBalance() : BigDecimal.ZERO;
					success = smsHandlersService.handleGuarantorLimitUpdateNotification(guarantor, oldGuaranteeLimit,
							newGuaranteeLimit, updateDate, limitUpdateReason, effectiveDateLimit, approvedByLimit,
							totalActiveGuarantees, availableGuarantee, loan.getAdOrgID(), loan.getAdClientId(),
							reminderId);
				}
				break;
			case GUARANTOR_STATUS_CHANGE_NOTIFICATION:
				if (guarantor != null) {
					String oldStatus = "ACTIVE";
					String newStatus = "INACTIVE";
					Date changeDate = new Date();
					String statusChangeReason = "Requested by customer";
					Date effectiveDateStatus = new Date();
					String approvedByStatus = "Manager";
					Integer affectedLoansCount = 1;
					BigDecimal outstandingBalance = loan.getBalance();
					success = smsHandlersService.handleGuarantorStatusChangeNotification(guarantor, oldStatus,
							newStatus, changeDate, statusChangeReason, effectiveDateStatus, approvedByStatus,
							affectedLoansCount, outstandingBalance, loan.getAdOrgID(), loan.getAdClientId(),
							reminderId);
				}
				break;
			case GUARANTOR_CALL_NOTIFICATION:
				if (guarantor != null) {
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					BigDecimal callAmount = loan.getBalance();
					BigDecimal totalOutstanding = loan.getBalance();
					Date callDate = new Date();
					String callReason = "Overdue payment";
					Integer gracePeriodForPayment = 7;
					Date paymentDeadline = new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
					BigDecimal guaranteeAmount = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					BigDecimal currentGuaranteeUsed = gLoan != null
							? gLoan.getGuaranteeAmount().subtract(gLoan.getGuaranteeAmountBalance())
							: BigDecimal.ZERO;
					String recoveryContact = "Customer Service: 0800-XXX-XXX";
					success = smsHandlersService.handleGuarantorCallNotification(guarantor, loan, callAmount,
							totalOutstanding, callDate, callReason, gracePeriodForPayment, paymentDeadline,
							guaranteeAmount, currentGuaranteeUsed, recoveryContact, reminderId);
				}
				break;
			case GUARANTOR_RECOVERY_NOTIFICATION:
				if (guarantor != null) {
					MGuarantorLoan gLoan = guarantorLoanRepository.findTop1ByLoanAndGuarantorAndIsActive(loan,
							guarantor, true);
					BigDecimal recoveryAmount = loan.getBalance();
					Date recoveryDate = new Date();
					String recoveryMethod = "Bank Transfer";
					BigDecimal remainingBalanceRec = BigDecimal.ZERO;
					String recoveryAgent = "John Doe";
					BigDecimal guaranteeAmountRec = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					BigDecimal guaranteeReleased = gLoan != null ? gLoan.getGuaranteeAmount() : BigDecimal.ZERO;
					success = smsHandlersService.handleGuarantorRecoveryNotification(guarantor, loan, recoveryAmount,
							recoveryDate, recoveryMethod, remainingBalanceRec, recoveryAgent, guaranteeAmountRec,
							guaranteeReleased, reminderId);
				}
				break;
			case GUARANTOR_MISSED_REPAYMENT_ALERT:
				if (guarantor != null) {
					success = smsHandlersService.handleGuarantorMissedRepaymentAlert(guarantor, loan, reminderId);
				}
				break;
			case GUARANTOR_PAYMENT_REMINDER:
				if (guarantor != null) {
					success = smsHandlersService.handleGuarantorPaymentReminder(guarantor, loan, reminderId);
				}
				break;
			default:
				success = false;
				break;
			}
			return success;
		} catch (Exception e) {
			return false;
		}
	}
}