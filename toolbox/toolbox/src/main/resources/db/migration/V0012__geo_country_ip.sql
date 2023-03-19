CREATE TABLE geo_country_ip (
  id SERIAL PRIMARY KEY,
  country_name VARCHAR(150) not null,
  country_code character(2),
  start_ip VARCHAR(15) not null,
  end_ip VARCHAR(15) not null,
  netmask VARCHAR(15) not null
);

CREATE INDEX ON geo_country_ip(start_ip, end_ip);
CREATE INDEX ON geo_country_ip(lower(country_name));
CREATE INDEX ON geo_country_ip(lower(country_code));