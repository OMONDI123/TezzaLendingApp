package co.ke.tezza.loanapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MAmendmentConfiguration;
import co.ke.tezza.loanapp.enums.AmendmentType;

import java.util.List;

@Repository
public interface AmendmentConfigurationRepository extends JpaRepository<MAmendmentConfiguration, Long> {

	// Basic queries
	List<MAmendmentConfiguration> findByAmendmentTypeAndIsActive(AmendmentType amendmentType, boolean isActive);

	List<MAmendmentConfiguration> findByAmendmentTypeAndIsActiveAndIsDefaultConfiguration(AmendmentType amendmentType,
			boolean isActive, boolean isDefaultConfiguration);

	List<MAmendmentConfiguration> findByIsActiveAndIsDefaultConfiguration(boolean isActive,
			boolean isDefaultConfiguration);

	List<MAmendmentConfiguration> findByIsActiveOrderByConfigurationNameAsc(boolean isActive);
	

	// PAGINATED QUERIES
	Page<MAmendmentConfiguration> findByIsActive(boolean isActive, Pageable pageable);

	Page<MAmendmentConfiguration> findByIsActiveAndAdOrgID(boolean isActive, long adOrgID, Pageable pageable);

	Page<MAmendmentConfiguration> findByIsActiveAndAdOrgIDOrderByConfigurationNameAsc(boolean isActive, long adOrgID,
			Pageable pageable);

	// Search with pagination
	@Query("SELECT c FROM MAmendmentConfiguration c WHERE " + "(:isActive IS NULL OR c.isActive = :isActive) AND "
			+ "c.adOrgID = :adOrgID AND " + "(:searchTerm IS NULL OR "
			+ "LOWER(c.configurationName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(c.configurationDescription) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(CAST(c.amendmentType AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
	Page<MAmendmentConfiguration> searchByAdOrgID(@Param("adOrgID") long adOrgID, @Param("isActive") boolean isActive,
			@Param("searchTerm") String searchTerm, Pageable pageable);

	@Query("SELECT c FROM MAmendmentConfiguration c WHERE " + "c.isActive = :isActive AND "
			+ "c.adOrgID = :adOrgID AND " + "(:searchTerm IS NULL OR "
			+ "LOWER(c.configurationName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(c.configurationDescription) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
	Page<MAmendmentConfiguration> findByIsActiveAndAdOrgIDAndSearchTerm(@Param("isActive") boolean isActive,
			@Param("adOrgID") long adOrgID, @Param("searchTerm") String searchTerm, Pageable pageable);

	// Complex queries with JOIN
	@Query("SELECT DISTINCT c FROM MAmendmentConfiguration c " + "JOIN c.applicableLoanProducts p "
			+ "WHERE c.amendmentType = :amendmentType " + "AND c.isActive = true " + "AND c.adOrgID = :adOrgID "
			+ "AND p.loanProductConfigId = :loanProductId")
	List<MAmendmentConfiguration> findByAmendmentTypeAndApplicableLoanProducts_LoanProductConfigId(
			@Param("amendmentType") AmendmentType amendmentType, @Param("loanProductId") long loanProductId,
			@Param("adOrgID") long adOrgID);

	// Alternative query using EXISTS (kept for backward compatibility if needed)
	@Query("SELECT c FROM MAmendmentConfiguration c WHERE c.amendmentType = :amendmentType " + "AND c.isActive = true "
			+ "AND c.adOrgID = :adOrgID "
			+ "AND EXISTS (SELECT p FROM c.applicableLoanProducts p WHERE p.loanProductConfigId = :loanProductId)")
	List<MAmendmentConfiguration> findByAmendmentTypeAndIsActiveAndApplicableLoanProducts_LoanProductConfigId(
			@Param("amendmentType") AmendmentType amendmentType, @Param("loanProductId") long loanProductId,
			@Param("adOrgID") long adOrgID);

	// Additional useful queries
	List<MAmendmentConfiguration> findByAmendmentType(AmendmentType amendmentType);

	List<MAmendmentConfiguration> findByIsActiveTrue();

	List<MAmendmentConfiguration> findByIsActiveTrueOrderByConfigurationNameAsc();

	// Find by name pattern
	List<MAmendmentConfiguration> findByConfigurationNameContainingIgnoreCaseAndIsActive(String configurationName,
			boolean isActive);

	// Find by multiple amendment types
	@Query("SELECT c FROM MAmendmentConfiguration c WHERE c.amendmentType IN :amendmentTypes "
			+ "AND c.isActive = :isActive " + "AND c.adOrgID = :adOrgID")
	List<MAmendmentConfiguration> findByAmendmentTypeInAndIsActiveAndAdOrgID(
			@Param("amendmentTypes") List<AmendmentType> amendmentTypes, @Param("isActive") boolean isActive,
			@Param("adOrgID") long adOrgID);

	// Check if configuration name exists
	boolean existsByConfigurationNameAndIsActiveAndAdOrgID(String configurationName, boolean isActive, long adOrgID);

	// Count active configurations
	long countByIsActiveAndAdOrgID(boolean isActive, long adOrgID);

	// Find configurations that apply to multiple loan products
	@Query("SELECT DISTINCT c FROM MAmendmentConfiguration c " + "JOIN c.applicableLoanProducts p "
			+ "WHERE p.loanProductConfigId IN :loanProductIds " + "AND c.isActive = true " + "AND c.adOrgID = :adOrgID")
	List<MAmendmentConfiguration> findByApplicableLoanProductsInAndIsActiveAndAdOrgID(
			@Param("loanProductIds") List<Long> loanProductIds, @Param("adOrgID") long adOrgID);

	// Non-paginated version (keep for backward compatibility)
	List<MAmendmentConfiguration> findByIsActiveAndAdOrgIDOrderByConfigurationNameAsc(boolean isActive, long adOrgID);

	long countByAmendmentTypeAndIsActiveAndAdOrgID(AmendmentType type, boolean isActive, long adOrgID);

	long countByIsActiveAndIsDefaultConfigurationAndAdOrgID(boolean isActive, boolean isDefaultConfiguration,
			long adOrgID);

	long countByRequiresApprovalAndIsActiveAndAdOrgID(boolean requiresApproval, boolean isActive, long adOrgID);

	List<MAmendmentConfiguration> findByAmendmentTypeAndIsActiveAndAdOrgIDOrderByConfigurationNameAsc(
			AmendmentType amendmentType, boolean isActive, long adOrgID);
}