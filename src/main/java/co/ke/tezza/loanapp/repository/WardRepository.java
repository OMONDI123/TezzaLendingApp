package co.ke.tezza.loanapp.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MCounty;
import co.ke.tezza.loanapp.entity.MSubCounty;
import co.ke.tezza.loanapp.entity.MWard;

@Repository
public interface WardRepository extends JpaRepository<MWard, Long> {

	public Page<MWard> findByIsActiveAndSubCountyOrderByNameAsc(boolean isActive, MSubCounty subCounty,
			Pageable pageable);

	MWard findTop1ByIsActiveAndSubCountyAndName(boolean isActive, MSubCounty subCounty, String name);

	@Query("SELECT w FROM MWard w " + "JOIN FETCH w.subCounty sc " + "JOIN FETCH sc.county c "
			+ "JOIN FETCH c.country co " + "WHERE w.name = :wardName " + "AND sc.name = :subCountyName "
			+ "AND c.name = :countyName " + "AND co.name = :countryName")
	Optional<MWard> findByWardNameAndSubCountyNameAndCountyNameAndCountryName(@Param("wardName") String wardName,
			@Param("subCountyName") String subCountyName, @Param("countyName") String countyName,
			@Param("countryName") String countryName);

}
