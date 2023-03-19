package hu.lanoga.toolbox.vcard;

import ezvcard.VCard;
import ezvcard.parameter.Encoding;
import ezvcard.parameter.ImageType;
import ezvcard.property.*;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.i18n.I18nUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;

public class ToolboxVCardDataToEzVCardConverter implements Converter<ToolboxVCardData, VCard> {

	@Override
	public VCard convert(ToolboxVCardData vCardData) {

		final VCard vCard = new VCard();

		// ---

		// név

		final boolean n1 = StringUtils.isNotBlank(vCardData.getTitle());
		final boolean n2 = StringUtils.isNotBlank(vCardData.getGivenName());
		final boolean n3 = StringUtils.isNotBlank(vCardData.getFamilyName());

		if (!n2 || !n3) {
			throw new VCardException("Family name and given name are required!");
		}

		final FormattedName formattedName = vCard.setFormattedName(I18nUtil.buildFullName(vCardData.getTitle(), vCardData.getFamilyName(), vCardData.getGivenName(), vCardData.getLocale(), false));
		formattedName.getParameters().setEncoding(Encoding.QUOTED_PRINTABLE);
		formattedName.getParameters().setCharset("ISO-8859-1");
		vCard.setFormattedName(formattedName);

		final StructuredName structuredName = new StructuredName();

		structuredName.setFamily(vCardData.getFamilyName());
		structuredName.setGiven(vCardData.getGivenName());

		if (n1) {
			structuredName.getPrefixes().add(vCardData.getTitle());
		}

		structuredName.getParameters().setEncoding(Encoding.QUOTED_PRINTABLE);
		structuredName.getParameters().setCharset("ISO-8859-1");

		vCard.setStructuredName(structuredName);

		// születésnap

		vCard.setBirthday(new Birthday(vCardData.getDateOfBirth()));

		// szervezet és munkakör

		if (vCardData.getOrganization() != null) { // TODO: több ilyen null check kell(het)
			vCard.setOrganization(vCardData.getOrganization());
		}
		
		vCard.addRole(vCardData.getJobTitle());
		vCard.addExpertise(vCardData.getExpertise());

		// elérhetőségek
		if (vCardData.getEmails() != null) {
			for (Integer key : vCardData.getEmails().keySet()) {
				Email email = new Email(vCardData.getEmails().get(key));
				email.setAltId(key.toString());
				if (key.equals(ToolboxSysKeys.ContactAddressType.WORK_EMAIL)) {
					email.setPref(1);
				}
				vCard.addEmail(email);
			}
		}

		vCard.addTelephoneNumber(vCardData.getTelephoneNumber());

		vCard.addUrl(vCardData.getSocialMedia()); // EzVcard-ban nincs külön social media lehetőség, ezért került az URL-be a social media link

		if (vCardData.getGeoAddresses() != null) {

			for (Integer key : vCardData.getGeoAddresses().keySet()) {
				Address address = new Address();
				address.setCountry(vCardData.getGeoAddresses().get(key).getCountryCode());
				address.setPostalCode(vCardData.getGeoAddresses().get(key).getPostalCode());
				address.setStreetAddress(vCardData.getGeoAddresses().get(key).getStreet() + " " +
						vCardData.getGeoAddresses().get(key).getHouseNumber());
				address.setAltId(key.toString());
				if (key.equals(ToolboxSysKeys.GeoAddressType.WORK_ADDRESS)) {
					address.setPref(1);
				} else if (key.equals(ToolboxSysKeys.GeoAddressType.COMPANY_SITE)) {
					address.setPref(2);
				} else if (key.equals(ToolboxSysKeys.GeoAddressType.COMPANY_HEADQUARTERS)) {
					address.setPref(3);
				}

				vCard.addAddress(address);
			}
		}

		// egyéb infók
		if (vCardData.getHobbies() != null) {
			for (String hobby : vCardData.getHobbies()) {
				vCard.addHobby(hobby);
			}
		}

		if (vCardData.getInterests() != null) {
			for (String interest : vCardData.getInterests()) {
				vCard.addInterest(interest);
			}
		}

		if (vCardData.getLanguages() != null) {
			for (String language : vCardData.getLanguages()) {
				vCard.addLanguage(language);
			}
		}

		// fénykép

		if (vCardData.getPhoto() != null) {
			try {
				vCard.addPhoto(new Photo(vCardData.getPhoto(), ImageType.JPEG));
			} catch (IOException e) {
				throw new VCardException("Vcard photo creation failed." + e);
			}
		}

		// ---

		return vCard;
	}
}
