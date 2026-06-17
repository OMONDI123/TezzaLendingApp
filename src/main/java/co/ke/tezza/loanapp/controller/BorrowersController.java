package co.ke.tezza.loanapp.controller;

import javax.validation.Valid;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.google.gson.GsonBuilder;

import co.ke.tezza.loanapp.model.GroupBorrowerModel;
import co.ke.tezza.loanapp.model.IndividualBorrowerModel;
import co.ke.tezza.loanapp.model.InstitutionBorrowerModel;
import co.ke.tezza.loanapp.response.GroupBorrowerResponse;
import co.ke.tezza.loanapp.response.IndividualBorrowerResponse;
import co.ke.tezza.loanapp.response.InstitutionBorrowerResponse;
import co.ke.tezza.loanapp.service.BorrowersServices;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;
import co.ke.tezza.loanapp.util.ResponseEntity;

@RestController
@RequestMapping("/borrowers")
public class BorrowersController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private BorrowersServices borrowersServices;

    @GetMapping(value = "/getAllBorrowers/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('BorrowersController','getAllBorrowers/{page}/{size}')")
    public String getAllBorrowers(@PathVariable("page") int page, 
                                  @PathVariable("size") int size,
                                  @RequestParam(value = "searchTerm", required = false) String searchTerm) {
        
        Page<IndividualBorrowerResponse> borrowers = borrowersServices.getAllIndividualBorrowers(page, size, searchTerm);
        logger.debug("Called BorrowersController.getAllBorrowers");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(borrowers);
    }

    @PostMapping(value = "/createUpdateBorrower", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('BorrowersController','createUpdateBorrower')")
    public String createUpdateBorrower(@RequestBody @Valid IndividualBorrowerModel model) {
        ResponseEntity<?> response = borrowersServices.createUpdateIndividualBorrowers(model);
        logger.debug("Called BorrowersController.createUpdateBorrower");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(response);
    }

    @PutMapping(value = "/deleteBorrower/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('BorrowersController','deleteBorrower/{id}')")
    public String deleteBorrower(@PathVariable("id") Long id) {
        ResponseEntity<?> response = borrowersServices.deleteIndividualBorrowerById(id);
        logger.debug("Called BorrowersController.deleteBorrower");
        return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
                .toJson(response);
    }
    

    // --------------------- Institution Borrowers ---------------------

    @GetMapping(value = "/getAllInstitutionBorrowers/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('BorrowersController','getAllInstitutionBorrowers/{page}/{size}')")
    public String getAllInstitutionBorrowers(@PathVariable("page") int page, @PathVariable("size") int size,@QueryParam(value = "searchTerm") String searchTerm) {
        Page<InstitutionBorrowerResponse> borrowers = borrowersServices.getAllInstitutionBorrowers(page, size,searchTerm);
        logger.debug("Called getAllInstitutionBorrowers");
        return toJson(borrowers);
    }

    @PostMapping(value = "/createUpdateInstitutionBorrower", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('BorrowersController','createUpdateInstitutionBorrower')")
    public String createUpdateInstitutionBorrower(@RequestBody @Valid InstitutionBorrowerModel model) {
        ResponseEntity<?> response = borrowersServices.createUpdateInstitutionBorrower(model);
        logger.debug("Called createUpdateInstitutionBorrower");
        return toJson(response);
    }

    @PutMapping(value = "/deleteInstitutionBorrower/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('BorrowersController','deleteInstitutionBorrower/{id}')")
    public String deleteInstitutionBorrower(@PathVariable("id") Long id) {
        ResponseEntity<?> response = borrowersServices.deleteInstitutionBorrowerById(id);
        logger.debug("Called deleteInstitutionBorrower");
        return toJson(response);
    }

    // --------------------- Group Borrowers ---------------------

    @GetMapping(value = "/getAllGroupBorrowers/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('BorrowersController','getAllGroupBorrowers/{page}/{size}')")
    public String getAllGroupBorrowers(
        @PathVariable("page") int page, 
        @PathVariable("size") int size,
        @RequestParam(value = "searchTerm", required = false) String searchTerm,
        @RequestParam(value = "statusFilter", required = false) String statusFilter,
        @RequestParam(value = "typeFilter", required = false) String typeFilter) {
        
        Page<GroupBorrowerResponse> borrowers = borrowersServices.getAllGroupBorrowers(
            page, size, searchTerm, statusFilter, typeFilter);
        logger.debug("Called getAllGroupBorrowers with search: {}, status: {}, type: {}", 
            searchTerm, statusFilter, typeFilter);
        return toJson(borrowers);
    }
    @PostMapping(value = "/createUpdateGroupBorrower", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('BorrowersController','createUpdateGroupBorrower')")
    public String createUpdateGroupBorrower(@RequestBody @Valid GroupBorrowerModel model) {
        ResponseEntity<?> response = borrowersServices.createUpdateGroupBorrower(model);
        logger.debug("Called createUpdateGroupBorrower");
        return toJson(response);
    }

    @PutMapping(value = "/deleteGroupBorrower/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('BorrowersController','deleteGroupBorrower/{id}')")
    public String deleteGroupBorrower(@PathVariable("id") Long id) {
        ResponseEntity<?> response = borrowersServices.deleteGroupBorrowerById(id);
        logger.debug("Called deleteGroupBorrower");
        return toJson(response);
    }
    
    private String toJson(Object object) {
        return new GsonBuilder()
                .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
                .create()
                .toJson(object);
    }
}
