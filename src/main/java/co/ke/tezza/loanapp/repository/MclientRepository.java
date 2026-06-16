package co.ke.tezza.loanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MClient;

@Repository
public interface MclientRepository extends JpaRepository<MClient, Long>{
	MClient findTop1ByName(String name);

}
