package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MInstallmentStatement;
import co.ke.tezza.loanapp.entity.MInstallments;

@Repository
public interface InstallmentStatementRepository extends JpaRepository<MInstallmentStatement ,Long>{
	List<MInstallmentStatement> findByIsActiveAndInstallmentOrderByStatementIdAsc(boolean active, MInstallments installment);

	MInstallmentStatement findTopByIsActiveAndAdOrgIDAndInstallmentOrderByStatementIdDesc(boolean b, long adOrgID,
			MInstallments installment);
	MInstallmentStatement findTop1ByIsActiveAndAdOrgIDAndInstallmentAndPaymentIdOrderByStatementIdDesc(
		    boolean isActive,long ad_org_id, MInstallments installment,Long paymentId);

}
