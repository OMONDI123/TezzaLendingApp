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

import co.ke.tezza.loanapp.entity.MDebtor;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.response.GroupBorrowerResponse;

@Repository
public interface IndividualBorrowersRepository extends JpaRepository<MDebtor, Long> {
	Page<MDebtor> findByIsActiveAndAdOrgIDOrderByIndividualBorrowerIdDesc(boolean isActive, long adOrgId,
			Pageable pageable);

	@Query(value = "SELECT d FROM MDebtor d WHERE " + 
	        "d.isActive = :isActive AND " + 
	        "d.adOrgID = :adOrgId AND " +
	        "(LOWER(d.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.middleName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "CONCAT(\n"
	        + "    COALESCE(LOWER(d.firstName), ''),\n"
	        + "    ' ',\n"
	        + "    COALESCE(LOWER(d.middleName), ''),\n"
	        + "    ' ',\n"
	        + "    COALESCE(LOWER(d.lastName), '')\n"
	        + ") LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.nationalId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.externalRefrenceNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.location) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.physicalAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.postalAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.employmentStatus) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.occupation) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.employer) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.referralSource) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.riskRating) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
	Page<MDebtor> searchIndividualBorrowers(@Param("isActive") boolean isActive, 
	                                         @Param("adOrgId") long adOrgId,
	                                         @Param("searchTerm") String searchTerm, 
	                                         Pageable pageable);

	@Query(value = "SELECT d FROM MDebtor d WHERE " + 
	        "d.isActive = :isActive AND " + 
	        "d.adOrgID = :adOrgId AND d.createdBy = :createdBy AND " +
	        "(LOWER(d.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.middleName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "CONCAT(\n"
	        + "    COALESCE(LOWER(d.firstName), ''),\n"
	        + "    ' ',\n"
	        + "    COALESCE(LOWER(d.middleName), ''),\n"
	        + "    ' ',\n"
	        + "    COALESCE(LOWER(d.lastName), '')\n"
	        + ") LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.nationalId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.externalRefrenceNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.location) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.physicalAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.postalAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.employmentStatus) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.occupation) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.employer) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.referralSource) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.riskRating) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(d.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
	Page<MDebtor> searchIndividualBorrowersByCreatedBy(@Param("isActive") boolean isActive, 
	                                                   @Param("adOrgId") long adOrgId,
	                                                   @Param("createdBy") long createdBy,
	                                                   @Param("searchTerm") String searchTerm, 
	                                                   Pageable pageable);

	Page<MDebtor> findByIsActiveAndAdOrgIDAndDocStatusOrderByIndividualBorrowerIdDesc(boolean isActive, long adOrgId,
			String docStatus, Pageable pageable);

	MDebtor findTop1ByIsActiveAndAdOrgIDAndNationalId(boolean isActive, long ad_Org_ID, String nationalId);

	MDebtor findTop1ByIsActiveAndAdOrgIDAndPhone(boolean isActive, long ad_Org_ID, String phone);

	MDebtor findTop1ByIsActiveAndAdOrgIDAndEmail(boolean isActive, long ad_Org_ID, String email);

	List<MDebtor> findByIsActiveAndAdOrgID(boolean b, long ad_Org_ID);

	Optional<MDebtor> findByIsActiveAndAdOrgIDAndDocumentNo(boolean b, long ad_Org_ID, String borrowerIdentifier);

	Page<MDebtor> findByIsActiveAndAdOrgIDAndCreatedByOrderByIndividualBorrowerIdDesc(boolean b, long ad_Org_ID,
			Long userId, Pageable pageable);

}
