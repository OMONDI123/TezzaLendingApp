package co.ke.tezza.loanapp.repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;

@Repository
public interface InstallmentRepository extends JpaRepository<MInstallments, Long> {
	List<MInstallments> findByIsActiveAndBalanceGreaterThanOrderByInstallmentIdAsc(boolean active, BigDecimal balance);

	List<MInstallments> findByIsActiveAndLoanOrderByInstallmentIdAsc(boolean active, MLoanApplication loan);

	MInstallments findTop1ByIsActiveAndBalanceGreaterThanAndLoanOrderByInstallmentIdAsc(boolean active,
			BigDecimal balance, MLoanApplication loan);

	List<MInstallments> findByLoanAndIsActiveOrderByPeriodEndAsc(MLoanApplication app, boolean b);

	List<MInstallments> findByLoanAndIsActiveOrderByCreatedDesc(MLoanApplication app, boolean b);

	List<MInstallments> findByIsActiveAndLoanOrderByPeriodEndAsc(boolean b, MLoanApplication loan);

	// ============ NEW METHODS ADDED ============

	/**
	 * Find installments by loan with balance greater than zero, ordered by period
	 * end (ascending)
	 */
	List<MInstallments> findByIsActiveAndLoanAndBalanceGreaterThanOrderByPeriodEndAsc(boolean active,
			MLoanApplication loan, BigDecimal balance);

	/**
	 * Find the first installment with balance greater than zero for a loan, ordered
	 * by period end
	 */
	Optional<MInstallments> findFirstByIsActiveAndLoanAndBalanceGreaterThanOrderByPeriodEndAsc(boolean active,
			MLoanApplication loan, BigDecimal balance);

	/**
	 * Find overdue installments (period end before current date) with balance
	 */
	List<MInstallments> findByIsActiveAndLoanAndBalanceGreaterThanAndPeriodEndBeforeOrderByPeriodEndAsc(boolean active,
			MLoanApplication loan, BigDecimal balance, Date currentDate);

	/**
	 * Find installments due soon (within specified days)
	 */
	@Query("SELECT i FROM MInstallments i WHERE i.isActive = true AND i.loan = :loan AND i.balance > 0 AND i.periodEnd BETWEEN :startDate AND :endDate ORDER BY i.periodEnd ASC")
	List<MInstallments> findDueInstallmentsInRange(@Param("loan") MLoanApplication loan,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate);

	/**
	 * Find installments by loan ordered by period start (ascending)
	 */
	List<MInstallments> findByIsActiveAndLoanOrderByPeriodStartAsc(boolean active, MLoanApplication loan);

	/**
	 * Find the next installment after a given installment
	 */
	@Query("SELECT i FROM MInstallments i WHERE i.isActive = true AND i.loan = :loan AND i.periodStart > :currentPeriodStart ORDER BY i.periodStart ASC")
	List<MInstallments> findNextInstallments(@Param("loan") MLoanApplication loan,
			@Param("currentPeriodStart") Date currentPeriodStart);

	/**
	 * Find installments with reminders sent less than max reminders
	 */
	List<MInstallments> findByIsActiveAndLoanAndNoOfRemindersSentLessThanOrderByPeriodEndAsc(boolean active,
			MLoanApplication loan, Integer maxReminders);

	/**
	 * Find installments that haven't had reminders sent today
	 */
	@Query("SELECT i FROM MInstallments i WHERE i.isActive = true AND i.loan = :loan AND i.balance > 0 AND (i.lastReminderSent IS NULL OR DATE(i.lastReminderSent) != CURRENT_DATE) ORDER BY i.periodEnd ASC")
	List<MInstallments> findInstallmentsWithoutTodayReminder(@Param("loan") MLoanApplication loan);

	/**
	 * Count active installments with balance for a loan
	 */
	long countByIsActiveAndLoanAndBalanceGreaterThan(boolean active, MLoanApplication loan, BigDecimal balance);

	/**
	 * Find installment by loan and period end date
	 */
	Optional<MInstallments> findByIsActiveAndLoanAndPeriodEnd(boolean active, MLoanApplication loan, Date periodEnd);

	/**
	 * Find installments with penalty calculation due
	 */
	List<MInstallments> findByIsActiveAndLoanAndNextPenaltyCalculationDateBeforeOrderByNextPenaltyCalculationDateAsc(
			boolean active, MLoanApplication loan, Date currentDate);

	/**
	 * Find installments that are currently in their grace period
	 */
	@Query("SELECT i FROM MInstallments i WHERE i.isActive = true AND i.loan = :loan AND i.balance > 0 AND i.periodStart <= :currentDate AND i.periodEnd >= :currentDate ORDER BY i.periodEnd ASC")
	List<MInstallments> findCurrentInstallments(@Param("loan") MLoanApplication loan,
			@Param("currentDate") Date currentDate);

	/**
	 * Find the earliest installment with balance for a loan
	 */
	Optional<MInstallments> findTopByIsActiveAndLoanAndBalanceGreaterThanOrderByPeriodEndAsc(boolean active,
			MLoanApplication loan, BigDecimal balance);

	/**
	 * Find installments by multiple IDs
	 */
	List<MInstallments> findByInstallmentIdInAndIsActive(List<Long> installmentIds, boolean active);

	List<MInstallments> findByIsActiveAndLoanAndBalanceGreaterThanAndPeriodEndBeforeOrderByPeriodEndDesc(boolean b,
			MLoanApplication loan, BigDecimal zero, Date date);

	List<MInstallments> findByIsActiveAndLoanAndBalanceGreaterThanAndPeriodEndAfterOrderByPeriodEndAsc(boolean b,
			MLoanApplication loan, BigDecimal zero, Date date);

	List<MInstallments> findByIsActiveAndLoanAndCreatedAfter(boolean b, MLoanApplication loan, Date time);
}