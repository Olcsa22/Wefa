CREATE TABLE ip2location_db11 (
	ip_from bigint NOT NULL,
	ip_to bigint NOT NULL,
	country_code character(2) NOT NULL,
	country_name character varying(64) NOT NULL,
	region_name character varying(128) NOT NULL,
	city_name character varying(128) NOT NULL,
	latitude real NOT NULL,
	longitude real NOT NULL,
	zip_code character varying(30) NOT NULL,
	time_zone character varying(8) NOT NULL
);

CREATE INDEX ON ip2location_db11(ip_from, ip_to);


-- CREATE TABLE ip2location_db11_ipv6 (
-- 	ip_from decimal(39,0) NOT NULL,
-- 	ip_to decimal(39,0) NOT NULL,
-- 	country_code character(2) NOT NULL,
-- 	country_name character varying(64) NOT NULL,
-- 	region_name character varying(128) NOT NULL,
-- 	city_name character varying(128) NOT NULL,
-- 	latitude real NOT NULL,
-- 	longitude real NOT NULL,
-- 	zip_code character varying(30) NOT NULL,
-- 	time_zone character varying(8) NOT NULL
-- );