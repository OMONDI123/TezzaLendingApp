package co.ke.tezza.loanapp.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MController;
import co.ke.tezza.loanapp.entity.MTable;

@Repository
public interface MControllerRepository extends JpaRepository<MController, Long> {
	Optional<MController> findByControllerNameAndMethodName(String controllerName, String methosName);

	Page<MController> findByControllerNameContainingOrMethodNameContainingOrUrlPatternContainingOrHttpMethodContaining(
			String controllerName, String methosName, String urlPattern, String httpMethod, Pageable pageable);

	Page<MController> findByDescriptionContainsIgnoreCaseOrControllerNameContainsIgnoreCaseOrMethodNameContainsIgnoreCaseOrEndpointContainsIgnoreCaseOrUrlPatternContainsIgnoreCaseOrHttpMethodContainsIgnoreCase(
			String search, String search2, String search3, String search4, String search5, String search6,
			Pageable of);

}
