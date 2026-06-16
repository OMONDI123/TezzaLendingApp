package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MSmsSetup;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;

@Repository
public interface SmsSetupRepository extends JpaRepository<MSmsSetup, Long>{
	MSmsSetup findTop1ByAdOrgIDAndIsActiveAndSmsTypeOrderBySmsSetupIdDesc(long orgId,boolean isActive,SmsTypeEnum smsTypeEnum);
	List<MSmsSetup> findByIsActiveAndAdOrgIDOrderBySmsSetupIdDesc(boolean active,long orgId);
	MSmsSetup findTop1ByAdOrgIDAndIsActiveAndSmsTypeAndDebtOrderBySmsSetupIdDesc(long adOrgID, boolean b,
			SmsTypeEnum loanApplicationOrDebtRegistration, boolean isDebtProduct);

}
