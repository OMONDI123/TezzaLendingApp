package co.ke.tezza.loanapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MCounty;
import co.ke.tezza.loanapp.entity.MSubCounty;

@Repository
public interface SubCountyRepository extends JpaRepository<MSubCounty, Long> {
	public Page<MSubCounty> findByIsActiveAndCountyOrderByNameAsc(boolean isActive,MCounty county,Pageable pageable);
	MSubCounty findTop1ByIsActiveAndCountyAndName(boolean isActive,MCounty county,String name);


}
