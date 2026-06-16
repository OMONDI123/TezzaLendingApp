/**
 * 
 */
package co.ke.tezza.loanapp.service;

import java.util.List;

import org.springframework.data.domain.Page;

import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.RoleModel;
import co.ke.tezza.loanapp.response.RoleResponse;

/**
 * @author austine
 *
 */
public interface RoleService {
	
public MRoles getRolesById(Long role_id) throws SetUpExceptions;
	
	public RoleResponse createRoles(RoleModel role) throws SetUpExceptions;
	
	
	public MRoles deleteById(Long role_id) throws SetUpExceptions;
	
	public List<MRoles> getAllRoles() throws SetUpExceptions;
	
	public Page<MRoles> findByIsActiveAndAdOrgIDOrAndSuperAdmin(boolean isActive,int page,int size) throws SetUpExceptions;
	public Page<MRoles> findByIsActiveAndAdOrgID(boolean isActive,int page, int size) throws SetUpExceptions;

	public List<MRoles> getRolesByUserId();


}
