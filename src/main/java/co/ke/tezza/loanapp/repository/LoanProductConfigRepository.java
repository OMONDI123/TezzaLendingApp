package co.ke.tezza.loanapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanProductConfiguration;

@Repository
public interface LoanProductConfigRepository extends JpaRepository<MLoanProductConfiguration, Long> {
	MLoanProductConfiguration findTop1ByIsActiveTrueOrderByLoanProductConfigIdDesc();

	Page<MLoanProductConfiguration> findByIsActiveAndAdOrgIDOrderByLoanProductConfigIdDesc(boolean isActive,
			long adOrgId, Pageable pageable);

	MLoanProductConfiguration findTop1ByIsActiveAndAdOrgIDAndNameContainingIgnoreCase(boolean b, long ad_Org_ID,
			String name);

	@Query("SELECT lp FROM MLoanProductConfiguration lp WHERE "
			+ "lp.isActive = :isActive AND lp.adOrgID = :adOrgID AND "
			+ "(LOWER(lp.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(lp.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(lp.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CAST(lp.debtType AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CAST(lp.interestCalculationMethod AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CAST(lp.interestFrequency AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CAST(lp.flatRateType AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CAST(lp.defaultPenaltyCalculationBase AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CAST(lp.penaltyAppliesTo AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CAST(lp.repaymentScheduleType AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CAST(lp.installmentFrequency AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CASE lp.debtType " + "  WHEN 'FLAT_RATE' THEN 'no interest even when overdue' "
			+ "  WHEN 'INTEREST_WHEN_OVERDUE' THEN 'accrues interest only if overdue' "
			+ "  WHEN 'INTERESTED' THEN 'has normal interest throughout the term' "
			+ "  ELSE '' END) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CASE lp.interestCalculationMethod " + "  WHEN 'FLAT' THEN 'flat rate' "
			+ "  WHEN 'DECLINING_BALANCE' THEN 'declining balance equal principal payments reduce' "
			+ "  WHEN 'DECLINING_BALANCE_EMI' THEN 'declining balance emi payments are equal all through' "
			+ "  WHEN 'CYCLE_BASED' THEN 'cycle based' " + "  WHEN 'SIMPLE_INTEREST' THEN 'simple interest' "
			+ "  WHEN 'COMPOUND' THEN 'compound' " + "  ELSE '' END) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CASE lp.interestFrequency " + "  WHEN 'DAILY' THEN 'daily' " + "  WHEN 'WEEKLY' THEN 'weekly' "
			+ "  WHEN 'MONTHLY' THEN 'monthly' " + "  WHEN 'YEARLY' THEN 'yearly' "
			+ "  ELSE '' END) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " + "LOWER(CASE lp.flatRateType "
			+ "  WHEN 'PERCENTAGE_BASED' THEN 'flat rate percentage based' "
			+ "  WHEN 'AMOUNT_BASED' THEN 'flat rate amount based' "
			+ "  ELSE '' END) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CASE lp.defaultPenaltyCalculationBase "
			+ "  WHEN 'PER_DAY' THEN 'penalty applied per day overdue' "
			+ "  WHEN 'PER_WEEK' THEN 'penalty applied per week overdue' "
			+ "  WHEN 'PER_MONTH' THEN 'penalty applied per month overdue' "
			+ "  WHEN 'PER_CYCLE' THEN 'penalty applied per cycle missed' " + "  WHEN 'ONCE' THEN 'once' "
			+ "  ELSE '' END) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " + "LOWER(CASE lp.penaltyAppliesTo "
			+ "  WHEN 'PRINCIPAL' THEN 'penalty is calculated only on the principal amount excluding any interest or fees' "
			+ "  WHEN 'FULL_LOAN_BALANCE' THEN 'penalty is calculated on the entire outstanding loan balance including both principal and accrued interest' "
			+ "  WHEN 'CURRENT_INSTALLMENT_OVERDUE' THEN 'penalty is applied based on the total overdue amount for the current installment including principal and interest due' "
			+ "  ELSE '' END) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " + "LOWER(CASE lp.repaymentScheduleType "
			+ "  WHEN 'ONE_TIME' THEN 'one time' " + "  WHEN 'INSTALLMENTS' THEN 'installments' "
			+ "  ELSE '' END) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " + "LOWER(CASE lp.installmentFrequency "
			+ "  WHEN 'DAILY' THEN 'daily' " + "  WHEN 'WEEKLY' THEN 'weekly' " + "  WHEN 'BIWEEKLY' THEN 'bi weekly' "
			+ "  WHEN 'MONTHLY' THEN 'monthly' " + "  WHEN 'QUARTERLY' THEN 'quarterly' "
			+ "  WHEN 'YEARLY' THEN 'yearly' " + "  ELSE '' END) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "EXISTS (SELECT 1 FROM lp.borrowerTypes bt "
			+ "  WHERE LOWER(CAST(bt AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "        LOWER(CASE bt " + "          WHEN 'INDIVIDUAL' THEN 'individual borrower' "
			+ "          WHEN 'GROUP' THEN 'group borrower' "
			+ "          WHEN 'INSTITUTION' THEN 'institution borrower' "
			+ "          ELSE '' END) LIKE LOWER(CONCAT('%', :searchTerm, '%')))) ")
	Page<MLoanProductConfiguration> searchLoanProduct(@Param("isActive") boolean isActive,
			@Param("adOrgID") long adOrgID, @Param("searchTerm") String searchTerm, Pageable pageable);

	Optional<MLoanProductConfiguration> findTop1ByLoanProductConfigIdOrDocumentNoOrNameOrderByLoanProductConfigIdDesc(
			Long id, String docNo, String name);

	List<MLoanProductConfiguration> findByIsActiveAndAdOrgIDOrderByNameAsc(boolean b, long ad_Org_ID);
}
