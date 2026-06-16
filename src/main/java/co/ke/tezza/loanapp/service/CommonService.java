package co.ke.tezza.loanapp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.JsonElement;

import co.ke.tezza.loanapp.entity.LetterHeaads;
import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MAttachment;
import co.ke.tezza.loanapp.entity.MClient;
import co.ke.tezza.loanapp.entity.MCountry;
import co.ke.tezza.loanapp.entity.MCounty;
import co.ke.tezza.loanapp.entity.MEmail;
import co.ke.tezza.loanapp.entity.MPaymentGatewayConfig;
import co.ke.tezza.loanapp.entity.MPaymentMethod;
import co.ke.tezza.loanapp.entity.MRemindersConfiguration;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MSMSConfig;
import co.ke.tezza.loanapp.entity.MSmsSetup;
import co.ke.tezza.loanapp.entity.MSubCounty;
import co.ke.tezza.loanapp.entity.MUserClientAudit;
import co.ke.tezza.loanapp.entity.MWard;
import co.ke.tezza.loanapp.enums.ReminderFrequency;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.EmailConfigRequest;
import co.ke.tezza.loanapp.model.FileUpload;
import co.ke.tezza.loanapp.model.FileUploads;
import co.ke.tezza.loanapp.model.MOrgModel;
import co.ke.tezza.loanapp.model.MUserClientAuditModel;
import co.ke.tezza.loanapp.model.PaymentGateWayConfigRequest;
import co.ke.tezza.loanapp.model.PaymentMethodModel;
import co.ke.tezza.loanapp.model.RemindersConfigRequest;
import co.ke.tezza.loanapp.model.SetupModel;
import co.ke.tezza.loanapp.model.SmsConfigRequest;
import co.ke.tezza.loanapp.model.SmsSetupRequest;
import co.ke.tezza.loanapp.model.SystemSettingsConfig;
import co.ke.tezza.loanapp.repository.AttachmentRepository;
import co.ke.tezza.loanapp.repository.CountryRepository;
import co.ke.tezza.loanapp.repository.CountyRepository;
import co.ke.tezza.loanapp.repository.LetterHeadRepository;
import co.ke.tezza.loanapp.repository.MADSysConfigRepository;
import co.ke.tezza.loanapp.repository.MEmailRepository;
import co.ke.tezza.loanapp.repository.MUserClientAuditRepository;
import co.ke.tezza.loanapp.repository.MclientRepository;
import co.ke.tezza.loanapp.repository.PaymentGatewayConfigRepository;
import co.ke.tezza.loanapp.repository.PaymentMethodRepository;
import co.ke.tezza.loanapp.repository.ReminderConfigRepository;
import co.ke.tezza.loanapp.repository.RoleRepository;
import co.ke.tezza.loanapp.repository.SmsConfigRepository;
import co.ke.tezza.loanapp.repository.SmsSetupRepository;
import co.ke.tezza.loanapp.repository.SubCountyRepository;
import co.ke.tezza.loanapp.repository.WardRepository;
import co.ke.tezza.loanapp.response.AttachmentResponse;
import co.ke.tezza.loanapp.response.Country;
import co.ke.tezza.loanapp.response.County;
import co.ke.tezza.loanapp.response.PaymentGateWayConfigResponse;
import co.ke.tezza.loanapp.response.RemindersConfigResponse;
import co.ke.tezza.loanapp.response.SmsConfigResponse;
import co.ke.tezza.loanapp.response.SmsSetupResponse;
import co.ke.tezza.loanapp.response.SubCounty;
import co.ke.tezza.loanapp.response.Ward;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

@Service
public class CommonService {

	@Autowired
	private MADSysConfigRepository sysConfigRepository;

	@Autowired
	private Utils utils;

	@Autowired
	private MclientRepository mclientRepository;

	@Autowired
	private MUserClientAuditRepository auditRepository;

	@Autowired
	private AttachmentRepository attachmentRepository;

	@Autowired
	CountryRepository countryRepository;
	@Autowired
	private CountyRepository countyRepository;
	@Autowired
	private SubCountyRepository subCountyRepository;
	@Autowired
	private WardRepository wardRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private MEmailRepository emailRepository;

	@Autowired
	private SmsConfigRepository smsConfigRepository;

	@Autowired
	private SmsSetupRepository smsSetupRepository;

	@Autowired
	private ReminderConfigRepository remindersRepository;

	@Autowired
	private ObjectsMapper objectsMapper;
	@Autowired
	private PaymentGatewayConfigRepository paymentGatewayConfigRepository;

	@Autowired
	private MpesaService mpesaService;

	@Autowired
	private LetterHeadRepository letterHeadRepository;

	@Autowired
	private PaymentMethodRepository paymentMethodRepository;

	public ResponseEntity<MPaymentMethod> createUpdatePaymentMethod(@Valid PaymentMethodModel model) {
		MPaymentMethod paymentMethod = paymentMethodRepository.findById(model.getPaymentMethodId()).orElse(null);
		MPaymentMethod paymentMethodExists = paymentMethodRepository.findTop1ByIsActiveAndAdOrgIDAndName(true,
				utils.getAD_Org_ID(), model.getName());
		if ((paymentMethodExists != null && model.getPaymentMethodId() == 0)
				|| (paymentMethodExists != null && model.getPaymentMethodId() > 0
						&& model.getPaymentMethodId() != paymentMethod.getPaymentMethodId())) {
			throw new SetUpExceptions("Payment Method " + paymentMethodExists.getName() + " already exists.");

		}

		if (paymentMethod == null) {
			paymentMethod = new MPaymentMethod();
		}
		paymentMethod.setName(model.getName());
		paymentMethod.setDescription(model.getDescription());
		paymentMethod.setPaymentType(model.getPaymentType());
		paymentMethodRepository.save(paymentMethod);
		return new ResponseEntity<MPaymentMethod>("Payment Method Added Successfully.", 200, paymentMethod);
	}

