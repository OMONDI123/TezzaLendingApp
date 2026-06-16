package co.ke.tezza.loanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MLoanApprovalHistory;

@Repository
public interface MLoanApprovalHistoryRepository extends JpaRepository<MLoanApprovalHistory, Long>{

}
