package co.ke.tezza.loanapp.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MTable;

@Repository
public interface MTableRepository extends JpaRepository<MTable, Long> {
	 Optional<MTable> findByEntityName(String entityName);
	 
	 Page<MTable> findByEntityNameContainingOrderByEntityNameAsc(String entityName,Pageable pageable);

}
