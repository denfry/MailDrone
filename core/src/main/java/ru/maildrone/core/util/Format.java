package ru.maildrone.core.util;

import org.bukkit.Location;
import org.bukkit.World;

/** Мелкие помощники форматирования для сообщений. */
public final class Format {

    private Format() {
    }

    /** Координаты блока в человекочитаемом виде: {@code world 10, 64, -20}. */
    public static String pos(Location loc) {
        if (loc == null) {
            return "—";
        }
        World w = loc.getWorld();
        String world = w == null ? "?" : w.getName();
        return world + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    /** Оставшееся время до прибытия. */
    public static String eta(long deliverAt, long now) {
        if (deliverAt <= 0) {
            return "—";
        }
        long left = deliverAt - now;
        if (left <= 0) {
            return "прибывает";
        }
        return duration(left);
    }

    /** Длительность по миллисекундам: {@code 2 мин 5 с} / {@code 40 с}. */
    public static String duration(long millis) {
        long totalSec = Math.max(0, millis / 1000L);
        long min = totalSec / 60L;
        long sec = totalSec % 60L;
        if (min > 0) {
            return min + " мин " + sec + " с";
        }
        return sec + " с";
    }
}
