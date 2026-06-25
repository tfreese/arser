package de.freese.arser.core.connector.core;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

import de.freese.arser.core.connector.api.AttributeKey;

/**
 * @author Thomas Freese
 */
public final class Attributes {
    public static final AttributeKey<byte[]> BODY = AttributeKey.of("body", byte[].class);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final AttributeKey<Supplier<InputStream>> BODY_STREAM = (AttributeKey) AttributeKey.of("body.stream", Supplier.class);

    public static final AttributeKey<String> ETAG = AttributeKey.of("etag", String.class);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final AttributeKey<Map<String, String>> HEADERS = (AttributeKey) AttributeKey.of("headers", Map.class);

    public static final AttributeKey<String> METHOD = AttributeKey.of("method", String.class);
    
    public static final AttributeKey<Duration> TIMEOUT = AttributeKey.of("timeout", Duration.class);

    private Attributes() {
        super();
    }
}
