package co.ke.tezza.loanapp.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.QueryParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MMenuRoleMapping;
import co.ke.tezza.loanapp.entity.MOrg;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.entity.MWFMail;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.model.AuthResponse;
import co.ke.tezza.loanapp.model.ForgotPassword;
import co.ke.tezza.loanapp.model.LoginRequest;
import co.ke.tezza.loanapp.model.PasswordReset;
import co.ke.tezza.loanapp.repository.MADSysConfigRepository;
import co.ke.tezza.loanapp.repository.MMenuRoleMappingRepository;
import co.ke.tezza.loanapp.repository.MOrgRepository;
import co.ke.tezza.loanapp.repository.MWFMailRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.response.RoleResponse;
import co.ke.tezza.loanapp.response.User;
import co.ke.tezza.loanapp.util.JwtUtil;
import co.ke.tezza.loanapp.util.Utils;
import javassist.NotFoundException;
import lombok.val;

@CrossOrigin
@RestController
@RequestMapping("/auth/")
public class AuthController {
	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private MOrgRepository mOrgRepository;

	@Autowired
	private MADSysConfigRepository sysConfigRepository;

	@Autowired
	private Utils utils;

	@Autowired
	private MMenuRoleMappingRepository mMenuRoleMappingRepository;

	@Autowired
	private MOrgRepository orgRepository;

	@PostMapping("authenticate")
	public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest authenticationRequest,
			HttpServletRequest request) throws Exception {
		String message = "Login Successful.";

		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					authenticationRequest.getUserName(), authenticationRequest.getPassword()));
		} catch (BadCredentialsException e) {
			throw new Exception("Incorrect username or password", e);
		}

		final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUserName());
		MUser mUser = userRepo.findTop1ByEmailAndIsActive(authenticationRequest.getUserName(), true);
		boolean hasAdminRole = false;
		boolean isMultiOrg = false;

		Set<RoleResponse> roleRes = new HashSet<>();
		Set<MRoles> roles = mUser.getRoles();
		List<MMenuRoleMapping> mapping = null;
		if (roles != null && roles.size() > 0) {
			for (MRoles role : roles) {
				mapping = mMenuRoleMappingRepository.findByIsActiveAndRoleAndAdOrgIDOrderByMenu_MenuOrderAsc(true, role,
						utils.getAD_Org_ID());
				roleRes.add(utils.mapRole(role));
				if (role.getName().contains("ADMIN")) {
					hasAdminRole = true;

				}
				// role.setMenusetMenuMappingMapping(mapping);

			}
		}

		List<MOrg> orgs = orgRepository.findByIsActiveAndAdClientId(true, utils.getAD_Client_ID());
		if (orgs.size() > 1) {
			isMultiOrg = true;
		}
		final String jwt = jwtUtil.generateToken(userDetails, mUser.getUserId(), mUser.getEmail(), roles);
		AuthResponse auth = new AuthResponse(jwt,
				mUser.getFullName() != null ? mUser.getFullName() : mUser.getFirstName() + " " + mUser.getLastName(),
				mUser.getUserId(), roleRes);
