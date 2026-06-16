package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MSMSConfig;

@Repository
public interface SmsConfigRepository extends JpaRepository<MSMSConfig, Long>{
	MSMSConfig findTop1ByAdOrgIDAndIsActiveOrderBySmsConfigIdDesc(long orgId,boolean isActive);

	List<MSMSConfig> findByIsActiveAndAdOrgIDOrderBySmsConfigId(boolean b, long ad_Org_ID);

}
