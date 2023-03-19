package hu.lanoga.toolbox.geo2;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeoCountryIpService {

    @Autowired
    private GeoCountryIpRepository geoCountryIpRepository;

    /**
     * GDPR köteles országok kódja
     */
    private String[] gdprCountries = new String[] {"AN", "AT", "BE", "BG", "CY", "CZ", "DE", "DK",
            "EE", "ES", "FI", "FR", "GB", "GR", "HR", "HU", "IE", "IT", "LA", "LT", "LU", "MT", "PL",
            "PT", "RO", "SE", "SI", "SK"};

    /**
     * IP cím alapján visszaadja, hogy az adott ország GDPR köteles-e
     * @param ipAddress IP cím
     * @return Igaz/hamis
     */
    public Boolean isCountryGdprBound(String ipAddress) {
        GeoCountryIp geoCountryIp = geoCountryIpRepository.getCountryFromIp(ipAddress);
        return geoCountryIp != null && Arrays.asList(gdprCountries).contains(geoCountryIp.getCountryCode());
    }

    /**
     * IP cím alapján visszaad egy GeoCountryIp objektumot
      * @param ipAddress IP cím
     * @return GeoCountryIp
     */
    public GeoCountryIp getCountryFromIp(String ipAddress) {
        return geoCountryIpRepository.getCountryFromIp(ipAddress);
    }

}
