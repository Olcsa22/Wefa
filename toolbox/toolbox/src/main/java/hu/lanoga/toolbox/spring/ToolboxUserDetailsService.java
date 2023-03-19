package hu.lanoga.toolbox.spring;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * a Spring Security {@link UserDetailsService}-vel azonos interface, 
 * egyetlen különbség a {@link ToolboxUserDetails} használata {@link UserDetails} helyett
 */
public interface ToolboxUserDetailsService extends UserDetailsService {

    /**
     * Spring Security-nek kell ez, ebben a formában API-ról ne legyen elérhető (vagy alaposan át kell nézni/gondolni)!
     */
	@Override
	ToolboxUserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

}
