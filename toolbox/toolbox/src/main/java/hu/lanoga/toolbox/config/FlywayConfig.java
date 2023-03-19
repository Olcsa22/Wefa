package hu.lanoga.toolbox.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hu.lanoga.toolbox.db.DbInitHelper;

@ConditionalOnProperty(name = "spring.flyway.enabled")
@Configuration
public class FlywayConfig {

	@Autowired
	private DbInitHelper dbInitHelper;
	
//	@Autowired
//	private FlywayAnnotationHelper flywayAnnotationHelper;

	@Bean
	public FlywayMigrationInitializer flywayInitializer(final Flyway flyway) {

		dbInitHelper.initDb2();

		// flyway.setCallbacks(flywayCallback());
		
		// flywayAnnotationHelper.scanAndGenerate(); // nincs használva semelyik projektben, ezért kiszedve dependency-k (de menne)

		return new FlywayMigrationInitializer(flyway);
	}

}