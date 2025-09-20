// Created: 06.08.23
package de.freese.arser.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public final class CalculatorMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(CalculatorMain.class);

    static void main() {
        final Calculator calculator = new DefaultCalculator();

        LOGGER.info("1 + 1 = {}", calculator.add(1, 1));
    }

    private CalculatorMain() {
        super();
    }
}
