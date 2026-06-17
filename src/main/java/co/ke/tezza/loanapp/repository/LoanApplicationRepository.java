package co.ke.tezza.loanapp.repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MDebtor;
import co.ke.tezza.loanapp.entity.MGroupDebtors;
import co.ke.tezza.loanapp.entity.MInstitutionBorrower;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MPayments;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.LoanStateEnum;

@Repository
public interface LoanApplicationRepository extends JpaRepository<MLoanApplication, Long> {

	Page<MLoanApplication> findByIsActiveAndAdOrgIDAndCreatedBetweenOrderByLoanApplicationIdDesc(boolean isActive,
			long adOrgId, Date dateFrom, Date dateTo, Pageable pageable);

	@Query(value = "SELECT la.* FROM ad_loan_application la "
			+ "LEFT JOIN ad_debtor ind ON la.ad_debtor_id = ind.ad_debtor_id "
			+ "LEFT JOIN ad_institution_borrower inst ON la.ad_institution_borrower_id = inst.ad_institution_borrower_id "
			+ "LEFT JOIN ad_group_borrower grp ON la.ad_group_borrower_id = grp.ad_group_borrower_id "
			+ "LEFT JOIN ad_loan_product_configuration lpc ON la.ad_loan_product_id = lpc.ad_loan_product_configuration_id "
			+ "WHERE la.isactive = true AND  la.isapproved=true AND la.Ammend=false AND la.ad_org_id = :adOrgID " + "AND  la.created >= :dateFrom "
			+ "AND  la.created <= :dateTo AND " +

			"(LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(la.documentno) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ", countQuery = "SELECT COUNT(*) FROM ad_loan_application la "
					+ "LEFT JOIN ad_debtor ind ON la.ad_debtor_id = ind.ad_debtor_id "
					+ "LEFT JOIN ad_institution_borrower inst ON la.ad_institution_borrower_id = inst.ad_institution_borrower_id "
					+ "LEFT JOIN ad_group_borrower grp ON la.ad_group_borrower_id = grp.ad_group_borrower_id "
					+ "LEFT JOIN ad_loan_product_configuration lpc ON la.ad_loan_product_id = lpc.ad_loan_product_configuration_id "
					+ "WHERE la.isactive = true AND la.ad_org_id = :adOrgID AND la.Ammend=false AND  la.isapproved=true "
					+ "AND la.created >= :dateFrom " + "AND  la.created <= :dateTo " + "AND ("
					+ "LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(la.documentno) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))", nativeQuery = true)
	Page<MLoanApplication> searchLoanApplications(@Param("adOrgID") long adOrgID,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);
	
	
	
	@Query(value = "SELECT la.* FROM ad_loan_application la "
			+ "LEFT JOIN ad_debtor ind ON la.ad_debtor_id = ind.ad_debtor_id "
			+ "LEFT JOIN ad_institution_borrower inst ON la.ad_institution_borrower_id = inst.ad_institution_borrower_id "
			+ "LEFT JOIN ad_group_borrower grp ON la.ad_group_borrower_id = grp.ad_group_borrower_id "
			+ "LEFT JOIN ad_loan_product_configuration lpc ON la.ad_loan_product_id = lpc.ad_loan_product_configuration_id "
			+ "WHERE la.isactive = true AND  la.isapproved=true AND la.Ammend=false AND la.ad_org_id = :adOrgID " + "AND  la.created >= :dateFrom "
			+ "AND  la.created <= :dateTo AND la.approvalstage='REJECTED' AND " +

			"(LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(la.documentno) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ", countQuery = "SELECT COUNT(*) FROM ad_loan_application la "
					+ "LEFT JOIN ad_debtor ind ON la.ad_debtor_id = ind.ad_debtor_id "
					+ "LEFT JOIN ad_institution_borrower inst ON la.ad_institution_borrower_id = inst.ad_institution_borrower_id "
					+ "LEFT JOIN ad_group_borrower grp ON la.ad_group_borrower_id = grp.ad_group_borrower_id "
					+ "LEFT JOIN ad_loan_product_configuration lpc ON la.ad_loan_product_id = lpc.ad_loan_product_configuration_id "
					+ "WHERE la.isactive = true AND la.ad_org_id = :adOrgID AND la.Ammend=false AND  la.isapproved=true "
					+ "AND la.created >= :dateFrom " + "AND  la.created <= :dateTo   AND la.approvalstage='REJECTED' " + "AND ("
					+ "LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(la.documentno) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))", nativeQuery = true)
	Page<MLoanApplication> searchRejectedLoanApplications(@Param("adOrgID") long adOrgID,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);
	
	
	

