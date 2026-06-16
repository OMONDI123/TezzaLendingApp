package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MWFMail;

@Repository
public interface MWFMailRepository extends JpaRepository<MWFMail, Long>{
	
	List<MWFMail> findByIsActiveAndProcessed(boolean isActive,boolean processed);

}
