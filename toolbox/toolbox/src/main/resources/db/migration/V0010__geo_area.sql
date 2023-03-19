CREATE TABLE geo_area (
	id SERIAL PRIMARY KEY,
	geo_area_type INT NOT NULL REFERENCES code_store_item(id),
	geo_area_name VARCHAR(100) NOT NULL,
	geo_area_url_name VARCHAR(100) NOT NULL,
	parent_area INT
);

CREATE INDEX ON geo_area (lower(geo_area_url_name));
CREATE INDEX ON geo_area (parent_area);