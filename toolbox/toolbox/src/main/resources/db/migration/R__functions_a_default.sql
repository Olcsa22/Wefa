CREATE OR REPLACE FUNCTION extr_from_lang(j jsonb, lang1 text, lang2 text)
RETURNS VARCHAR AS $$
BEGIN
RETURN COALESCE(j->>lang1, j->>lang2, (SELECT d.value FROM jsonb_each_text(j) d limit 1));
END;
$$
LANGUAGE plpgsql;

CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE OR REPLACE FUNCTION normalize_string_for_url(name text)
RETURNS VARCHAR AS $$
BEGIN
RETURN regexp_replace(regexp_replace(lower(COALESCE ((SELECT * FROM unnest(ts_lexize('unaccent',name)) LIMIT 1), name)),'\s+', '-', 'g'),'[^a-z0-9-]', '', 'g');
END;
$$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION abbreviate_string(str text, desired_length integer, abbrev_marker text)
RETURNS VARCHAR AS $$
BEGIN
		
	IF CHARACTER_LENGTH(str)>=desired_length THEN 
		RETURN LEFT(str, desired_length - CHARACTER_LENGTH(abbrev_marker)) || abbrev_marker;
	ELSE 
		RETURN str;
	END IF;

END;
$$
LANGUAGE plpgsql;

