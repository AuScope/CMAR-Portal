package org.auscope.portal.server.web.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.UsernameNotFoundException;

/**
 * This is a temporary extension of the user details service that will assign anyone with a non null
 * identity a "default" identity.
 *
 * This is to be used by Shibboleth so that if someone authenticates with an IDP but we don't already recognize their
 * identity, give them a basic user role (Rather than ROLE_ANONYMOUS).
 * 
 */
public class PortalUserDetailsService implements UserDetailsService{

	protected final Logger logger = Logger.getLogger(getClass());
	
	Map<String,UserDetails> userDetailsMap;
	String defaultRole;
	
	public PortalUserDetailsService(String defaultRole, ArrayList userDetailsList) {
		this.defaultRole = defaultRole;
		this.userDetailsMap = new HashMap<String,UserDetails>();
		
		for (Object item : userDetailsList) {
			UserDetails details = (UserDetails) item;
			
			this.userDetailsMap.put(details.getUsername(), details);
		}
	}
	
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException, DataAccessException {
		
		logger.debug("username : " + username);
		
		if (username == null || username.length() == 0)
			throw new UsernameNotFoundException(username);
		
		UserDetails details = userDetailsMap.get(username);
		if (details != null)
		{
			logger.debug("Matched existing");
			return details;
		}
		
		logger.debug("Creating default user");
		return new User(username, "", true, true, true, true, new GrantedAuthority[] {new GrantedAuthorityImpl(this.defaultRole)});
	}

}
