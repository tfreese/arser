// Created: 23.07.23
package de.freese.arser.core;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Thomas Freese
 */
public final class Misc {
    public static void main(final String[] args) throws Exception {
        testUrl();
        removeSnapshotTimestamp();
    }

    private static void printlnOut(final Object value) {
        printlnOut(String.valueOf(value));
    }

    private static void printlnOut(final String text) {
        System.out.println(text);
    }

    private static void removeSnapshotTimestamp() throws Exception {
        final Pattern pattern = Pattern.compile("\\d{8}\\.\\d{6}-\\d");
        final Matcher matcher = pattern.matcher("/de/freese/arser/test-project/0.0.1-SNAPSHOT/test-project-0.0.1-20230806.084242-1.pom");

        if (matcher.find()) {
            printlnOut(matcher.group());
        }

        printlnOut(matcher.replaceAll("SNAPSHOT"));
    }

    private static void testUrl() throws Exception {
        final URI uri = URI.create("file:///tmp/arser/cache/");

        final String relative = "public-cached";

        printlnOut(uri.relativize(URI.create(relative)));
        printlnOut(uri.resolve(URI.create(relative)));
        printlnOut(uri.resolve(relative));

        printlnOut(uri.resolve(relative));
    }

    private Misc() {
        super();
    }
}
