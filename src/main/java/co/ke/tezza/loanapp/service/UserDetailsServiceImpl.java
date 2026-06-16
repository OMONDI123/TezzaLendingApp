package co.ke.tezza.loanapp.service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.model.UserModel;
import co.ke.tezza.loanapp.repository.UserRepository;
/**
 * @author austine
 *
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	private LoginAttemptService loginAttemptService;

	@Override
	@Transactional
	public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
		MUser mUser = userRepository.findTop1ByEmailAndIsActive(userName,true);
		final String ip = getClientIP();
		if (loginAttemptService.isBlocked(ip)) {
			throw new RuntimeException("blocked");
		}
		if (mUser == null) {
			throw new UsernameNotFoundException("User not found with username: " + userName);
		}
		
		return UserModel.build(mUser);
	}
	private final String getClientIP() {
		final String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader == null) {
			return request.getRemoteAddr();
		}

		return xfHeader.split(",")[0];
	}
}
