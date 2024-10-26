// Created: 23.07.23
package de.freese.arser.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public class DefaultCalculator implements Calculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCalculator.class);

    @Override
    public int add(final int a, final int b) {
        LOGGER.info("calculate: {} + {}", a, b);

        return a + b;
    }
}