	@Query(value = "SELECT la.* FROM ad_loan_application la "
			+ "LEFT JOIN ad_debtor ind ON la.ad_debtor_id = ind.ad_debtor_id "
			+ "LEFT JOIN ad_institution_borrower inst ON la.ad_institution_borrower_id = inst.ad_institution_borrower_id "
			+ "LEFT JOIN ad_group_borrower grp ON la.ad_group_borrower_id = grp.ad_group_borrower_id "
			+ "LEFT JOIN ad_loan_product_configuration lpc ON la.ad_loan_product_id = lpc.ad_loan_product_configuration_id "
			+ "WHERE la.isactive = true AND la.ad_org_id = :adOrgID " + "AND  la.created >= :dateFrom "
			+ "AND  la.created <= :dateTo AND la.Ammend=true AND la.isapproved=true AND " +

			"(LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(la.documentno) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ", countQuery = "SELECT COUNT(*) FROM ad_loan_application la "
					+ "LEFT JOIN ad_debtor ind ON la.ad_debtor_id = ind.ad_debtor_id "
					+ "LEFT JOIN ad_institution_borrower inst ON la.ad_institution_borrower_id = inst.ad_institution_borrower_id "
					+ "LEFT JOIN ad_group_borrower grp ON la.ad_group_borrower_id = grp.ad_group_borrower_id "
					+ "LEFT JOIN ad_loan_product_configuration lpc ON la.ad_loan_product_id = lpc.ad_loan_product_configuration_id "
					+ "WHERE la.isactive = true AND la.ad_org_id = :adOrgID  AND la.Ammend=true AND la.isapproved=false "
					+ "AND la.created >= :dateFrom " + "AND  la.created <= :dateTo " + "AND ("
					+ "LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(la.documentno) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))", nativeQuery = true)
	Page<MLoanApplication> searchAmmendedLoanApplications(@Param("adOrgID") long adOrgID,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);

	Page<MLoanApplication> findByIsActiveAndAdOrgIDAndCreatedBetweenAndBorrowerTypeOrderByLoanApplicationIdDesc(
			boolean isActive, long adOrgId, Date dateFrom, Date dateTo, BorrowerTypeEnum borrowerType,
			Pageable pageable);

	Page<MLoanApplication> findByIsActiveAndAdOrgIDAndCreatedBetweenAndAppliedByOrderByLoanApplicationIdDesc(
			boolean isActive, long adOrgId, Date dateFrom, Date dateTo, MUser appliedBy, Pageable pageable);

	MLoanApplication findTop1ByIsActiveAndBorrowerTypeAndIndividualBorrowerAndDocStatusIn(boolean isActive,
			BorrowerTypeEnum borrowerType, MDebtor borrower, List<DocStatus> docStatus);

	MLoanApplication findTop1ByIsActiveAndBorrowerTypeAndInstitutionBorrowerAndDocStatusIn(boolean isActive,
			BorrowerTypeEnum borrowerType, MInstitutionBorrower borrower, List<DocStatus> docStatus);

	MLoanApplication findTop1ByIsActiveAndBorrowerTypeAndGroupBorrowerAndDocStatusIn(boolean isActive,
			BorrowerTypeEnum borrowerType, MGroupDebtors borrower, List<DocStatus> docStatus);

	List<MLoanApplication> findByBalanceGreaterThanAndApprovalStageAndIsActive(BigDecimal zero, ApprovalStage approved,
			boolean isactive);

	MLoanApplication findTop1ByBalanceGreaterThanAndApprovalStageAndDocumentNoAndIsActive(BigDecimal zero,
			ApprovalStage approved, String documentNo, boolean isActive);

	Page<MLoanApplication> findByIsActiveAndAdOrgIDOrderByLoanApplicationIdDesc(boolean active, long orgId,
			Pageable pageable);

	Optional<MLoanApplication> findByDocumentNo(String billRefNumber);

	Page<MLoanApplication> findByBalanceGreaterThanAndApprovalStage(Pageable of, BigDecimal zero,
			ApprovalStage approved);

	Page<MLoanApplication> findByIsActiveAndAdOrgIDAndBalanceGreaterThanAndApprovalStageOrderByLoanApplicationIdAsc(boolean b,
			long ad_Org_ID, BigDecimal zero,ApprovalStage approvalStage, Pageable of);

	List<MLoanApplication> findTop10ByIsActiveAndAdOrgIDOrderByLoanApplicationIdDesc(boolean b, long ad_Org_ID);

