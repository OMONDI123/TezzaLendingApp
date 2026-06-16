package co.ke.tezza.loanapp.web.security;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import co.ke.tezza.loanapp.service.UserDetailsServiceImpl;
import co.ke.tezza.loanapp.util.JwtRequestFilter;

/**
 * @author austine
 *
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfigurer extends WebSecurityConfigurerAdapter {

	@Autowired
	private JwtRequestFilter jwtRequestFilter;

	@Autowired
	private UserDetailsServiceImpl userDetailsService;

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService);
	}


	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable().cors().and() .authorizeRequests().antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.antMatchers("/auth/authenticate**", "/auth/validateEmail**", "/auth/forgotPassword**",
						"/user/createUser**", "/bbb/**", "/swagger-ui.html", "/swagger-ui/**", "/v2/api-docs",
						"/swagger-resources/**", "/configuration/ui", "/configuration/security", "/webjars/**",
						"/common/downloadCountryCsvTemplate","/file/**", "/loanApplication/downloadLoanApplicationCsvTemplate**",
						"/common/downloadCountySubCountyAndWardCsvTemplate","/api/payments/**","/api/paypal/**",
						"/auth/sendVerificationCode**","/auth/verifyPasswordResetOTP**"
						,"/role/**","/dashboard/getAllActiveMenus"
						)
				.permitAll().anyRequest().authenticated().and().sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
	}

	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public GrantedAuthoritiesMapper authoritiesMapper() {
		return (authorities) -> {

			return authorities.stream().map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
					.collect(Collectors.toList());
		};
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedOrigins("*")

						.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
						.allowedHeaders("Authorization", "Content-Type").allowCredentials(true);
			}
		};
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/v2/api-docs", "/configuration/ui", "/swagger-resources/**",
				"/configuration/security", "/swagger-ui.html", "/webjars/**");
	}
}