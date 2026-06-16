package co.ke.tezza.loanapp.repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MSms;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.MessageStatus;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;

@Repository
public interface SmsRepository extends JpaRepository<MSms, Long> {

	// =========================================================================
	// BASIC METHODS
	// =========================================================================

	List<MSms> findByProcessedAndIsActiveAndAdOrgIDOrderBySmsIdAsc(boolean processed, boolean active, long orgId);

	List<MSms> findByProcessedAndIsActiveAndSmsTypeAndAdOrgIDOrderBySmsIdAsc(boolean processed, boolean active,
			SmsTypeEnum smsTypeEnum, long orgId);

	List<MSms> findByProcessedAndIsActiveOrderBySmsIdAsc(boolean b, boolean c);

	MSms findTop1ByIsActiveAndLoanIdOrderBySmsIdDesc(boolean active, long loanId);

	MSms findTop1ByIsActiveAndInstallmentIdOrderBySmsIdDesc(boolean active, long installmentId);

	boolean existsByLoanIdAndSmsTypeAndCreatedAfter(Long loanApplicationId, SmsTypeEnum type, Date dateDaysAgo);

	Optional<MSms> findTopByReminderIdAndInstallmentIdOrderByCreatedDesc(long reminderId, long installmentId);

	Optional<MSms> findTopByReminderIdAndLoanIdOrderByCreatedDesc(long reminderId, long loanId);

	boolean existsByLoanIdAndSmsTypeAndCreatedAfter(long loanId, SmsTypeEnum smsType, Date after);

	// =========================================================================
	// FREQUENCY CHECKING METHODS
	// =========================================================================

	Optional<MSms> findTopBySmsTypeAndLoanIdOrderByCreatedDesc(SmsTypeEnum smsType, Long loanId);

	Optional<MSms> findTopBySmsTypeAndInstallmentIdOrderByCreatedDesc(SmsTypeEnum smsType, Long installmentId);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndLoanIdOrderByCreatedDesc(long reminderId, SmsTypeEnum smsType,
			Long loanId);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndInstallmentIdOrderByCreatedDesc(long reminderId, SmsTypeEnum smsType,
			Long installmentId);

	boolean existsByInstallmentIdAndSmsTypeAndCreatedAfter(Long installmentId, SmsTypeEnum smsType, Date after);

	Optional<MSms> findTopBySmsTypeAndLoanIdAndCreatedAfterOrderByCreatedDesc(SmsTypeEnum statementReadyNotification,
			Long loanApplicationId, Date firstDayOfMonth);

	List<MSms> findByProcessedAndIsActiveAndSmsTypeNotOrderBySmsIdAsc(boolean processed, boolean isActive,
			SmsTypeEnum type);

	List<MSms> findByProcessedAndIsActiveAndSmsTypeOrderBySmsIdAsc(boolean b, boolean c,
			SmsTypeEnum manualSmsFromMessageCenter);

	List<MSms> findByProcessedAndIsActiveAndSmsTypeNotAndTimesTosendLessThanEqualOrderBySmsIdAsc(boolean b, boolean c,
			SmsTypeEnum manualSmsFromMessageCenter, LocalDateTime now);

	boolean existsBySmsTypeAndLoanIdAndCreatedAfter(SmsTypeEnum statementReadyNotification, Long loanApplicationId,
			Date firstDayOfMonth);

	boolean existsByReminderIdAndSmsTypeAndInstallmentId(long reminderId, SmsTypeEnum smsType, Long installmentId);

	boolean existsByReminderIdAndSmsTypeAndLoanId(long reminderId, SmsTypeEnum smsType, Long loanId);

	// =========================================================================
	// PAGINATION METHODS
	// =========================================================================

	Page<MSms> findByIsActiveAndAdOrgIDAndCreatedBetweenOrderByCreatedDesc(boolean isActive, long adOrgId,
			Date dateFrom, Date dateTo, Pageable pageable);

	Page<MSms> findByIsActiveAndAdOrgIDAndMessageStatusAndCreatedBetweenOrderByCreatedDesc(boolean isActive,
			long adOrgId, MessageStatus messageStatus, Date dateFrom, Date dateTo, Pageable pageable);

	// =========================================================================
	// BORROWER/LOAN SEARCH METHODS
	// =========================================================================

