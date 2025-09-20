// Created: 23.07.23
package de.freese.arser.core;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.arser.core.utils.ArserUtils;

/**
 * @author Thomas Freese
 */
public final class Misc {
    private static final Logger LOGGER = LoggerFactory.getLogger(Misc.class);

    static void main() {
        testUrl();
        removeSnapshotTimestamp();
    }

    private static void removeSnapshotTimestamp() {
        final Pattern pattern = ArserUtils.PATTERN_SNAPSHOT_TIMESTAMP;
        final Matcher matcher = pattern.matcher("/de/freese/arser/test-project/0.0.1-SNAPSHOT/test-project-0.0.1-20230806.084242-1.pom");

        if (matcher.find()) {
            LOGGER.info(matcher.group());
        }

        LOGGER.info(matcher.replaceAll("SNAPSHOT"));
    }

    private static void testUrl() {
        final URI uri = URI.create("file:///tmp/arser/cache/");

        final String relative = "public-cached";

        LOGGER.info("{}", uri.relativize(URI.create(relative)));
        LOGGER.info("{}", uri.resolve(URI.create(relative)));
        LOGGER.info("{}", uri.resolve(relative));
    }

    private Misc() {
        super();
    }
}
