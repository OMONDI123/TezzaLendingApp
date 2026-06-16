package co.ke.tezza.loanapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MCounty;
@Repository
public interface CountyRepository extends JpaRepository<MCounty, Long>{
	public Page<MCounty> findByIsActiveOrderByNameAsc(boolean isActive, Pageable pageable);
	MCounty findTop1ByIsActiveAndName(boolean isActive,String name);


}
