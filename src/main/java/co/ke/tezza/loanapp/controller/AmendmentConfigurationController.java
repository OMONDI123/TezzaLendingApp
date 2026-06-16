package co.ke.tezza.loanapp.controller;

import com.google.gson.GsonBuilder;

import co.ke.tezza.loanapp.model.AmendmentConfigurationRequest;
import co.ke.tezza.loanapp.service.AmendmentConfigurationService;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;

import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/amendmentConfig")
public class AmendmentConfigurationController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AmendmentConfigurationService amendmentConfigurationService;

    @PostMapping(value = "/createOrUpdateAmendmentConfiguration", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('AmendmentConfigurationController','createOrUpdateAmendmentConfiguration')")
    public String createOrUpdateAmendmentConfiguration(@RequestBody AmendmentConfigurationRequest model) {
        logger.debug("Called AmendmentConfigurationController.createOrUpdateAmendmentConfiguration");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(amendmentConfigurationService.createUpdateConfiguration(model));
    }

    @GetMapping(value = "/getAllAmendmentConfiguration/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('AmendmentConfigurationController','getAllAmendmentConfiguration/{page}/{size}')")
    public String getAllAmendmentConfiguration(@PathVariable int page, @PathVariable int size, @QueryParam(value = "searchTerm") String searchTerm) {
        logger.debug("Called AmendmentConfigurationController.getAllAmendmentConfiguration");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(amendmentConfigurationService.getAllAmendmentConfigs(page, size, searchTerm));
    }

    @DeleteMapping(value = "/deleteAmendmentConfig/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('AmendmentConfigurationController','deleteAmendmentConfig/{id}')")
    public String deleteAmendmentConfig(@PathVariable long id) {
        logger.debug("Called AmendmentConfigurationController.deleteAmendmentConfig");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(amendmentConfigurationService.deactivateConfiguration(id));
    }

    @PostMapping(value = "/cloneAmendmentConfig/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('AmendmentConfigurationController','cloneAmendmentConfig/{id}')")
    public String cloneAmendmentConfig(@PathVariable long id, @RequestParam String newConfigName) {
        logger.debug("Called AmendmentConfigurationController.cloneAmendmentConfig");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(amendmentConfigurationService.cloneConfiguration(id, newConfigName));
    }
}