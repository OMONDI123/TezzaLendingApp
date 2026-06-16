/**
 * 
 */
package co.ke.tezza.loanapp.controller;

import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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

import co.ke.tezza.loanapp.model.UserDetails;
import co.ke.tezza.loanapp.response.User;
import co.ke.tezza.loanapp.response.UserResponse;
import co.ke.tezza.loanapp.service.UserService;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;
import co.ke.tezza.loanapp.util.ResponseEntity;

/**
 * @author austine
 *
 */
@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private UserService service;

	@GetMapping(value = "/getReferralCode", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('UserController','getReferralCode')")
	@ResponseBody
	public String getReferralCode() {

		logger.debug("Called UserController.getReferralCode");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(service.getReferralCode());
	}
	
	
	@GetMapping(value = "/getMyProfile", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('UserController','getMyProfile')")
	@ResponseBody
	public String getMyProfile() {

		logger.debug("Called UserController.getMyProfile");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(service.getMyProfile());
	}
	
	
	@GetMapping(value = "/getUsersByRole/{roleId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('UserController','getUsersByRole/{roleId}')")
	@ResponseBody
	public String getUsersByRole(@PathVariable("roleId") Long roleId, @QueryParam("searchTerm") String searchTerm) {

		logger.debug("Called UserController.getUsersByRole");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(service.getUsersByRole(roleId, searchTerm));
	}

	@PostMapping(value = "/createUser", produces = MediaType.APPLICATION_JSON_VALUE)
	// @PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('UserController','createUser')")
	@ResponseBody
	public String createUser(@RequestBody UserDetails user) {
		String message = "";
		UserResponse existingUser = service.getUserByUserName(user.getEmail());
		if (existingUser != null) {
			message = "User with the same Username or email already exists. Please use a different email or username";
			throw new DuplicateKeyException(message);
		}

		UserResponse createdUser = service.createUser(user);
		logger.debug("Called UserController.createUser");
		if (createdUser.getCreatedBy() <= 0) {
			if (user.getPassword() == null || user.getPassword().isEmpty()) {
				message = "Dear " + createdUser.getLastName()
						+ " your Registration is Successful, the login password has been sent to your registered email and phone number. Please use your email and password to login.";

			} else {
				message = "Dear " + createdUser.getLastName()
						+ " your Registration is Successful, Please proceed to login using the email and password provided";

			}
		} else {
			UserResponse createdBy = service.getUserById(createdUser.getCreatedBy());
			if (createdBy != null) {
				message = "Dear " + createdBy.getFullName() + " you have Successfully Added "
						+ createdUser.getFullName() + " into the system";
			}
		}

		ResponseEntity<?> response = new ResponseEntity<UserResponse>(message, 200, createdUser);
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(response);
	}

	@PostMapping(value = "/updateUser", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('UserController','updateUser')")
	public String updateUser(@RequestBody UserDetails user) {
		UserResponse updatedUser = service.updateUser(user);
		logger.debug("Called UserController.updateUser");
		String message = "User Profile Updated Successfully.";
		ResponseEntity<?> response = new ResponseEntity<UserResponse>(message, 200, updatedUser);
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(response);
	}

	@PutMapping(value = "/deActivateUserByID/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('UserController','deActivateUserByID/{id}')")
	@ResponseBody
	public String deleteById(@PathVariable Long id) {
		UserResponse deletedUser = service.deleteById(id);
		logger.debug("Called UserController.deleteById");
		String message = "UserResponse Deleted Successfully.";
		ResponseEntity<?> response = new ResponseEntity<UserResponse>(message, 200, deletedUser);
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(response);
	}

	@GetMapping(value = "/getActiveUser/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('UserController','getActiveUser/{page}/{size}')")
	@ResponseBody
	public String getActiveUser(@PathVariable int page, @PathVariable int size) {
		Page<UserResponse> UserResponse = service.findByIsActive(page, size);
		logger.debug("Called UserController.getActiveUser");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(UserResponse);
	}
	
	@GetMapping(value = "/getMyReferrals/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('UserController','getMyReferrals/{page}/{size}')")
	@ResponseBody
	public String getMyReferrals(@PathVariable int page, @PathVariable int size) {
		Page<User> UserResponse = service.getMyReferrals(page, size);
		logger.debug("Called UserController.getActiveUser");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(UserResponse);
	}

	@GetMapping(value = "/getUserByOrganizationPage/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('UserController','getUserByOrganizationPage/{page}/{size}')")
	@ResponseBody
	public String getUserByOrganizationPage(@PathVariable int page, @PathVariable int size,@QueryParam("search") String search) {
		Page<UserResponse> user = service.getUserByOrganizationPage(page, size,search);
		logger.debug("Called UserController.getUserByOrganizationPage");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(user);
	}

}
