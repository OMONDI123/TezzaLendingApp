package co.ke.tezza.loanapp.metadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import co.ke.tezza.loanapp.entity.MAPIEndpointsMapping;
import co.ke.tezza.loanapp.entity.MController;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.repository.MAPIEndpointsMappingRepository;
import co.ke.tezza.loanapp.repository.MControllerRepository;
import co.ke.tezza.loanapp.repository.RoleRepository;
import co.ke.tezza.loanapp.util.Utils;

import java.util.*;


@Component
public class EndpointEntityListener {
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private MControllerRepository endpointRepository;

	@Autowired
	private MAPIEndpointsMappingRepository mAPIEndpointsMappingRepository;

	@Autowired
	private RoleRepository roleRepository;

	@org.springframework.context.event.EventListener(ApplicationReadyEvent.class)

	public void registerEndpoints() {
		System.out.println("🔍 Scanning and registering API endpoints...");

		List<MController> existingEndpoints = endpointRepository.findAll();
		Set<String> existingUrls = new HashSet<>();
		for (MController endpoint : existingEndpoints) {
			existingUrls.add(endpoint.getUrlPattern() + "-" + endpoint.getHttpMethod());
		}

		RequestMappingHandlerMapping handlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
		Map<RequestMappingInfo, HandlerMethod> map = handlerMapping.getHandlerMethods();

		List<MController> newEndpoints = new ArrayList<>();
		for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : map.entrySet()) {
			RequestMappingInfo info = entry.getKey();
			HandlerMethod method = entry.getValue();

			String controllerName = method.getBeanType().getSimpleName();
			String methodName = method.getMethod().getName();
			String descrition = makeReadable(methodName);
			Set<String> patterns = info.getPatternsCondition().getPatterns();
			Set<RequestMethod> methods = info.getMethodsCondition().getMethods();

			for (String pattern : patterns) {
				String extractedPart = pattern.replaceFirst("^/[^/]+/", "");
				for (RequestMethod requestMethod : methods) {
					String uniqueKey = pattern + "-" + requestMethod.name();

					if (!existingUrls.contains(uniqueKey)) {
						MController controller = new MController(0, controllerName, methodName, extractedPart, pattern,
								requestMethod.name(), descrition);

						newEndpoints.add(controller);
						existingUrls.add(uniqueKey);
					}
				}
			}
		}

		if (!newEndpoints.isEmpty()) {
			endpointRepository.saveAll(newEndpoints);
			System.out.println("✅ Registered " + newEndpoints.size() + " new endpoints.");
		} else {
			System.out.println("✅ No new endpoints detected.");
		}
		List<Long> roleIds = new ArrayList<>();
		MRoles role = roleRepository.findTop1ByNameAndIsActive("ROLE_SUPER_ADMIN", true);

		Set<MRoles> roles = new HashSet<>();

		if (role != null) {
			roles.add(role);
			roleIds.add(role.getId());
			System.out.println("Found Role===>" + role.getId() + "Role Name ==" + role.getName());

		}
		List<MController> controllers = endpointRepository.findAll();
		System.out.println("List of Endpoints found==" + controllers.size());
		if (controllers.size() > 0) {
			for (MController contoller : controllers) {
				String controllerName = contoller.getControllerName();
				String methodName = contoller.getMethodName();
				MAPIEndpointsMapping mapping = null;
				if (roleIds.size() > 0) {
					mapping = mAPIEndpointsMappingRepository
							.findBySuperAdminRoleContainingAndControllerNameAndMethodNameAndIsActive(roleIds,
									contoller.getId());
					if (mapping == null) {

						mapping = new MAPIEndpointsMapping();
						mapping.setActive(true);
						mapping.setAD_API_Endpoint_Mapping_UU(UUID.randomUUID().toString());
						mapping.setAdClientId(0L);
						mapping.setAdOrgID(0L);
						mapping.setAmmend(false);
						mapping.setRoles(roles);
						mapping.setEndpoint(contoller);
						mapping.setAdClientId(role.getAdClientId());
						mapping.setAdOrgID(role.getAdOrgID());

						if (mapping != null) {
							mAPIEndpointsMappingRepository.save(mapping);
							mapping.setDocumentNo("ADAPI/ENDP/MAPP/" + Utils.getCurrentYear() + "/" + mapping.getId());
							mAPIEndpointsMappingRepository.save(mapping);
							System.out.println("Automapping Super Admin Role, given access to===>" + methodName + "==="
									+ controllerName + "----" + contoller.getDescription());

						}

					}

				}

			}
			if (role != null) {
				int affected = mAPIEndpointsMappingRepository.updateOrgAndClientWhereZero(role.getAdOrgID(),
						role.getAdClientId());
				System.out.println("Rows updated: " + affected);

			}

		}
	}

	private String makeReadable(String input) {
		String result = input.replaceAll("([a-z])([A-Z])", "$1 $2");

		return result.substring(0, 1).toUpperCase() + result.substring(1);
	}

}
