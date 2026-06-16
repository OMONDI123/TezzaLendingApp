package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MAmendmentApprovalSteps;
import co.ke.tezza.loanapp.entity.MAmendmentConfiguration;

@Repository
public interface AmendmentApprovalStepsRepository extends JpaRepository<MAmendmentApprovalSteps, Long> {
    
  
}
