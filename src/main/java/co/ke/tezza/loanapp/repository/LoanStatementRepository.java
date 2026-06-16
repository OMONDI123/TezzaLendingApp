package co.ke.tezza.loanapp.repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MLoanStatement;

@Repository
public interface LoanStatementRepository extends JpaRepository<MLoanStatement, Long> {

	MLoanStatement findTopByIsActiveAndAdOrgIDAndLoanOrderByStatementIdDesc(boolean b, long ad_Org_ID,
			MLoanApplication loan);
	

	List<MLoanStatement> findByLoanOrderByStatementIdAsc(MLoanApplication loan);

	List<MLoanStatement> findByIsActiveAndLoanOrderByStatementIdAsc(boolean b, MLoanApplication loan);

	Page<MLoanStatement> findByIsActiveAndAdOrgIDOrderByStatementIdAsc(boolean b, long ad_Org_ID, Pageable page);
	
	MLoanStatement findTop1ByIsActiveAndAdOrgIDAndLoanAndPaymentIdOrderByStatementIdDesc(
	    boolean isActive,long ad_org_id, MLoanApplication loan,Long paymentId);

	List<MLoanStatement> findByIsActiveAndAdOrgIDAndLoanAndActualDateLessThanEqualOrderByStatementIdDesc(boolean b,
			long adOrgID, MLoanApplication loan, LocalDateTime asOfDateObj);

	MLoanStatement findTop1ByIsActiveAndAdOrgIDAndLoanAndActualDateBeforeOrderByActualDateDesc(boolean b, long adOrgID,
			MLoanApplication loan, LocalDateTime from);

	MLoanStatement findTop1ByIsActiveAndAdOrgIDAndLoanAndActualDateAfterOrderByActualDateAsc(boolean b, long adOrgId,
			MLoanApplication loan, LocalDateTime date);

	
}
