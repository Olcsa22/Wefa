package hu.lanoga.toolbox.geo2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class GeoCountryIpRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * IP cím alapján visszaad egy GeoCountryIp objektumot, vagy nullt.
     *
     * @param ipAddress IP cím
     * @return GeoCountryIp
     */
    public GeoCountryIp getCountryFromIp(String ipAddress) {
        final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("ip_address", ipAddress);

        try {
            return namedParameterJdbcTemplate.queryForObject("SELECT * FROM geo_country_ip WHERE :ip_address::inet BETWEEN start_ip::inet and end_ip::inet",
                    namedParameters,
                    new BeanPropertyRowMapper<>(GeoCountryIp.class));
        } catch (final IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

}
