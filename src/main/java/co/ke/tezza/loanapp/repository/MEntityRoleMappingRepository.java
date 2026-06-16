package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MEntityRoleMapping;

@Repository
public interface MEntityRoleMappingRepository extends JpaRepository<MEntityRoleMapping, Long> {
	@Query(value = "SELECT e.* FROM AD_Entity_Role e " + "INNER JOIN AD_Table t ON t.AD_Table_ID = e.AD_Table_ID "
			+ "INNER JOIN AD_Entity_Role_Mapping mapp ON mapp.AD_Entity_Role_ID = e.AD_Entity_Role_ID "
			+ "INNER JOIN AD_Role r ON r.AD_Role_ID = mapp.AD_Role_ID " + "WHERE r.AD_Role_ID IN :roleIds " +
																												
			"AND e.isActive = true " + "AND e.AD_Org_ID = :AD_Org_ID "
			+ "AND t.entity_name = :entityName", nativeQuery = true)
	List<MEntityRoleMapping> findByRolesContainingAndOrganisationAndEntityNameAndActive(
			@Param("roleIds") List<Long> roleIds, @Param("AD_Org_ID") long AD_Org_ID,
			@Param("entityName") String entityName);

	Page<MEntityRoleMapping> findByAdOrgIDAndIsActive(long Ad_Org_ID, boolean isActive, Pageable pageable);

	@Query(value = "SELECT e.* FROM AD_Entity_Role e " + "INNER JOIN AD_Table t ON t.AD_Table_ID = e.AD_Table_ID "
			+ "INNER JOIN AD_Entity_Role_Mapping mapp ON mapp.AD_Entity_Role_ID = e.AD_Entity_Role_ID "
			+ "INNER JOIN AD_Role r ON r.AD_Role_ID = mapp.AD_Role_ID " + "WHERE r.AD_Role_ID IN :roleIds " +
																												
			"AND e.isActive = true " + "AND e.AD_Org_ID = :AD_Org_ID ", nativeQuery = true)

	List<MEntityRoleMapping> findByRoleContainingAndOrganisation(@Param("roleIds") List<Long> roleIds,
			@Param("AD_Org_ID") long AD_Org_ID);

}
