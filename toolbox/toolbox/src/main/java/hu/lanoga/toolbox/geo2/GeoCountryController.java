package hu.lanoga.toolbox.geo2;

import hu.lanoga.toolbox.controller.ToolboxHttpUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@ConditionalOnMissingBean(name = "geoCountryControllerOverrideBean")
@ConditionalOnProperty(name="tools.geo.country.controller.enabled", matchIfMissing = true)
@RestController
public class GeoCountryController {

	@Autowired
    private GeoCountryIpService geoCountryIpService;

    /**
     * IP cím alapján visszaadja, hogy az adott ország GDPR köteles-e
     * @param request HttpServletRequest
     * @return Igaz/hamis
     */
    @RequestMapping(value = "api/public/geocountry/is-country-gdpr-bound", method = RequestMethod.GET)
    public Boolean isCountryGdprBound(HttpServletRequest request) {
        return geoCountryIpService.isCountryGdprBound(ToolboxHttpUtils.determineIpAddress(request));
    }

    /**
     * IP cím alapján visszaad egy GeoCountryIp objektumot
     * @param request HttpServletRequest
     * @return GeoCountryIp
     */
    @RequestMapping(value = "api/public/geocountry/get-my-country", method = RequestMethod.GET)
    public String getCountryFromIp(HttpServletRequest request) {
        GeoCountryIp geoCountryIp = geoCountryIpService.getCountryFromIp(ToolboxHttpUtils.determineIpAddress(request));
        if (geoCountryIp == null) {
            return "Unknown country from ip: " + ToolboxHttpUtils.determineIpAddress(request);
        }
        return geoCountryIp.getCountryName();
    }

    /*
     * CSAK teszteléshez!
     * Paraméterben kapott IP címmel tesztelhető metódusok
     */
    @RequestMapping(value = "api/public/geocountry/is-country-gdpr-bound/{ipAddress:.+}", method = RequestMethod.GET)
    public Boolean isCountryGdprBoundTest(@PathVariable("ipAddress") final String ipAddress) {
        return geoCountryIpService.isCountryGdprBound(ipAddress);
    }

    @RequestMapping(value = "api/public/geocountry/get-my-country/{ipAddress:.+}", method = RequestMethod.GET)
    public String getCountryFromIpTest(@PathVariable("ipAddress") final String ipAddress) {
        GeoCountryIp geoCountryIp = geoCountryIpService.getCountryFromIp(ipAddress);
        if (geoCountryIp == null) {
            return "Unknown country from ip: " + ipAddress;
        }
        return geoCountryIp.getCountryName();
    }

}
