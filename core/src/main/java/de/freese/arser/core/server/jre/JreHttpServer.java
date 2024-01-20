// Created: 22.07.23
package de.freese.arser.core.server.jre;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;

import de.freese.arser.core.server.AbstractArserServer;
import de.freese.arser.core.utils.ArserThreadFactory;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class JreHttpServer extends AbstractArserServer {

    //    private final List<HttpContext> httpContexts = new ArrayList<>();

    private ExecutorService executorService;
    private HttpServer httpServer;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final int port = getServerConfig().getPort();
        final int threadPoolCoreSize = getServerConfig().getThreadPoolCoreSize();
        final int threadPoolMaxSize = getServerConfig().getThreadPoolMaxSize();
        final String threadNamePattern = getServerConfig().getThreadNamePattern();

        this.executorService = new ThreadPoolExecutor(threadPoolCoreSize, threadPoolMaxSize, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new ArserThreadFactory(threadNamePattern));

        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.httpServer.setExecutor(executorService);

        this.httpServer.createContext("/", new JreHttpServerHandler(getArser()));

        this.httpServer.start();
        //        new Thread(this.httpServer::start, "arser").start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        //        this.httpContexts.clear();
        this.httpServer.stop(3);

        ArserUtils.shutdown(this.executorService, getLogger());
    }
}