	public Page<MPaymentMethod> getPaymentMethodsPaginated(int page, int size) {
		return paymentMethodRepository.findByIsActiveAndAdOrgID(true, utils.getAD_Org_ID(), PageRequest.of(page, size));

	}

	public List<MPaymentMethod> getPaymentMethodList() {
		return paymentMethodRepository.findByIsActiveAndAdOrgID(true, utils.getAD_Org_ID());

	}

	public ResponseEntity<LetterHeaads> uploadLetterHead(@Valid FileUpload model) {
		try {
			LetterHeaads letterHead = letterHeadRepository.findTop1ByIsActiveAndAdOrgIDOrderByIdDesc(true,
					utils.getAD_Org_ID());
			if (letterHead == null) {
				letterHead = new LetterHeaads();
			}
			if (model.getBase64File() != null) {
				FileUploads file = utils.uploadFileFromBase64(model.getBase64File());
				letterHead.setActive(true);
				letterHead.setFilePath(file.getFullFilePath());
				letterHeadRepository.save(letterHead);
				return new ResponseEntity<LetterHeaads>("Letter Head Uploaded Successfully.", 200, letterHead);
			} else {
				throw new RuntimeException("File to be uploaded cannot be empty.");
			}

		} catch (Exception e) {
			// TODO: handle exception
			throw new RuntimeException("Failed to upload the file, " + e);

		}

	}

	public ResponseEntity<LetterHeaads> deleteLetterHead(@Valid long id) {
		try {
			LetterHeaads letterHead = letterHeadRepository.findById(id).get();

			letterHead.setActive(false);
			letterHeadRepository.save(letterHead);
			return new ResponseEntity<LetterHeaads>("Letter Head Deleted Successfully.", 200, letterHead);

		} catch (Exception e) {
			// TODO: handle exception
			throw new RuntimeException(e.getMessage());
		}

	}

	public List<LetterHeaads> getLetterHeaadsByOrganisation() {
		List<LetterHeaads> list = letterHeadRepository.findByIsActiveAndAdOrgID(true, utils.getAD_Org_ID());
		if (!list.isEmpty()) {
			for (LetterHeaads l : list) {
				l.setOrganizationName(utils.getOrganizationName(l.getAdOrgID()));
				l.setClientName(utils.getClientName(l.getAdClientId()));
			}
		}
		return list;
	}

