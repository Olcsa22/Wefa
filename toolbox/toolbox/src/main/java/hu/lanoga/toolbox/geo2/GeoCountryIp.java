package hu.lanoga.toolbox.geo2;

import hu.lanoga.toolbox.repository.ToolboxPersistable;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class GeoCountryIp implements ToolboxPersistable {

    @NotNull
    private Integer id;

    @NotNull
    private String countryName;

    private String countryCode;

    @NotNull
    private String startIp;

    @NotNull
    private String endIp;

    @NotNull
    private String netmask;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("GeoCountryIp: ");
        result.append("id = ").append(id);
        result.append(", countryName = ").append(countryName);
        result.append(", countryCode = ").append(countryCode);
        result.append(", startIp = ").append(startIp);
        result.append(", endIp = ").append(endIp);
        result.append(", netmask = ").append(netmask);
        return result.toString();
    }
}
