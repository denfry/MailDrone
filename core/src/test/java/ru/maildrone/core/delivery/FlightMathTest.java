package ru.maildrone.core.delivery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightMathTest {

    // Таймлайн: departure=0, leg1End=100, hoverEnd=200, arrival=300
    @Test
    void phasesWithSorting() {
        assertEquals(FlightMath.Phase.LEG1, FlightMath.phaseAt(0, 100, 200, 300, true));
        assertEquals(FlightMath.Phase.LEG1, FlightMath.phaseAt(99, 100, 200, 300, true));
        assertEquals(FlightMath.Phase.HOVER, FlightMath.phaseAt(100, 100, 200, 300, true));
        assertEquals(FlightMath.Phase.HOVER, FlightMath.phaseAt(199, 100, 200, 300, true));
        assertEquals(FlightMath.Phase.LEG2, FlightMath.phaseAt(200, 100, 200, 300, true));
        assertEquals(FlightMath.Phase.LEG2, FlightMath.phaseAt(299, 100, 200, 300, true));
        assertEquals(FlightMath.Phase.ARRIVED, FlightMath.phaseAt(300, 100, 200, 300, true));
        assertEquals(FlightMath.Phase.ARRIVED, FlightMath.phaseAt(999, 100, 200, 300, true));
    }

    @Test
    void phasesWithoutSortingSkipHover() {
        // Без сортировки hoverEnd==leg1End; фазы HOVER быть не должно.
        assertEquals(FlightMath.Phase.LEG1, FlightMath.phaseAt(50, 100, 100, 300, false));
        assertEquals(FlightMath.Phase.LEG2, FlightMath.phaseAt(100, 100, 100, 300, false));
        assertEquals(FlightMath.Phase.LEG2, FlightMath.phaseAt(150, 100, 100, 300, false));
        assertEquals(FlightMath.Phase.ARRIVED, FlightMath.phaseAt(300, 100, 100, 300, false));
    }

    @Test
    void fractionClampsAndInterpolates() {
        assertEquals(0.0, FlightMath.fraction(0, 100, 0));
        assertEquals(0.5, FlightMath.fraction(0, 100, 50));
        assertEquals(1.0, FlightMath.fraction(0, 100, 100));
        // зажатие за границами
        assertEquals(0.0, FlightMath.fraction(0, 100, -20));
        assertEquals(1.0, FlightMath.fraction(0, 100, 200));
        // вырожденный интервал
        assertEquals(1.0, FlightMath.fraction(100, 100, 100));
    }

    @Test
    void fractionMidpointIsHalf() {
        double f = FlightMath.fraction(1_000L, 3_000L, 2_000L);
        assertTrue(Math.abs(f - 0.5) < 1.0e-9);
    }
}
