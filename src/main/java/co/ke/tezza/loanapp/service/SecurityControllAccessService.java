package co.ke.tezza.loanapp.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.repository.MAPIEndpointsMappingRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.util.Utils;

@Service
public class SecurityControllAccessService {

	private final MAPIEndpointsMappingRepository endpointsMappingRepository;
	private final Utils utils;
	private final UserRepository userRepo;

	@Autowired
	public SecurityControllAccessService(MAPIEndpointsMappingRepository endpointsMappingRepository, Utils utils,
			UserRepository userRepo) {
		this.endpointsMappingRepository = endpointsMappingRepository;
		this.utils = utils;
		this.userRepo = userRepo;
	}

	public boolean hasAccessToAPIEndpoint(String controllerName, String methodName) {
	   // System.out.println("Checking access for controller: " + controllerName + ", method: " + methodName);
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		//System.out.println("Authenticated user: " + authentication.getName());
	   // System.out.println("Authorities: " + authentication.getAuthorities());
		if (authentication == null || !authentication.isAuthenticated()) {
			return false;
		}

		MUser user = userRepo.findById(utils.getAD_User_ID()).orElse(null);
		// Fetch active roles from DB based on authority name
		Set<MRoles> userRoles = null;
		if (user != null) {
			userRoles = user.getRoles();
		}

		// Extract role IDs
		List<Long> roleIds = userRoles.stream().map(MRoles::getId).filter(Objects::nonNull)
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(roleIds)) {
			return false;
		}

		return endpointsMappingRepository.findByRolesContainingAndOrganisationAndControllerNameAndMethodNameAndIsActive(
				roleIds, utils.getAD_Org_IDRole(), controllerName, methodName).size() > 0;

	}
}
