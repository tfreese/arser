// Created: 09 März 2025
package de.freese.arser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

/**
 * @author Thomas Freese
 */
public final class EnabledIfReachableCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext context) {
        return AnnotationUtils.findAnnotation(context.getElement(), EnabledIfReachable.class)
                .map(this::enableIfReachable)
                .orElse(ConditionEvaluationResult.enabled("@EnabledIfReachable is not present"));
    }

    private ConditionEvaluationResult enableIfReachable(final EnabledIfReachable annotation) {
        final String uri = annotation.uri();
        final int timeoutMillis = annotation.timeoutMillis();

        final boolean reachable = isReachable(URI.create(uri), timeoutMillis);

        if (reachable) {
            return ConditionEvaluationResult.enabled(String.format("Enabled because %s is reachable", uri));
        }
        else {
            return ConditionEvaluationResult.disabled(String.format("Disabled because %s could not be reached in %dms", uri, timeoutMillis));
        }
    }

    private boolean isReachable(final URI uri, final int timeoutMillis) {
        if ("file".equals(uri.getScheme())) {
            return Files.exists(Path.of(uri));
        }

        try (HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofMillis(timeoutMillis))
                .build()) {
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .HEAD()
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .build();

            httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());

            // State is irrelevant.
            return true;

            // final HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
            //
            // return httpResponse.statusCode() == HttpsURLConnection.HTTP_OK;
        }
        catch (Exception ex) {
            return false;
        }
    }
}
