package hu.lanoga;

import hu.lanoga.toolbox.spring.StartManager;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Start {

	public static void main(final String[] args) {
		StartManager.start(Start.class, args);
	}
}
