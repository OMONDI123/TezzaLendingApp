package co.ke.tezza.loanapp.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MInstallments;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MPayments;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.PaymentType;

@Repository
public interface PaymentRepository extends JpaRepository<MPayments, Long> {

	Page<MPayments> findByIsActiveAndAdOrgIDOrderByPaymentIdDesc(boolean active, long orgId, Pageable pageable);

	Page<MPayments> findByIsActiveAndAdOrgIDAndLoanOrderByPaymentIdDesc(boolean active, long orgId,
			MLoanApplication loan, Pageable pageable);

	Page<MPayments> findByInstallmentsAndIsActiveAndAdOrgIDOrderByPaymentIdDesc(MInstallments installment,
			boolean active, long orgId, Pageable pageable);

	MPayments findTop1ByMerchantrequestAndCheckoutrequest(String merchantRequestID, String checkoutRequestID);

	List<MPayments> findByIsActiveAndAdOrgIDAndLoanOrderByPaymentIdDesc(boolean b, long ad_Org_ID,
			MLoanApplication app);

	Page<MPayments> findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndPaymentDateBetweenAndDocStatusOrderByPaymentIdDesc(
			boolean active, boolean waiverWriteOff,boolean walletDeposit, long orgId, String startDate, String endDate, DocStatus docStatus,
			Pageable pageable);

	Page<MPayments> findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndPaymentMethodAndPaymentDateBetweenAndApprovalStageAndDocStatusOrderByPaymentIdDesc(
			boolean active, boolean waiverWriteOff,boolean walletDeposit, long orgId, PaymentType paymentMethod, String startDate,
			String endDate, ApprovalStage approvalStage, DocStatus docStatus, Pageable pageable);

	Page<MPayments> findByIsActiveAndAdOrgIDAndIspaidAndPaymentDateBetween(boolean active, long orgId, boolean isPaid,
			String startDate, String endDate, Pageable pageable);

	@Query(value = "SELECT p.* \n" +
            "FROM AD_Payment p\n" +
            "LEFT JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = p.AD_Loan_Application_ID\n" +
            "LEFT JOIN ad_debtor ind ON l.ad_debtor_id = ind.ad_debtor_id \n" +
            "LEFT JOIN ad_institution_borrower inst ON l.ad_institution_borrower_id = inst.ad_institution_borrower_id \n" +
            "LEFT JOIN ad_group_borrower grp ON l.ad_group_borrower_id = grp.ad_group_borrower_id \n" +
            "LEFT JOIN ad_loan_product_configuration lpc ON l.ad_loan_product_id = lpc.ad_loan_product_configuration_id \n" +
            "WHERE p.payment_date BETWEEN :startDate AND :endDate \n" +
            "  AND p.isactive = :isActive\n" +
            "  AND p.AD_Org_ID = :adOrgID AND p.waiver_Or_Write_Off = :waiverWriteOff AND p.wallet_deposit = :walletDeposit \n" +
            "  AND (\n" +
            "    p.reference ILIKE LOWER(CONCAT('%', :searchTerm, '%')) \n" +
            "    OR p.documentno ILIKE LOWER(CONCAT('%', :searchTerm, '%')) \n" +
            "    OR p.docstatus ILIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "  )", nativeQuery = true)
Page<MPayments> searchPayments(@Param("isActive") boolean isActive, @Param("adOrgID") long adOrgID,
                           @Param("startDate") String startDate, @Param("endDate") String endDate,
                           @Param("searchTerm") String searchTerm, @Param("waiverWriteOff") boolean waiverWriteOff,
                           @Param("walletDeposit") boolean walletDeposit, Pageable pageable);

@Query(value = "SELECT p.* \n" +
            "FROM AD_Payment p\n" +
            "LEFT JOIN AD_Loan_Application l ON l.AD_Loan_Application_ID = p.AD_Loan_Application_ID\n" +
            "LEFT JOIN ad_debtor ind ON l.ad_debtor_id = ind.ad_debtor_id \n" +
            "LEFT JOIN ad_institution_borrower inst ON l.ad_institution_borrower_id = inst.ad_institution_borrower_id \n" +
            "LEFT JOIN ad_group_borrower grp ON l.ad_group_borrower_id = grp.ad_group_borrower_id \n" +
            "LEFT JOIN ad_loan_product_configuration lpc ON l.ad_loan_product_id = lpc.ad_loan_product_configuration_id \n" +
            "WHERE p.payment_date BETWEEN :startDate AND :endDate \n" +
            "  AND p.isactive = :isActive\n" +
            "  AND p.is_paid = false\n" +
            "  AND p.approvalstage = 'REJECTED'\n" +
            "  AND p.docstatus = 'REJECTED'\n" +
            "  AND p.AD_Org_ID = :adOrgID AND p.waiver_Or_Write_Off = :waiverWriteOff AND p.wallet_deposit = :walletDeposit \n" +
            "  AND (\n" +
            "    p.reference ILIKE LOWER(CONCAT('%', :searchTerm, '%')) \n" +
            "    OR p.documentno ILIKE LOWER(CONCAT('%', :searchTerm, '%')) \n" +
            "    OR p.docstatus ILIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.middle_name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.national_id) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(ind.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.institution_name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.contact_person) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.tax_id) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(inst.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(grp.group_name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(grp.registration_number) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(grp.contact_phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(grp.contact_email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(grp.external_refrence_no) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "    OR LOWER(lpc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))\n" +
            "  )", nativeQuery = true)
