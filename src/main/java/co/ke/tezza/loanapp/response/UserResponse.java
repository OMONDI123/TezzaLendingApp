package co.ke.tezza.loanapp.response;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserResponse {
	private Long userId;
	private String documentNo;
	private String firstName;
	private String externalRefrenceNo;
	private String email;
	private String phoneNumber;
	private String lastName;
	private String dateOfBirth;
	private boolean isActive;
	private long createdBy;
	private long updatedBy;
	private Date updated;
	private Date created;
	private String fullName;
	private Set<RoleResponse> roles = new HashSet<>();
	private String clientName;
	private String organizationName;
	private String gender;
	private String countryName;
	private String countryCode;
	private String timeZone;
	private String city;
	private String language;
	private String currency;
	private String countryCapital;
	private int noOftimesLoggedIn;
	private String referredBy;
	

}
