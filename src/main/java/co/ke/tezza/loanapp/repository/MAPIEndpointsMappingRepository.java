package co.ke.tezza.loanapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MAPIEndpointsMapping;
import co.ke.tezza.loanapp.entity.MController;
import co.ke.tezza.loanapp.entity.MRoles;

@Repository
public interface MAPIEndpointsMappingRepository extends JpaRepository<MAPIEndpointsMapping, Long>{
	List<MAPIEndpointsMapping> findByIsActiveAndAdClientIdAndRoles(boolean active,long clientId,MRoles role);
	
	@Query(value = "SELECT e.* FROM AD_API_Endpoint_Mapping e " + "INNER JOIN AD_Controller t ON t.AD_Controller_ID = e.AD_Controller_ID "
			+ "INNER JOIN AD_Endpoint_Role_Mapping mapp ON mapp.AD_API_Endpoint_Mapping_ID = e.AD_API_Endpoint_Mapping_ID "
			+ "INNER JOIN AD_Role r ON r.AD_Role_ID = mapp.AD_Role_ID " + "WHERE r.AD_Role_ID IN :roleIds " +
																												
			"AND e.isActive = true " + "AND e.AD_Org_ID = :AD_Org_ID "
			+ "AND t.endpoint = :methodName AND t.controller_name=:controllerName", nativeQuery = true)
	List<MAPIEndpointsMapping> findByRolesContainingAndOrganisationAndControllerNameAndMethodNameAndIsActive(
			@Param("roleIds") List<Long> roleIds, @Param("AD_Org_ID") long AD_Org_ID,@Param("controllerName") String controllerName,
			@Param("methodName") String methodName);

	Page<MAPIEndpointsMapping> findByAdOrgIDAndIsActive(long Ad_Org_ID, boolean isActive, Pageable pageable);

	@Query(value = "SELECT e.* FROM AD_API_Endpoint_Mapping e " + "INNER JOIN AD_Controller t ON t.AD_Controller_ID = e.AD_Controller_ID "
			+ "INNER JOIN AD_Endpoint_Role_Mapping mapp ON mapp.AD_API_Endpoint_Mapping_ID = e.AD_API_Endpoint_Mapping_ID "
			+ "INNER JOIN AD_Role r ON r.AD_Role_ID = mapp.AD_Role_ID " + "WHERE r.AD_Role_ID IN :roleIds " +
																												
			"AND e.isActive = true " + "AND e.AD_Org_ID = :AD_Org_ID ", nativeQuery = true)

	List<MAPIEndpointsMapping> findByRoleContainingAndOrganisation(@Param("roleIds") List<Long> roleIds,
			@Param("AD_Org_ID") long AD_Org_ID);
	@Query(value = "SELECT e.* FROM AD_API_Endpoint_Mapping e " + "INNER JOIN AD_Controller t ON t.AD_Controller_ID = e.AD_Controller_ID "
			+ "INNER JOIN AD_Endpoint_Role_Mapping mapp ON mapp.AD_API_Endpoint_Mapping_ID = e.AD_API_Endpoint_Mapping_ID "
			+ "INNER JOIN AD_Role r ON r.AD_Role_ID = mapp.AD_Role_ID " + "WHERE r.AD_Role_ID IN :roleIds " +
																												
			"AND e.isActive = true " 
			+ "AND t.AD_Controller_ID=:AD_Controller_ID  ORDER BY e.AD_API_Endpoint_Mapping_ID DESC LIMIT 1", nativeQuery = true)
	MAPIEndpointsMapping findBySuperAdminRoleContainingAndControllerNameAndMethodNameAndIsActive(
			@Param("roleIds") List<Long> roleIds, @Param("AD_Controller_ID") long AD_Controller_ID);
	
	@Modifying
    @Transactional
    @Query(value = "UPDATE public.ad_api_endpoint_mapping AS ap\n"
    		+ "SET \n"
    		+ "    ad_org_id = r.ad_org_id,\n"
    		+ "    ad_client_id = r.ad_client_id\n"
    		+ "FROM public.ad_endpoint_role_mapping AS m\n"
    		+ "INNER JOIN public.ad_role AS r ON r.ad_role_id = m.ad_role_id\n"
    		+ "WHERE m.ad_api_endpoint_mapping_id = ap.ad_api_endpoint_mapping_id\n"
    		+ "  AND (ap.ad_org_id = 0 OR ap.ad_org_id != r.ad_org_id\n"
    		+ "       OR ap.ad_client_id = 0 OR ap.ad_client_id != r.ad_client_id);\n"
    		+ " ", nativeQuery = true)
    int updateOrgAndClientWhereZero(@Param("orgId") long orgId, @Param("clientId") long clientId);

	@Query(value="SELECT e.*\n"
			+ "    FROM AD_API_Endpoint_Mapping e\n"
			+ "    INNER JOIN AD_Controller t ON t.AD_Controller_ID = e.AD_Controller_ID\n"
			+ "    WHERE e.isActive = :isActive\n"
			+ "      AND e.AD_Org_ID = :AD_Org_ID\n"
			+ "      AND (\n"
			+ "        t.endpoint ILIKE :search\n"
			+ "        OR t.controller_name ILIKE :search\n"
			+ "        OR t.description ILIKE :search\n"
			+ "      )",nativeQuery = true)
	Page<MAPIEndpointsMapping> searchMappedAPIEndpointsRoleAccessByOrganisation(@Param("AD_Org_ID") long ad_Org_ID,@Param("search") String search,
			@Param("isActive") boolean isActive, Pageable pageRequest);
	
	
	
	@Query(value = " SELECT e.*\n"
			+ "FROM AD_API_Endpoint_Mapping e\n"
			+ "INNER JOIN AD_Controller t \n"
			+ "    ON t.AD_Controller_ID = e.AD_Controller_ID\n"
			+ "INNER JOIN AD_Endpoint_Role_Mapping mapp \n"
			+ "    ON mapp.AD_API_Endpoint_Mapping_ID = e.AD_API_Endpoint_Mapping_ID\n"
			+ "INNER JOIN AD_Role r \n"
			+ "    ON r.AD_Role_ID = mapp.AD_Role_ID\n"
			+ "WHERE r.AD_Role_ID = :AD_Role_ID\n"
			+ "  AND e.isActive = TRUE\n"
			+ "  AND e.AD_Org_ID = :AD_Org_ID\n"
			+ "  AND(t.description ILIKE :searchTerm OR t.controller_name ILIKE :searchTerm OR t.method_name ILIKE :searchTerm OR \n"
			+ "  t.endpoint ILIKE :searchTerm OR t.url_pattern ILIKE :searchTerm)\n"
			+ " ", nativeQuery = true)

	List<MAPIEndpointsMapping> searchAcceMapingByRoleId(@Param("AD_Role_ID") long AD_Role_ID,
			@Param("AD_Org_ID") long AD_Org_ID,@Param("searchTerm") String searchTerm);

	List<MAPIEndpointsMapping> findByIsActiveAndRoles(boolean b, MRoles roleFrom);

	

}
