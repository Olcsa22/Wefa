package hu.lanoga;

import hu.lanoga.toolbox.spring.StartManager;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class Start {

	public static void main(final String[] args) {
		StartManager.start(Start.class, args);
	}
}
