package ru.maildrone.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    @Test
    void detectsNewerVersions() {
        assertTrue(UpdateChecker.isNewer("0.2.0", "0.1.0"));
        assertTrue(UpdateChecker.isNewer("0.1.1", "0.1.0"));
        assertTrue(UpdateChecker.isNewer("1.0.0", "0.9.9"));
        // числовое, а не лексикографическое сравнение
        assertTrue(UpdateChecker.isNewer("0.10.0", "0.9.0"));
        assertTrue(UpdateChecker.isNewer("26.1.0", "1.21.0"));
    }

    @Test
    void rejectsSameOrOlder() {
        assertFalse(UpdateChecker.isNewer("0.1.0", "0.1.0"));
        assertFalse(UpdateChecker.isNewer("0.1.0", "0.2.0"));
        assertFalse(UpdateChecker.isNewer("1.21.0", "26.1.0"));
    }
}