	@Query(value = "SELECT la.* FROM ad_loan_application la "
			+ "LEFT JOIN ad_debtor ind ON la.ad_debtor_id = ind.ad_debtor_id "
			+ "LEFT JOIN ad_institution_borrower inst ON la.ad_institution_borrower_id = inst.ad_institution_borrower_id "
			+ "LEFT JOIN ad_group_borrower grp ON la.ad_group_borrower_id = grp.ad_group_borrower_id "
			+ "LEFT JOIN ad_loan_product_configuration lpc ON la.ad_loan_product_id = lpc.ad_loan_product_configuration_id "
			+ "WHERE la.isactive = true AND  la.isapproved=true AND la.Ammend=false AND la.ad_org_id = :adOrgID " + "AND  la.created >= :dateFrom "
			+ "AND  la.created <= :dateTo AND " +

			"(LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(la.documentno) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ", countQuery = "SELECT COUNT(*) FROM ad_loan_application la "
					+ "LEFT JOIN ad_debtor ind ON la.ad_debtor_id = ind.ad_debtor_id "
					+ "LEFT JOIN ad_institution_borrower inst ON la.ad_institution_borrower_id = inst.ad_institution_borrower_id "
					+ "LEFT JOIN ad_group_borrower grp ON la.ad_group_borrower_id = grp.ad_group_borrower_id "
					+ "LEFT JOIN ad_loan_product_configuration lpc ON la.ad_loan_product_id = lpc.ad_loan_product_configuration_id "
					+ "WHERE la.isactive = true AND la.ad_org_id = :adOrgID AND la.Ammend=false AND  la.isapproved=true AND la.approvalstage='APPROVED' AND la.balance>0 "
					+ "AND la.created >= :dateFrom " + "AND  la.created <= :dateTo " + "AND ("
					+ "LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(la.documentno) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))", nativeQuery = true)
	Page<MLoanApplication> searchLoanApplicationsWithBalances(@Param("adOrgID") long adOrgID,
			@Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
			Pageable pageable);

	List<MLoanApplication> findTop10ByIsActiveAndAdOrgIDAndBalanceGreaterThanAndDueDateLessThanAndExpectedDisbursementDateBetweenAndApprovalStageOrderByDueDateAsc(
			boolean b, Long ad_org_id, BigDecimal zero, Date date, Date dateFrom, Date dateTo,ApprovalStage approvalStage);
	
	List<MLoanApplication> findTop10ByIsActiveAndAdOrgIDAndBalanceGreaterThanAndDueDateLessThanAndExpectedDisbursementDateBetweenAndApprovalStageOrderByBalanceDesc(
			boolean b, Long ad_org_id, BigDecimal zero, Date date, Date dateFrom, Date dateTo,ApprovalStage approvalStage);

	Page<MLoanApplication> findByIsActiveAndAdOrgIDAndCreatedBetweenAndAmmendAndIsApprovedAndApprovalStageOrderByLoanApplicationIdDesc(boolean b,
			long ad_Org_ID, Date dateFrom, Date dateTo, boolean c,boolean isapproved,ApprovalStage approvalStage, Pageable pageable);

	@Query(value = "SELECT DISTINCT l.* FROM ad_loan_application l "
			+ "INNER JOIN ad_loan_product_configuration lp ON lp.ad_loan_product_configuration_id = l.ad_loan_product_id "
			+ "INNER JOIN ad_approval_steps stp ON stp.ad_loan_product_configuration_id = lp.ad_loan_product_configuration_id "
			+ "WHERE l.isactive = true AND stp.ad_role_id = :currentApprovalRoleId "
			+ "AND stp.step = l.current_approval_level " + "AND l.updated BETWEEN :dateFrom AND :dateTo "
			+ "AND l.ad_org_id = :orgId " + "AND l.ammend = false   AND l.isactive=true AND l.isapproved=false AND l.approvalstage!='REJECTED' "
			+ "ORDER BY l.updated ASC", countQuery = "SELECT COUNT(DISTINCT l.*) FROM ad_loan_application l "
					+ "INNER JOIN ad_loan_product_configuration lp ON lp.ad_loan_product_configuration_id = l.ad_loan_product_id "
					+ "INNER JOIN ad_approval_steps stp ON stp.ad_loan_product_configuration_id = lp.ad_loan_product_configuration_id "
					+ "WHERE l.isactive = true AND stp.ad_role_id = :currentApprovalRoleId "
					+ "AND stp.step = l.current_approval_level " + "AND l.updated BETWEEN :dateFrom AND :dateTo "
					+ "AND l.ad_org_id = :orgId " + "AND l.ammend = false  AND l.isactive=true AND l.isapproved=false AND l.approvalstage!='REJECTED' ", nativeQuery = true)
	Page<MLoanApplication> findPendingApprovalsByRoleAndStep(@Param("currentApprovalRoleId") long currentApprovalRoleId,
			@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, @Param("orgId") long orgId,
			Pageable pageable);

