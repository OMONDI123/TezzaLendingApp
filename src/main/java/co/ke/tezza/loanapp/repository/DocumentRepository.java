package co.ke.tezza.loanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MDocuments;

@Repository
public interface DocumentRepository extends JpaRepository<MDocuments, Long>{

}
