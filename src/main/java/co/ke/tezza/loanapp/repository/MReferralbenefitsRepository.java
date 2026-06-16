package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MReferralbenefits;
import co.ke.tezza.loanapp.entity.MUser;

@Repository
public interface MReferralbenefitsRepository extends JpaRepository<MReferralbenefits, Long>{
	List<MReferralbenefits> findByIsActiveAndAdOrgIDAndClaimedAndUpline(boolean active,long adOrgId,boolean claimed,MUser upline);
	Page<MReferralbenefits> findByIsActiveAndAdOrgIDAndUplineOrderByRefferalBenefitIdDesc(boolean active,long adOrgId,MUser upline,Pageable pageable);

}