	public ResponseEntity<MADSysConfig> createOrUpdateSystemSettings(@Valid SystemSettingsConfig m)
			throws SetUpExceptions {
		String message;
		int code = 403;
		MADSysConfig systemConfig = sysConfigRepository.findById(m.getId()).orElse(null);

		try {
			if (systemConfig != null) {
				mapFields(systemConfig, m);
				systemConfig = sysConfigRepository.save(systemConfig);
				systemConfig.setDocumentNo("ADSYS/CONFIG/ORG" + systemConfig.getAdOrgID() + "/" + Utils.getCurrentYear()
						+ "/" + systemConfig.getId());
				systemConfig = sysConfigRepository.save(systemConfig);
				message = "System Configuration Updated Successfully.";
				code = 200;
			} else {
				// Create new if not duplicate
				systemConfig = sysConfigRepository.findTopByIsActiveAndAdOrgIDAndSettingCategory(true,
						utils.getAD_Org_ID(), m.getSettingCategory());
				if (systemConfig != null) {
					message = "Error: A system configuration already exists for this organization and category.";
					code = 403;
					throw new SetUpExceptions(message);
				} else {
					systemConfig = new MADSysConfig();
					mapFields(systemConfig, m);
					systemConfig.setAD_Sys_Config_UU(UUID.randomUUID().toString());

					systemConfig = sysConfigRepository.save(systemConfig);
					systemConfig.setDocumentNo("ADSYS/CONFIG/ORG" + systemConfig.getAdOrgID() + "/"
							+ Utils.getCurrentYear() + "/" + systemConfig.getId());
					systemConfig = sysConfigRepository.save(systemConfig);

					message = "System Configuration Created Successfully.";
					code = 200;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			message = "Unexpected error occurred while saving configuration.";
			code = 500;
		}

		return new ResponseEntity<MADSysConfig>(message, code, systemConfig);
	}

	private void mapFields(MADSysConfig config, SystemSettingsConfig m) {
		config.setId(m.getId());

		config.setOTPAllowedOnLogin(m.isOTPAllowedOnLogin());
		config.setSearchEnabled(m.isSearchEnabled());
		config.setSettingCategory(m.getSettingCategory());
		config.setSendMessageToclientOnRejection(m.isSendMessageToclientOnRejection());
		config.setReferralBenefitsEnabled(m.isReferralBenefitsEnabled());
		config.setReferralBenefitIsOneTime(m.isReferralBenefitIsOneTime());
		config.setReferralFlatBonusAmount(m.getReferralFlatBonusAmount());
		config.setReferralRewardType(m.getReferralRewardType());
		config.setReferralFlatBonusInterestRate(m.getReferralFlatBonusInterestRate());

		config.setPlatformName(m.getPlatformName());
		config.setPlatformPrimaryColor(m.getPlatformPrimaryColor());

		config.setDomainUrl(m.getDomainUrl());
		config.setFilePaths(m.getFilePaths());
		config.setDocumentUploadDir(m.getDocumentUploadDir());
		config.setAllowSystemNotifications(m.isAllowSystemNotifications());
		config.setEmail(m.getEmail());
		config.setPhone(utils.formatPhoneNumber(m.getPhone(), "KE"));
		config.setApproveAutoGeneratedBills(m.isApproveAutoGeneratedBills());
		config.setDownloadPath(m.getDownloadPath());
	}
	public ResponseEntity<MEmail> createUpdateSystemOrganisationEmail(@Valid EmailConfigRequest request) {
		String message = "Failed to configure Email for this organization";
		int code = 500;
		MEmail systemEmail = emailRepository.findTop1ByIsActiveAndAdOrgID(true, utils.getAD_Org_ID());
		if (systemEmail != null) {
			if (request.getEmailId() == 0) {
				throw new SetUpExceptions(
						"The email has already been configured for this organization. Please update the current existing configurations.");
			} else {
				mapEmailFields(systemEmail, request);
				emailRepository.save(systemEmail);
				if (systemEmail.getDocumentNo() == null) {
					systemEmail.setDocumentNo("EM/EML/" + Utils.getCurrentYear() + "/" + systemEmail.getEmailId());

				}
				emailRepository.save(systemEmail);
				message = "Organization Email Configurations Updated Successfully.";
				code = 200;
			}
		} else {
			systemEmail = new MEmail();
			mapEmailFields(systemEmail, request);
			emailRepository.save(systemEmail);
			if (systemEmail.getDocumentNo() == null) {
				systemEmail.setDocumentNo("EM/EML/" + Utils.getCurrentYear() + "/" + systemEmail.getEmailId());

			}
			emailRepository.save(systemEmail);
			message = "Organization Email Configurations Created Successfully.";
			code = 200;
		}
		return new ResponseEntity<MEmail>(message, code, systemEmail);

	}

	public void mapEmailFields(MEmail e, EmailConfigRequest r) {

		e.setActive(true);
		e.setUsername(r.getUsername());
		e.setHost(r.getHost());
		e.setPassword(r.getPassword());
		e.setSmtpAuth(r.isSmtpAuth());
		e.setSslEnabled(r.isSslEnabled());
		e.setStarttlsEnabled(r.isStarttlsEnabled());
		e.setPort(r.getPort());
		e.setEmailId(r.getEmailId());
		e.setName(r.getOrganisationName());
	}

	public ResponseEntity<MADSysConfig> deleteOrganisationSystemSettings(@Valid long AD_Sys_Config_ID)
			throws SetUpExceptions {
		String message = "";
		int code = 403;
		MADSysConfig sysConfig = sysConfigRepository.findById(AD_Sys_Config_ID).orElse(null);
		if (sysConfig != null) {
			sysConfig.setActive(false);
			sysConfigRepository.save(sysConfig);
			code = 200;
			message = "Organisation's System Configurations deleted Successfully.";
		}
		return new ResponseEntity<MADSysConfig>(message, code, sysConfig);
	}

	public List<MADSysConfig> getOrganizationSystemConfigurations() {

		return sysConfigRepository.findByIsActiveAndAdOrgID(true, utils.getAD_Org_ID());
	}

	public MADSysConfig getOrganizationSystemConfigurationsBySettingCategory(SettingCategoriesEnum settingCategory) {
		return sysConfigRepository.findTopByIsActiveAndAdOrgIDAndSettingCategory(true, utils.getAD_Org_ID(),
				settingCategory);
	}

	public ResponseEntity<MClient> createAndUpdateClient(MOrgModel m) {
		// TODO Auto-generated method stub
		try {
			MClient client = mclientRepository.findById(m.getId()).orElse(null);
			String message = "";
			if (client == null) {
				MClient exists = mclientRepository.findTop1ByName(m.getName());
				if (exists != null) {
					message = "Client Already exists, please create a different client.";
					throw new SetUpExceptions(message);
				}
				client = new MClient();

			}

			client.setActive(true);
			client.setName(m.getName());
			client.setDescription(m.getDescription());
			client.setValue(m.getValue());
			client.setAD_Client_UU(UUID.randomUUID().toString());
			mclientRepository.save(client);
			client.setDocumentNo("ADC/" + Utils.getCurrentYear() + "/" + client.getId());
			mclientRepository.save(client);
			message = "Client Created Successfully.";

			return new ResponseEntity<>(message, 200, client);

		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Could Not create or update Client.");
		}
	}

	public MClient getClientById() {
		return mclientRepository.findById(utils.getAD_Client_ID()).orElse(null);
	}

	public Page<MClient> getAllClients(Pageable pageable) {
		return mclientRepository.findAll(pageable);
	}

	public Page<MEmail> getOrganizationsEmailConfiguration(Pageable pageable) {
		return emailRepository.findByIsActive(true, pageable);
	}

	public MEmail getEmailonfigurationByOrganization() {
		return emailRepository.findTop1ByIsActiveAndAdOrgID(true, utils.getAD_Org_ID());
	}

	public void deleteClientById(long id) {
		mclientRepository.deleteById(id);
	}

	public void auditLogedInUserClientOrganisation(MUserClientAuditModel userModel) {

//		if (!utils.isSuperUser()) {
//			if (utils.getLoggedInUser().getAdClientId() != userModel.getAD_Client_ID()) {
//				throw new SetUpExceptions("Cross tenant PO is not allowed. You have no access to this client.");
//			}
//		}
		MUserClientAudit audit = null;
		if (utils.getLoggedInUser() != null) {
			audit = auditRepository.findByUser(utils.getLoggedInUser());

		}
		if (audit == null) {
			audit = new MUserClientAudit();
		}
		audit.setAD_Client_ID(userModel.getAD_Client_ID());
		audit.setAD_Org_ID(userModel.getAD_Org_ID());
		audit.setUser(utils.getLoggedInUser());
		Set<MRoles> roles = new HashSet<>();
		if (userModel.getRoleId() > 0) {
			MRoles role = roleRepository.findById(userModel.getRoleId()).get();
			roles.add(role);
			audit.setRoles(roles);
		}

		auditRepository.save(audit);

	}

	public List<MClient> getAllClientsList() {
		// TODO Auto-generated method stub
		return mclientRepository.findAll();
	}

	public ResponseEntity<AttachmentResponse> createUpdateAttachments(@Valid SetupModel model) {
		String message = "Failed. Could not Create Attachment";
		int code = 201;
		MAttachment attachment = attachmentRepository.findById(model.getId()).orElse(null);
		if (attachment == null) {
			attachment = new MAttachment();
		}
		attachment.setName(model.getName());
		attachment.setDescription(model.getDescription());
		attachment.setAD_Attachment_UU(UUID.randomUUID().toString());
		attachment.setAttachmentType(model.getAttachmentType());
		attachmentRepository.save(attachment);
		attachment.setDocumentNo("ADM/ATT/" + Utils.getCurrentYear() + "/" + attachment.getAdClientId());

		attachmentRepository.save(attachment);
		AttachmentResponse response = null;
		if (attachment != null) {
			response = mapAttachmentToResponse(attachment);
			message = "Attachment Saved Successfully.";
			code = 200;
		}
		return new ResponseEntity<AttachmentResponse>(message, code, response);
	}

	public AttachmentResponse mapAttachmentToResponse(MAttachment attachment) {
		AttachmentResponse response = new AttachmentResponse();
		response.setAD_Attachment_UU(attachment.getAD_Attachment_UU());
		response.setAttachmentId(attachment.getAttachmentId());
		response.setCreated(attachment.getCreated());
		response.setUpdated(attachment.getUpdated());
		response.setName(attachment.getName());
		response.setDescription(attachment.getDescription());
		response.setDocumentNo(attachment.getDocumentNo());
		response.setAttachmentType(attachment.getAttachmentType());
		response.setActive(attachment.isActive());
		return response;
	}

	public ResponseEntity<Country> createUpdateCountry(@Valid SetupModel model) {
		String message = "Failed. Could not Create Country";
		int code = 201;
		Country response = null;
		MCountry country = countryRepository.findById(model.getId()).orElse(null);
		if (country == null) {
			MCountry existingCountry = countryRepository.findTop1ByIsActiveAndName(true, model.getName());
			if (existingCountry != null) {
				throw new SetUpExceptions(
						"There is an already exisiting country with the same name as " + existingCountry.getName());

			} else {
				country = new MCountry();
				country.setName(model.getName());

				country.setDescription(model.getDescription());
				country.setCode(model.getCode());
				country.setAD_Sub_Country_UU(UUID.randomUUID().toString());
				countryRepository.save(country);
				country.setDocumentNo("AD/COTRY/" + Utils.getCurrentYear() + "/" + country.getCountryId());

				countryRepository.save(country);
				code = 200;
				message = "Country created Sucessfully.";

			}

		} else {

			country.setName(model.getName());

			country.setDescription(model.getDescription());
			country.setCode(model.getCode());
			country.setAD_Sub_Country_UU(UUID.randomUUID().toString());
			countryRepository.save(country);
			country.setDocumentNo("AD/COTRY/" + Utils.getCurrentYear() + "/" + country.getAdClientId());

			countryRepository.save(country);

			if (country != null) {
				response = mappCountry(country);
				message = "Country Updated Successfully.";
				code = 200;
			}

		}

		return new ResponseEntity<>(message, code, response);
	}

	public ResponseEntity<Country> deleteCountry(@Valid Long id) {
		String message = "Failed. Could not Delete Country";
		int code = 201;
		Country response = null;
		MCountry country = countryRepository.findById(id).get();
		country.setActive(false);

		countryRepository.save(country);

		if (country != null) {
			response = mappCountry(country);
			message = "Country Deleted Successfully.";
			code = 200;
		}

		return new ResponseEntity<>(message, code, response);

	}

	public ResponseEntity<List<Country>> createCountriesFromFile(MultipartFile file) {
		String message = "Failed to upload countries";
		int code = 201;
		List<Country> savedCountries = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
			String line;
			boolean isFirst = true;

			while ((line = reader.readLine()) != null) {
				if (isFirst) {
					isFirst = false; // skip header
					continue;
				}

				String[] parts = line.split(",");
				if (parts.length < 1)
					continue;

				String countryCode = parts[0].trim();
				String name = parts[1].trim();
				String description = parts.length > 2 ? parts[2].trim() : "";

				if (name.isEmpty())
					continue;

				MCountry existing = countryRepository.findTop1ByIsActiveAndName(true, name);
				if (existing != null)
					continue;

				MCountry country = new MCountry();
				country.setName(name);
				country.setCode(countryCode);
				country.setDescription(description);
				country.setAD_Sub_Country_UU(UUID.randomUUID().toString());
				country.setActive(true);
				country = countryRepository.save(country);
				country.setDocumentNo("AD/CTRY/" + Utils.getCurrentYear() + "/" + country.getCountryId());
				countryRepository.save(country);

				savedCountries.add(mappCountry(country));
			}

			message = savedCountries.size() + " Countries uploaded successfully.";
			code = 200;

		} catch (IOException e) {
			e.printStackTrace();
			message = "Error processing file: " + e.getMessage();
		}

		return new ResponseEntity<>(message, code, savedCountries);
	}

	public Page<Country> getCountries(int page, int size) {
		Page<MCountry> countries = countryRepository.findByIsActiveOrderByNameAsc(true, PageRequest.of(page, size));

		return countries.map(this::mappCountry);
	}

	public ResponseEntity<Country> deleteCountry(@Valid long id) {
		String message = "Failed. Could not Delete Country";
		int code = 201;
		MCountry attachment = countryRepository.findById(id).get();
		attachment.setActive(false);
		countryRepository.save(attachment);

		Country response = null;
		if (attachment != null) {
			response = mappCountry(attachment);
			message = "Country Deleted Successfully.";
			code = 200;
		}
		return new ResponseEntity<>(message, code, response);
	}

	public Page<County> getCounties(int page, int size) {
		Page<MCounty> counties = countyRepository.findByIsActiveOrderByNameAsc(true, PageRequest.of(page, size));

		return counties.map(this::mappCounty);
	}

	public Page<SubCounty> getSubCountiesByCountyId(long countyId, int page, int size) {
		MCounty county = countyRepository.findById(countyId).get();
		Page<MSubCounty> subCounties = subCountyRepository.findByIsActiveAndCountyOrderByNameAsc(true, county,
				PageRequest.of(page, size));

		return subCounties.map(this::mappSubCounty);
	}

	public Page<Ward> getWardsBySubCountyId(long subCountyId, int page, int size) {
		MSubCounty subCounty = subCountyRepository.findById(subCountyId).get();
		Page<MWard> wards = wardRepository.findByIsActiveAndSubCountyOrderByNameAsc(true, subCounty,
				PageRequest.of(page, size));

		return wards.map(this::mappWard);
	}

	public ResponseEntity<County> createUpdateCountySubCountyAndWard(co.ke.tezza.loanapp.model.County model) {
		String message = "Failed To save the records. please try again later";
		int code = 200;
		MCounty county = countyRepository.findById(model.getId()).orElse(null);
		if (county == null) {
			MCounty exisitingCounty = countyRepository.findTop1ByIsActiveAndName(true, model.getName());
			if (exisitingCounty != null) {
				throw new SetUpExceptions(
						"There is an already exisiting county with the same name as " + exisitingCounty.getName());
			} else {
				county = new MCounty();
			}

		}
		MCountry country = countryRepository.findTop1ByIsActiveAndNameContainingIgnoreCase(true, "Kenya");
		if (country == null) {
			throw new SetUpExceptions("Country Kenya does not exists. Please create it first to proceed.");
		}
		county.setActive(true);
		county.setCountry(country);
		county.setName(model.getName());
		county.setDescription(model.getName());
		county.setAD_County_UU(UUID.randomUUID().toString());
		countyRepository.save(county);
		county.setDocumentNo("CC/ADCTY/" + Utils.getCurrentYear() + "/" + county.getCountyId());
		countyRepository.save(county);
		// Saving Sub counties

		if (model.getSubCounties().size() > 0) {
			for (co.ke.tezza.loanapp.model.SubCounty subCounty : model.getSubCounties()) {
				MSubCounty sub = subCountyRepository.findById(subCounty.getId()).orElse(null);

				if (sub == null) {
					MSubCounty exisitingSubCounty = subCountyRepository.findTop1ByIsActiveAndCountyAndName(true, county,
							subCounty.getName());
					if (exisitingSubCounty != null) {
						throw new SetUpExceptions("There is an already exisiting sub county with the same name as "
								+ exisitingSubCounty.getName() + " created for this County.");
					} else {
						sub = new MSubCounty();
					}

				}
				sub.setActive(true);
				sub.setName(subCounty.getName());
				sub.setDescription(subCounty.getName());
				sub.setAD_Sub_County_UU(UUID.randomUUID().toString());
				sub.setCounty(county);
				subCountyRepository.save(sub);
				sub.setDocumentNo("SBC/ADCTY/" + Utils.getCurrentYear() + "/" + sub.getSubContyId());
				subCountyRepository.save(sub);

				//// saving wards

				if (subCounty.getWards().size() > 0) {
					for (co.ke.tezza.loanapp.model.Ward ward : subCounty.getWards()) {
						MWard wd = wardRepository.findById(ward.getId()).orElse(null);

						if (wd == null) {
							MWard exisitingWard = wardRepository.findTop1ByIsActiveAndSubCountyAndName(true, sub,
									ward.getName());
							if (exisitingWard != null) {
								throw new SetUpExceptions("There is an already exisiting Ward with the same name as "
										+ exisitingWard.getName() + " created for this Sub County.");
							} else {
								wd = new MWard();
							}

						}
						wd.setActive(true);
						wd.setName(ward.getName());
						wd.setDescription(ward.getName());
						wd.setAD_Ward_UU(UUID.randomUUID().toString());
						wd.setSubCounty(sub);
						wardRepository.save(wd);
						wd.setDocumentNo("WRD/AD/" + Utils.getCurrentYear() + "/" + wd.getWardId());
						wardRepository.save(wd);

					}
				}

			}
		}
		message = "County updated successfully.";
		code = 200;
		County response = mappCounty(county);

		return new ResponseEntity<County>(message, code, response);

	}

	public ResponseEntity<List<County>> createCountiesSubCountiesAndWardsFromFile(MultipartFile file) {
		List<County> createdCounties = new ArrayList<>();
		String message = "Failed to upload data";
		int code = 201;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
			String line;
			boolean isFirst = true;

			while ((line = reader.readLine()) != null) {
				if (isFirst) {
					isFirst = false; // skip header
					continue;
				}

				String[] parts = line.split(",");
				if (parts.length < 3)
					continue;

				String countyName = parts[0].trim();
				String subCountyName = parts[1].trim();
				String wardName = parts[2].trim();

				if (countyName.isEmpty() || subCountyName.isEmpty() || wardName.isEmpty())
					continue;

				// Step 1: Save County
				MCountry country = countryRepository.findTop1ByIsActiveAndNameContainingIgnoreCase(true, "Kenya");
				if (country == null) {
					throw new SetUpExceptions("Country Kenya does not exists. Please create it first to proceed.");
				}
				MCounty county = countyRepository.findTop1ByIsActiveAndName(true, countyName);
				if (county == null) {
					county = new MCounty();
					county.setCountry(country);
					county.setName(countyName);
					county.setDescription(countyName);
					county.setAD_County_UU(UUID.randomUUID().toString());
					county.setActive(true);
					county = countyRepository.save(county);
					county.setDocumentNo("CC/ADCTY/" + Utils.getCurrentYear() + "/" + county.getCountyId());
					county = countyRepository.save(county);
				}

				// Step 2: Save Sub County
				MSubCounty subCounty = subCountyRepository.findTop1ByIsActiveAndCountyAndName(true, county,
						subCountyName);
				if (subCounty == null) {
					subCounty = new MSubCounty();
					subCounty.setName(subCountyName);
					subCounty.setDescription(subCountyName);
					subCounty.setAD_Sub_County_UU(UUID.randomUUID().toString());
					subCounty.setCounty(county);
					subCounty.setActive(true);
					subCounty = subCountyRepository.save(subCounty);
					subCounty.setDocumentNo("SBC/ADCTY/" + Utils.getCurrentYear() + "/" + subCounty.getSubContyId());
					subCounty = subCountyRepository.save(subCounty);
				}

				// Step 3: Save Ward
				MWard ward = wardRepository.findTop1ByIsActiveAndSubCountyAndName(true, subCounty, wardName);
				if (ward == null) {
					ward = new MWard();
					ward.setName(wardName);
					ward.setDescription(wardName);
					ward.setAD_Ward_UU(UUID.randomUUID().toString());
					ward.setSubCounty(subCounty);
					ward.setActive(true);
					ward = wardRepository.save(ward);
					ward.setDocumentNo("WRD/AD/" + Utils.getCurrentYear() + "/" + ward.getWardId());
					wardRepository.save(ward);
				}

				// Optional: add to response list
				createdCounties.add(mappCounty(county));
			}

			message = createdCounties.size()
					+ " County(s) and their/it's Sub counties with their associated Wards uploaded successfully.";
			code = 200;

		} catch (IOException e) {
			e.printStackTrace();
			message = "Error reading file: " + e.getMessage();
		}

		return new ResponseEntity<>(message, code, createdCounties);
	}

