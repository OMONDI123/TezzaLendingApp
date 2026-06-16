package co.ke.tezza.loanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MApprovalSteps;

@Repository
public interface ApprovalStepsRepository extends JpaRepository<MApprovalSteps, Long>{

}
