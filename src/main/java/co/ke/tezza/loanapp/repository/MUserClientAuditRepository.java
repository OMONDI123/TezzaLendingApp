package co.ke.tezza.loanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.entity.MUserClientAudit;

@Repository
public interface MUserClientAuditRepository extends JpaRepository<MUserClientAudit, Long> {
	MUserClientAudit findByUser(MUser user);

}
