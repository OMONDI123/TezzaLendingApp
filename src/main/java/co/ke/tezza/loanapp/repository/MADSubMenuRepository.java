package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MADMenu;
import co.ke.tezza.loanapp.entity.MADSubMenu;

@Repository
public interface MADSubMenuRepository extends JpaRepository<MADSubMenu, Long> {
	Page<MADSubMenu> findByIsActiveAndAdClientIdAndViewContainsIgnoreCaseOrTitleContainsIgnoreCaseOrSubMenuOrderContainsIgnoreCase(
			boolean active, long AD_Org_ID, String view, String title, String submenuOrder, Pageable pageable);

	Page<MADSubMenu> findByIsActiveAndAdClientIdOrderByIdAsc(boolean active, long AD_Org_ID, Pageable pageable);

	MADSubMenu findTop1ByIsActiveAndViewEquals(boolean b, String view);
	

}
