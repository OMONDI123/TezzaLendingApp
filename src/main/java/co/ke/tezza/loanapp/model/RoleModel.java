/**
 * 
 */
package co.ke.tezza.loanapp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author austine
 *
 */
public class RoleModel {
	
	private Long id;
	
	private String roleDescription;
	
	private String roleName;
	private boolean allowMultiOrgAccess;
	private List<Long> allowedOrganisationIds=new ArrayList<>();

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the roleDescription
	 */
	public String getRoleDescription() {
		return roleDescription;
	}

	/**
	 * @param roleDescription the roleDescription to set
	 */
	public void setRoleDescription(String roleDescription) {
		this.roleDescription = roleDescription;
	}

	/**
	 * @return the roleName
	 */
	public String getRoleName() {
		return roleName;
	}

	/**
	 * @param roleName the roleName to set
	 */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public boolean isAllowMultiOrgAccess() {
		return allowMultiOrgAccess;
	}

	public void setAllowMultiOrgAccess(boolean allowMultiOrgAccess) {
		this.allowMultiOrgAccess = allowMultiOrgAccess;
	}

	public List<Long> getAllowedOrganisationIds() {
		return allowedOrganisationIds;
	}

	public void setAllowedOrganisationIds(List<Long> allowedOrganisationIds) {
		this.allowedOrganisationIds = allowedOrganisationIds;
	}
	
	
	

}
