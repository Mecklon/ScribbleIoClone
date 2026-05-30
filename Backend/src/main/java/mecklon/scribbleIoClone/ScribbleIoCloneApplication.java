package mecklon.scribbleIoClone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScribbleIoCloneApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScribbleIoCloneApplication.class, args);
	}

}
