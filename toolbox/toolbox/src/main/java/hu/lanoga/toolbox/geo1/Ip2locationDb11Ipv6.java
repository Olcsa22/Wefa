package hu.lanoga.toolbox.geo1;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class Ip2locationDb11Ipv6 implements ToolboxIpLocation {

    private BigDecimal ipFrom;
    private BigDecimal ipTo;

    private String countryCode;
    private String countryName;
    private String regionName;
    private String cityName;
    private Float latitude;
    private Float longitude;
    private String zipCode;
    private String timeZone;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Ip2locationDb11Ipv6{");
        sb.append("ipFrom=").append(ipFrom);
        sb.append(", ipTo=").append(ipTo);
        sb.append(", countryCode='").append(countryCode).append('\'');
        sb.append(", countryName='").append(countryName).append('\'');
        sb.append(", regionName='").append(regionName).append('\'');
        sb.append(", cityName='").append(cityName).append('\'');
        sb.append(", latitude=").append(latitude);
        sb.append(", longitude=").append(longitude);
        sb.append(", zipCode='").append(zipCode).append('\'');
        sb.append(", timeZone='").append(timeZone).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
