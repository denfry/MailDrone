package ru.maildrone.core.parcel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParcelStatusTest {

    @Test
    void inDeliveryCoversTransitStates() {
        assertTrue(ParcelStatus.ACCEPTED.inDelivery());
        assertTrue(ParcelStatus.IN_TRANSIT.inDelivery());
        assertTrue(ParcelStatus.AT_SORTING.inDelivery());

        assertFalse(ParcelStatus.CREATED.inDelivery());
        assertFalse(ParcelStatus.ARRIVED.inDelivery());
        assertFalse(ParcelStatus.DELIVERED.inDelivery());
        assertFalse(ParcelStatus.RETURNED.inDelivery());
    }

    @Test
    void terminalStatesAreDeliveredAndReturned() {
        assertTrue(ParcelStatus.DELIVERED.isTerminal());
        assertTrue(ParcelStatus.RETURNED.isTerminal());

        assertFalse(ParcelStatus.CREATED.isTerminal());
        assertFalse(ParcelStatus.ACCEPTED.isTerminal());
        assertFalse(ParcelStatus.IN_TRANSIT.isTerminal());
        assertFalse(ParcelStatus.AT_SORTING.isTerminal());
        assertFalse(ParcelStatus.ARRIVED.isTerminal());
    }

    @Test
    void everyStatusHasLabelAndColor() {
        for (ParcelStatus s : ParcelStatus.values()) {
            assertFalse(s.label().isBlank(), "label пуст: " + s);
            assertTrue(s.colorTag().startsWith("<"), "colorTag не MiniMessage: " + s);
        }
    }
}
