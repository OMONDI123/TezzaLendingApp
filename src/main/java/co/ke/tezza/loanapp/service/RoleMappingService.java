package co.ke.tezza.loanapp.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MAPIEndpointsMapping;
import co.ke.tezza.loanapp.entity.MController;
import co.ke.tezza.loanapp.entity.MEntityRoleMapping;
import co.ke.tezza.loanapp.entity.MMenuRoleMapping;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MTable;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.AccessMappingCopyRequest;
import co.ke.tezza.loanapp.model.MAPIEndpointsMappingModel;
import co.ke.tezza.loanapp.model.MEntityRoleMappingModel;
import co.ke.tezza.loanapp.repository.MAPIEndpointsMappingRepository;
import co.ke.tezza.loanapp.repository.MControllerRepository;
import co.ke.tezza.loanapp.repository.MEntityRoleMappingRepository;
import co.ke.tezza.loanapp.repository.MOrgRepository;
import co.ke.tezza.loanapp.repository.MTableRepository;
import co.ke.tezza.loanapp.repository.MclientRepository;
import co.ke.tezza.loanapp.repository.RoleRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

@Service
public class RoleMappingService {

	@Autowired
	private MEntityRoleMappingRepository mappingRepository;

	@Autowired
	private MTableRepository mTableRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private Utils utils;
	@Autowired
	private MAPIEndpointsMappingRepository apiEndpointappingRepository;

	@Autowired
	private MControllerRepository mControllerRepository;
	@Autowired
	private MclientRepository mclientRepository;
	@Autowired
	private MOrgRepository mOrgRepository;

