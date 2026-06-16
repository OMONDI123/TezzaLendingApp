package co.ke.tezza.loanapp.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.GsonBuilder;

import co.ke.tezza.loanapp.entity.LetterHeaads;
import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MClient;
import co.ke.tezza.loanapp.entity.MEmail;
import co.ke.tezza.loanapp.entity.MOrg;
import co.ke.tezza.loanapp.entity.MPaymentMethod;
import co.ke.tezza.loanapp.enums.PriorityEnum;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import co.ke.tezza.loanapp.model.EmailConfigRequest;
import co.ke.tezza.loanapp.model.FileUpload;
import co.ke.tezza.loanapp.model.MOrgModel;
import co.ke.tezza.loanapp.model.MUserClientAuditModel;
import co.ke.tezza.loanapp.model.PaymentGateWayConfigRequest;
import co.ke.tezza.loanapp.model.PaymentMethodModel;
import co.ke.tezza.loanapp.model.RemindersConfigRequest;
import co.ke.tezza.loanapp.model.SetupModel;
import co.ke.tezza.loanapp.model.SmsConfigRequest;
import co.ke.tezza.loanapp.model.SmsSetupRequest;
import co.ke.tezza.loanapp.model.SystemSettingsConfig;
import co.ke.tezza.loanapp.response.Country;
import co.ke.tezza.loanapp.response.County;
import co.ke.tezza.loanapp.response.PaymentGateWayConfigResponse;
import co.ke.tezza.loanapp.response.RemindersConfigResponse;
import co.ke.tezza.loanapp.response.SmsConfigResponse;
import co.ke.tezza.loanapp.response.SmsSetupResponse;
import co.ke.tezza.loanapp.response.SubCounty;
import co.ke.tezza.loanapp.response.Ward;
import co.ke.tezza.loanapp.service.CommonService;
import co.ke.tezza.loanapp.service.MOrgService;
import co.ke.tezza.loanapp.util.DatabaseBackupUtil;
import co.ke.tezza.loanapp.util.DatabaseCleanUpUtil;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;
import lombok.val;

@RestController
@RequestMapping(value = "/common")
@CrossOrigin
public class CommonController {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private CommonService commonService;

