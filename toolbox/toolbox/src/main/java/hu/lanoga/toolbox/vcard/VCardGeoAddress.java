package hu.lanoga.toolbox.vcard;

import java.math.BigDecimal;

public interface VCardGeoAddress {
	
    String getCountryCode();
    String getPostalCode();
    String getCity();
    String getStreet();
    String getHouseNumber();

    BigDecimal getLongitude();
    BigDecimal getLatitude();
    
}
