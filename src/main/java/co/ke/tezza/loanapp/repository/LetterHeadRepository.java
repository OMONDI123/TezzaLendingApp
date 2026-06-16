package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.LetterHeaads;

@Repository
public interface LetterHeadRepository extends JpaRepository<LetterHeaads, Long>{
	LetterHeaads findTop1ByIsActiveAndAdOrgIDOrderByIdDesc(boolean active,long adorgId);

	List<LetterHeaads> findByIsActiveAndAdOrgID(boolean b, long ad_Org_ID);

}