	@Autowired
	private MOrgService orgService;
	@Autowired DatabaseCleanUpUtil databaseCleanUpUtil;
	@Autowired private DatabaseBackupUtil databaseBackupUtil;
	
	
	@PostMapping(value = "/clearDbTables", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','clearDbTables')")
	public String clearDbTables() {
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(databaseCleanUpUtil.clearTables());
	}
	
	
	@PostMapping(value = "/backupDbAndUploadToDrive", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','clearDbTables')")
	public String backupDbAndUploadToDrive() throws Exception {
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(databaseBackupUtil.backup());
	}
	
	// PAYMENT GATEWAY CONFIGURATION ENDPOINTS
	@PostMapping(value = "/createUpdatePaymentGatewayConfig", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createUpdatePaymentGatewayConfig')")
	public String createUpdatePaymentGatewayConfig(@RequestBody PaymentGateWayConfigRequest request) {
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(commonService.createUpdatePaymentGatewayConfig(request));
	}
	
	@PostMapping(value = "/uploadLetterHead", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','uploadLetterHead')")
	public String uploadLetterHead(@RequestBody FileUpload request) {
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(commonService.uploadLetterHead(request));
	}
	
	
	@DeleteMapping(value = "/deleteLetterHead/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','deleteLetterHead/{id}')")
	public String deleteLetterHead(@PathVariable long id) {
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(commonService.deleteLetterHead(id));
	}
	@GetMapping(value = "/getLetterHead", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getLetterHead')")
	@ResponseBody
	public String getLetterHead() {
	    List<LetterHeaads> configs = commonService.getLetterHeaadsByOrganisation();
	    logger.debug("Called CommonController.getLetterHead");
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(configs);
	}
	
	@GetMapping(value = "/getPaymentMethodsList", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getPaymentMethodsList')")
	@ResponseBody
	public String getPaymentMethodsList() {
	    List<MPaymentMethod> paymentMethods = commonService.getPaymentMethodList();
	    logger.debug("Called CommonController.getLetterHead");
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(paymentMethods);
	}
	
	
	@GetMapping(value = "/getPaymentMethodsPaginated/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getPaymentMethodsPaginated/{page}/{size}')")
	@ResponseBody
	public String getPaymentMethodsPaginated(@PathVariable (value = "page") int page, @PathVariable (value = "size") int size) {
	    Page<MPaymentMethod> paymentMethods = commonService.getPaymentMethodsPaginated(page, size);
	    logger.debug("Called CommonController.getLetterHead");
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(paymentMethods);
	}
	
	
	@PostMapping(value = "/createUpdatePaymentMethod", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createUpdatePaymentMethod')")
	public String createUpdatePaymentMethod(@RequestBody PaymentMethodModel request) {
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(commonService.createUpdatePaymentMethod(request));
	}
	
	
	
	
	

	@DeleteMapping(value = "/deletePaymentGatewayConfig/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','deletePaymentGatewayConfig/{id}')")
	public String deletePaymentGatewayConfig(@PathVariable Long id) {
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(commonService.deletePaymentGatewayConfig(id));
	}

	@GetMapping(value = "/getAllPaymentGatewayConfigs", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getAllPaymentGatewayConfigs')")
	@ResponseBody
	public String getAllPaymentGatewayConfigs() {
	    List<PaymentGateWayConfigResponse> configs = commonService.getAllPaymentGatewayConfigs();
	    logger.debug("Called CommonController.getAllPaymentGatewayConfigs");
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(configs);
	}

	
	// SMS Configuration Endpoints
	@PostMapping(value = "/createUpdateSmsConfig", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createUpdateSmsConfig')")
	public String createUpdateSmsConfig(@RequestBody SmsConfigRequest request) {
	    return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
	            .toJson(commonService.createUpdateSmsConfig(request));
	}

	@DeleteMapping(value = "/deleteSmsConfig/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','deleteSmsConfig/{id}')")
	public String deleteSmsConfig(@PathVariable Long id) {
	    return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
	            .toJson(commonService.deleteSmsConfig(id));
	}

	@GetMapping(value = "/getAllSmsConfigurations", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getAllSmsConfigurations')")
	@ResponseBody
	public String getAllSmsConfigurations() {
	    List<SmsConfigResponse> configs = commonService.getAllSmsConfigurations();
	    logger.debug("Called CommonController.getAllSmsConfigurations");
	    return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
	            .toJson(configs);
	}

	// SMS Setup/Template Endpoints
	@PostMapping(value = "/createUpdateSmsSetup", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createUpdateSmsSetup')")
	public String createUpdateSmsSetup(@RequestBody SmsSetupRequest request) {
	    return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
	            .toJson(commonService.createUpdateSmsSetup(request));
	}

	@DeleteMapping(value = "/deleteSmsSetup/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','deleteSmsSetup/{id}')")
	public String deleteSmsSetup(@PathVariable Long id) {
	    return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
	            .toJson(commonService.deleteSmsSetup(id));
	}

	@GetMapping(value = "/getAllSmsTemplates", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getAllSmsTemplates')")
	@ResponseBody
	public String getAllSmsTemplates() {
	    List<SmsSetupResponse> templates = commonService.getAllSmsTemplates();
	    logger.debug("Called CommonController.getAllSmsTemplates");
	    return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
	            .toJson(templates);
	}
	
	@PostMapping(value = "/createUpdateReminderConfig", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createUpdateReminderConfig')")
	public String createUpdateReminderConfig(@RequestBody RemindersConfigRequest request) {
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(commonService.createUpdateReminderConfig(request));
	}

	@DeleteMapping(value = "/deleteReminderConfig/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','deleteReminderConfig/{id}')")
	public String deleteReminderConfig(@PathVariable Long id) {
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(commonService.deleteReminderConfig(id));
	}

	@GetMapping(value = "/getAllReminderConfigs", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getAllReminderConfigs')")
	@ResponseBody
	public String getAllReminderConfigs() {
	    List<RemindersConfigResponse> configs = commonService.getAllReminderConfigs();
	    logger.debug("Called CommonController.getAllReminderConfigs");
	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(configs);
	}

	// SMS Type and Placeholder Endpoints
	@GetMapping(value = "/getSmsTypeEnum", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getSmsTypeEnum')")
	@ResponseBody
	public String getSmsTypeEnum() {
	    List<SmsTypeEnum> smsTypes = commonService.getSmsTypeEnum();
	    logger.debug("Called CommonController.getSmsTypeEnum");
	    return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
	            .toJson(smsTypes);
	}

	@GetMapping(value = "/getSupportedPlaceHolders/{smsType}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getSupportedPlaceHolders/{smsType}')")
	@ResponseBody
	public String getSupportedPlaceHolders(@PathVariable SmsTypeEnum smsType) {
	    List<String> placeholders = commonService.getSupportedPlaceHolders(smsType);
	    logger.debug("Called CommonController.getSupportedPlaceHolders");
	    return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
	            .toJson(placeholders);
	}

	@GetMapping(value = "/getSettingsCategories", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getSettingsCategories')")
	@ResponseBody
	public String getSettingsCategories() {
		List<SettingCategoriesEnum> settings = new ArrayList<>(Arrays.asList(SettingCategoriesEnum.values()));
		logger.debug("Called CommonController.getSettingsCategories");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(settings);
	}

	@GetMapping(value = "/getPriorities", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getPriorities')")
	@ResponseBody
	public String getPriorities() {
		List<PriorityEnum> priorities = Arrays.asList(PriorityEnum.values());
		logger.debug("Called CommonController.getPriorities");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(priorities);
	}

	@PostMapping(value = "/createAndUpdateOrganisationSystemSettings", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createAndUpdateOrganisationSystemSettings')")
	public String createAndUpdateOrganisationSystemSettings(@RequestBody SystemSettingsConfig sysCondig) {

		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.createOrUpdateSystemSettings(sysCondig));
	}

	@PostMapping(value = "/createUpdateSystemOrganisationEmail", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createUpdateSystemOrganisationEmail')")
	public String createUpdateSystemOrganisationEmail(@RequestBody EmailConfigRequest request) {

		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.createUpdateSystemOrganisationEmail(request));
	}
	
	@DeleteMapping(value = "/deleteEmailConfig/{emailId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','deleteEmailConfig/{emailId}')")
	public String deleteEmailConfig(@PathVariable long emailId) {

		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.deleteEmailConfig(emailId));
	}

	@GetMapping(value = "/getOrganizationsEmailConfiguration/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getOrganizationsEmailConfiguration/{page}/{size}')")
	@ResponseBody
	public String getOrganizationsEmailConfiguration(@PathVariable int page, @PathVariable int size) {
		PageRequest pageRequest = PageRequest.of(page, size);
		Page<MEmail> emails = commonService.getOrganizationsEmailConfiguration(pageRequest);
		logger.debug("Called CommonController.getAllClients");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(emails);
	}

	@GetMapping(value = "/getEmailonfigurationByOrganization", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getEmailonfigurationByOrganization')")
	@ResponseBody
	public String getEmailonfigurationByOrganization() {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.getEmailonfigurationByOrganization());
	}

	@PostMapping(value = "/createAndUpdateClient", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createAndUpdateClient')")
	public String createAndUpdateClient(@RequestBody MOrgModel model) {

		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.createAndUpdateClient(model));
	}

	@PostMapping(value = "/createOrUpdateOrganisation", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createOrUpdateOrganisation')")
	public String createOrUpdateOrg(@RequestBody MOrgModel model) {

		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(orgService.createOrUpdateOrganisations(model));
	}

	@GetMapping(value = "/getAllOrganizations/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getAllOrganizations/{page}/{size}')")
	public String getAllOrganizations(@PathVariable int page, @PathVariable int size) {

		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(orgService.getAllOrganisations(page, size));
	}

	@PostMapping(value = "/selectClientAndOrganisation", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','selectClientAndOrganisation')")
	public void auditLogedInUserClientOrganisation(@RequestBody MUserClientAuditModel model) {
		commonService.auditLogedInUserClientOrganisation(model);

	}

	@DeleteMapping(value = "/deleteOrganisationById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','deleteOrganisationById/{id}')")
	public void deleteOrganisationById(@PathVariable long id) {
		orgService.deleteOrganisationById(id);

	}

	@GetMapping(value = "/getOrganisationByClientId/{AD_Client_ID}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getOrganisationByClientId/{AD_Client_ID}')")
	@ResponseBody
	public String getOrganisationByClientId(@PathVariable long AD_Client_ID) {
		List<MOrg> list = orgService.getOrganisationByClientId(AD_Client_ID);
		logger.debug("Called CommonController.getOrganisationByClientId");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(list);
	}
	
	@GetMapping(value = "/getOrganisationByCurrentClient", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getOrganisationByCurrentClient')")
	@ResponseBody
	public String getOrganisationByCurrentClient(@QueryParam(value = "search") String search) {
		List<MOrg> list = orgService.getOrganisationByCurrentClient(search);
		logger.debug("Called CommonController.getOrganisationByCurrentClient");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(list);
	}

	@DeleteMapping(value = "/deleteClientById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','deleteClientById/{id}')")
	public void deleteClientById(@PathVariable long id) {
		commonService.deleteClientById(id);

	}

	@GetMapping(value = "/getClientById", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getClientById')")
	@ResponseBody
	public String getClientById() {
		MClient client = commonService.getClientById();
		logger.debug("Called CommonController.getClientById");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(client);
	}

	@GetMapping(value = "/getAllClients/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getAllClients/{page}/{size}')")
	@ResponseBody
	public String getAllClients(@PathVariable int page, @PathVariable int size) {
		PageRequest pageRequest = PageRequest.of(page, size);
		Page<MClient> client = commonService.getAllClients(pageRequest);
		logger.debug("Called CommonController.getAllClients");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(client);
	}

	

	@GetMapping(value = "/getAllClientsList", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getAllClientsList')")
	@ResponseBody
	public String getAllClientsList() {
		List<MClient> client = commonService.getAllClientsList();
		logger.debug("Called CommonController.getAllClientsList");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(client);
	}

	@PutMapping(value = "/deleteOrganisationSystemSettings/{AD_Sys_Config_ID}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','deleteOrganisationSystemSettings/{AD_Sys_Config_ID}')")
	@ResponseBody
	public String deleteById(@PathVariable Long AD_Sys_Config_ID) {

		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.deleteOrganisationSystemSettings(AD_Sys_Config_ID));
	}

	@GetMapping(value = "/getOrganisationSystemSettings", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getOrganisationSystemSettings')")
	@ResponseBody
	public String getOrganisationSystemSettings() {
		List<MADSysConfig> settings = commonService.getOrganizationSystemConfigurations();
		logger.debug("Called CommonController.getOrganisationSystemSettings");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(settings);
	}

	@GetMapping(value = "/getOrganisationSystemSettingsByCategory/{settingCategory}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getOrganisationSystemSettingsByCategory/{settingCategory}')")
	@ResponseBody
	public String getOrganisationSystemSettingsByCategory(
			@PathVariable(value = "settingCategory") String settingCategory) {
		MADSysConfig setting = null;
		SettingCategoriesEnum category = SettingCategoriesEnum.valueOf(settingCategory);
		if (category != null) {
			setting = commonService.getOrganizationSystemConfigurationsBySettingCategory(category);
		}

		logger.debug("Called CommonController.getOrganisationSystemSettingsByCategory");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(setting);
	}

	@PostMapping(value = "/createUpdateAttachments", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createUpdateAttachments')")
	public String createUpdateAttachments(@RequestBody SetupModel model) {
		logger.debug("Called CommonController.createUpdateAttachments");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.createUpdateAttachments(model));

	}

	@GetMapping(value = "/getAttachments/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getAttachments/{page}/{size}')")
	@ResponseBody
	public String getAttachments(@PathVariable int page, @PathVariable int size) {
		logger.debug("Called CommonController.getAttachments");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.getAttachements(page, size));
	}

	@DeleteMapping(value = "/deleteAttachment/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','deleteAttachment/{id}')")
	public String deleteAttachment(@PathVariable long id) {
		logger.debug("Called CommonController.deleteAttachment");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.deleteAttachment(id));
	}

	@DeleteMapping(value = "/deleteCountry/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','deleteCountry/{id}')")
	public String deleteCountry(@PathVariable long id) {
		logger.debug("Called CommonController.deleteCountry");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.deleteCountry(id));
	}

	@PostMapping(value = "/createUpdateCountry", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createUpdateCountry')")
	public String createUpdateCountry(@RequestBody SetupModel model) {
		logger.debug("Called CommonController.createUpdateCountry");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.createUpdateCountry(model));
	}

	@PostMapping(value = "/createUpdateCountySubCountyAndWard", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','createUpdateCountySubCountyAndWard')")
	public String createUpdateCountySubCountyAndWard(@RequestBody co.ke.tezza.loanapp.model.County model) {
		logger.debug("Called CommonController.createUpdateCountySubCountyAndWard");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.createUpdateCountySubCountyAndWard(model));
	}

	@GetMapping(value = "/getCountries/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getCountries/{page}/{size}')")
	public String getCountries(@PathVariable int page, @PathVariable int size) {
		Page<Country> response = commonService.getCountries(page, size);
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(response);
	}

	@GetMapping(value = "/getCounties/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getCounties/{page}/{size}')")
	public String getCounties(@PathVariable int page, @PathVariable int size) {
		Page<County> response = commonService.getCounties(page, size);
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(response);
	}

	@GetMapping(value = "/getSubCountiesByCountyId/{countyId}/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getSubCountiesByCountyId/{countyId}/{page}/{size}')")
	public String getSubCountiesByCountyId(@PathVariable long countyId, @PathVariable int page,
			@PathVariable int size) {
		Page<SubCounty> response = commonService.getSubCountiesByCountyId(countyId, page, size);
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(response);
	}

	@GetMapping(value = "/getWardsBySubCountyId/{subCountyId}/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','getWardsBySubCountyId/{subCountyId}/{page}/{size}')")
	public String getWardsBySubCountyId(@PathVariable long subCountyId, @PathVariable int page,
			@PathVariable int size) {
		Page<Ward> response = commonService.getWardsBySubCountyId(subCountyId, page, size);
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(response);
	}

	@PostMapping(value = "/uploadCountries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','uploadCountries')")
	public String uploadCountries(@RequestParam("file") MultipartFile file) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.createCountriesFromFile(file));
	}

	@PostMapping(value = "/uploadCountiesSubCountiesAndWardsFromFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','uploadCountiesSubCountiesAndWardsFromFile')")
	public String uploadCountiesSubCountiesAndWardsFromFile(@RequestParam("file") MultipartFile file) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(commonService.createCountiesSubCountiesAndWardsFromFile(file));
	}

	@GetMapping("/downloadCountryCsvTemplate")
	// @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','downloadCountryCsvTemplate')")

	public void downloadCountryCsvTemplate(HttpServletResponse response) throws IOException {
		response.setContentType("text/csv");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=country_template.csv");

		try (PrintWriter writer = response.getWriter()) {
			writer.println("code,name,description");
			writer.println("KE,Kenya,East African Country");
			writer.println("UG,Uganda,Another East African Country");
		}
	}

	@GetMapping("/downloadCountySubCountyAndWardCsvTemplate")
	// @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('CommonController','downloadCountySubCountyAndWardCsvTemplate')")

	public void downloadCountySubCountyAndWardCsvTemplate(HttpServletResponse response) throws IOException {
		response.setContentType("text/csv");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=county_template.csv");

		try (PrintWriter writer = response.getWriter()) {
			writer.println("countyName,subCountyName,ward");
			writer.println("Nairobi,Westlands,Kangemi");
			writer.println("Nairobi,Langata,Karen");
		}
	}

}
