// Created: 23.07.23
package de.freese.arser.core.component;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.freese.arser.config.HttpClientConfig;
import de.freese.arser.core.lifecycle.AbstractLifecycle;
import de.freese.arser.core.utils.ArserThreadFactory;
import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public class JreHttpClientComponent extends AbstractLifecycle {

    private final HttpClientConfig httpClientConfig;

    private ExecutorService executorService;
    private HttpClient httpClient;

    public JreHttpClientComponent(final HttpClientConfig httpClientConfig) {
        super();

        this.httpClientConfig = assertNotNull(httpClientConfig, () -> "HttpClientConfig");
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        assertNotNull(httpClientConfig, () -> "ClientConfig");

        final int threadPoolCoreSize = assertValue(httpClientConfig.getThreadPoolCoreSize(), value -> value <= 0, () -> "ThreadPoolCoreSize has invalid range");
        final int threadPoolMaxSize = assertValue(httpClientConfig.getThreadPoolMaxSize(), value -> value <= 0, () -> "ThreadPoolMaxSize has invalid range");
        final String threadNamePattern = assertNotNull(httpClientConfig.getThreadNamePattern(), () -> "ThreadNamePattern");

        this.executorService = new ThreadPoolExecutor(threadPoolCoreSize, threadPoolMaxSize, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new ArserThreadFactory(threadNamePattern));

        // @formatter:off
        final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NEVER)
                .proxy(ProxySelector.getDefault())
                .connectTimeout(Duration.ofSeconds(3))
                .executor(this.executorService)
                ;
        // @formatter:on
        // .authenticator(Authenticator.getDefault())
        // .cookieHandler(CookieHandler.getDefault())
        // .sslContext(SSLContext.getDefault())
        // .sslParameters(new SSLParameters())

        this.httpClient = httpClientBuilder.build();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        this.httpClient.shutdownNow();
        this.httpClient.close();
        this.httpClient = null;

        ArserUtils.shutdown(this.executorService, getLogger());
    }
}
