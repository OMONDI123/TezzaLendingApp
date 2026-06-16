package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MADMenu;
import co.ke.tezza.loanapp.entity.MMenuRoleMapping;
import co.ke.tezza.loanapp.entity.MRoles;

@Repository
public interface MMenuRoleMappingRepository extends JpaRepository<MMenuRoleMapping, Long> {

	Page<MMenuRoleMapping> findByIsActiveAndAdOrgIDOrderByIdAsc(boolean isActive, long AD_Org_ID, Pageable pageable);

	@Query(
		    value = "SELECT mr.* FROM AD_Menu_Role mr " +
		            "INNER JOIN AD_Role r ON r.AD_Role_ID = mr.AD_Role_ID " +
		            "INNER JOIN AD_Menu m ON m.AD_Menu_ID = mr.AD_Menu_ID " +
		            "INNER JOIN AD_Sub_Menu_Role_Mapping sb ON sb.MMenu_Role_Mapping_AD_Menu_Role_ID = mr.AD_Menu_Role_ID " +
		            "INNER JOIN AD_Sub_Menu sm ON sm.AD_Sub_Menu_ID = sb.Associated_Sub_Menus_AD_Sub_Menu_ID " +
		            "WHERE mr.AD_Org_ID=:AD_Org_ID AND mr.isactive=true " +
		            "AND (r.formatted_name ILIKE :search OR m.title ILIKE :search OR sm.view ILIKE :search OR m.menu_icon ILIKE :search OR CAST(m.menu_order AS TEXT) ILIKE :search)",
		    countQuery = "SELECT COUNT(*) FROM AD_Menu_Role mr " +
		            "INNER JOIN AD_Role r ON r.AD_Role_ID = mr.AD_Role_ID " +
		            "INNER JOIN AD_Menu m ON m.AD_Menu_ID = mr.AD_Menu_ID " +
		            "INNER JOIN AD_Sub_Menu_Role_Mapping sb ON sb.MMenu_Role_Mapping_AD_Menu_Role_ID = mr.AD_Menu_Role_ID " +
		            "INNER JOIN AD_Sub_Menu sm ON sm.AD_Sub_Menu_ID = sb.Associated_Sub_Menus_AD_Sub_Menu_ID " +
		            "WHERE mr.AD_Org_ID=:AD_Org_ID AND mr.isactive=true " +
		            "AND (r.formatted_name ILIKE :search OR m.title ILIKE :search OR sm.view ILIKE :search OR m.menu_icon ILIKE :search OR CAST(m.menu_order AS TEXT) ILIKE :search)",
		    nativeQuery = true
		)
		Page<MMenuRoleMapping> searchMappedSubMenus(@Param("AD_Org_ID") long AD_Org_ID, @Param("search") String search, Pageable pageable);

	List<MMenuRoleMapping> findByIsActiveAndRoleAndAdOrgIDOrderByMenu_MenuOrderAsc(boolean b, MRoles role,
			long ad_Org_ID);

	List<MMenuRoleMapping> findByIsActiveAndAdOrgIDOrAdOrgIDOrderByMenu_MenuOrderAsc(boolean b, long AD_Org_ID,
			long AD_Org_ID1);

	List<MMenuRoleMapping> findByIsActiveAndRoleAndAdClientIdOrderByMenu_MenuOrderAsc(boolean b, MRoles role,
			long ad_Client_ID);

	MMenuRoleMapping findTop1ByIsActiveAndRoleAndMenu(boolean b, MRoles roleTo, MADMenu menu);
}
