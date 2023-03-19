package hu.lanoga.toolbox.geo1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class Ip2LocationRepository {

    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ToolboxIpLocation findIp2LocationIpv4(final Long ipAddress) {

        final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("ip_address", ipAddress);

        try {
            return namedParameterJdbcTemplate.queryForObject("SELECT * FROM ip2location_db11 WHERE ip_from <= :ip_address AND ip_to >= :ip_address", namedParameters, new BeanPropertyRowMapper<>(Ip2locationDb11.class));
        } catch (final IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

//    public ToolboxIpLocation findIp2LocationIpv6(final BigDecimal ipAddress) {
//
//        final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("ip_address", ipAddress.toString());
//
//        try {
//            return namedParameterJdbcTemplate.queryForObject("SELECT * FROM ip2location_db11_ipv6 WHERE ip_from <= :ip_address AND ip_to >= :ip_address", namedParameters, new BeanPropertyRowMapper<>(Ip2locationDb11Ipv6.class));
//        } catch (final IncorrectResultSizeDataAccessException e) {
//            return null;
//        }
//    }

}
