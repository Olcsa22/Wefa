package hu.lanoga.toolbox.spring;

import java.sql.Timestamp;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * {@link UserDetails} interface kiegészítve pár common mezővel
 */
public interface ToolboxUserDetails extends UserDetails {

	Integer getTenantId();
	Integer getId();
	
	String getTitle();
	String getGivenName();
	String getFamilyName();
	
	String getJobTitle();
	Integer getSuperior();
	
	String getNote();

	String getEmail();
	
	java.sql.Date getDateOfBirth();

	Integer getParentId();

	/**
	 * JSON tömb kell legyen (később ebből az Authorities)
	 *  
	 * @return
	 */
	String getUserRoles();
	
	Integer getCreatedBy();
	Timestamp getCreatedOn();
	Integer getModifiedBy();
	Timestamp getModifiedOn();
			
}
