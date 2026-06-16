package co.ke.tezza.loanapp.web.security;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import co.ke.tezza.loanapp.model.UserModel;
/**
 * @author austine
 *
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<Long> {

	@Override
	public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserModel) {
            return Optional.of(((UserModel) principal).getId());
            
        } else {
            return Optional.empty();
        }
    }
}