	@Query("SELECT m FROM MSms m WHERE m.isActive = :isActive AND m.adOrgID = :adOrgId "
			+ "AND m.messageStatus = :messageStatus AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchMessages(@Param("isActive") boolean isActive, @Param("adOrgId") long adOrgId,
			@Param("messageStatus") MessageStatus messageStatus, @Param("searchTerm") String searchTerm,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = :isActive AND m.adOrgID = :adOrgId "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchMessagesNoStatus(@Param("isActive") boolean isActive, @Param("adOrgId") long adOrgId,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);

	// =========================================================================
	// INDIVIDUAL BORROWER METHODS
	// =========================================================================

	Page<MSms> findByIsActiveAndAdOrgIDAndIndividualBorrowerIdAndCreatedBetweenOrderByCreatedDesc(boolean isActive,
			long adOrgId, long individualBorrowerId, Date dateFrom, Date dateTo, Pageable pageable);

	Page<MSms> findByIsActiveAndAdOrgIDAndIndividualBorrowerIdAndMessageStatusAndCreatedBetweenOrderByCreatedDesc(
			boolean isActive, long adOrgId, long individualBorrowerId, MessageStatus messageStatus, Date dateFrom,
			Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.individualBorrowerId = :individualBorrowerId " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByIndividualBorrowerId(@Param("adOrgId") long adOrgId,
			@Param("individualBorrowerId") long individualBorrowerId, @Param("searchTerm") String searchTerm,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.individualBorrowerId = :individualBorrowerId " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByIndividualBorrowerIdWithStatus(@Param("adOrgId") long adOrgId,
			@Param("individualBorrowerId") long individualBorrowerId,
			@Param("messageStatus") MessageStatus messageStatus, @Param("searchTerm") String searchTerm,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	// =========================================================================
	// GROUP BORROWER METHODS
	// =========================================================================

	Page<MSms> findByIsActiveAndAdOrgIDAndGroupBorrowerIdAndCreatedBetweenOrderByCreatedDesc(boolean isActive,
			long adOrgId, long groupBorrowerId, Date dateFrom, Date dateTo, Pageable pageable);

	Page<MSms> findByIsActiveAndAdOrgIDAndGroupBorrowerIdAndMessageStatusAndCreatedBetweenOrderByCreatedDesc(
			boolean isActive, long adOrgId, long groupBorrowerId, MessageStatus messageStatus, Date dateFrom,
			Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.groupBorrowerId = :groupBorrowerId " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByGroupBorrowerId(@Param("adOrgId") long adOrgId, @Param("groupBorrowerId") long groupBorrowerId,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.groupBorrowerId = :groupBorrowerId " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByGroupBorrowerIdWithStatus(@Param("adOrgId") long adOrgId,
			@Param("groupBorrowerId") long groupBorrowerId, @Param("messageStatus") MessageStatus messageStatus,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);

	// =========================================================================
	// INSTITUTION BORROWER METHODS
	// =========================================================================

	Page<MSms> findByIsActiveAndAdOrgIDAndInstitutionBorrowerIdAndCreatedBetweenOrderByCreatedDesc(boolean isActive,
			long adOrgId, long institutionBorrowerId, Date dateFrom, Date dateTo, Pageable pageable);

	Page<MSms> findByIsActiveAndAdOrgIDAndInstitutionBorrowerIdAndMessageStatusAndCreatedBetweenOrderByCreatedDesc(
			boolean isActive, long adOrgId, long institutionBorrowerId, MessageStatus messageStatus, Date dateFrom,
			Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.institutionBorrowerId = :institutionBorrowerId " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByInstitutionBorrowerId(@Param("adOrgId") long adOrgId,
			@Param("institutionBorrowerId") long institutionBorrowerId, @Param("searchTerm") String searchTerm,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.institutionBorrowerId = :institutionBorrowerId " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByInstitutionBorrowerIdWithStatus(@Param("adOrgId") long adOrgId,
			@Param("institutionBorrowerId") long institutionBorrowerId,
			@Param("messageStatus") MessageStatus messageStatus, @Param("searchTerm") String searchTerm,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	// =========================================================================
	// MEMBERSHIP METHODS
	// =========================================================================

	/**
	 * Find SMS by membership account ID with date range
	 */
	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.membershipAccountId = :membershipAccountId " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "ORDER BY m.created DESC")
	Page<MSms> findByMembershipAccount(@Param("adOrgId") long adOrgId,
			@Param("membershipAccountId") Long membershipAccountId, @Param("dateFrom") Date dateFrom,
			@Param("dateTo") Date dateTo, Pageable pageable);

	/**
	 * Find SMS by membership account ID with date range and status
	 */
	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.membershipAccountId = :membershipAccountId " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo " + "ORDER BY m.created DESC")
	Page<MSms> findByMembershipAccountWithStatus(@Param("adOrgId") long adOrgId,
			@Param("membershipAccountId") Long membershipAccountId, @Param("messageStatus") MessageStatus messageStatus,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	/**
	 * Search SMS by membership account ID
	 */
	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.membershipAccountId = :membershipAccountId " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByMembershipAccount(@Param("adOrgId") long adOrgId,
			@Param("membershipAccountId") Long membershipAccountId, @Param("searchTerm") String searchTerm,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	/**
	 * Search SMS by membership account ID with status filter
	 */
	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.membershipAccountId = :membershipAccountId " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByMembershipAccountWithStatus(@Param("adOrgId") long adOrgId,
			@Param("membershipAccountId") Long membershipAccountId, @Param("messageStatus") MessageStatus messageStatus,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);

	/**
	 * Search all membership SMS with date range and status
	 */
	@Query("SELECT m FROM MSms m WHERE m.isActive = :isActive AND m.adOrgID = :adOrgId "
			+ "AND m.membershipAccountId > 0 " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchMembershipMessages(@Param("isActive") boolean isActive, @Param("adOrgId") long adOrgId,
			@Param("messageStatus") MessageStatus messageStatus, @Param("searchTerm") String searchTerm,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	/**
	 * Search all membership SMS with date range (no status filter)
	 */
	@Query("SELECT m FROM MSms m WHERE m.isActive = :isActive AND m.adOrgID = :adOrgId "
			+ "AND m.membershipAccountId > 0 " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchMembershipMessagesNoStatus(@Param("isActive") boolean isActive, @Param("adOrgId") long adOrgId,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);

	/**
	 * Find all membership SMS with date range and status
	 */
	@Query("SELECT m FROM MSms m WHERE m.isActive = :isActive AND m.adOrgID = :adOrgId "
			+ "AND m.membershipAccountId > 0 " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo " + "ORDER BY m.created DESC")
	Page<MSms> findByIsActiveAndAdOrgIDAndMessageStatusAndCreatedBetweenOrderByCreatedDescForMembership(
			@Param("isActive") boolean isActive, @Param("adOrgId") long adOrgId,
			@Param("messageStatus") MessageStatus messageStatus, @Param("dateFrom") Date dateFrom,
			@Param("dateTo") Date dateTo, Pageable pageable);

	/**
	 * Find all membership SMS with date range only
	 */
	@Query("SELECT m FROM MSms m WHERE m.isActive = :isActive AND m.adOrgID = :adOrgId "
			+ "AND m.membershipAccountId > 0 " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "ORDER BY m.created DESC")
	Page<MSms> findByIsActiveAndAdOrgIDAndCreatedBetweenOrderByCreatedDescForMembership(
			@Param("isActive") boolean isActive, @Param("adOrgId") long adOrgId, @Param("dateFrom") Date dateFrom,
			@Param("dateTo") Date dateTo, Pageable pageable);

	// =========================================================================
	// INDIVIDUAL MEMBER METHODS
	// =========================================================================

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.individualMemberId = :individualMemberId " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "ORDER BY m.created DESC")
	Page<MSms> findByIndividualMemberId(@Param("adOrgId") long adOrgId,
			@Param("individualMemberId") long individualMemberId, @Param("dateFrom") Date dateFrom,
			@Param("dateTo") Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.individualMemberId = :individualMemberId " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo " + "ORDER BY m.created DESC")
	Page<MSms> findByIndividualMemberIdWithStatus(@Param("adOrgId") long adOrgId,
			@Param("individualMemberId") long individualMemberId, @Param("messageStatus") MessageStatus messageStatus,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.individualMemberId = :individualMemberId " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByIndividualMemberId(@Param("adOrgId") long adOrgId,
			@Param("individualMemberId") long individualMemberId, @Param("searchTerm") String searchTerm,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.individualMemberId = :individualMemberId " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByIndividualMemberIdWithStatus(@Param("adOrgId") long adOrgId,
			@Param("individualMemberId") long individualMemberId, @Param("messageStatus") MessageStatus messageStatus,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);

	// =========================================================================
	// GROUP MEMBER METHODS
	// =========================================================================

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.groupMemberId = :groupMemberId " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "ORDER BY m.created DESC")
	Page<MSms> findByGroupMemberId(@Param("adOrgId") long adOrgId, @Param("groupMemberId") long groupMemberId,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.groupMemberId = :groupMemberId " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo " + "ORDER BY m.created DESC")
	Page<MSms> findByGroupMemberIdWithStatus(@Param("adOrgId") long adOrgId, @Param("groupMemberId") long groupMemberId,
			@Param("messageStatus") MessageStatus messageStatus, @Param("dateFrom") Date dateFrom,
			@Param("dateTo") Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.groupMemberId = :groupMemberId " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByGroupMemberId(@Param("adOrgId") long adOrgId, @Param("groupMemberId") long groupMemberId,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.groupMemberId = :groupMemberId " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByGroupMemberIdWithStatus(@Param("adOrgId") long adOrgId,
			@Param("groupMemberId") long groupMemberId, @Param("messageStatus") MessageStatus messageStatus,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);

	// =========================================================================
	// INSTITUTION MEMBER METHODS
	// =========================================================================

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.institutionMemberId = :institutionMemberId " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "ORDER BY m.created DESC")
	Page<MSms> findByInstitutionMemberId(@Param("adOrgId") long adOrgId,
			@Param("institutionMemberId") long institutionMemberId, @Param("dateFrom") Date dateFrom,
			@Param("dateTo") Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.institutionMemberId = :institutionMemberId " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo " + "ORDER BY m.created DESC")
	Page<MSms> findByInstitutionMemberIdWithStatus(@Param("adOrgId") long adOrgId,
			@Param("institutionMemberId") long institutionMemberId, @Param("messageStatus") MessageStatus messageStatus,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.institutionMemberId = :institutionMemberId " + "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByInstitutionMemberId(@Param("adOrgId") long adOrgId,
			@Param("institutionMemberId") long institutionMemberId, @Param("searchTerm") String searchTerm,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

	@Query("SELECT m FROM MSms m WHERE m.isActive = true AND m.adOrgID = :adOrgId "
			+ "AND m.institutionMemberId = :institutionMemberId " + "AND m.messageStatus = :messageStatus "
			+ "AND m.created BETWEEN :dateFrom AND :dateTo "
			+ "AND (LOWER(m.phoneNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " + "ORDER BY m.created DESC")
	Page<MSms> searchByInstitutionMemberIdWithStatus(@Param("adOrgId") long adOrgId,
			@Param("institutionMemberId") long institutionMemberId, @Param("messageStatus") MessageStatus messageStatus,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);

	// =========================================================================
	// BORROWER CONVENIENCE METHODS
	// =========================================================================

	default Page<MSms> findByIndividualBorrowerId(long adOrgId, long individualBorrowerId, Date dateFrom, Date dateTo,
			Pageable pageable) {
		return findByIsActiveAndAdOrgIDAndIndividualBorrowerIdAndCreatedBetweenOrderByCreatedDesc(true, adOrgId,
				individualBorrowerId, dateFrom, dateTo, pageable);
	}

	default Page<MSms> findByIndividualBorrowerIdWithStatus(long adOrgId, long individualBorrowerId,
			MessageStatus messageStatus, Date dateFrom, Date dateTo, Pageable pageable) {
		return findByIsActiveAndAdOrgIDAndIndividualBorrowerIdAndMessageStatusAndCreatedBetweenOrderByCreatedDesc(true,
				adOrgId, individualBorrowerId, messageStatus, dateFrom, dateTo, pageable);
	}

	default Page<MSms> findByGroupBorrowerId(long adOrgId, long groupBorrowerId, Date dateFrom, Date dateTo,
			Pageable pageable) {
		return findByIsActiveAndAdOrgIDAndGroupBorrowerIdAndCreatedBetweenOrderByCreatedDesc(true, adOrgId,
				groupBorrowerId, dateFrom, dateTo, pageable);
	}

	default Page<MSms> findByGroupBorrowerIdWithStatus(long adOrgId, long groupBorrowerId, MessageStatus messageStatus,
			Date dateFrom, Date dateTo, Pageable pageable) {
		return findByIsActiveAndAdOrgIDAndGroupBorrowerIdAndMessageStatusAndCreatedBetweenOrderByCreatedDesc(true,
				adOrgId, groupBorrowerId, messageStatus, dateFrom, dateTo, pageable);
	}

	default Page<MSms> findByInstitutionBorrowerId(long adOrgId, long institutionBorrowerId, Date dateFrom, Date dateTo,
			Pageable pageable) {
		return findByIsActiveAndAdOrgIDAndInstitutionBorrowerIdAndCreatedBetweenOrderByCreatedDesc(true, adOrgId,
				institutionBorrowerId, dateFrom, dateTo, pageable);
	}

	default Page<MSms> findByInstitutionBorrowerIdWithStatus(long adOrgId, long institutionBorrowerId,
			MessageStatus messageStatus, Date dateFrom, Date dateTo, Pageable pageable) {
		return findByIsActiveAndAdOrgIDAndInstitutionBorrowerIdAndMessageStatusAndCreatedBetweenOrderByCreatedDesc(true,
				adOrgId, institutionBorrowerId, messageStatus, dateFrom, dateTo, pageable);
	}

	// =========================================================================
	// EXISTENCE CHECK METHODS
	// =========================================================================

	boolean existsBySmsTypeAndInstallmentIdAndCreatedAfter(SmsTypeEnum smsType, Long installmentId, Date oneHourAgo);

	boolean existsBySmsTypeAndReminderIdAndLoanIdAndCreatedAfter(SmsTypeEnum smsType, Long reminderId, Long loanId,
			Date oneHourAgo);

	long countBySmsTypeAndReminderIdAndLoanIdAndDocStatus(SmsTypeEnum smsType, long reminderId, Long loanId,
			DocStatus approved);

	long countBySmsTypeAndReminderIdAndLoanIdAndInstallmentIdAndDocStatus(SmsTypeEnum smsType, long reminderId,
			Long loanId, Long installmentId, DocStatus approved);

	boolean existsBySmsTypeAndLoanIdAndInstallmentIdAndCreatedAfter(SmsTypeEnum smsType, Long loanApplicationId,
			long installmentId, Date firstDayOfMonth);

	boolean existsBySmsTypeAndLoanIdAndGuarantorIdAndCreatedAfter(SmsTypeEnum smsType, Long loanApplicationId,
			Long nextOfKinId, Date startOfDay);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndInstallmentIdAndDocStatusOrderByCreatedDesc(long reminderId,
			SmsTypeEnum smsType, Long installmentId, DocStatus approved);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndLoanIdAndDocStatusOrderByCreatedDesc(long reminderId,
			SmsTypeEnum smsType, Long loanId, DocStatus approved);

	boolean existsByReminderIdAndSmsTypeAndLoanIdAndDocStatus(long reminderId, SmsTypeEnum smsType, Long loanId,
			DocStatus approved);

	boolean existsByReminderIdAndSmsTypeAndInstallmentIdAndDocStatus(long reminderId, SmsTypeEnum smsType,
			Long installmentId, DocStatus approved);

	boolean existsBySmsTypeAndLoanIdAndTimesTosendAfter(SmsTypeEnum smsType, Long loanApplicationId,
			LocalDateTime firstDayOfMonth);

	boolean existsBySmsTypeAndLoanIdAndInstallmentIdAndTimesTosendAfter(SmsTypeEnum smsType, Long loanApplicationId,
			long installmentId, LocalDateTime firstDayOfMonth);

	boolean existsBySmsTypeAndLoanIdAndGuarantorIdAndTimesTosendAfter(SmsTypeEnum smsType, Long loanApplicationId,
			Long nextOfKinId, LocalDateTime startOfDay);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndInstallmentIdAndDocStatusOrderByTimesTosendDesc(long reminderId,
			SmsTypeEnum smsType, Long installmentId, DocStatus approved);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndLoanIdAndDocStatusOrderByTimesTosendDesc(long reminderId,
			SmsTypeEnum smsType, Long loanId, DocStatus approved);

	boolean existsBySmsTypeAndLoanIdAndInstallmentIdAndGuarantorIdAndTimesTosendAfter(SmsTypeEnum smsType, Long loanId,
			Long installmentId, Long nextOfKinId, LocalDateTime after);

	long countBySmsTypeAndReminderIdAndLoanIdAndInstallmentIdAndGuarantorIdAndDocStatus(SmsTypeEnum smsType,
			long reminderId, Long loanId, Long installmentId, Long guarantorId, DocStatus approved);

	long countBySmsTypeAndReminderIdAndLoanIdAndGuarantorIdAndDocStatus(SmsTypeEnum smsType, long reminderId,
			Long loanId, Long guarantorId, DocStatus approved);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndInstallmentIdAndGuarantorIdAndDocStatusOrderByTimesTosendDesc(
			long reminderId, SmsTypeEnum smsType, Long installmentId, long guarantorId, DocStatus approved);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndLoanIdAndGuarantorIdAndDocStatusOrderByTimesTosendDesc(
			long reminderId, SmsTypeEnum smsType, Long loanId, long guarantorId, DocStatus approved);

	@Query("SELECT COUNT(s) FROM MSms s WHERE s.loanId = :loanId AND s.created >= :startTime")
	Long countByLoanIdAndCreatedAfter(@Param("loanId") Long loanId, @Param("startTime") Date startTime);

	long countBySmsTypeAndReminderIdAndInstallmentIdAndDocStatusAndIndividualBorrowerId(SmsTypeEnum smsType,
			long reminderId, Long installmentId, DocStatus approved, long individualBorrowerId);

	long countBySmsTypeAndReminderIdAndInstallmentIdAndDocStatusAndGroupBorrowerId(SmsTypeEnum smsType, long reminderId,
			Long installmentId, DocStatus approved, long individualBorrowerId);

	long countBySmsTypeAndReminderIdAndInstallmentIdAndDocStatusAndInstitutionBorrowerId(SmsTypeEnum smsType,
			long reminderId, Long installmentId, DocStatus approved, long individualBorrowerId);

	long countBySmsTypeAndReminderIdAndLoanIdAndDocStatusAndIndividualBorrowerId(SmsTypeEnum smsType, long reminderId,
			Long loanId, DocStatus approved, long individualBorrowerId);

	long countBySmsTypeAndReminderIdAndLoanIdAndDocStatusAndGroupBorrowerId(SmsTypeEnum smsType, long reminderId,
			Long loanId, DocStatus approved, long individualBorrowerId);

	long countBySmsTypeAndReminderIdAndLoanIdAndDocStatusAndInstitutionBorrowerId(SmsTypeEnum smsType, long reminderId,
			Long loanId, DocStatus approved, long individualBorrowerId);

	boolean existsBySmsTypeAndInstallmentIdAndDocStatusAndIndividualBorrowerIdAndTimesTosendAfter(SmsTypeEnum smsType,
			Long installmentId, DocStatus approved, long individualBorrowerId, LocalDateTime date);

	boolean existsBySmsTypeAndInstallmentIdAndDocStatusAndGroupBorrowerIdAndTimesTosendAfter(SmsTypeEnum smsType,
			Long installmentId, DocStatus approved, long individualBorrowerId, LocalDateTime date);

	boolean existsBySmsTypeAndInstallmentIdAndDocStatusAndInstitutionBorrowerIdAndTimesTosendAfter(SmsTypeEnum smsType,
			Long installmentId, DocStatus approved, long individualBorrowerId, LocalDateTime date);

	boolean existsBySmsTypeAndLoanIdAndDocStatusAndIndividualBorrowerIdAndTimesTosendAfter(SmsTypeEnum smsType,
			Long loanId, DocStatus approved, long individualBorrowerId, LocalDateTime date);

	boolean existsBySmsTypeAndLoanIdAndDocStatusAndGroupBorrowerIdAndTimesTosendAfter(SmsTypeEnum smsType, Long loanId,
			DocStatus approved, long individualBorrowerId, LocalDateTime date);

	boolean existsBySmsTypeAndLoanIdAndDocStatusAndInstitutionBorrowerIdAndTimesTosendAfter(SmsTypeEnum smsType,
			Long loanId, DocStatus approved, long individualBorrowerId, LocalDateTime date);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndInstallmentIdAndIndividualBorrowerIdAndDocStatusOrderByTimesTosendDesc(
			long reminderId, SmsTypeEnum smsType, Long installmentId, Long borrowerId, DocStatus approved);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndInstallmentIdAndGroupBorrowerIdAndDocStatusOrderByTimesTosendDesc(
			long reminderId, SmsTypeEnum smsType, Long installmentId, Long borrowerId, DocStatus approved);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndInstallmentIdAndInstitutionBorrowerIdAndDocStatusOrderByTimesTosendDesc(
			long reminderId, SmsTypeEnum smsType, Long installmentId, Long borrowerId, DocStatus approved);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndLoanIdAndIndividualBorrowerIdAndDocStatusOrderByTimesTosendDesc(
			long reminderId, SmsTypeEnum smsType, Long loanId, Long borrowerId, DocStatus approved);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndLoanIdAndGroupBorrowerIdAndDocStatusOrderByTimesTosendDesc(
			long reminderId, SmsTypeEnum smsType, Long loanId, Long borrowerId, DocStatus approved);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndLoanIdAndInstitutionBorrowerIdAndDocStatusOrderByTimesTosendDesc(
			long reminderId, SmsTypeEnum smsType, Long loanId, Long borrowerId, DocStatus approved);
	
	
	
	
	
	
	
	// By membershipAccountId
	long countBySmsTypeAndReminderIdAndDocStatusAndMembershipAccountId(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long membershipAccountId);

	// By customerId
	long countBySmsTypeAndReminderIdAndDocStatusAndCustomerId(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long customerId);

	// By billId
	long countBySmsTypeAndReminderIdAndDocStatusAndBillId(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long billId);

	// By proformaInvoiceId
	long countBySmsTypeAndReminderIdAndDocStatusAndProformaInvoiceId(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long proformaInvoiceId);
	
	
	
	
	
	
	// By membershipAccountId
	boolean existsBySmsTypeAndReminderIdAndDocStatusAndMembershipAccountIdAndTimesTosendAfter(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long membershipAccountId, LocalDateTime after);

	// By customerId
	boolean existsBySmsTypeAndReminderIdAndDocStatusAndCustomerIdAndTimesTosendAfter(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long customerId, LocalDateTime after);

	// By billId
	boolean existsBySmsTypeAndReminderIdAndDocStatusAndBillIdAndTimesTosendAfter(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long billId, LocalDateTime after);

	// By proformaInvoiceId
	boolean existsBySmsTypeAndReminderIdAndDocStatusAndProformaInvoiceIdAndTimesTosendAfter(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long proformaInvoiceId, LocalDateTime after);
	
	
	
	// By membershipAccountId
	Optional<MSms> findTopBySmsTypeAndReminderIdAndDocStatusAndMembershipAccountIdOrderByTimesTosendDesc(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long membershipAccountId);

	// By customerId
	Optional<MSms> findTopBySmsTypeAndReminderIdAndDocStatusAndCustomerIdOrderByTimesTosendDesc(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long customerId);

	// By billId
	Optional<MSms> findTopBySmsTypeAndReminderIdAndDocStatusAndBillIdOrderByTimesTosendDesc(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long billId);

	// By proformaInvoiceId
	Optional<MSms> findTopBySmsTypeAndReminderIdAndDocStatusAndProformaInvoiceIdOrderByTimesTosendDesc(
	        SmsTypeEnum smsType, long reminderId, DocStatus approved, long proformaInvoiceId);

	long countBySmsTypeAndReminderIdAndBillIdAndDocStatus(SmsTypeEnum smsType, long reminderId, Long billId,
			DocStatus approved);

	boolean existsBySmsTypeAndBillIdAndTimesTosendAfter(SmsTypeEnum smsType, Long billId, LocalDateTime startOfDay);

	Optional<MSms> findTopByReminderIdAndSmsTypeAndBillIdAndDocStatusOrderByTimesTosendDesc(long reminderId,
			SmsTypeEnum smsType, Long billId, DocStatus approved);

	
}