package co.ke.tezza.loanapp.controller;

import java.util.List;

import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.GsonBuilder;

import co.ke.tezza.loanapp.entity.MAPIEndpointsMapping;
import co.ke.tezza.loanapp.entity.MController;
import co.ke.tezza.loanapp.model.AccessMappingCopyRequest;
import co.ke.tezza.loanapp.model.MAPIEndpointsMappingModel;
import co.ke.tezza.loanapp.service.RoleMappingService;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;
import co.ke.tezza.loanapp.util.Utils;

@RestController
@RequestMapping(value = "/apiEndpoints")
public class APIEndpointsAccessMappingController {

    @Autowired
    private RoleMappingService roleMappingService;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    Utils utils;

    @PostMapping(value = "/mappOrUpdateAPIEndpointRoleAccess", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN')")
    public String mappOrUpdateAPIEndpointRoleAccess(@RequestBody MAPIEndpointsMappingModel model) {
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(roleMappingService.mappOrUpdateRoleAccess(model));
    }
    
    @PostMapping(value = "/copyRightsAccessMapping", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN')")
    public String copyRightsAccessMapping(@RequestBody AccessMappingCopyRequest model) {
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(roleMappingService.copyMappedEndpoints(model));
    }
    

    @PutMapping(value = "/deleteMappedAPIEndpointRoleAccess/{endPointMappingId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN')")
    @ResponseBody
    public String deleteMappedAPIEndpointRoleAccess(@PathVariable("endPointMappingId") Long endPointMappingId) {
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(roleMappingService.deleteMappedAPIEndpointRoleAccess(endPointMappingId));
    }

    @GetMapping(value = "/getMappedAPIEndpointsRoleAccessByOrganisation/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN')")
    @ResponseBody
    public String getMappedAPIEndpointsRoleAccessByOrganisation(
            @PathVariable(value = "page") int page,
            @PathVariable(value = "size") int size,
            @QueryParam(value = "search") String search) {
        Page<MAPIEndpointsMapping> mappedRoleAccess = roleMappingService
                .getMappedAPIEndpointsRoleAccessByOrganisation(utils.getAD_Org_ID(), page, size, search);
        logger.debug("Called APIEndpointsAccessMappingController.getMappedAPIEndpointsRoleAccessByOrganisation with search: {}", search);
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(mappedRoleAccess);
    }

    @GetMapping(value = "/getAPIEndpointsTobeMappedPaginated/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN')")
    @ResponseBody
    public String getAPIEndpointsTobeMappedPaginated(
            @PathVariable(value = "page") int page,
            @PathVariable(value = "size") int size,
            @QueryParam(value = "search") String search) {
        Page<MController> controllers = roleMappingService.getAllEndpoints(page, size, search);
        logger.debug("Called APIEndpointsAccessMappingController.getAPIEndpointsTobeMappedPaginated with search: {}", search);
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(controllers);
    }

    

    @GetMapping(value = "/getMappedAPIEndpointsRoleAccessByRoles", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN')")
    @ResponseBody
    public String getMappedAPIEndpointsRoleAccessByRoles() {
        List<MAPIEndpointsMapping> controllers = roleMappingService.getMappedAPIEndpointsRoleAccessByRoles();
        logger.debug("Called APIEndpointsAccessMappingController.getMappedAPIEndpointsRoleAccessByRoles");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(controllers);
    }
    
    @GetMapping(value = "/getAccessRightsByRoleId/{roleFromId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN')")
    @ResponseBody
    public String getAccessRightsByRoleId(@PathVariable(value = "roleFromId") long roleFromId, @QueryParam(value = "searchTerm") String searchTerm) {
        List<MAPIEndpointsMapping> controllers = roleMappingService.getMappedAPIEndpointsRoleAccessByRoleId(roleFromId,searchTerm);
        logger.debug("Called APIEndpointsAccessMappingController.getAccessRightsByRoleId");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(controllers);
    }
}