	public Page<AttachmentResponse> getAttachements(int page, int size) {
		Page<MAttachment> attachments = attachmentRepository.findByIsActiveAndAdOrgID(true, utils.getAD_Org_ID(),
				PageRequest.of(page, size));

		return attachments.map(this::mapAttachmentToResponse);
	}

	public ResponseEntity<AttachmentResponse> deleteAttachment(@Valid long id) {
		String message = "Failed. Could not Delete Attachment";
		int code = 201;
		MAttachment attachment = attachmentRepository.findById(id).get();
		attachment.setActive(false);
		attachmentRepository.save(attachment);

		AttachmentResponse response = null;
		if (attachment != null) {
			response = mapAttachmentToResponse(attachment);
			message = "Attachment Deleted Successfully.";
			code = 200;
		}
		return new ResponseEntity<AttachmentResponse>(message, code, response);
	}

	public Country mappCountry(MCountry c) {
		Country response = new Country();
		response.setActive(c.isActive());
		response.setCountryId(c.getCountryId());
		response.setCreated(c.getCreated());
		response.setDocStatus(c.getDocStatus());
		response.setDocumentNo(c.getDocumentNo());
		response.setName(c.getName());
		response.setCode(c.getCode());
		response.setDescription(c.getDescription());
		response.setUpdated(c.getUpdated());

		return response;

	}