//		auth.setMenusAllowedAccess(mapping);
		auth.setHasAdminRole(hasAdminRole);
		auth.setMultiOrganisationClient(isMultiOrg);
		auth.setPhoneNumber(mUser.getPhoneNumber());
		auth.setFullName(mUser.getFirstName() + " " + mUser.getLastName());
		auth.setAdClientId(mUser.getAdClientId());
		auth.setAdOrgId(mUser.getAdOrgId());

		MOrg org = mOrgRepository.findById(mUser.getAdOrgId()).orElse(null);
		if (org != null) {
			MADSysConfig sysconfig = sysConfigRepository.findTopByIsActiveAndAdOrgIDAndSettingCategory(true,
					org.getId(), SettingCategoriesEnum.SECURITY_SETTINGS);

			if (sysconfig != null) {
				auth.setOtpAllowedOnLogin(sysconfig.isOTPAllowedOnLogin());

				if (sysconfig.isOTPAllowedOnLogin()) {
					int otp = utils.generatedOTP(mUser.getUserId());
					utils.sendOTPEmail(mUser, otp);
					message = "Login successful. Please enter the OTP sent to your email: <strong>" + mUser.getEmail()
							+ "</strong> or phone number: <strong>" + mUser.getPhoneNumber() + "</strong>"
							+ " to verify your login.";
					String smsMessage = String.format(
							"OTP CODE:\n Dear %s, Your login verification code is %s. Valid for 5 minutes.",
							mUser.getFirstName() + " " + mUser.getLastName(), otp);
					long orgId = mUser.getAdOrgId();
					long clientId = mUser.getAdClientId();
					String phone = mUser.getPhoneNumber();
					LocalDateTime now = LocalDateTime.now();
					utils.saveSmsLoanAmendmentAprrovalSms(phone, smsMessage, orgId, clientId, now);

				}
			}
		}
		mUser.setNoOftimesLoggedIn(mUser.getNoOftimesLoggedIn() + 1);
		userRepo.save(mUser);
		co.ke.tezza.loanapp.util.ResponseEntity<AuthResponse> response = new co.ke.tezza.loanapp.util.ResponseEntity<AuthResponse>();
		response = new co.ke.tezza.loanapp.util.ResponseEntity<AuthResponse>(message, 200, auth);

		return ResponseEntity.ok(response);

	}

	@PostMapping(value = "resendOTP")
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('AuthController','resendOTP')")
	@ResponseBody
	public ResponseEntity<?> resendOTP() {
		String message = "Failed To Resend the OTP. Please Try Again.";
		int code = 201;
		MUser mUser = userRepo.findById(utils.getAD_User_ID()).get();
		int otp = utils.generatedOTP(mUser.getUserId());

		utils.sendOTPEmail(mUser, otp);
		code = 200;
		message = "OTP has been successfully resent to your email at " + mUser.getEmail()
				+ " and registered phone number " + mUser.getPhoneNumber()
				+ ",\nPlease check either your email or phone number to try again. ";
		String smsMessage = String.format(
				"OTP CODE:\n Dear %s, Your login verification code is %s. Valid for 5 minutes.",
				mUser.getFirstName() + " " + mUser.getLastName(), otp);
		long orgId = mUser.getAdOrgId();
		long clientId = mUser.getAdClientId();
		String phone = mUser.getPhoneNumber();
		LocalDateTime now = LocalDateTime.now();
		utils.saveSmsLoanAmendmentAprrovalSms(phone, smsMessage, orgId, clientId, now);

		AuthResponse auth = new AuthResponse();
		auth.setFullName(mUser.getFullName());
		auth.setUser_id(mUser.getUserId());
		// auth.setRoles(mUser.getRoles());

		co.ke.tezza.loanapp.util.ResponseEntity<AuthResponse> response = new co.ke.tezza.loanapp.util.ResponseEntity<AuthResponse>(
				message, code, auth);

		return ResponseEntity.ok(response);

	}

	@PostMapping(value = "sendVerificationCode")
	@ResponseBody
	public ResponseEntity<?> sendVerificationCode(@RequestBody LoginRequest request) {
		try {
			if (request == null || request.getUserName() == null || request.getUserName().trim().isEmpty()) {
				return ResponseEntity.badRequest()
						.body(new co.ke.tezza.loanapp.util.ResponseEntity<>("Email is required", 400, null));
			}

			MUser mUser = userRepo.findTop1ByEmailAndIsActive(request.getUserName().trim(), true);
			if (mUser == null) {

				return ResponseEntity.ok(new co.ke.tezza.loanapp.util.ResponseEntity<>(
						"If your email is registered, you will receive a password reset OTP.", 200, null));
			}

			// Generate 6-digit OTP
			int otp = utils.generatedSixDigitOTP(mUser.getUserId());
			if (otp < 100000 || otp > 999999) {
				return ResponseEntity.status(500).body(new co.ke.tezza.loanapp.util.ResponseEntity<>(
						"Failed to generate OTP. Please try again.", 500, null));
			}

			String otpStr = String.format("%06d", otp);

			utils.sendOTPEmail(mUser, otp);

			if (mUser.getPhoneNumber() != null && !mUser.getPhoneNumber().isEmpty()) {
				String smsMessage = String.format(
						"Password Reset OTP: %s\nDear %s, Use this OTP to reset your password. Valid for 5 minutes.",
						otpStr, mUser.getFirstName());

				utils.saveSmsLoanAmendmentAprrovalSms(mUser.getPhoneNumber(), smsMessage, mUser.getAdOrgId(),
						mUser.getAdClientId(), LocalDateTime.now());
			}

			String successMessage = "A password reset OTP has been sent to your registered email address"
					+ (mUser.getPhoneNumber() != null ? " and phone number" : "")
					+ ". Please check and enter the OTP to reset your password.";

			// Return minimal user info for password reset flow
			LoginRequest response = new LoginRequest();

			return ResponseEntity.ok(new co.ke.tezza.loanapp.util.ResponseEntity<>(successMessage, 200, response));

		} catch (Exception e) {
			return ResponseEntity.status(500).body(new co.ke.tezza.loanapp.util.ResponseEntity<>(
					"An error occurred while sending verification code. Please try again.", 500, null));
		}
	}

	@PostMapping(value = "verifyOTP")
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('AuthController','verifyOTP')")
	@ResponseBody
	public ResponseEntity<?> verifyOTP(@QueryParam(value = "otp") int otp) throws NotFoundException {
		MUser user = userRepo.findByOtpCode(otp);
		AuthResponse response = new AuthResponse();
		LoginRequest login = new LoginRequest();
		if (user != null) {

			response.setFullName(user.getFullName());
			// response.setRoles(user.getRoles());
			response.setUser_id(user.getUserId());
			login.setUserName(user.getEmail());
			MOrg org = mOrgRepository.findById(user.getAdOrgId()).orElse(null);
			if (org != null) {
				MADSysConfig sysconfig = sysConfigRepository.findTopByIsActiveAndAdOrgIDAndSettingCategory(true,
						org.getId(), SettingCategoriesEnum.SECURITY_SETTINGS);

				if (sysconfig != null) {
					response.setOtpAllowedOnLogin(sysconfig.isOTPAllowedOnLogin());
				}
			}
		}

		String message = "OTP Verification Failed. Please try again";
		int code = 201;

		if (user != null) {
			if (user.getOtpCode() == otp) {
				message = "OTP Verification Successfull.";
				code = 200;
			} else {
				message = "OTP Verification Failed. Please try again";
				code = 201;
				throw new NotFoundException("OTP Verification Failed. Please try again");

			}

		} else {
			response = null;
		}
		co.ke.tezza.loanapp.util.ResponseEntity<AuthResponse> body = new co.ke.tezza.loanapp.util.ResponseEntity<AuthResponse>(
				message, code, response);
		return ResponseEntity.ok(body);

	}
	
	@PostMapping(value = "verifyPasswordResetOTP")
	@ResponseBody
	public ResponseEntity<?> verifyPasswordResetOTP(@RequestBody PasswordReset request) throws NotFoundException {
		MUser user = userRepo.findTop1ByIsActiveAndOtpCode(true,request.getOtp());
		AuthResponse response = new AuthResponse();
		LoginRequest login = new LoginRequest();
		if (user != null) {

			response.setFullName(user.getFullName());
			// response.setRoles(user.getRoles());
			response.setUser_id(user.getUserId());
			login.setUserName(user.getEmail());
			MOrg org = mOrgRepository.findById(user.getAdOrgId()).orElse(null);
			if (org != null) {
				MADSysConfig sysconfig = sysConfigRepository.findTopByIsActiveAndAdOrgIDAndSettingCategory(true,
						org.getId(), SettingCategoriesEnum.SECURITY_SETTINGS);

				if (sysconfig != null) {
					response.setOtpAllowedOnLogin(sysconfig.isOTPAllowedOnLogin());
				}
			}
		}

		String message = "OTP Verification Failed. Please try again";
		int code = 201;

		if (user != null) {
			if (user.getOtpCode() == request.getOtp()||user.getOtpCode() == request.getOtpVerificationCode()) {
				message = "OTP Verification Successfull.";
				code = 200;
			} else {
				message = "OTP Verification Failed. Please try again";
				code = 201;
				throw new NotFoundException("OTP Verification Failed. Please try again");

			}

		} else {
			response = null;
		}
		co.ke.tezza.loanapp.util.ResponseEntity<AuthResponse> body = new co.ke.tezza.loanapp.util.ResponseEntity<AuthResponse>(
				message, code, response);
		return ResponseEntity.ok(body);

	}
	
	

	@PostMapping(value = "validateEmail")
	public ResponseEntity<?> validateEmail(@QueryParam(value = "email") String email) {
		MUser user = userRepo.findTop1ByEmailAndIsActive(email, true);
		co.ke.tezza.loanapp.util.ResponseEntity<User> body = null;
		String message = "";
		int code = 201;
		if (user != null) {
			code = 200;
			message = "Verification Successful. " + user.getFullName()
					+ " Your Account is Active. Please Proceed to login";
			user.setPassword(null);
			body = new co.ke.tezza.loanapp.util.ResponseEntity<User>(message, code, utils.mapUserBreif(user));
		} else {
			code = 201;
			message = "Verification Failed. Account with that email does not exists. Please Register first before you login";
			body = new co.ke.tezza.loanapp.util.ResponseEntity<User>(message, code, utils.mapUserBreif(user));

		}

		return ResponseEntity.ok(body);

	}

	@PostMapping(value = "forgotPassword")
	public ResponseEntity<?> forgotPassword(@RequestBody ForgotPassword passwordReset) {
		MUser user = userRepo.findTop1ByIsActiveAndOtpCode(true,passwordReset.getOtp());

		if (user != null) {
			user.setPassword(utils.encodePassword(passwordReset.getNewPassword()));
			userRepo.save(user);
			user.setPassword(null);

			String message = "Password Changed Successful. " + user.getFullName()
					+ " Your Account is Active. Please Proceed to login";

			return ResponseEntity.ok(new co.ke.tezza.loanapp.util.ResponseEntity<User>(message, 200, utils.mapUserBreif(user)));
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"You have entered an incorrect OTP Verification code.");
		}
	}

	@PostMapping(value = "resetPassword")
	public ResponseEntity<?> resetPassword(@RequestBody PasswordReset passwordReset) {
		MUser user = userRepo.findById(utils.getAD_User_ID()).orElse(null);
		co.ke.tezza.loanapp.util.ResponseEntity<User> body = null;
		String message = "";
		int code = 201;
		if (user != null) {
			if (user.getPassword().matches(utils.encodePassword(passwordReset.getOldPassword()))) {
				user.setPassword(passwordReset.getNewPassword());
				userRepo.save(user);
				code = 200;
				message = "Password Reset Successful. ";

				user.setPassword(null);
				body = new co.ke.tezza.loanapp.util.ResponseEntity<User>(message, code, utils.mapUserBreif(user));

			} else {
				code = 201;
				message = "Failed. The password provided did match any of your recently used password.";
				body = new co.ke.tezza.loanapp.util.ResponseEntity<User>(message, code, utils.mapUserBreif(user));

			}

		}

		return ResponseEntity.ok(body);

	}

}
