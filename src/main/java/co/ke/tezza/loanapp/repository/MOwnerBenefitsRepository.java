package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MOwnerBenefits;
import co.ke.tezza.loanapp.entity.MUser;

@Repository
public interface MOwnerBenefitsRepository extends JpaRepository<MOwnerBenefits, Long>{
	List<MOwnerBenefits> findByIsActiveAndAdOrgIDAndClaimed(boolean isActive,long orgId,boolean claimed);
	
	List<MOwnerBenefits> findByIsActiveAndAdOrgIDAndClaimedAndOwner(boolean isActive,long orgId,boolean claimed,MUser owner);
	
	
	Page<MOwnerBenefits> findByIsActiveAndAdOrgIDOrderByOwnerBenefitIdDesc(boolean isActive,long orgId,Pageable pageable);
	
	Page<MOwnerBenefits> findByIsActiveAndAdOrgIDAndOwnerOrderByOwnerBenefitIdDesc(boolean isActive,long orgId,MUser owner,Pageable pageable);
	

}
