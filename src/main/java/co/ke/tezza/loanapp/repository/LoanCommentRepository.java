package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanComment;
import co.ke.tezza.loanapp.entity.MUser;

@Repository
public interface LoanCommentRepository extends JpaRepository<MLoanComment, Long> {
    
    // =========================================================================
    // LOAN/BORROWER RELATED METHODS
    // =========================================================================
    
    List<MLoanComment> findByIsActiveAndAdOrgIDAndLoan(boolean active, long adOrgId, MLoanApplication loan);
    
    List<MLoanComment> findByIsActiveAndAdOrgIDAndInstallment(boolean active, long adOrgId, MInstallments installment);
    
    // Find by individual borrower
    @Query(value = "SELECT c.* FROM AD_Loan_Comment c " +
            "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
            "INNER JOIN AD_Debtor d ON d.AD_Debtor_ID = l.AD_Debtor_ID AND l.borrower_type = 'INDIVIDUAL' " +
            "WHERE d.AD_Debtor_ID = :debtorId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
            "ORDER BY c.created ASC",
            countQuery = "SELECT COUNT(c.*) FROM AD_Loan_Comment c " +
                    "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
                    "INNER JOIN AD_Debtor d ON d.AD_Debtor_ID = l.AD_Debtor_ID AND l.borrower_type = 'INDIVIDUAL' " +
                    "WHERE d.AD_Debtor_ID = :debtorId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId",
            nativeQuery = true)
    Page<MLoanComment> findByIndividualBorrower(
            @Param("debtorId") Long debtorId,
            @Param("isActive") Boolean isActive,
            @Param("adOrgId") Long adOrgId,
            Pageable pageable);
    
    // Find by group borrower
    @Query(value = "SELECT c.* FROM AD_Loan_Comment c " +
            "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
            "INNER JOIN AD_Group_Borrower g ON g.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID AND l.borrower_type = 'GROUP' " +
            "WHERE g.AD_Group_Borrower_ID = :groupId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
            "ORDER BY c.created ASC",
            countQuery = "SELECT COUNT(c.*) FROM AD_Loan_Comment c " +
                    "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
                    "INNER JOIN AD_Group_Borrower g ON g.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID AND l.borrower_type = 'GROUP' " +
                    "WHERE g.AD_Group_Borrower_ID = :groupId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId",
            nativeQuery = true)
    Page<MLoanComment> findByGroupBorrower(
            @Param("groupId") Long groupId,
            @Param("isActive") Boolean isActive,
            @Param("adOrgId") Long adOrgId,
            Pageable pageable);
    
    // Find by institution borrower
    @Query(value = "SELECT c.* FROM AD_Loan_Comment c " +
            "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
            "INNER JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID AND l.borrower_type = 'INSTITUTION' " +
            "WHERE inst.AD_Institution_Borrower_ID = :institutionId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
            "ORDER BY c.created ASC",
            countQuery = "SELECT COUNT(c.*) FROM AD_Loan_Comment c " +
                    "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
                    "INNER JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID AND l.borrower_type = 'INSTITUTION' " +
                    "WHERE inst.AD_Institution_Borrower_ID = :institutionId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId",
            nativeQuery = true)
    Page<MLoanComment> findByInstitutionBorrower(
            @Param("institutionId") Long institutionId,
            @Param("isActive") Boolean isActive,
            @Param("adOrgId") Long adOrgId,
            Pageable pageable);
    
