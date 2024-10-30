// Created: 21.01.24
package de.freese.arser.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author Thomas Freese
 */
@SpringBootApplication
public class ArserSpringServerApplication {
    public static void main(final String[] args) {
        // SpringApplication.run(ArserSpringServerApplication.class, args);
        //
        final SpringApplication application = new SpringApplicationBuilder(ArserSpringServerApplication.class)
                // .properties("spring.config.name:application-Server")
                .headless(true) // Default true
                .registerShutdownHook(true) // Default true
                // .profiles("web")
                .profiles("rest-client")
                // .profiles("web-client")
                //.banner(new MyBanner())
                //.listeners(new ApplicationPidFileWriter("pim-server.pid"))
                //.run(args)
                .build();

        application.run(args);
    }
}
