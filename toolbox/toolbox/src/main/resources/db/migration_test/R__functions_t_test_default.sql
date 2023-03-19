CREATE OR REPLACE FUNCTION create_tenant_test(tenant_name text DEFAULT 'test-tenant', email text DEFAULT 'example@example.com', password text DEFAULT '$2a$10$XxN0sMcfvgC1gCqVt.ARguVhX7XmA.eQlf2/gSByNdG2l0BqoHeGe', phone text DEFAULT NULL)
RETURNS void AS $$	
BEGIN

	PERFORM create_tenant(tenant_name, email, password, phone);
	
	-- projekt specifikusan felulirhato (teszt adatok)
	-- plusz lasd meg TenantService Java lehetosegei (teszt adatok)
	
END;
$$
LANGUAGE plpgsql;