	public County mappCounty(MCounty c) {
		County response = new County();
		response.setActive(c.isActive());
		response.setCountyId(c.getCountyId());
		response.setCreated(c.getCreated());
		response.setDocStatus(c.getDocStatus());
		response.setDocumentNo(c.getDocumentNo());
		response.setName(c.getName());
		response.setUpdated(c.getUpdated());
		response.setCountry(mappCountry(c.getCountry()));

		return response;

	}

	public SubCounty mappSubCounty(MSubCounty c) {
		SubCounty response = new SubCounty();
		response.setActive(c.isActive());
		response.setSubCountyId(c.getSubContyId());
		response.setCreated(c.getCreated());
		response.setDocStatus(c.getDocStatus());
		response.setDocumentNo(c.getDocumentNo());
		response.setName(c.getName());
		response.setUpdated(c.getUpdated());
		response.setCounty(mappCounty(c.getCounty()));

		return response;

	}

	public Ward mappWard(MWard c) {
		Ward response = new Ward();
		response.setActive(c.isActive());
		response.setWardId(c.getWardId());
		response.setCreated(c.getCreated());
		response.setDocStatus(c.getDocStatus());
		response.setDocumentNo(c.getDocumentNo());
		response.setName(c.getName());
		response.setUpdated(c.getUpdated());
		response.setSubCounty(mappSubCounty(c.getSubCounty()));

		return response;

	}

