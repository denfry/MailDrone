package ru.maildrone.core.parcel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingNumbersTest {

    @Test
    void generatedNumbersAreValidAndWellFormed() {
        for (int i = 0; i < 200; i++) {
            String t = TrackingNumbers.generate();
            assertTrue(t.startsWith("ПР"), "должен начинаться с ПР: " + t);
            assertTrue(t.endsWith("RU"), "должен заканчиваться на RU: " + t);
            assertEquals(13, t.length(), "длина 13: " + t);
            assertTrue(TrackingNumbers.isValid(t), "должен быть валидным: " + t);
        }
    }

    @Test
    void normalizeTrimsAndUppercases() {
        assertEquals("ПР123456789RU", TrackingNumbers.normalize("  пр123456789ru  "));
        assertEquals("ПР000000000RU", TrackingNumbers.normalize("ПР000000000RU"));
    }

    @Test
    void invalidInputsRejected() {
        assertFalse(TrackingNumbers.isValid(null));
        assertFalse(TrackingNumbers.isValid(""));
        assertFalse(TrackingNumbers.isValid("ABC"));
        assertFalse(TrackingNumbers.isValid("ПР12345RU"));        // мало цифр
        assertFalse(TrackingNumbers.isValid("XX123456789RU"));    // не тот префикс
        assertFalse(TrackingNumbers.isValid("ПР123456789US"));    // не тот суффикс
    }

    @Test
    void normalizedLowercaseInputIsValid() {
        assertTrue(TrackingNumbers.isValid(TrackingNumbers.normalize("пр987654321ru")));
    }
}
