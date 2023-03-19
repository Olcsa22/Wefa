package hu.lanoga.toolbox.vcard;

import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ToolboxVCardData {
	
	/**
	 * készített vcard locale (nem biztos, hogy minden érték figyelembe veszi)
	 */
	private String locale;
	
	/**
	 * név titulus (dr. stb.)
	 */
	private String title;
	private String familyName;
	private String givenName;
	
	private Date dateOfBirth;

	/**
	 * a lehetséges kulcsokhoz lásd {@link hu.lanoga.toolbox.ToolboxSysKeys.GeoAddressType}
	 */
	private Map<Integer, VCardGeoAddress> geoAddresses;

	/**
	 * a lehetséges kulcsokhoz lásd {@link hu.lanoga.toolbox.ToolboxSysKeys.ContactAddressType}
	 */
	private Map<Integer, String> emails;
	
	private String telephoneNumber;
	private String socialMedia;

	private String organization;
	private String jobTitle;
	private String expertise;

	private List<String> hobbies;
	private List<String> interests;
	private List<String> languages;

	private InputStream photo;

}
