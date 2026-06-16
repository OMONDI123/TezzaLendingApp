package co.ke.tezza.loanapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MCountry;

@Repository
public interface CountryRepository extends JpaRepository<MCountry, Long> {

	public Page<MCountry> findByIsActiveOrderByNameAsc(boolean isActive, Pageable pageable);
	MCountry findTop1ByIsActiveAndName(boolean isActive,String name);
	public MCountry findTop1ByIsActiveAndNameContainingIgnoreCase(boolean b, String string);


}
