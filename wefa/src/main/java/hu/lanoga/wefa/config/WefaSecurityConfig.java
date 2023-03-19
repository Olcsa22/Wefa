package hu.lanoga.wefa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity // fontos (nem Inheritable az annotáció az ősosztályon) !!!
@EnableGlobalMethodSecurity(securedEnabled = true) // fontos (nem Inheritable az annotáció az ősosztályon) !!!
@Order(99)
public class WefaSecurityConfig extends hu.lanoga.toolbox.config.SecurityConfig {

	// mindenképp kell, mert nem tudja létrehozni a táblákat megfelelő sorrendben az Activiti és a Flyway
	// TODO: érdemes lenne megnézni, hogy a Flyway lifecycle-t lehet-e megfelelően módosítani

}