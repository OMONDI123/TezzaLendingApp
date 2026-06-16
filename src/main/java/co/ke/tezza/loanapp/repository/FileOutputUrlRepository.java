package co.ke.tezza.loanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.FileOutputUrl;


@Repository
public interface FileOutputUrlRepository extends JpaRepository<FileOutputUrl, Long>{
	FileOutputUrl findTop1ByIsActive(boolean isActive);

}
