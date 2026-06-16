/**
 * 
 */
package co.ke.tezza.loanapp.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.google.gson.JsonElement;

import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.UserDetails;
import co.ke.tezza.loanapp.response.ReferralCode;
import co.ke.tezza.loanapp.response.User;
import co.ke.tezza.loanapp.response.UserResponse;

/**
 * @author austine
 *
 */
public interface UserService {

	public UserResponse createUser(UserDetails userModel) throws SetUpExceptions;

	public UserResponse updateUser(UserDetails userModel) throws SetUpExceptions;

	public UserResponse deleteById(Long user_id) throws SetUpExceptions;

	public Page<UserResponse> findByIsActive(int page, int size) throws SetUpExceptions;

	public UserResponse getUserByUserName(String username) throws SetUpExceptions;

	public UserResponse getUserById(long id) throws SetUpExceptions;

	public Page<UserResponse> getUserByCreatedBy(int page, int size) throws SetUpExceptions;

	public ReferralCode getReferralCode() throws SetUpExceptions;

	public Page<UserResponse> getUserByOrganizationPage(int page, int size,String search);

	Page<User> getMyReferrals(int page,int size) throws SetUpExceptions;
	public List<UserResponse> getUsersByRole(Long roleId,String searchTerm) throws SetUpExceptions;


	public UserResponse getMyProfile();

}
