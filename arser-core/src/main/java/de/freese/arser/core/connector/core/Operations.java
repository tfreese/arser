package de.freese.arser.core.connector.core;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.freese.arser.core.connector.api.AttributeKey;
import de.freese.arser.core.connector.api.Operation;

/**
 * @author Thomas Freese
 */
public final class Operations {
    public static final Operation<Void> DELETE = new SimpleOp<>("delete", Void.class, true, false, Set.of());
    public static final Operation<byte[]> DOWNLOAD = readOnly("download", byte[].class);
    public static final Operation<InputStream> DOWNLOAD_STREAM = readOnly("download.stream", InputStream.class);
    public static final Operation<Boolean> EXISTS = readOnly("exists", Boolean.class);
    @SuppressWarnings({"rawtypes"})
    public static final Operation<Map> HEAD = readOnly("head", Map.class);
    @SuppressWarnings({"rawtypes"})
    public static final Operation<List> LIST = readOnly("list", List.class);
    public static final Operation<Void> UPLOAD = new SimpleOp<>("upload", Void.class, true, false, Set.of(Attributes.BODY));
    public static final Operation<Long> UPLOAD_STREAM = new SimpleOp<>("upload.stream", Long.class, true, false, Set.of(Attributes.BODY_STREAM));

    private record SimpleOp<R>(String name, Class<R> resultType, boolean isIdempotent, boolean isReadOnly, Set<AttributeKey<?>> requiredAttributes) implements Operation<R> {
    }

    public static <R> Operation<R> custom(final String name, final Class<R> type, final boolean idempotent, final boolean readOnly, final Set<AttributeKey<?>> required) {
        return new SimpleOp<>(name, type, idempotent, readOnly, required);
    }

    private static <R> Operation<R> readOnly(final String name, final Class<R> type) {
        return new SimpleOp<>(name, type, true, true, Set.of());
    }

    private Operations() {
        super();
    }
}
