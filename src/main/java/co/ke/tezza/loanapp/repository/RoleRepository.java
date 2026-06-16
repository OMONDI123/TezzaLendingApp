package co.ke.tezza.loanapp.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MRoles;

@Repository
public interface RoleRepository extends JpaRepository<MRoles, Long>{
	
	Page<MRoles> findByIsActiveAndAdOrgIDOrAdOrgIDOrderByIdAsc(boolean isactive,long AD_Org_ID,long AD_Org_ID1,Pageable pageable);
	List<MRoles> findByNameAndIsActive(String name,boolean active);
	MRoles findTop1ByNameAndIsActiveAndAdOrgIDOrderByCreatedAsc(String name,boolean active,long orgId);
	MRoles findTop1ByNameAndIsActiveAndAdOrgID(String name,boolean active,long orgId);
	Page<MRoles> findByIsActiveAndAdOrgIDOrderByIdAsc(boolean isActive,long AD_Org_ID,Pageable pageable);
	Page<MRoles> findByIsActiveAndAdClientIdOrderByIdAsc(boolean isActive, long ad_Client_ID, Pageable of);
	Set<MRoles> findByNameAndIsActiveTrue(String string);
	MRoles findTop1ByNameAndIsActive(String string, boolean b);

}
