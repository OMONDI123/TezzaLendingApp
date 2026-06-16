package co.ke.tezza.loanapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MEmail;

@Repository
public interface MEmailRepository extends JpaRepository<MEmail, Long>{
	MEmail findTop1ByIsActiveAndAdOrgID(boolean isActive,long orgId);
	Page<MEmail> findByIsActive(boolean active ,Pageable pageable);

}