Page<MPayments> searchRejectedPayments(@Param("isActive") boolean isActive, @Param("adOrgID") long adOrgID,
                                   @Param("startDate") String startDate, @Param("endDate") String endDate,
                                   @Param("searchTerm") String searchTerm, @Param("waiverWriteOff") boolean waiverWriteOff,
                                   @Param("walletDeposit") boolean walletDeposit, Pageable pageable);

	// Statistics queries
	Long countByIsActiveAndAdOrgIDAndPaymentDateBetween(boolean active, long orgId, String startDate, String endDate);

	@Query("SELECT COALESCE(SUM(p.amount), 0) FROM MPayments p WHERE p.isActive = :isActive AND p.adOrgID = :adOrgID AND p.paymentDate BETWEEN :startDate AND :endDate")
	BigDecimal sumAmountByIsActiveAndAdOrgIDAndPaymentDateBetween(@Param("isActive") boolean isActive,
			@Param("adOrgID") long adOrgID, @Param("startDate") String startDate, @Param("endDate") String endDate);

	Long countByIsActiveAndAdOrgIDAndIspaidAndPaymentDateBetween(boolean active, long orgId, boolean isPaid,
			String startDate, String endDate);

	@Query("SELECT p.paymentMethod, COUNT(p) FROM MPayments p WHERE p.isActive = :isActive AND p.adOrgID = :adOrgID AND p.paymentDate BETWEEN :startDate AND :endDate GROUP BY p.paymentMethod")
	List<Object[]> countPaymentsByMethodAndAdOrgID(@Param("isActive") boolean isActive, @Param("adOrgID") long adOrgID,
			@Param("startDate") String startDate, @Param("endDate") String endDate);

	MPayments findTop1ByIsActiveAndAdOrgIDAndReferenceAndApprovalStageAndIspaidOrderByPaymentIdDesc(boolean b,
			long ad_Org_ID, String reference, ApprovalStage approvalStage, boolean isPaid);

	MPayments findByReference(String transID);

	MPayments findTop1ByCheckoutrequestOrderByPaymentIdDesc(String checkoutRequestID);

	List<MPayments> findByIsActiveAndDocStatusAndIspaid(boolean b, DocStatus pending, boolean c);

	List<MPayments> findByIsActiveAndAdOrgIDAndLoanAndDocStatusOrderByPaymentIdDesc(boolean b, long ad_Org_ID,
			MLoanApplication app, DocStatus completed);

	Page<MPayments> findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndDocStatusAndPaymentDateBetween(boolean b,
			boolean waiverWriteOff,boolean walletDeposit, long adOrgID, DocStatus valueOf, String dateFrom, String dateTo, Pageable pageable);

	Page<MPayments> findByIsActiveAndWaiverWriteOffAndWalletDepositAndAdOrgIDAndDocStatusAndPaymentMethodAndPaymentDateBetweenOrderByPaymentIdDesc(
			boolean b, boolean waiverWriteOff, boolean walletDeposit, long adOrgID, DocStatus valueOf,
			PaymentType valueOf2, String dateFrom, String dateTo, Pageable pageable);

	List<MPayments> findByIsActiveAndAdOrgIDAndLoanAndDocStatusAndPaymentDateTimeBetweenOrderByPaymentIdAsc(boolean b,
			long adOrgID, MLoanApplication loan, DocStatus approved, LocalDateTime datefrom, LocalDateTime dateTo);

	List<MPayments> findByIsActiveAndAdOrgIDAndLoanAndDocStatusAndPaymentDateTimeBetweenOrderByPaymentIdDesc(boolean b,
			long adOrgID, MLoanApplication loan, DocStatus approved, LocalDateTime datefrom, LocalDateTime dateTo);

	List<MPayments> findByIsActiveAndAdOrgIDAndInstallmentsAndDocStatusAndPaymentDateTimeBetweenOrderByPaymentIdAsc(
			boolean b, long adOrgID, MInstallments installment, DocStatus approved, LocalDateTime datefrom,
			LocalDateTime dateTo);

	List<MPayments> findByIsActiveAndAdOrgIDAndInstallmentsAndDocStatusAndPaymentDateTimeBetweenOrderByPaymentIdDesc(
			boolean b, long adOrgID, MInstallments installment, DocStatus approved, LocalDateTime datefrom,
			LocalDateTime dateTo);

	List<MPayments> findByIsActiveAndAdOrgIDAndLoanAndDocStatusOrderByPaymentDateTimeAsc(boolean b, long adOrgID,
			MLoanApplication loan, DocStatus completed);

	List<MPayments> findByIsActiveAndAdOrgIDAndLoanAndDocStatusAndPaymentDateTimeBetween(boolean b, long adOrgID,
			MLoanApplication loan, DocStatus completed, LocalDateTime format, LocalDateTime format2);

	List<MPayments> findByIsActiveAndAdOrgIDAndLoanAndDocStatusAndPaymentDateTimeBeforeOrderByPaymentDateTimeDesc(
			boolean b, long adOrgID, MLoanApplication loan, DocStatus completed, LocalDateTime date);

	List<MPayments> findByIsActiveAndSecurityPaymentAndDocStatusAndExpectedAllocationDateLessThanEqual(boolean b,
			boolean c, DocStatus pendingAllocation, LocalDateTime now);


}
