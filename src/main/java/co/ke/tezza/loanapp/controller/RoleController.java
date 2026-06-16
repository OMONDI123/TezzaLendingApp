/**
 * 
 */
package co.ke.tezza.loanapp.controller;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.GsonBuilder;

import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.model.RoleModel;
import co.ke.tezza.loanapp.response.RoleResponse;
import co.ke.tezza.loanapp.service.RoleService;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;
import co.ke.tezza.loanapp.util.ResponseEntity;

/**
 * @author austine
 *
 */
@RestController
@RequestMapping("/role")
@CrossOrigin
public class RoleController {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private RoleService service;
	
	

	

	@PostMapping(value = "/createAndUpdateRole", produces = MediaType.APPLICATION_JSON_VALUE)
    //@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('RoleController','createAndUpdateRole')")
	@ResponseBody
	public String createRole(@RequestBody RoleModel role) {
		RoleResponse createdRole = service.createRoles(role);
		logger.debug("Called RoleController.createRole");
		String message="Role Added Successfully.";
		ResponseEntity<?> response=new ResponseEntity<RoleResponse>(message, 200, createdRole);
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(response);
	}

	@PutMapping(value = "/deleteRoleById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('RoleController','deleteRoleById/{id}')")
	@ResponseBody
	public String deleteById(@PathVariable Long id) {
		MRoles deletedRole = service.deleteById(id);
		logger.debug("Called RoleController.deleteById");
		String message="Role Deleted Successfully.";
		ResponseEntity<?> response=new ResponseEntity<MRoles>(message, 200, deletedRole);
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(response);
	}

	@GetMapping(value = "/getActiveRoleByorganizationAndSuperAdmin/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('RoleController','getActiveRoleByorganizationAndSuperAdmin/{page}/{size}')")
	@ResponseBody
	public String getActiveRole(@PathVariable int page, @PathVariable int size) {
		Page<MRoles> mRoles = service.findByIsActiveAndAdOrgIDOrAndSuperAdmin(true,page,size);
		logger.debug("Called RoleController.getActiveRoleByorganizationAndSuperAdmin");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(mRoles);
	}
	
	@GetMapping(value = "/getActiveRoleByorganization/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('RoleController','getActiveRoleByorganization/{page}/{size}')")
	@ResponseBody
	public String getActiveRoleByorganization(@PathVariable int page, @PathVariable int size) {
		Page<MRoles> mRoles = service.findByIsActiveAndAdOrgID(true,page,size);
		logger.debug("Called RoleController.getActiveRoleByorganization");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(mRoles);
	}
	
	@GetMapping(value = "/getRolesByUserId", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('RoleController','getRolesByUserId')")
	@ResponseBody
	public String getRolesByUserId() {
		List<MRoles> mRoles = service.getRolesByUserId();
		logger.debug("Called RoleController.getRolesByUserId");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(mRoles);
	}

}