    // Search by notes and added by user
    @Query(value = "SELECT c.* FROM AD_Loan_Comment c " +
            "WHERE c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
            "AND (LOWER(c.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "     OR c.notes_taken_by = :userId) " +
            "ORDER BY c.created ASC",
            countQuery = "SELECT COUNT(c.*) FROM AD_Loan_Comment c " +
                    "WHERE c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
                    "AND (LOWER(c.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                    "     OR c.notes_taken_by = :userId)",
            nativeQuery = true)
    Page<MLoanComment> searchByNotesOrAddedBy(
            @Param("isActive") Boolean isActive,
            @Param("adOrgId") Long adOrgId,
            @Param("searchTerm") String searchTerm,
            @Param("userId") Long userId,
            Pageable pageable);
    
    // Find by individual borrower with search
    @Query(value = "SELECT c.* FROM AD_Loan_Comment c " +
            "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
            "INNER JOIN AD_Debtor d ON d.AD_Debtor_ID = l.AD_Debtor_ID AND l.borrower_type = 'INDIVIDUAL' " +
            "WHERE d.AD_Debtor_ID = :debtorId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
            "AND (LOWER(c.notes) LIKE LOWER(:searchTerm) " +
            "     OR c.notes_taken_by = :userId) " +
            "ORDER BY c.created ASC",
            countQuery = "SELECT COUNT(c.*) FROM AD_Loan_Comment c " +
                    "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
                    "INNER JOIN AD_Debtor d ON d.AD_Debtor_ID = l.AD_Debtor_ID AND l.borrower_type = 'INDIVIDUAL' " +
                    "WHERE d.AD_Debtor_ID = :debtorId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
                    "AND (LOWER(c.notes) LIKE LOWER(:searchTerm) " +
                    "     OR c.notes_taken_by = :userId)",
            nativeQuery = true)
    Page<MLoanComment> findByIndividualBorrowerWithSearch(
            @Param("debtorId") Long debtorId,
            @Param("isActive") Boolean isActive,
            @Param("adOrgId") Long adOrgId,
            @Param("searchTerm") String searchTerm,
            @Param("userId") Long userId,
            Pageable pageable);
    
    // Find by group borrower with search
    @Query(value = "SELECT c.* FROM AD_Loan_Comment c " +
            "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
            "INNER JOIN AD_Group_Borrower g ON g.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID AND l.borrower_type = 'GROUP' " +
            "WHERE g.AD_Group_Borrower_ID = :groupId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
            "AND (LOWER(c.notes) LIKE LOWER(:searchTerm) " +
            "     OR c.notes_taken_by = :userId) " +
            "ORDER BY c.created ASC",
            countQuery = "SELECT COUNT(c.*) FROM AD_Loan_Comment c " +
                    "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
                    "INNER JOIN AD_Group_Borrower g ON g.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID AND l.borrower_type = 'GROUP' " +
                    "WHERE g.AD_Group_Borrower_ID = :groupId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
                    "AND (LOWER(c.notes) LIKE LOWER(:searchTerm) " +
                    "     OR c.notes_taken_by = :userId)",
            nativeQuery = true)
    Page<MLoanComment> findByGroupBorrowerWithSearch(
            @Param("groupId") Long groupId,
            @Param("isActive") Boolean isActive,
            @Param("adOrgId") Long adOrgId,
            @Param("searchTerm") String searchTerm,
            @Param("userId") Long userId,
            Pageable pageable);
    
    // Find by institution borrower with search
    @Query(value = "SELECT c.* FROM AD_Loan_Comment c " +
            "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
            "INNER JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID AND l.borrower_type = 'INSTITUTION' " +
            "WHERE inst.AD_Institution_Borrower_ID = :institutionId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
            "AND (LOWER(c.notes) LIKE LOWER(:searchTerm) " +
            "     OR c.notes_taken_by = :userId) " +
            "ORDER BY c.created ASC",
            countQuery = "SELECT COUNT(c.*) FROM AD_Loan_Comment c " +
                    "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
                    "INNER JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID AND l.borrower_type = 'INSTITUTION' " +
                    "WHERE inst.AD_Institution_Borrower_ID = :institutionId AND c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
                    "AND (LOWER(c.notes) LIKE LOWER(:searchTerm) " +
                    "     OR c.notes_taken_by = :userId)",
            nativeQuery = true)
    Page<MLoanComment> findByInstitutionBorrowerWithSearch(
            @Param("institutionId") Long institutionId,
            @Param("isActive") Boolean isActive,
            @Param("adOrgId") Long adOrgId,
            @Param("searchTerm") String searchTerm,
            @Param("userId") Long userId,
            Pageable pageable);
    
    // Enhanced comprehensive search with borrower filtering
    @Query(value = "SELECT c.* FROM AD_Loan_Comment c " +
            "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
            "LEFT JOIN AD_Debtor d ON d.AD_Debtor_ID = l.AD_Debtor_ID AND l.borrower_type = 'INDIVIDUAL' " +
            "LEFT JOIN AD_Group_Borrower g ON g.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID AND l.borrower_type = 'GROUP' " +
            "LEFT JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID AND l.borrower_type = 'INSTITUTION' " +
            "WHERE c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
            "AND (:borrowerId IS NULL OR " +
            "     (l.borrower_type = 'INDIVIDUAL' AND d.AD_Debtor_ID = :borrowerId) OR " +
            "     (l.borrower_type = 'GROUP' AND g.AD_Group_Borrower_ID = :borrowerId) OR " +
            "     (l.borrower_type = 'INSTITUTION' AND inst.AD_Institution_Borrower_ID = :borrowerId)) " +
            "AND (:borrowerType IS NULL OR l.borrower_type = :borrowerType) " +
            "AND (LOWER(c.notes) LIKE LOWER(:searchTerm) " +
            "     OR c.notes_taken_by = :userId " +
            "     OR (l.borrower_type = 'INDIVIDUAL' AND (LOWER(d.first_name) LIKE LOWER(:searchTerm) " +
            "                                              OR LOWER(d.last_name) LIKE LOWER(:searchTerm) " +
            "                                              OR LOWER(CONCAT(d.first_name, ' ', d.last_name)) LIKE LOWER(:searchTerm))) " +
            "     OR (l.borrower_type = 'GROUP' AND LOWER(g.group_name) LIKE LOWER(:searchTerm)) " +
            "     OR (l.borrower_type = 'INSTITUTION' AND LOWER(inst.institution_name) LIKE LOWER(:searchTerm))) " +
            "ORDER BY c.created ASC",
            countQuery = "SELECT COUNT(c.*) FROM AD_Loan_Comment c " +
                    "INNER JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = c.AD_Loan_Application_ID " +
                    "LEFT JOIN AD_Debtor d ON d.AD_Debtor_ID = l.AD_Debtor_ID AND l.borrower_type = 'INDIVIDUAL' " +
                    "LEFT JOIN AD_Group_Borrower g ON g.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID AND l.borrower_type = 'GROUP' " +
                    "LEFT JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID AND l.borrower_type = 'INSTITUTION' " +
                    "WHERE c.isactive = :isActive AND c.AD_Org_ID = :adOrgId " +
                    "AND (:borrowerId IS NULL OR " +
                    "     (l.borrower_type = 'INDIVIDUAL' AND d.AD_Debtor_ID = :borrowerId) OR " +
                    "     (l.borrower_type = 'GROUP' AND g.AD_Group_Borrower_ID = :borrowerId) OR " +
                    "     (l.borrower_type = 'INSTITUTION' AND inst.AD_Institution_Borrower_ID = :borrowerId)) " +
                    "AND (:borrowerType IS NULL OR l.borrower_type = :borrowerType) " +
                    "AND (LOWER(c.notes) LIKE LOWER(:searchTerm) " +
                    "     OR c.notes_taken_by = :userId " +
                    "     OR (l.borrower_type = 'INDIVIDUAL' AND (LOWER(d.first_name) LIKE LOWER(:searchTerm) " +
                    "                                              OR LOWER(d.last_name) LIKE LOWER(:searchTerm) " +
                    "                                              OR LOWER(CONCAT(d.first_name, ' ', d.last_name)) LIKE LOWER(:searchTerm))) " +
                    "     OR (l.borrower_type = 'GROUP' AND LOWER(g.group_name) LIKE LOWER(:searchTerm)) " +
                    "     OR (l.borrower_type = 'INSTITUTION' AND LOWER(inst.institution_name) LIKE LOWER(:searchTerm)))",
            nativeQuery = true)
    Page<MLoanComment> enhancedComprehensiveSearch(
            @Param("isActive") Boolean isActive,
            @Param("adOrgId") Long adOrgId,
            @Param("borrowerType") String borrowerType,
            @Param("borrowerId") Long borrowerId,
            @Param("searchTerm") String searchTerm,
            @Param("userId") Long userId,
            Pageable pageable);
    
    // Find by added by user ID (native query)
    @Query(value = "SELECT c.* FROM AD_Loan_Comment c " +
            "WHERE c.isactive = :isActive AND c.AD_Org_ID = :adOrgId AND c.notes_taken_by = :userId " +
            "ORDER BY c.created ASC",
            countQuery = "SELECT COUNT(c.*) FROM AD_Loan_Comment c " +
                    "WHERE c.isactive = :isActive AND c.AD_Org_ID = :adOrgId AND c.notes_taken_by = :userId",
            nativeQuery = true)
    Page<MLoanComment> findByAddedBy(
            @Param("isActive") Boolean isActive,
            @Param("adOrgId") Long adOrgId,
            @Param("userId") Long userId,
            Pageable pageable);
    
    // Find by added by user (JPA method)
    Page<MLoanComment> findByIsActiveAndAdOrgIDAndNotesTakenBy(
            boolean isActive,
            long adOrgId,
            MUser notesTakenBy,
            Pageable pageable);
    
    // Find all cardex by org with pagination
    Page<MLoanComment> findByIsActiveAndAdOrgID(boolean isActive, long adOrgId, Pageable pageable);
    
   
}