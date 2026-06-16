package co.ke.tezza.loanapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MInstitutionBorrower;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MUser;

@Repository
public interface InstitutionBorrowersRepository extends JpaRepository<MInstitutionBorrower, Long> {

	Page<MInstitutionBorrower> findByIsActiveAndAdOrgIDOrderByInstitutionBorrowerIdDesc(boolean isActive, long adOrgId,
			Pageable pageable);

	@Query(value = "SELECT i FROM MInstitutionBorrower i WHERE " + "i.isActive = :isActive AND "
			+ "i.adOrgID = :adOrgId AND " + "(LOWER(i.institutionName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(i.registrationNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(i.taxId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(i.externalRefrenceNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(i.contactPerson) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(i.contactPhone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(i.contactEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(i.sector) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(i.physicalAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(i.postalAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(i.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
	Page<MInstitutionBorrower> searchInstitutionBorrower(@Param("isActive") boolean isActive,
			@Param("adOrgId") long adOrgId, @Param("searchTerm") String searchTerm, Pageable pageable);

	Page<MInstitutionBorrower> findByIsActiveAndAdOrgIDAndDocStatusOrderByInstitutionBorrowerIdDesc(boolean isActive,
			long adOrgId, String docStatus, Pageable pageable);

	MInstitutionBorrower findTop1ByIsActiveAndAdOrgIDAndRegistrationNumber(boolean isActive, long adOrgId,
			String registrationNumber);

	MInstitutionBorrower findTop1ByIsActiveAndAdOrgIDAndContactEmail(boolean isActive, long adOrgId,
			String contactEmail);

	MInstitutionBorrower findTop1ByIsActiveAndAdOrgIDAndContactPhone(boolean isActive, long adOrgId,
			String contactPhone);

	MInstitutionBorrower findTop1ByIsActiveAndAdOrgIDAndRepresentative(boolean isActive, long adOrgId, MUser rep);

	List<MInstitutionBorrower> findByIsActiveAndAdOrgID(boolean b, long ad_Org_ID);

	Optional<MInstitutionBorrower> findByIsActiveAndAdOrgIDAndDocumentNo(boolean b, long ad_Org_ID,
			String borrowerIdentifier);

}
