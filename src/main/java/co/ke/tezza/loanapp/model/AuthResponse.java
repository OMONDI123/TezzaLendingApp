package co.ke.tezza.loanapp.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.ke.tezza.loanapp.entity.MMenuRoleMapping;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.response.RoleResponse;
import lombok.Data;

@Data
public class AuthResponse {
	private String jwtToken;
	private String fullName;
	private boolean viewAvailableShares;
	private List<MMenuRoleMapping> menusAllowedAccess=new ArrayList<>();
	
	private boolean otpAllowedOnLogin;
	private long user_id;
	private boolean isMultiOrganisationClient;
	private boolean hasAdminRole;
	private String phoneNumber;
	private long adOrgId;
	private long adClientId;

	private Set<RoleResponse> roles=new HashSet<RoleResponse>();
	
	public AuthResponse() {
		
	}
	
	
	
	public AuthResponse(String jwtToken, String fullName, long user_id,
			Set<RoleResponse> roles,boolean otpAllowedOnLogin) {
		super();
		this.jwtToken = jwtToken;
		this.fullName = fullName;
		this.user_id = user_id;
		this.roles = roles;
		this.otpAllowedOnLogin=otpAllowedOnLogin;
	}
	
	
	public AuthResponse( String jwtToken, String fullName, long user_id,
			Set<RoleResponse> roles) {
		super();
		this.jwtToken = jwtToken;
		this.fullName = fullName;
		this.user_id = user_id;
		this.roles = roles;
	}
	
	

}
