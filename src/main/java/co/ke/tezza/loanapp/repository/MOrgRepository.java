package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MOrg;

@Repository
public interface MOrgRepository extends JpaRepository<MOrg, Long> {
	
	Page<MOrg> findByIsActive(boolean isActive,Pageable pageable);
	List<MOrg> findByIsActiveAndAdClientId(boolean isActive,long AD_Client_ID);
	List<MOrg> findByIsActiveAndAdClientIdAndNameContainsIgnoreCase(boolean b, long ad_Client_ID, String search);

}
