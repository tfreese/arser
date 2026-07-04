package de.freese.arser.utils;

/**
 * @author Thomas Freese
 * @since 19.07.23
 */
public enum HttpMethod {
    /**
     * Exist Query
     */
    HEAD,
    /**
     * Download Binary
     */
    GET,
    /**
     * Deploy
     */
    PUT;

    public static HttpMethod get(final String method) {
        for (final HttpMethod httpMethod : values()) {
            if (httpMethod.name().equalsIgnoreCase(method)) {
                return httpMethod;
            }
        }

        throw new UnsupportedOperationException("HttpMethod not supported: " + method);
    }
}
