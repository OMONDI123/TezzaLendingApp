package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MLoanAmendmentDetail;
import co.ke.tezza.loanapp.enums.AmendmentType;
import co.ke.tezza.loanapp.enums.ApprovalStage;

@Repository
public interface LoanAmendmentDetailRepository extends JpaRepository<MLoanAmendmentDetail, Long>{

	List<MLoanAmendmentDetail> findByIsActiveTrueAndApprovalStageAndAmendmentTypeIn(ApprovalStage approved,
			List<AmendmentType> types);

}