	@Query(value = "SELECT DISTINCT la.* FROM ad_loan_application la "
			+ "INNER JOIN ad_loan_product_configuration lpc ON la.ad_loan_product_id = lpc.ad_loan_product_configuration_id "
			+ "INNER JOIN ad_approval_steps stp ON stp.ad_loan_product_configuration_id = lpc.ad_loan_product_configuration_id "
			+ "LEFT JOIN ad_debtor ind ON la.ad_debtor_id = ind.ad_debtor_id "
			+ "LEFT JOIN ad_institution_borrower inst ON la.ad_institution_borrower_id = inst.ad_institution_borrower_id "
			+ "LEFT JOIN ad_group_borrower grp ON la.ad_group_borrower_id = grp.ad_group_borrower_id "
			+ "WHERE la.isactive = true " + "AND stp.ad_role_id = :currentApprovalRoleId "
			+ "AND stp.step = la.current_approval_level " + "AND la.updated BETWEEN :dateFrom AND :dateTo "
			+ "AND la.ad_org_id = :orgId " + "AND la.ammend = false  AND l.isactive=true  AND l.isapproved=false AND l.approvalstage!='REJECTED' " + "AND ("
			+ "LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
			+ "LOWER(la.documentno) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
			+ "ORDER BY la.updated ASC", countQuery = "SELECT COUNT(DISTINCT la.*) FROM ad_loan_application la "
					+ "INNER JOIN ad_loan_product_configuration lpc ON la.ad_loan_product_id = lpc.ad_loan_product_configuration_id "
					+ "INNER JOIN ad_approval_steps stp ON stp.ad_loan_product_configuration_id = lpc.ad_loan_product_configuration_id "
					+ "LEFT JOIN ad_debtor ind ON la.ad_debtor_id = ind.ad_debtor_id "
					+ "LEFT JOIN ad_institution_borrower inst ON la.ad_institution_borrower_id = inst.ad_institution_borrower_id "
					+ "LEFT JOIN ad_group_borrower grp ON la.ad_group_borrower_id = grp.ad_group_borrower_id "
					+ "WHERE la.isactive = true " + "AND stp.ad_role_id = :currentApprovalRoleId "
					+ "AND stp.step = la.current_approval_level " + "AND la.updated BETWEEN :dateFrom AND :dateTo "
					+ "AND la.ad_org_id = :orgId " + "AND la.ammend = false   AND l.isactive=true AND l.isapproved=false AND l.approvalstage!='REJECTED' " + "AND ("
					+ "LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
					+ "LOWER(la.documentno) LIKE LOWER(CONCAT('%', :searchTerm, '%')))", nativeQuery = true)
	Page<MLoanApplication> searchPendingApprovals(@Param("currentApprovalRoleId") long currentApprovalRoleId,
			@Param("orgId") long orgId, @Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom,
			@Param("dateTo") Date dateTo, Pageable pageable);

	List<MLoanApplication> findTop100ByBalanceGreaterThanAndApprovalStageAndIsActiveOrderByLoanApplicationIdAsc(
			BigDecimal zero, ApprovalStage approved, boolean b);

	List<MLoanApplication> findByIsActiveAndBorrowerTypeAndIndividualBorrower(boolean b, BorrowerTypeEnum individual,
			MDebtor debtor);

	List<MLoanApplication> findByIsActiveAndBorrowerTypeAndGroupBorrower(boolean b, BorrowerTypeEnum group,
			MGroupDebtors group2);

	List<MLoanApplication> findByIsActiveAndBorrowerTypeAndInstitutionBorrower(boolean b, BorrowerTypeEnum institution,
			MInstitutionBorrower institution2);

	List<MLoanApplication> findByApprovalStageAndIsActiveAndExpectedDisbursementDateBefore(ApprovalStage approved,
			boolean b, Date now);

	List<MLoanApplication> findByIsActiveTrueAndLoanStateAndApprovalStageAndBalanceGreaterThan(LoanStateEnum open,
			ApprovalStage approved, BigDecimal zero);

	List<MLoanApplication> findByIsActiveTrueAndBalanceAndLoanStateNot(BigDecimal zero, LoanStateEnum closed);

	List<MLoanApplication> findIsActiveTrueAndByLoanState(LoanStateEnum reinstated);

	List<MLoanApplication> findByIsActiveTrueAndApprovalStage(ApprovalStage approved);
}
