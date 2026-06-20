package ru.maildrone.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatTest {

    @Test
    void durationFormatsSecondsAndMinutes() {
        assertEquals("0 с", Format.duration(0));
        assertEquals("40 с", Format.duration(40_000));
        assertEquals("1 мин 5 с", Format.duration(65_000));
        assertEquals("2 мин 0 с", Format.duration(120_000));
    }

    @Test
    void etaHandlesUnknownArrivedAndFuture() {
        long now = 1_000_000L;
        assertEquals("—", Format.eta(0, now));               // нет срока
        assertEquals("прибывает", Format.eta(now - 500, now)); // срок прошёл
        assertEquals("1 мин 5 с", Format.eta(now + 65_000, now));
    }
}