	public MEmail deleteEmailConfig(long emailId) {
		// TODO Auto-generated method stub
		MEmail email = emailRepository.findById(emailId).get();
		email.setActive(false);
		return emailRepository.save(email);
	}

	public SmsConfigResponse mapSmsConfig(MSMSConfig c) {
		if (c == null) {
			return null;
		}
		SmsConfigResponse response = new SmsConfigResponse();
		response.setApiKey(c.getApiKey());
		response.setCallBackUrl(c.getCallBackUrl());
		response.setPartnerId(c.getPartnerId());
		response.setSenderId(c.getSenderId());
		response.setSmsBaseUrl(c.getSmsBaseUrl());
		response.setSmsConfigId(c.getSmsConfigId());
		response.setActive(c.isActive());
		response.setUsername(c.getUsername());
		response.setApiSecret(c.getApiSecret());
		response.setSmsProvider(c.getSmsProvider());

		return response;

	}

	public ResponseEntity<SmsConfigResponse> createUpdateSmsConfig(SmsConfigRequest c) {
		MSMSConfig config = smsConfigRepository.findById(c.getSmsConfigId()).orElse(null);
		String message = "";
		int code = 200;
		if (config == null) {
			config = new MSMSConfig();
		}
		config.setApiKey(c.getApiKey());
		config.setCallBackUrl(c.getCallBackUrl());
		config.setPartnerId(c.getPartnerId());
		config.setSenderId(c.getSenderId());
		config.setSmsBaseUrl(c.getSmsBaseUrl());
		config.setUsername(c.getUsername());
		config.setApiSecret(c.getApiSecret());
		config.setSmsProvider(c.getSmsProvider());
		smsConfigRepository.save(config);
		if (c.getSmsConfigId() > 0) {
			message = "SMS Configuration has been successfully updated.";

		} else {
			message = "SMS Configurations has been created successfully.";
		}
		return new ResponseEntity<SmsConfigResponse>(message, code, mapSmsConfig(config));

	}

	public ResponseEntity<SmsConfigResponse> deleteSmsConfig(Long id) {
		MSMSConfig config = smsConfigRepository.findById(id).get();
		String message = "";
		int code = 200;

		config.setActive(false);
		smsConfigRepository.save(config);

		message = "SMS Configurations has been deleted successfully.";

		return new ResponseEntity<SmsConfigResponse>(message, code, mapSmsConfig(config));

	}

