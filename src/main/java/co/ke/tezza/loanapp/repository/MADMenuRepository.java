package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MADMenu;

@Repository
public interface MADMenuRepository extends JpaRepository<MADMenu, Long> {

	Page<MADMenu> findByIsActiveAndAdClientIdOrderByMenuOrderAsc(boolean b, long ad_Client_ID, Pageable pageable);
	@Query(" SELECT m FROM MADMenu m\n"
			+ "			  WHERE m.isActive = true\n"
			+ "			    AND m.adClientId = :clientId\n"
			+ "			    AND (\n"
			+ "			         LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))\n"
			+ "			         OR LOWER(m.menuIcon) LIKE LOWER(CONCAT('%', :search, '%'))\n"
			+ "			    )")
			Page<MADMenu> searchActiveMenusByClientAndNameOrIcon(
			    @Param("clientId") Long clientId,
			    @Param("search") String search,
			    Pageable pageable
			);
	MADMenu findTopByIsActiveAndExternalMenuIdAndExternalClientId(boolean isActive,long externalMenuId,long externalClientId);
	List<MADMenu> findByIsActive(boolean isActive);
	MADMenu findTopByIsActiveAndExternalMenuIdAndExternalClientIdAndAdClientIdAndNameOrTitle(boolean b, long id, Long adClientId,Long clientId,
			String name, String title);
	
}
