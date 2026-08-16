// package de.freese.arser.server;
//
// import java.net.InetSocketAddress;
// import java.util.Objects;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.SynchronousQueue;
// import java.util.concurrent.ThreadPoolExecutor;
// import java.util.concurrent.TimeUnit;
//
// import com.sun.net.httpserver.HttpServer;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
//
// import de.freese.arser.api.Arser;
// import de.freese.arser.component.AbstractComponent;
// import de.freese.arser.config.ServerConfig;
// import de.freese.arser.config.ThreadPoolConfig;
// import de.freese.arser.utils.ArserThreadFactory;
// import de.freese.arser.utils.ArserUtils;
//
// /**
//  * @author Thomas Freese
//  * @since 22.07.23
//  */
// public final class JreHttpServer extends AbstractComponent {
//     private static final Logger LOGGER = LoggerFactory.getLogger(JreHttpServer.class);
//
//     private final Arser arser;
//     private final ServerConfig serverConfig;
//     private ExecutorService executorService;
//     private HttpServer httpServer;
//     // private HttpContext httpContext;
//
//     public JreHttpServer(final Arser arser, final ServerConfig serverConfig) {
//         super();
//
//         this.arser = Objects.requireNonNull(arser, "arser required");
//         this.serverConfig = Objects.requireNonNull(serverConfig, "serverConfig required");
//     }
//
//     @Override
//     public String toString() {
//         return getClass().getSimpleName()
//                 + " ["
//                 + "port=" + serverConfig.getPort()
//                 + ']';
//     }
//
//     @Override
//     protected void doStart() throws Exception {
//         final int port = serverConfig.getPort();
//         final ThreadPoolConfig threadPoolConfig = serverConfig.getThreadPoolConfig();
//
//         executorService = new ThreadPoolExecutor(threadPoolConfig.getCoreSize(),
//                 threadPoolConfig.getMaxSize(),
//                 60L,
//                 TimeUnit.SECONDS,
//                 new SynchronousQueue<>(),
//                 new ArserThreadFactory(threadPoolConfig.getNamePattern()));
//
//         // httpServer = HttpsServer.create(new InetSocketAddress(port), 0);
//         // if (httpServer instanceof HttpsServer https) {
//         //     https.setHttpsConfigurator(new HttpsConfigurator(SSLContext.getDefault()) {
//         //         @Override
//         //         public void configure(final HttpsParameters params) {
//         //             super.configure(params);
//         //         }
//         //     });
//         // }
//
//         httpServer = HttpServer.create(new InetSocketAddress(port), 0);
//         httpServer.setExecutor(executorService);
//         // httpContext = httpServer.createContext("/", new JreHttpServerHandler(arser));
//         httpServer.createContext("/", new JreHttpServerHandler(arser));
//
//         httpServer.start();
//         // new Thread(httpServer::start, "arser").start();
//     }
//
//     @Override
//     protected void doStop() throws Exception {
//         httpServer.stop(3);
//
//         ArserUtils.shutdown(executorService, getLogger());
//     }
// }