	public SmsSetupResponse mapSmsSetup(MSmsSetup c) {
		if (c == null) {
			return null;
		}
		SmsSetupResponse response = new SmsSetupResponse();
		response.setActive(c.isActive());
		response.setDebt(c.isDebt());
		response.setMessageTemplate(c.getMessageTemplate());
		response.setSmsSetupId(c.getSmsSetupId());
		response.setSmsType(c.getSmsType());
		return response;

	}

	private RemindersConfigResponse mapReminderConfig(MRemindersConfiguration config) {
		if (config == null) {
			return null;
		}
		RemindersConfigResponse response = new RemindersConfigResponse();
		response.setReminderId(config.getReminderId());
		response.setReminderFrequency(config.getReminderFrequency());
		response.setMaxReminders(config.getMaxReminders());
		response.setStartNoOfDaysBefore(config.getStartNoOfDaysBefore());
		response.setStartNoOfDaysAfter(config.getStartNoOfDaysAfter());

		if (config.getSmsMessageTemplate() != null) {
			response.setSmsTemplate(mapSmsSetup(config.getSmsMessageTemplate()));
		}

		response.setSendTime(config.getSendTime());
		response.setSendTimeEnabled(config.getSendTimeEnabled());
		response.setTimezone(config.getTimezone());
		response.setSendTimes(config.getSendTimes());
		response.setUseMultipleTimes(config.getUseMultipleTimes());
		response.setSpecificDays(config.getSpecificDays());
		response.setActive(config.isActive());

		return response;
	}

	public ResponseEntity<RemindersConfigResponse> createUpdateReminderConfig(RemindersConfigRequest c) {
		MRemindersConfiguration response = remindersRepository.findById(c.getReminderId()).orElse(null);
		MSmsSetup temp = smsSetupRepository.findById(c.getSmsMessageTemplateId()).orElseThrow(
				() -> new RuntimeException("SMS template not found with ID: " + c.getSmsMessageTemplateId()));
		MRemindersConfiguration existingOne = remindersRepository
				.findTop1ByIsActiveAndAdOrgIDAndSmsMessageTemplate(true, utils.getAD_Org_ID(), temp);
		if (existingOne != null && c.getReminderId() == 0) {
			throw new SetUpExceptions(
					"This Sms Template has already been configured. please choose a different template");
		}

		if (existingOne != null && existingOne.getReminderId() != c.getReminderId() && c.getReminderId() > 0) {
			throw new SetUpExceptions(
					"This Sms Template is used by another configuration. please choose a different template");
		}
		if (c.getReminderFrequency().equals(ReminderFrequency.SPECIFIC_DAYS)
				&& (c.getSpecificDays().size() == 0 || c.getSpecificDays().isEmpty())) {
			throw new SetUpExceptions("Please Select the Specific Days to send this type of reminder.");
		}
		String message = "";
		int code = 200;

		if (response == null) {
			response = new MRemindersConfiguration();
			// Set default values for new records
			response.setSendTimeEnabled(c.getSendTimeEnabled() != null ? c.getSendTimeEnabled() : true);
			response.setTimezone(c.getTimezone() != null ? c.getTimezone() : "Africa/Nairobi");
			response.setUseMultipleTimes(c.getUseMultipleTimes() != null ? c.getUseMultipleTimes() : false);
			response.setActive(c.getActive() != null ? c.getActive() : true);
			response.setAdOrgID(utils.getAD_Org_ID());
			response.setCreatedBy(utils.getAD_User_ID());
		} else {
			// For updates, only set time config if provided
			if (c.getSendTimeEnabled() != null) {
				response.setSendTimeEnabled(c.getSendTimeEnabled());
			}
			if (c.getTimezone() != null) {
				response.setTimezone(c.getTimezone());
			}
			if (c.getUseMultipleTimes() != null) {
				response.setUseMultipleTimes(c.getUseMultipleTimes());
			}
			if (c.getActive() != null) {
				response.setActive(c.getActive());
			}
			response.setUpdatedBy(utils.getAD_User_ID());
		}

		response.setMaxReminders(c.getMaxReminders());
		response.setReminderFrequency(c.getReminderFrequency());
		response.setStartNoOfDaysAfter(c.getStartNoOfDaysAfter());
		response.setStartNoOfDaysBefore(c.getStartNoOfDaysBefore());
		response.setSmsMessageTemplate(temp);
		response.setSpecificDays(c.getSpecificDays());

		if (Boolean.TRUE.equals(c.getSendTimeEnabled())) {
			if (Boolean.TRUE.equals(c.getUseMultipleTimes())) {
				if (c.getSendTimes() != null && !c.getSendTimes().isEmpty()) {
					for (String time : c.getSendTimes()) {
						if (!isValidTimeFormat(time)) {
							throw new IllegalArgumentException("Invalid time format: " + time + ". Use HH:mm format.");
						}
					}
					response.setSendTimes(c.getSendTimes());
					response.setSendTime(null);
				}
			} else {
				if (c.getSendTime() != null && !c.getSendTime().trim().isEmpty()) {
					if (!isValidTimeFormat(c.getSendTime())) {
						throw new IllegalArgumentException(
								"Invalid time format: " + c.getSendTime() + ". Use HH:mm format.");
					}
					response.setSendTime(c.getSendTime());
					response.setSendTimes(null);
				}
			}
		} else {
			response.setSendTime(null);
			response.setSendTimes(null);
		}

		remindersRepository.save(response);

		if (c.getReminderId() > 0) {
			message = "Reminder Configuration has been updated successfully.";
		} else {
			message = "Reminder Configuration has been created successfully.";
		}

		return new ResponseEntity<RemindersConfigResponse>(message, code, mapReminderConfig(response));
	}

	private boolean isValidTimeFormat(String time) {
		if (time == null || time.trim().isEmpty()) {
			return false;
		}
		return time.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
	}

