package co.ke.tezza.loanapp.response;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RoleResponse {

	private long id;
	private String name;
	private String formattedName;
	private boolean allowMultiOrgAccess;
	private Set<OrgResponse> allowedOrganisations=new HashSet<>();

}
