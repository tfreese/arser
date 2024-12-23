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
                // .profiles("rest-client")
                // .profiles("web-client")
                //.banner(new MyBanner())
                //.listeners(new ApplicationPidFileWriter("arser.pid"))
                //.run(args)
                .build();

        application.run(args);
    }

    // private static void showShutdownFrame(final ConfigurableApplicationContext applicationContext) {
    //     final JFrame jFrame = new JFrame("Shutdown");
    //     jFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    //     jFrame.addWindowListener(new WindowAdapter() {
    //         @Override
    //         public void windowClosed(final WindowEvent event) {
    //             // applicationContext.close();
    //             SpringApplication.exit(applicationContext, () -> 0);
    //         }
    //     });
    //     jFrame.setSize(250, 100);
    //     jFrame.setLocationRelativeTo(null);
    //     jFrame.setVisible(true);
    // }

    // @Bean
    // ApplicationRunner openShutdownFrame(final ConfigurableApplicationContext applicationContext) {
    //     return args -> showShutdownFrame(applicationContext);
    // }

    // @EventListener(ApplicationReadyEvent.class)
    // void openShutdownFrame(final ApplicationReadyEvent event) {
    //     showShutdownFrame(event.getApplicationContext());
    // }
}
