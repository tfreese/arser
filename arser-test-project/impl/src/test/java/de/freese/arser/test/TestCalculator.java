package de.freese.arser.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Thomas Freese
 * @since 26.10.2024
 */
class TestCalculator {
    @Test
    void testAdd() {
        final Calculator calculator = new DefaultCalculator();

        assertEquals(2, calculator.add(1, 1));
    }
}
