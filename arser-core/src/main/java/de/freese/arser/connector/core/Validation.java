package de.freese.arser.connector.core;

import de.freese.arser.connector.api.AttributeKey;
import de.freese.arser.connector.api.ConnectorRequest;
import de.freese.arser.connector.spi.ValidationException;

/**
 * @author Thomas Freese
 */
public final class Validation {
    public static void validate(final ConnectorRequest<?> req) {
        for (final AttributeKey<?> key : req.operation().requiredAttributes()) {
            if (req.attribute(key).isEmpty()) {
                throw new ValidationException("Mandatory-Attribute missing for Operation '" + req.operation().name() + "': " + key.name());
            }
        }
    }

    private Validation() {
        super();
    }
}