	public ResponseEntity<RemindersConfigResponse> deleteReminderConfig(Long id) {
		MRemindersConfiguration config = remindersRepository.findById(id).get();
		String message = "";
		int code = 200;

		config.setActive(false);
		config = remindersRepository.save(config); // Assign the saved entity

		// Explicitly refresh the entity
		remindersRepository.flush(); // Force the save to database

		// Refresh the entity from database
		config = remindersRepository.findById(id).orElse(config);

		message = "Reminder Configuration has been deleted successfully.";

		return new ResponseEntity<RemindersConfigResponse>(message, code, mapReminderConfig(config));
	}

	public List<RemindersConfigResponse> getAllReminderConfigs() {
		List<MRemindersConfiguration> entities = remindersRepository.findByIsActiveAndAdOrgIDOrderByReminderIdDesc(true,
				utils.getAD_Org_ID());

		return entities.stream().map(this::mapReminderConfig).collect(Collectors.toList());
	}

	public ResponseEntity<SmsSetupResponse> createUpdateSmsSetup(SmsSetupRequest c) {
		MSmsSetup response = smsSetupRepository.findById(c.getSmsSetupId()).orElse(null);
		String message = "";
		int code = 200;
		if (response == null) {
			response = new MSmsSetup();
		}

		response.setDebt(c.isDebt());
		response.setMessageTemplate(c.getMessageTemplate());
		response.setSmsSetupId(c.getSmsSetupId());
		response.setSmsType(c.getSmsType());
		smsSetupRepository.save(response);
		if (c.getSmsSetupId() > 0) {
			message = "SMS Template has been Updated Successfully..";

		} else {
			message = "SMS Template has been created successfully.";
		}
		return new ResponseEntity<SmsSetupResponse>(message, code, mapSmsSetup(response));

	}

	public ResponseEntity<SmsSetupResponse> deleteSmsSetup(Long id) {
		MSmsSetup config = smsSetupRepository.findById(id).get();
		String message = "";
		int code = 200;

		config.setActive(false);
		smsSetupRepository.save(config);

		message = "SMS Template has been deleted successfully.";

		return new ResponseEntity<SmsSetupResponse>(message, code, mapSmsSetup(config));

	}

	public List<SmsSetupResponse> getAllSmsTemplates() {
		List<MSmsSetup> entities = smsSetupRepository.findByIsActiveAndAdOrgIDOrderBySmsSetupIdDesc(true,
				utils.getAD_Org_ID());

		return entities.stream().map(this::mapSmsSetup).collect(Collectors.toList());
	}

	public List<SmsConfigResponse> getAllSmsConfigurations() {
		List<MSMSConfig> entities = smsConfigRepository.findByIsActiveAndAdOrgIDOrderBySmsConfigId(true,
				utils.getAD_Org_ID());

		return entities.stream().map(this::mapSmsConfig).collect(Collectors.toList());
	}

	public List<SmsTypeEnum> getSmsTypeEnum() {
		return Arrays.asList(SmsTypeEnum.values());
	}

	public List<String> getSupportedPlaceHolders(SmsTypeEnum smsType) {
		if (smsType == null) {
			return Collections.emptyList();
		}

		return Arrays.stream(smsType.getAllowedParameters()).map(param -> "{" + param + "}")
				.collect(Collectors.toList());
	}

	public ResponseEntity<PaymentGateWayConfigResponse> createUpdatePaymentGatewayConfig(
			PaymentGateWayConfigRequest c) {
		MPaymentGatewayConfig response = paymentGatewayConfigRepository.findById(c.getPaymentGatewayConfigId())
				.orElse(null);
		String message = "";
		int code = 200;
		if (response == null) {
			response = new MPaymentGatewayConfig();
		}

		response.setBusinessShortCode(c.getBusinessShortCode());
		response.setCallBackUrl(c.getCallBackUrl());
		response.setMpesaApiKey(c.getMpesaApiKey());
		response.setMpesaApiSecrete(c.getMpesaApiSecret());
		response.setMpesaOrganizationShortCode(c.getMpesaOrganizationShortCode());
		response.setMpesaProductionAllowed(c.isMpesaProductionAllowed());
		response.setMpesaProductionBaseUrl(c.getMpesaProductionBaseUrl());
		response.setMpesaTestBaseUrl(c.getMpesaTestBaseUrl());
		response.setPartyB(c.getPartyB());
		response.setPassKey(c.getPassKey());
		response.setTransactionType(c.getTransactionType());
		response.setStkCallBackUrl(c.getStkCallBackUrl());
		response.setValidationUrl(c.getValidationUrl());

		paymentGatewayConfigRepository.save(response);

		try {
			mpesaService.registerURL(c.getBusinessShortCode(), "Cancelled", c.getCallBackUrl(), c.getValidationUrl(),
					c.isMpesaProductionAllowed(), c.getMpesaProductionBaseUrl(), c.getMpesaTestBaseUrl(),
					response.getAdOrgID());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (c.getPaymentGatewayConfigId() > 0) {
			message = "Payment Gateway Configuration has been Updated Successfully..";
		} else {
			message = "Payment Gateway Configuration has been created successfully.";
		}

		return new ResponseEntity<PaymentGateWayConfigResponse>(message, code,
				objectsMapper.mapPaymentGateWayConfig(response));
	}

	public ResponseEntity<PaymentGateWayConfigResponse> deletePaymentGatewayConfig(Long id) {
		MPaymentGatewayConfig config = paymentGatewayConfigRepository.findById(id).get();
		String message = "";
		int code = 200;

		config.setActive(false);
		paymentGatewayConfigRepository.save(config);

		message = "Payment Gateway Configuration has been deleted successfully.";

		return new ResponseEntity<PaymentGateWayConfigResponse>(message, code,
				objectsMapper.mapPaymentGateWayConfig(config));

	}

	public List<PaymentGateWayConfigResponse> getAllPaymentGatewayConfigs() {
		List<MPaymentGatewayConfig> entities = paymentGatewayConfigRepository
				.findByIsActiveAndAdOrgIDOrderByCreatedDesc(true, utils.getAD_Org_ID());

		return entities.stream().map(objectsMapper::mapPaymentGateWayConfig).collect(Collectors.toList());
	}

}
