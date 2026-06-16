/**
 * 
 */
package co.ke.tezza.loanapp.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MBPartner;
import co.ke.tezza.loanapp.entity.MClient;
import co.ke.tezza.loanapp.entity.MOrg;
import co.ke.tezza.loanapp.entity.MReferralbenefits;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.CurrentLocation;
import co.ke.tezza.loanapp.model.UserDetails;
import co.ke.tezza.loanapp.repository.MBPartnerRepository;
import co.ke.tezza.loanapp.repository.MOrgRepository;
import co.ke.tezza.loanapp.repository.MReferralbenefitsRepository;
import co.ke.tezza.loanapp.repository.MclientRepository;
import co.ke.tezza.loanapp.repository.RoleRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.response.ReferralCode;
import co.ke.tezza.loanapp.response.User;
import co.ke.tezza.loanapp.response.UserResponse;
import co.ke.tezza.loanapp.util.ObjectsMapper;
import co.ke.tezza.loanapp.util.Utils;

/**
 * @author austine
 *
 */
@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepo;

	@Value("${domain.url}")
	private String domainUrl;

	@Autowired
	private MBPartnerRepository mBPartnerRepository;

	@Autowired
	private Utils utils;
	@Autowired
	private MclientRepository mclientRepository;
	@Autowired
	private MOrgRepository mOrgRepository;

	@Autowired
	private MReferralbenefitsRepository benefitRepository;

	@Autowired
	private ObjectsMapper objectsMapper;

	private final String googleApiKey = "AIzaSyAOnNBFkOwZE4xoTD_W69iHQxX-V8uR0-g";

	@Override
	public UserResponse createUser(UserDetails userModel) throws SetUpExceptions {
		// TODO Auto-generated method stubMbpa
		MADSysConfig config = utils.getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS);
		if (userModel.getCountryCode() == null) {
			userModel.setCountryCode("KE");
		}
		String phoneNo = utils.formatPhoneNumber(userModel.getPhoneNo(), userModel.getCountryCode());
		if (userModel.getCountry() != null) {
			if (userModel.getCountry().equals("The Netherlands") || !userModel.getCountryCode().equals("KE")) {
				throw new SetUpExceptions("This system is not allowed to operate in " + userModel.getCountry());
			}
		}

		if (userModel.getRecapcha() != null && !userModel.getRecapcha().isEmpty()) {
			userModel.setFullName(userModel.getFirstName() + " " + userModel.getLastName());
			MUser existingUser = userRepository.findTop1ByEmail(userModel.getEmail());
			MUser existingPhone = userRepository.findTop1ByPhoneNumber(phoneNo);
			MUser existingDevice = userRepository.findTop1ByIpAndIsActive(userModel.getIp(), false);

			MUser referrer = null;
			if (userModel.getReferralCode() != null && !userModel.getReferralCode().isBlank()
					&& !userModel.getReferralCode().isEmpty() && !userModel.getReferralCode().equals("string")) {
				referrer = userRepository.findTop1ByIsActiveAndReferralCode(true, userModel.getReferralCode());
				if (referrer == null) {
					// throw new SetUpExceptions("Invalid Referral code. Please use a valid referral
					// code");
				}

			}
			if (existingDevice != null) {
				throw new SetUpExceptions(
						"You are prohibited from using this system. Your device has been blocked due to mulpractices in the system. If the action was not comitted by you then please ask the Admin for help.");
			}

			if (existingUser != null) {
				throw new SetUpExceptions("User with Email " + existingUser.getEmail()
						+ " already exists, please use a different email address.");
			}
			if (existingPhone != null) {
				throw new SetUpExceptions("User with phone number " + existingPhone.getPhoneNumber()
						+ " already exists, please use a different phone number.");
			}
			if (phoneNo.equals("Invalid phone number")) {
				throw new SetUpExceptions("Invalid phone number");
			}
			MBPartner partner = new MBPartner();
			partner.setActive(true);
			partner.setC_BPartner_UU(UUID.randomUUID().toString());
			if (userModel.getFullName() != null) {
				partner.setName(userModel.getFullName());
			} else {
				partner.setName(userModel.getFirstName() + " " + userModel.getLastName());
			}

			partner.setValue(userModel.getEmail());
			mBPartnerRepository.save(partner);
			partner.setDocumentNo("CB/REG/" + Utils.getCurrentYear() + "/" + partner.getId());
			mBPartnerRepository.save(partner);
			MUser mUser = new MUser();

			if (referrer != null) {
				mUser.setReferredBy(referrer);
			}
			mUser.setAdOrgId(partner.getAD_Org_ID());
			mUser.setAdClientId(partner.getAD_Client_ID());
			mUser.setC_BPartner_ID(partner.getId());
			if (userModel.getFullName() != null && !userModel.getFullName().isBlank()
					&& !userModel.getFullName().isEmpty()) {
				mUser.setFullName(userModel.getFullName());
			} else {
				mUser.setFullName(userModel.getFirstName() + " " + userModel.getLastName());
			}
			mUser.setFirstName(userModel.getFirstName());
			mUser.setLastName(userModel.getLastName());
			mUser.setEmail(userModel.getEmail());
			if (userModel.getPassword() != null && !userModel.getPassword().isEmpty()) {
				String encodedPassword = passwordEncoder.encode(userModel.getPassword());
				mUser.setPassword(encodedPassword);
				mUser.setRand(userModel.getPassword());
			} else {
				String rand = Utils.generateRandomPassword();
				String encodedPassword = passwordEncoder.encode(rand);

				mUser.setPassword(encodedPassword);
				mUser.setRand(rand);

			}
			mUser.setApprovalStage("Approved");
			mUser.setApproved(true);
			mUser.setGender(userModel.getGender());
			mUser.setDocStatus("CO");
			mUser.setPhoneNumber(phoneNo);
			mUser.setDateOfBirth(userModel.getDateOfBirth());
			// 🌍 Location fields mapping
			mUser.setIp(userModel.getIp());
			mUser.setNetwork(userModel.getNetwork());
			mUser.setVersion(userModel.getVersion());
			mUser.setCity(userModel.getCity());
			mUser.setRegion(userModel.getRegion());
			mUser.setRegionCode(userModel.getRegionCode());
			mUser.setCountry(userModel.getCountry());
			mUser.setCountryName(userModel.getCountryName());
			mUser.setCountryCode(userModel.getCountryCode());
			mUser.setCountryCodeIso3(userModel.getCountryCodeIso3());
			mUser.setCountryCapital(userModel.getCountryCapital());
			mUser.setCountryTld(userModel.getCountryTld());
			mUser.setContinentCode(userModel.getContinentCode());
			mUser.setInEu(userModel.isInEu());
			mUser.setPostal(userModel.getPostal());
			mUser.setLatitude(userModel.getLatitude());
			mUser.setLongitude(userModel.getLongitude());
			mUser.setTimezone(userModel.getTimezone());
			mUser.setUtcOffset(userModel.getUtcOffset());
			mUser.setCountryCallingCode(userModel.getCountryCallingCode());
			mUser.setCurrency(userModel.getCurrency());
			mUser.setCurrencyName(userModel.getCurrencyName());
			mUser.setLanguages(userModel.getLanguages());
			mUser.setCountryArea(userModel.getCountryArea());
			mUser.setCountryPopulation(userModel.getCountryPopulation());
			mUser.setAsn(userModel.getAsn());
			mUser.setOrg(userModel.getOrg());
			if (userModel.getCurrentLat() != null && userModel.getCurrentLat() != 0
					&& userModel.getCurrentLong() != null && userModel.getCurrentLong() != 0) {
				CurrentLocation loc = getCurrentLocation(userModel.getCurrentLat(), userModel.getCurrentLong(),
						googleApiKey);
				mUser.setCurrentCountry(loc.getCurrentCountry());
				mUser.setCurrentCounty(loc.getCurrentCounty());
				mUser.setCurrentLat(userModel.getCurrentLat());
				mUser.setCurrentLng(userModel.getCurrentLong());
				mUser.setCurrentLocality(loc.getCurrentLocality());
				mUser.setCurrentNearestCity(loc.getCurrentNearestCity());
				mUser.setCurrentSubCounty(loc.getCurrentSubCounty());
				mUser.setCurrentLocationId(loc.getCurrentLocationId());
			}

			Set<MRoles> mRoles = new HashSet<>();
			if (userModel.getUserRoleIds().size() > 0) {
				for (int i = 0; i < userModel.getUserRoleIds().size(); i++) {
					MRoles role = roleRepo.findById(userModel.getUserRoleIds().get(i)).get();
					if (role != null) {
						mRoles.add(role);
					}
				}

			} else {
				String roleName = utils.getDefaultUserRole();
				MRoles role = roleRepo.findTop1ByNameAndIsActiveAndAdOrgIDOrderByCreatedAsc(roleName, true,
						utils.getAD_Org_ID());
				if (role != null) {
					mRoles.add(role);
				}

			}
			mUser.setRoles(mRoles);

			if (mUser.getAD_User_UU() == null) {
				mUser.setAD_User_UU(UUID.randomUUID().toString());
			}
			if (config != null) {
				if (mUser.getReferralCode() == null) {
					String referralCode = utils.generateReferralCode();
					String referralLink = config.getDomainUrl() + "?ref=" + referralCode;
					mUser.setReferralCode(referralCode);
					mUser.setReferralLink(referralLink);
				}

			}

			mUser.setFullName(userModel.getFullName());
			userRepository.save(mUser);

			if (config != null && config.isReferralBenefitIsOneTime()) {
				MReferralbenefits benefits = new MReferralbenefits();
				benefits.setActive(true);
				benefits.setClaimed(false);
				benefits.setUpline(referrer);
				benefits.setDownLine(mUser);
				benefits.setAmount(config.getReferralFlatBonusAmount());
				benefitRepository.save(benefits);

			}

			mUser.setDocumentNo("USER/REGNO/" + Utils.getCurrentYear() + "/" + mUser.getUserId());
			String systemUrl = utils.getSystemDomainUrl(mUser.getAdOrgId());
			String phone = mUser.getPhoneNumber();

			String firstName = mUser.getFirstName() != null ? mUser.getFirstName() : "";
			String lastName = mUser.getLastName() != null ? mUser.getLastName() : "";
			String systemName = "Smart System";

			String message = "Dear " + firstName + " " + lastName + ",\n\n"
					+ "Below are your Smart Debt Collection Application credentials:\n\n" + "Username: "
					+ mUser.getEmail() + "\n" + "Default Password: " + mUser.getRand() + "\n\n"
					+ "IMPORTANT: For security reasons, please change your password immediately.\n"
					+ "Click 'Forgot Password' on the login page, enter your email address, "
					+ "and set a new password.\n\n" + "System URL:\n" + systemUrl;

			utils.saveSmsLoanAmendmentAprrovalSms(phone, message, mUser.getAdOrgId(), mUser.getAdClientId(),
					LocalDateTime.now());

			String emailMessage = "Below are your Smart Debt Collection Application credentials:\n\n" + "Username: "
					+ mUser.getEmail() + "\n" + "Default Password: " + mUser.getRand() + "\n\n"
					+ "IMPORTANT: For security reasons, please change your password immediately.\n"
					+ "Click 'Forgot Password' on the login page, enter your email address, "
					+ "and set a new password.\n\n" + "System URL:\n";
			utils.sendEmail(mUser, emailMessage, "Login Credentials");

			return utils.mapUser(userRepository.save(mUser));

		} else {
			throw new SetUpExceptions("Please Verify Whether you are Human.");

		}

	}

	@Override
	public UserResponse updateUser(UserDetails userModel) throws SetUpExceptions {
		// TODO Auto-generated method stub
		MUser mUser = userRepository.findById(userModel.getId()).get();
		String phoneNo = utils.formatPhoneNumber(userModel.getPhoneNo(), userModel.getCountryCode());

		if (mUser == null) {
			throw new SetUpExceptions("MUser with Id " + userModel.getId() + " " + "Not found");
		}
		if (phoneNo.equals("Invalid phone number")) {
			throw new SetUpExceptions("Invalid phone number");
		}
		List<Long> userIds = new ArrayList<>();
		userIds.add(userModel.getId());
		MUser existingUser = userRepository.findTop1ByEmailAndUserIdNotIn(userModel.getEmail(), userIds);
		if (existingUser != null) {
			throw new SetUpExceptions("User with Email " + existingUser.getEmail()
					+ " already exists, please use a different email address or use your original email address");
		} else {
			mUser.setFullName(userModel.getFirstName() + " " + userModel.getLastName());
			mUser.setEmail(userModel.getEmail());
			if (userModel.getFullName() != null && !userModel.getFullName().isBlank()
					&& !userModel.getFullName().isEmpty()) {
				mUser.setFullName(userModel.getFullName());
			} else {
				mUser.setFullName(userModel.getFirstName() + " " + userModel.getLastName());
			}
			mUser.setFirstName(userModel.getFirstName());
			mUser.setLastName(userModel.getLastName());
			if (userModel.getPassword() != null) {
				String encodedPassword = passwordEncoder.encode(userModel.getPassword());
				mUser.setPassword(encodedPassword);

			}
			mUser.setPhoneNumber(utils.formatPhoneNumber(userModel.getPhoneNo(), mUser.getCountryCode()));
			mUser.setApprovalStage("Approved");
			mUser.setApproved(true);
			mUser.setGender(userModel.getGender());
			mUser.setDateOfBirth(userModel.getDateOfBirth());

			Set<MRoles> mRoles = new HashSet<>();
			if (userModel.getUserRoleIds().size() > 0) {
				for (int i = 0; i < userModel.getUserRoleIds().size(); i++) {
					MRoles role = roleRepo.findById(userModel.getUserRoleIds().get(i)).get();
					if (role != null) {
						mRoles.add(role);
					}
				}
				mUser.setRoles(mRoles);

			} else {
				String roleName = utils.getDefaultUserRole();
				MRoles role = roleRepo.findTop1ByNameAndIsActiveAndAdOrgIDOrderByCreatedAsc(roleName, true,
						utils.getAD_Org_ID());
				if (role != null) {
					mRoles.add(role);
					mUser.setRoles(mRoles);
				}

			}
			if (mUser.getAD_User_UU() == null) {
				mUser.setAD_User_UU(UUID.randomUUID().toString());
			}
			if (mUser.getReferralCode() == null) {
				MADSysConfig config = utils.getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS);

				String referralCode = utils.generateReferralCode();
				if (config != null && config.getDomainUrl() != null) {
					String referralLink = config.getDomainUrl() + "?ref=" + referralCode;
					mUser.setReferralLink(referralLink);
				}

				mUser.setReferralCode(referralCode);

			}
			mUser.setActive(userModel.isActive());

			return utils.mapUser(userRepository.save(mUser));

		}

	}

	public CurrentLocation getCurrentLocation(double lat, double lng, String apiKey) {
		try {
			String urlString = String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s",
					lat, lng, apiKey);

			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.connect();

			int responseCode = conn.getResponseCode();
			if (responseCode != 200) {
				System.out.println("Google API HTTP Response Code: " + responseCode);
				return null;
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder json = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				json.append(line);
			}
			reader.close();

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(json.toString());

			String status = root.path("status").asText();
			if (!"OK".equals(status)) {
				System.out.println("Google API returned status: " + status);
				return null;
			}

			JsonNode results = root.path("results");
			if (!results.isArray() || results.size() == 0) {
				System.out.println("No results found for coordinates.");
				return null;
			}

			CurrentLocation location = new CurrentLocation();
			location.setCurrentLat(lat);
			location.setCurrentLng(lng);

			// Iterate through results to find the most specific location
			for (JsonNode result : results) {
				JsonNode components = result.path("address_components");
				if (components.isArray()) {
					for (JsonNode comp : components) {
						JsonNode types = comp.path("types");
						if (types.isArray()) {
							for (JsonNode type : types) {
								String t = type.asText();
								switch (t) {
								case "country":
									location.setCurrentCountry(comp.path("long_name").asText());
									break;
								case "administrative_area_level_1":
									location.setCurrentCounty(comp.path("long_name").asText());
									break;
								case "administrative_area_level_2":
									location.setCurrentSubCounty(comp.path("long_name").asText());
									break;
								case "locality":
									location.setCurrentLocality(comp.path("long_name").asText());
									break;
								case "sublocality":
								case "sublocality_level_1":
								case "neighborhood":
								case "premise":
									// Pick first available sublocality/premise as "location"
									if (location.getCurrentLocality() == null
											|| location.getCurrentLocality().isEmpty()) {
										location.setCurrentLocality(comp.path("long_name").asText());
										location.setCurrentLocationId(result.path("place_id").asText());
									}
									break;
								}
							}
						}
					}
				}

				// Stop after we found a valid location + place_id
				if (location.getCurrentLocality() != null && !location.getCurrentLocality().isEmpty()) {
					break;
				}
			}

			return location;

		} catch (Exception e) {
			System.out.println("Error fetching location: " + e.getMessage());
			return null;
		}
	}

	@Override
	public UserResponse deleteById(Long user_id) throws SetUpExceptions {
		// TODO Auto-generated method stub
		try {
			MUser existEntity = userRepository.findById(user_id).orElse(null);

			if (existEntity == null) {
				throw new SetUpExceptions("MUser is not found");

			}
			existEntity.setActive(false);
			return utils.mapUser(userRepository.save(existEntity));
		} catch (ObjectNotFoundException e) {
			throw new SetUpExceptions("Could not find MUser by Id");
			// TODO: handle exception
		}
	}

	@Override
	public Page<UserResponse> findByIsActive(int page, int size) throws SetUpExceptions {
		// TODO Auto-generated method stub
		try {
			Page<MUser> users = userRepository.findByIsActiveOrderByUserIdAsc(true, PageRequest.of(page, size));
			if (users.hasContent()) {
				for (MUser user : users) {
					MClient client = mclientRepository.findById(user.getAdClientId()).orElse(null);
					MOrg org = mOrgRepository.findById(user.getAdOrgId()).orElse(null);
					if (org != null) {
						user.setOrganizationName(org.getName());
					}
					if (client != null) {
						user.setClientName(client.getName());

					}

				}
			}

			return users.map(utils::mapUser);
		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Could not fetch all users");
		}
	}

	@Override
	public UserResponse getUserByUserName(String username) throws SetUpExceptions {
		// TODO Auto-generated method stub
		try {
			return utils.mapUser(userRepository.findTop1ByEmailAndIsActive(username, true));

		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Error While trying to fetch user with username");
		}
	}

	@Override
	public Page<UserResponse> getUserByCreatedBy(int page, int size) throws SetUpExceptions {
		// TODO Auto-generated method stub
		try {
			Page<MUser> users = userRepository.findByCreatedByOrderByUserIdAsc(utils.getAD_User_ID(),
					PageRequest.of(page, size));
			if (users.hasContent()) {
				for (MUser user : users) {
					MClient client = mclientRepository.findById(user.getAdClientId()).orElse(null);
					MOrg org = mOrgRepository.findById(user.getAdOrgId()).orElse(null);
					if (org != null) {
						user.setOrganizationName(org.getName());
					}
					if (client != null) {
						user.setClientName(client.getName());

					}

				}
			}
			return users.map(utils::mapUser);

		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Error While trying to fetch users by CreatedBy");
		}
	}

	@Override
	public Page<UserResponse> getUserByOrganizationPage(int page, int size, String search) {
		// TODO Auto-generated method stub

		try {
			Page<MUser> users = null;
			if (search != null && !search.isEmpty()) {
				users = userRepository.searchUser(utils.getAD_Org_ID(), search, PageRequest.of(page, size));

			} else {
				users = userRepository.findByAdOrgIdOrderByNoOftimesLoggedInDesc(utils.getAD_Org_ID(),
						PageRequest.of(page, size));
			}

			if (users.hasContent()) {
				for (MUser user : users) {
					user.setClientName(mclientRepository.findById(user.getAdClientId()).get().getName());
					user.setOrganizationName(mOrgRepository.findById(user.getAdOrgId()).get().getName());
				}
			}
			return users.map(utils::mapUser);

		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Error While trying to fetch users by Organization" + e);
		}
	}

	@Override
	public ReferralCode getReferralCode() {
		MADSysConfig config = utils.getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS);

		String referralLink = "";
		if (config != null && config.getDomainUrl() != null) {
			referralLink = config.getDomainUrl();
		}

		MUser user = userRepository.findById(utils.getAD_User_ID())
				.orElseThrow(() -> new RuntimeException("User not found"));

		referralLink = referralLink + "?ref=" + user.getReferralCode();

		if (!Objects.equals(referralLink, user.getReferralLink())) {
			return new ReferralCode(referralLink);
		}

		return new ReferralCode(user.getReferralLink());
	}

	@Override
	public UserResponse getUserById(long user_id) throws SetUpExceptions {
		// TODO Auto-generated method stub
		try {
			Optional<MUser> mUser = userRepository.findById(user_id);
			if (mUser.isPresent()) {
				return utils.mapUser(mUser.get());
			} else {
				throw new SetUpExceptions("Failed to fetch user with ID");
			}
		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Failed to fetch user with ID");
		}
	}

	@Override
	public Page<User> getMyReferrals(int page, int size) {
		// TODO Auto-generated method stub
		try {
			Page<MUser> users = userRepository.findByIsActiveAndAdOrgIdAndReferredByOrderByUserIdDesc(true,
					utils.getAD_Org_ID(), utils.getLoggedInUser(), PageRequest.of(page, size));
			if (users.hasContent()) {
				for (MUser user : users) {
					user.setClientName(mclientRepository.findById(user.getAdClientId()).get().getName());
					user.setOrganizationName(mOrgRepository.findById(user.getAdOrgId()).get().getName());
				}
			}
			return users.map(utils::mapUserBreif);

		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Error While trying to fetch users by Organization" + e);
		}
	}

	@Override
	public UserResponse getMyProfile() {
		// TODO Auto-generated method stub
		return utils.mapUser(utils.getLoggedInUser());
	}

	@Override
	public List<UserResponse> getUsersByRole(Long roleId, String searchTerm) throws SetUpExceptions {
		// TODO Auto-generated method stub
		List<MUser> users = new ArrayList<>();
		MRoles role = roleRepo.findById(roleId).orElse(null);
		List<Long> adOrgIds = new ArrayList<>();
		if (role != null) {
			if (!role.getAllowedOrganisations().isEmpty()) {
				for (MOrg org : role.getAllowedOrganisations()) {
					adOrgIds.add(org.getId());
				}
			} else {
				adOrgIds.add(utils.getAD_Org_ID());
			}
		} else {
			throw new SetUpExceptions("The role selected is invalid.");
		}
		if (searchTerm == null || searchTerm.isEmpty()) {
			users = userRepository.getUserByRole(role.getId(), adOrgIds);
		} else {
			users = userRepository.getUserByRoleAndSearch(role.getId(), adOrgIds, searchTerm);
		}
		return users.stream().map(utils::mapUser).collect(Collectors.toList());
	}

}
