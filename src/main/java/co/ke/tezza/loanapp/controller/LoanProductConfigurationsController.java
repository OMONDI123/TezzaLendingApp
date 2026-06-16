package co.ke.tezza.loanapp.controller;

import com.google.gson.GsonBuilder;

import co.ke.tezza.loanapp.model.LoanProductConfig;
import co.ke.tezza.loanapp.service.LoanProductConfigurationsService;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;

import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/loanProductConfig")
public class LoanProductConfigurationsController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private LoanProductConfigurationsService loanProductConfigurationsService;

    @PostMapping(value = "/createOrUpdateLoanProductConfiguration", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanProductConfigurationsController','createOrUpdateLoanProductConfiguration')")
    public String createOrUpdateLoanProductConfiguration(@RequestBody LoanProductConfig model) {
        logger.debug("Called LoanProductConfigurationsController.createOrUpdateLoanProductConfiguration");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(loanProductConfigurationsService.createUpdateLoanProductConfig(model));
    }

    @GetMapping(value = "/getAllLoanProductConfiguration/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanProductConfigurationsController','getAllLoanProductConfiguration/{page}/{size}')")
    public String getAllLoanProductConfiguration(@PathVariable int page, @PathVariable int size, @QueryParam(value = "searchTerm") String searchTerm) {
        logger.debug("Called LoanProductConfigurationsController.getAllLoanProductConfiguration");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(loanProductConfigurationsService.getAllLoanProductConfigs(page, size, searchTerm));
    }

    @DeleteMapping(value = "/deleteLoanProductConfige/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('LoanProductConfigurationsController','deleteLoanProductConfige/{id}')")
    public String deleteLoanProductConfige(@PathVariable long id) {
        logger.debug("Called LoanProductConfigurationsController.deleteLoanProductConfige");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(loanProductConfigurationsService.deleteLoanProductConfig(id));
    }
}
