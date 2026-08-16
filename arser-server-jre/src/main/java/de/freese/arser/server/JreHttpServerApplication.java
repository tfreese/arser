// package de.freese.arser.server;
//
// import java.net.URI;
// import java.nio.file.Path;
// import java.time.Duration;
//
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.slf4j.bridge.SLF4JBridgeHandler;
//
// import de.freese.arser.api.Arser;
// import de.freese.arser.component.DefaultLifeCycleRegistry;
// import de.freese.arser.component.LifeCycleRegistry;
// import de.freese.arser.config.ServerConfig;
// import de.freese.arser.repository.file.FileRepository;
// import de.freese.arser.repository.http.HttpRepository;
// import de.freese.arser.repository.virtual.VirtualRepository;
//
// /**
//  * @author Thomas Freese
//  * @since 27.07.23
//  */
// public final class JreHttpServerApplication {
//     private static final Logger LOGGER = LoggerFactory.getLogger(JreHttpServerApplication.class);
//
//     private static final Path WORKING_PATH = Path.of(System.getProperty("java.io.tmpdir"), "arser");
//
//     static void main() {
//         try {
//             // Redirect Java-Util-Logger to Slf4J.
//             SLF4JBridgeHandler.removeHandlersForRootLogger();
//             SLF4JBridgeHandler.install();
//
//             if (LoggerFactory.getLogger("jdk.httpclient.HttpClient").isDebugEnabled()) {
//                 // System.setProperty("jdk.httpclient.HttpClient.log", "all");
//                 System.setProperty("jdk.httpclient.HttpClient.log", "requests");
//             }
//
//             final LifeCycleRegistry lifeCycleRegistry = new DefaultLifeCycleRegistry();
//
//             final Arser arser = Arser.builder()
//                     .add(HttpRepository.builder()
//                             .uri(URI.create("https://repo1.maven.org/maven2"))
//                             .name("maven-central")
//                             .withRetrying(3, Duration.ofSeconds(2L))
//                             .withCaching(WORKING_PATH.resolve("maven-central-cache"))
//                             .withLogging())
//                     .add(FileRepository.builder()
//                             .uri(WORKING_PATH.resolve("snapshots").toUri())
//                             .name("snapshots")
//                             .readOnly(false)
//                             .withLogging())
//                     .add(VirtualRepository.builder()
//                             .uri(URI.create("virtual://public"))
//                             .name("public")
//                             .addRepositoryRef("maven-central")
//                             .addRepositoryRef("snapshots")
//                     )
//                     .build(lifeCycleRegistry);
//
//             final JreHttpServer jreHttpServer = new JreHttpServer(arser, ServerConfig.builder().port(8080).build());
//             lifeCycleRegistry.register(jreHttpServer);
//
//             Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                 try {
//                     lifeCycleRegistry.stop();
//                 }
//                 catch (final Exception ex) {
//                     LOGGER.error(ex.getMessage(), ex);
//                 }
//             }, "Shutdown"));
//
//             lifeCycleRegistry.start();
//         }
//         catch (final Exception ex) {
//             LOGGER.error(ex.getMessage(), ex);
//
//             System.exit(-1);
//         }
//     }
//
//     private JreHttpServerApplication() {
//         super();
//     }
// }
