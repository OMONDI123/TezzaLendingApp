package co.ke.tezza.loanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MGuarantorLoan;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MNextOfKin;

@Repository
public interface GuarantorLoanRepository extends JpaRepository<MGuarantorLoan, Long> {
	MGuarantorLoan findTop1ByLoanAndGuarantorAndIsActive(MLoanApplication loan, MNextOfKin guarantor, boolean active);

}
