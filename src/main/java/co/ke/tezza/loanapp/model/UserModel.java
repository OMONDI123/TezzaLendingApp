package co.ke.tezza.loanapp.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import co.ke.tezza.loanapp.entity.MUser;
import lombok.Data;

@Data
public class UserModel implements UserDetails {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Long id;

	private String firstName;
	private String fullName;
	private String lastName;
	private String phoneNo;
	private String password;
	private String email;
	private String dateOfBirth;
	private List<Long> userRoleIds=new ArrayList<>();
	private Collection<? extends GrantedAuthority> authorities;
	private boolean isActive;
	private String recapture;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public List<Long> getUserRoleIds() {
		return userRoleIds;
	}

	public void setUserRoleIds(List<Long> userRoleIds) {
		this.userRoleIds = userRoleIds;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		return authorities;
	}

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}
	
	

	public UserModel(Long id, String password, String email,
			Collection<? extends GrantedAuthority> authorities, boolean isActive) {
		super();
		this.id = id;
		this.password = password;
		this.email = email;
		this.authorities = authorities;
		this.isActive = isActive;
	}

	public static UserModel build(MUser mUser) {
		List<GrantedAuthority> authorities = mUser.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());

		return new UserModel(mUser.getUserId(), mUser.getPassword(),mUser.getEmail(), authorities,mUser.isActive());
	}

	public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
		this.authorities = authorities;
	}
	
	

}
