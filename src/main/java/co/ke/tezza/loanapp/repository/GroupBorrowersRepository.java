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

import co.ke.tezza.loanapp.entity.MGroupDebtors;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.response.GroupBorrowerResponse;

@Repository
public interface GroupBorrowersRepository extends JpaRepository<MGroupDebtors, Long> {

	Page<MGroupDebtors> findByIsActiveAndAdOrgIDOrderByGroupBorrowerIdDesc(boolean isActive, long adOrgId,
			Pageable pageable);

	@Query(value = "SELECT g FROM MGroupDebtors g WHERE " +
	        "g.isActive = :isActive AND " +
	        "g.adOrgID = :adOrgId AND " +
	        "(LOWER(g.groupName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.registrationNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.externalRefrenceNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.contactPhone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.contactEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.groupType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.physicalAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.postalAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.groupRepresentative.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.groupRepresentative.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
	Page<MGroupDebtors> searchGrupBorrower(@Param("isActive") boolean isActive, 
	                                      @Param("adOrgId") long adOrgId,
	                                      @Param("searchTerm") String searchTerm, 
	                                      Pageable pageable);
	
	@Query(value = "SELECT g FROM MGroupDebtors g WHERE " +
	        "g.isActive = :isActive AND " +
	        "g.adOrgID = :adOrgId AND " +
	        "g.groupType=:groupType AND "+
	        "(LOWER(g.groupName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.registrationNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.externalRefrenceNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.contactPhone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.contactEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.physicalAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.postalAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.groupRepresentative.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
	        "LOWER(g.groupRepresentative.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
	Page<MGroupDebtors> searchGrupBorrowerByGroupType(@Param("isActive") boolean isActive, 
	                                      @Param("adOrgId") long adOrgId,
	                                      @Param("searchTerm") String searchTerm, @Param("groupType") String groupType,
	                                      Pageable pageable);

	Page<MGroupDebtors> findByIsActiveAndAdOrgIDAndDocStatusOrderByGroupBorrowerIdDesc(boolean isActive, long adOrgId,
			String docStatus, Pageable pageable);

	MGroupDebtors findTop1ByIsActiveAndAdOrgIDAndGroupName(boolean isActive, long adOrgId, String groupName);

	MGroupDebtors findTop1ByIsActiveAndAdOrgIDAndContactPhone(boolean isActive, long adOrgId, String contactPhone);

	MGroupDebtors findTop1ByIsActiveAndAdOrgIDAndContactEmail(boolean isActive, long adOrgId, String contactEmail);

	Page<MGroupDebtors> findByIsActiveAndAdOrgIDAndGroupTypeOrderByGroupBorrowerIdDesc(boolean b,
			long ad_Org_ID, String typeFilter, Pageable of);

	List<MGroupDebtors> findByIsActiveAndAdOrgID(boolean b, long ad_Org_ID);

	Optional<MGroupDebtors> findByIsActiveAndAdOrgIDAndDocumentNo(boolean b, long ad_Org_ID,
			String borrowerIdentifier);
}
