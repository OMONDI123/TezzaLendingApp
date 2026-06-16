package co.ke.tezza.loanapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.ke.tezza.loanapp.entity.*;
import co.ke.tezza.loanapp.enums.*;

import java.util.Date;
import java.util.List;

public interface LoanAmendmentRequestRepository extends JpaRepository<MLoanAmendmentRequest, Long> {

    // Basic queries
    List<MLoanAmendmentRequest> findByLoanToAmendOrderByCreatedDesc(MLoanApplication loan);
    int countByLoanToAmend(MLoanApplication loan);
    
    // Get All Records for Admin with mandatory date range
    Page<MLoanAmendmentRequest> findByIsActiveAndAdOrgIDAndCreatedBetweenOrderByCreatedDesc(
        boolean isActive, long adOrgId, Date dateFrom, Date dateTo, Pageable pageable);
    
    Page<MLoanAmendmentRequest> findByDocStatusAndIsActiveAndAdOrgIDAndCreatedBetweenOrderByCreatedDesc(
        DocStatus status, boolean isActive, long adOrgId, Date dateFrom, Date dateTo, Pageable pageable);

    // Get user specific entries with mandatory date range
    Page<MLoanAmendmentRequest> findByRequestedByAndIsActiveAndAdOrgIDAndCreatedBetweenOrderByCreatedDesc(
        MUser user, boolean isActive, long adOrgId, Date dateFrom, Date dateTo, Pageable pageable);
    
    Page<MLoanAmendmentRequest> findByRequestedByAndDocStatusAndIsActiveAndAdOrgIDAndCreatedBetweenOrderByCreatedDesc(
        MUser user, DocStatus status, boolean isActive, long adOrgId, Date dateFrom, Date dateTo, Pageable pageable);
    
    // Comprehensive search with mandatory date range
    @Query("SELECT DISTINCT lar FROM MLoanAmendmentRequest lar " +
           "JOIN lar.loanToAmend loan " +
           "LEFT JOIN loan.individualBorrower ind " +
           "LEFT JOIN loan.institutionBorrower inst " +
           "LEFT JOIN loan.groupBorrower grp " +
           "LEFT JOIN lar.amendmentDetails ad " +
           "WHERE lar.isActive = :isActive " +
           "AND lar.adOrgID = :adOrgId " +
           "AND lar.created >= :dateFrom " +
           "AND lar.created <= :dateTo " +
           "AND (:status IS NULL OR lar.docStatus = :status) " +
           "AND (:searchTerm IS NULL OR " +
           "   LOWER(lar.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(lar.requestReason) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(loan.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(loan.AD_LoanApplication_UU) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(ind.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(ind.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(ind.nationalId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   ind.phone LIKE CONCAT('%', :searchTerm, '%') OR " +
           "   LOWER(inst.institutionName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(inst.registrationNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(grp.groupName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(ad.amendmentType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(ad.amendmentReason) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           ") " +
           "ORDER BY lar.created DESC")
    Page<MLoanAmendmentRequest> searchAllRequests(
        @Param("searchTerm") String searchTerm,
        @Param("status") DocStatus status,
        @Param("dateFrom") Date dateFrom,
        @Param("dateTo") Date dateTo,
        @Param("isActive") boolean isActive,
        @Param("adOrgId") long adOrgId,
        Pageable pageable);
    
    // Search for non-admin (only user's requests) with mandatory date range
    @Query("SELECT DISTINCT lar FROM MLoanAmendmentRequest lar " +
           "JOIN lar.loanToAmend loan " +
           "LEFT JOIN loan.individualBorrower ind " +
           "LEFT JOIN loan.institutionBorrower inst " +
           "LEFT JOIN loan.groupBorrower grp " +
           "LEFT JOIN lar.amendmentDetails ad " +
           "WHERE lar.isActive = :isActive " +
           "AND lar.adOrgID = :adOrgId " +
           "AND lar.requestedBy = :user " +
           "AND lar.created >= :dateFrom " +
           "AND lar.created <= :dateTo " +
           "AND (:status IS NULL OR lar.docStatus = :status) " +
           "AND (:searchTerm IS NULL OR " +
           "   LOWER(lar.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(lar.requestReason) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(loan.documentNo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(loan.AD_LoanApplication_UU) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(ind.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(ind.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(ind.nationalId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   ind.phone LIKE CONCAT('%', :searchTerm, '%') OR " +
           "   LOWER(inst.institutionName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(inst.registrationNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(grp.groupName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(ad.amendmentType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "   LOWER(ad.amendmentReason) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           ") " +
           "ORDER BY lar.created DESC")
    Page<MLoanAmendmentRequest> searchUserRequests(
        @Param("user") MUser user,
        @Param("searchTerm") String searchTerm,
        @Param("status") DocStatus status,
        @Param("dateFrom") Date dateFrom,
        @Param("dateTo") Date dateTo,
        @Param("isActive") boolean isActive,
        @Param("adOrgId") long adOrgId,
        Pageable pageable);
}