	public ResponseEntity<MEntityRoleMapping> mappOrUpdateRoleAccess(MEntityRoleMappingModel model) {
		// TODO Auto-generated method stub
		String message = "";
		int code = 201;
		MEntityRoleMapping mappedRoleAccess = mappingRepository.findById(model.getId()).orElse(null);
		try {
			if (mappedRoleAccess != null) {
				MTable table = mTableRepository.findById(model.getAD_Table_ID()).orElse(null);
				if (table != null) {
					mappedRoleAccess.setTable(table);
				}
				Set<MRoles> roles = new HashSet<>();
				for (int i = 0; i < model.getRoleIds().size(); i++) {
					MRoles role = roleRepository.findById(model.getRoleIds().get(i)).orElse(null);
					if (role != null) {
						roles.add(role);
					}
				}
				mappedRoleAccess.setRoles(roles);

				mappingRepository.save(mappedRoleAccess);

				message = "Role Access Mapping has been successfully updated.";
				code = 200;
			} else {
				MTable table = mTableRepository.findById(model.getAD_Table_ID()).orElse(null);
				if (table != null) {
					mappedRoleAccess = new MEntityRoleMapping();
					mappedRoleAccess.setTable(table);
				}
				Set<MRoles> roles = new HashSet<>();
				for (int i = 0; i < model.getRoleIds().size(); i++) {
					MRoles role = roleRepository.findById(model.getRoleIds().get(i)).orElse(null);
					if (role != null) {
						roles.add(role);
					}
				}
				mappedRoleAccess.setRoles(roles);
				mappedRoleAccess.setAD_Entity_Role_UU(UUID.randomUUID().toString());

				mappingRepository.save(mappedRoleAccess);
				mappedRoleAccess.setDocumentNo("ADENT/MAPP/" + Utils.getCurrentYear() + "/" + mappedRoleAccess.getId());
				mappingRepository.save(mappedRoleAccess);
				message = "Role Access Mapping has been successfully created.";
				code = 200;

			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			message = "Could Not Delete the record." + e.getMessage();
			code = 201;
		}
		return new ResponseEntity<MEntityRoleMapping>(message, code, mappedRoleAccess);
	}

	public ResponseEntity<MEntityRoleMapping> deleteMappedRoleAccess(Long aD_Entity_Role_ID) {
		// TODO Auto-generated method stub

		String message = "";
		int code = 201;
		MEntityRoleMapping mappedRoleAccess = mappingRepository.findById(aD_Entity_Role_ID).orElse(null);
		try {
			if (mappedRoleAccess != null) {
				mappedRoleAccess.setActive(false);
				mappingRepository.save(mappedRoleAccess);
				message = "Role Access Mapping has been successfully deleted.";
				code = 200;
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			message = "Could Not Delete the record." + e.getMessage();
			code = 201;
		}
		return new ResponseEntity<MEntityRoleMapping>(message, code, mappedRoleAccess);
	}

	public Page<MEntityRoleMapping> getMappedModulesRoleAccessByOrganisation(long ad_Org_ID, int page, int size) {
		// TODO Auto-generated method stub
		Page<MEntityRoleMapping> mappedRoleAccess = null;

		PageRequest pageRequest = PageRequest.of(page, size);
		try {
			mappedRoleAccess = mappingRepository.findByAdOrgIDAndIsActive(ad_Org_ID, true, pageRequest);
			if (mappedRoleAccess == null) {
				mappedRoleAccess = new PageImpl<MEntityRoleMapping>(Collections.emptyList(), pageRequest, 0);
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();

		}
		return mappedRoleAccess;
	}

	public List<MEntityRoleMapping> getMappedRoleAccessByRoles() {
		// TODO Auto-generated method stub
		List<MEntityRoleMapping> mappedRoleAccess = null;
		MUser user = userRepo.findById(utils.getAD_User_ID()).orElse(null);
		List<Long> roleIds = new ArrayList<>();

		if (user != null) {
			Set<MRoles> roles = user.getRoles();
			for (MRoles role : roles) {
				roleIds.add(role.getId());
			}
			try {
				mappedRoleAccess = mappingRepository.findByRoleContainingAndOrganisation(roleIds, utils.getAD_Org_ID());

			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();

			}

		}

		return mappedRoleAccess;
	}

	public Page<MTable> getAllTables(int page, int size) {

		return mTableRepository.findAll(PageRequest.of(page, size));
	}

	public Page<MTable> getAllTablesByName(int page, int size, String tableName) {

		return mTableRepository.findByEntityNameContainingOrderByEntityNameAsc(tableName, PageRequest.of(page, size));

	}

	public ResponseEntity<MAPIEndpointsMapping> mappOrUpdateRoleAccess(MAPIEndpointsMappingModel model) {
		// TODO Auto-generated method stub
		String message = "";
		int code = 201;
		MAPIEndpointsMapping mappedRoleAccess = apiEndpointappingRepository.findById(model.getId()).orElse(null);
		try {
			if (mappedRoleAccess != null) {
				MController controller = mControllerRepository.findById(model.getAD_Endpoint_ID()).orElse(null);
				if (controller != null) {
					mappedRoleAccess.setEndpoint(controller);
				}
				Set<MRoles> roles = new HashSet<>();
				for (int i = 0; i < model.getRoleIds().size(); i++) {
					MRoles role = roleRepository.findById(model.getRoleIds().get(i)).orElse(null);
					if (role != null) {
						roles.add(role);
					}
				}
				mappedRoleAccess.setRoles(roles);

				apiEndpointappingRepository.save(mappedRoleAccess);

				message = "Role Access Mapping has been successfully updated.";
				code = 200;
			} else {
				mappedRoleAccess = new MAPIEndpointsMapping();
				MController controller = mControllerRepository.findById(model.getAD_Endpoint_ID()).orElse(null);
				if (controller != null) {

					mappedRoleAccess.setEndpoint(controller);
				}
				Set<MRoles> roles = new HashSet<>();
				for (int i = 0; i < model.getRoleIds().size(); i++) {
					MRoles role = roleRepository.findById(model.getRoleIds().get(i)).orElse(null);
					if (role != null) {
						roles.add(role);
					}
				}
				mappedRoleAccess.setRoles(roles);
				mappedRoleAccess.setAD_API_Endpoint_Mapping_UU(UUID.randomUUID().toString());

				apiEndpointappingRepository.save(mappedRoleAccess);
				mappedRoleAccess
						.setDocumentNo("ADAPI/ENDP/MAPP/" + Utils.getCurrentYear() + "/" + mappedRoleAccess.getId());
				apiEndpointappingRepository.save(mappedRoleAccess);
				message = "Role Access Mapping has been successfully created.";
				code = 200;

			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			message = "Could Not Delete the record." + e.getMessage();
			code = 201;
		}
		return new ResponseEntity<MAPIEndpointsMapping>(message, code, mappedRoleAccess);
	}

	public ResponseEntity<AccessMappingCopyRequest> copyMappedEndpoints(AccessMappingCopyRequest request) {
	    // 1. Validate and fetch roles in a single operation
	    MRoles roleFrom = roleRepository.findById(request.getRoleIdFrom())
	            .orElseThrow(() -> new SetUpExceptions("Source role not found."));
	    
	    MRoles roleTo = roleRepository.findById(request.getRoleIdTo())
	            .orElseThrow(() -> new SetUpExceptions("Target role not found."));
	    
	    // 2. Fetch active endpoint mappings for source role
	    List<MAPIEndpointsMapping> roleAccess = apiEndpointappingRepository
	            .findByIsActiveAndRoles(true, roleFrom);
	    
	    if (roleAccess.isEmpty()) {
	        throw new SetUpExceptions("No endpoint mappings found for source role.");
	    }
	    
	    int count = 0;
	    List<MAPIEndpointsMapping> newMappings = new ArrayList<>();
	    
	    // 3. Process mappings
	    for (MAPIEndpointsMapping model : roleAccess) {
	        // Skip if target role already has this mapping
	        if (model.getRoles().contains(roleTo)) {
	            continue;
	        }
	        
	        if (roleTo.getAdOrgID() == model.getAdOrgID()) {
	            // Same organization - add role to existing mapping
	            Set<MRoles> roles = new HashSet<>(model.getRoles()); // Create mutable copy
	            roles.add(roleTo);
	            model.setRoles(roles);
	            newMappings.add(model); // Will be updated
	        } else {
	            // Different organization - create new mapping
	            MAPIEndpointsMapping newModel = new MAPIEndpointsMapping();
	            
	            // FIXED: Use mutable HashSet instead of immutable Set.of()
	            Set<MRoles> roles = new HashSet<>();
	            roles.add(roleTo);
	            newModel.setRoles(roles);
	            
	            newModel.setAdOrgID(roleTo.getAdOrgID());
	            newModel.setAdClientId(roleTo.getAdClientId());
	            newModel.setEndpoint(model.getEndpoint());
	            newModel.setAD_API_Endpoint_Mapping_UU(UUID.randomUUID().toString());
	            newModel.setDocumentNo("ADAPI/ENDP/MAPP/" + Utils.getCurrentYear() + "/" + 0); // Will be updated after save
	            
	            newMappings.add(newModel);
	        }
	        
	        count++;
	    }
	    
	    // 4. Batch save all changes
	    if (!newMappings.isEmpty()) {
	        List<MAPIEndpointsMapping> savedMappings = apiEndpointappingRepository.saveAll(newMappings);
	        
	        // 5. Update document numbers for newly created mappings
	        List<MAPIEndpointsMapping> toUpdate = new ArrayList<>();
	        for (MAPIEndpointsMapping mapping : savedMappings) {
	            if (mapping.getId() > 0 && mapping.getDocumentNo().endsWith("/0")) {
	                mapping.setDocumentNo("ADAPI/ENDP/MAPP/" + Utils.getCurrentYear() + "/" + mapping.getId());
	                toUpdate.add(mapping);
	            }
	        }
	        
	        // Save document number updates if any
	        if (!toUpdate.isEmpty()) {
	            apiEndpointappingRepository.saveAll(toUpdate);
	        }
	    }
	    
	    // 6. Set response
	    request.setTotalMappedEndpoints(count);

	    return new ResponseEntity<AccessMappingCopyRequest>(count + " Access Mapping Copied Successfully.",200,request);
	}

	public ResponseEntity<MAPIEndpointsMapping> deleteMappedAPIEndpointRoleAccess(Long AD_API_Endpoint_Mapping_ID) {
		// TODO Auto-generated method stub

		String message = "";
		int code = 201;
		MAPIEndpointsMapping mappedRoleAccess = apiEndpointappingRepository.findById(AD_API_Endpoint_Mapping_ID)
				.orElse(null);
		try {
			if (mappedRoleAccess != null) {
				mappedRoleAccess.setActive(false);
				apiEndpointappingRepository.save(mappedRoleAccess);
				message = "Role Access Mapping has been successfully deleted.";
				code = 200;
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			message = "Could Not Delete the record." + e.getMessage();
			code = 201;
		}
		return new ResponseEntity<MAPIEndpointsMapping>(message, code, mappedRoleAccess);
	}

	public Page<MAPIEndpointsMapping> getMappedAPIEndpointsRoleAccessByOrganisation(long ad_Org_ID, int page, int size,
			String search) {
		// TODO Auto-generated method stub
		Page<MAPIEndpointsMapping> mappedRoleAccess = null;
		search = (search == null || search.trim().isEmpty()) ? "%" : "%" + search.trim() + "%";

		PageRequest pageRequest = PageRequest.of(page, size);
		try {
			if (search != null && !search.isEmpty()) {
				mappedRoleAccess = apiEndpointappingRepository
						.searchMappedAPIEndpointsRoleAccessByOrganisation(ad_Org_ID, search, true, pageRequest);

			} else {
				mappedRoleAccess = apiEndpointappingRepository.findByAdOrgIDAndIsActive(ad_Org_ID, true, pageRequest);

			}
			if (mappedRoleAccess == null) {
				mappedRoleAccess = new PageImpl<MAPIEndpointsMapping>(Collections.emptyList(), pageRequest, 0);
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();

		}
		if (mappedRoleAccess != null) {
			if (mappedRoleAccess.hasContent()) {
				for (MAPIEndpointsMapping map : mappedRoleAccess) {
					map.setClientName(mclientRepository.findById(map.getAdClientId()).get().getName());
					map.setOrganizationName(mOrgRepository.findById(map.getAdOrgID()).get().getName());
				}

			}

		}

		return mappedRoleAccess;
	}

	public List<MAPIEndpointsMapping> getMappedAPIEndpointsRoleAccessByRoles() {
		// TODO Auto-generated method stub
		List<MAPIEndpointsMapping> mappedRoleAccess = null;
		MUser user = userRepo.findById(utils.getAD_User_ID()).orElse(null);
		List<Long> roleIds = new ArrayList<>();

		if (user != null) {
			Set<MRoles> roles = user.getRoles();
			for (MRoles role : roles) {
				roleIds.add(role.getId());
			}
			try {
				mappedRoleAccess = apiEndpointappingRepository.findByRoleContainingAndOrganisation(roleIds,
						utils.getAD_Org_ID());

			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();

			}

		}

		return mappedRoleAccess;
	}

	public List<MAPIEndpointsMapping> getMappedAPIEndpointsRoleAccessByRoleId(long roleId, String searchTerm) {
		// TODO Auto-generated method stub
		List<MAPIEndpointsMapping> mappedRoleAccess = null;

		try {
			MRoles role = roleRepository.findById(roleId).get();
			if (searchTerm != null && !searchTerm.isEmpty()) {
				mappedRoleAccess = apiEndpointappingRepository.searchAcceMapingByRoleId(roleId, utils.getAD_Org_ID(),
						searchTerm);
			} else {
				mappedRoleAccess = apiEndpointappingRepository.findByIsActiveAndAdClientIdAndRoles(true,
						utils.getAD_Client_ID(), role);
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();

		}

		return mappedRoleAccess;
	}

	public Page<MController> getAllEndpoints(int page, int size, String search) {
		if (search != null && !search.isEmpty()) {
			return mControllerRepository
					.findByDescriptionContainsIgnoreCaseOrControllerNameContainsIgnoreCaseOrMethodNameContainsIgnoreCaseOrEndpointContainsIgnoreCaseOrUrlPatternContainsIgnoreCaseOrHttpMethodContainsIgnoreCase(
							search, search, search, search, search, search, PageRequest.of(page, size));

		} else {
			return mControllerRepository.findAll(PageRequest.of(page, size));

		}
	}

}
