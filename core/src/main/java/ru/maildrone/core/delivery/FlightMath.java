package ru.maildrone.core.delivery;

/**
 * Чистая (без Bukkit) математика фаз полёта дрона — вынесена отдельно, чтобы
 * покрыть тестами самую хитрую часть доставки.
 */
public final class FlightMath {

    private FlightMath() {
    }

    /** Фаза полёта в момент времени. */
    public enum Phase {
        /** Взлёт/полёт от старта к середине. */
        LEG1,
        /** Зависание на сортировке (только если sorting=true). */
        HOVER,
        /** Полёт от середины к финишу. */
        LEG2,
        /** Прибытие. */
        ARRIVED
    }

    /**
     * @param now      текущее время (epoch millis)
     * @param leg1End  конец первого участка
     * @param hoverEnd конец зависания на сортировке
     * @param arrival  момент прибытия
     * @param sorting  была ли задержка на сортировке
     */
    public static Phase phaseAt(long now, long leg1End, long hoverEnd, long arrival, boolean sorting) {
        if (now >= arrival) {
            return Phase.ARRIVED;
        }
        if (now < leg1End) {
            return Phase.LEG1;
        }
        if (sorting && now < hoverEnd) {
            return Phase.HOVER;
        }
        return Phase.LEG2;
    }

    /** Доля прохождения участка [start, end] в момент now, зажатая в [0, 1]. */
    public static double fraction(long start, long end, long now) {
        if (end <= start) {
            return 1.0;
        }
        double f = (double) (now - start) / (double) (end - start);
        return Math.max(0.0, Math.min(1.0, f));
    }
}
