// Created: 26 Okt. 2024
package de.freese.arser.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Thomas Freese
 */
class TestCalculator {
    @Test
    void testAdd() {
        final Calculator calculator = new DefaultCalculator();

        assertEquals(2, calculator.add(1, 1));
    }
}
