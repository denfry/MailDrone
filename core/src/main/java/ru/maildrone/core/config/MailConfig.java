package ru.maildrone.core.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Типобезопасная обёртка над {@code config.yml}. Перечитывается командой
 * {@code /post admin reload}.
 */
public final class MailConfig {

    private final JavaPlugin plugin;

    // delivery
    private int minSeconds;
    private int maxSeconds;
    private double sortingChance;
    private int sortingExtraMin;
    private int sortingExtraMax;
    private int minFlightSeconds;
    private int maxFlightSeconds;
    private double droneSpeed;
    private double cruiseHeight;

    // cost
    private CostType costType;
    private int costAmount;
    private Material costItem;

    // packing
    private int packingRows;

    // points
    private boolean requireOffice;
    private double officeRadius;

    // drone visuals
    private Material bodyMaterial;
    private Material rotorMaterial;
    private Material noseMaterial;
    private double droneScale;
    private int rotorCount;
    private boolean faceTravel;
    private boolean particles;
    private boolean sounds;

    private boolean notifyOnJoin;
    private boolean updateCheck;
    private int deliveredRetentionDays;
    private List<String> sortingNotes;

    public MailConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        minSeconds = c.getInt("delivery.min-seconds", 30);
        maxSeconds = c.getInt("delivery.max-seconds", 120);
        sortingChance = clamp01(c.getDouble("delivery.sorting-chance", 0.20));
        sortingExtraMin = c.getInt("delivery.sorting-extra-min-seconds", 20);
        sortingExtraMax = c.getInt("delivery.sorting-extra-max-seconds", 60);
        minFlightSeconds = c.getInt("delivery.min-flight-seconds", 5);
        maxFlightSeconds = c.getInt("delivery.max-flight-seconds", 25);
        droneSpeed = Math.max(1.0, c.getDouble("delivery.drone-speed", 8.0));
        cruiseHeight = c.getDouble("delivery.cruise-height", 12.0);

        costType = parseEnum(CostType.class, c.getString("cost.type", "XP_LEVELS"), CostType.XP_LEVELS);
        costAmount = Math.max(0, c.getInt("cost.amount", 1));
        costItem = parseMaterial(c.getString("cost.item", "EMERALD"), Material.EMERALD);

        packingRows = clamp(c.getInt("packing.rows", 4), 1, 5);

        requireOffice = c.getBoolean("points.require-office", false);
        officeRadius = Math.max(1.0, c.getDouble("points.office-radius", 12.0));

        bodyMaterial = parseMaterial(c.getString("drone.body-material", "GRAY_CONCRETE"), Material.GRAY_CONCRETE);
        rotorMaterial = parseMaterial(c.getString("drone.rotor-material", "IRON_NUGGET"), Material.IRON_NUGGET);
        noseMaterial = parseMaterial(c.getString("drone.nose-material", "REDSTONE_BLOCK"), Material.REDSTONE_BLOCK);
        droneScale = clampDouble(c.getDouble("drone.scale", 0.6), 0.1, 3.0);
        rotorCount = clamp(c.getInt("drone.rotor-count", 4), 1, 8);
        faceTravel = c.getBoolean("drone.face-travel", true);
        particles = c.getBoolean("drone.particles", true);
        sounds = c.getBoolean("drone.sounds", true);

        notifyOnJoin = c.getBoolean("notify-on-join", true);
        updateCheck = c.getBoolean("update-check", true);
        deliveredRetentionDays = Math.max(0, c.getInt("storage.delivered-retention-days", 7));
        sortingNotes = c.getStringList("delivery.sorting-notes");
        if (sortingNotes.isEmpty()) {
            sortingNotes = List.of(
                    "Обрабатывается в сортировочном центре «Подольск-ЦЕХ-3»",
                    "Ожидает обработки в логистическом центре Внуково",
                    "Проходит таможенное оформление",
                    "Передана в сортировочный центр Казани",
                    "Временно задержана: перегрузка сортировочного узла"
            );
        }

        if (maxSeconds < minSeconds) {
            maxSeconds = minSeconds;
        }
        if (sortingExtraMax < sortingExtraMin) {
            sortingExtraMax = sortingExtraMin;
        }
        if (maxFlightSeconds < minFlightSeconds) {
            maxFlightSeconds = minFlightSeconds;
        }
    }

    /** Случайная базовая задержка доставки (секунды) до прибытия. */
    public int randomDeliverySeconds() {
        return randomBetween(minSeconds, maxSeconds);
    }

    public int randomSortingExtraSeconds() {
        return randomBetween(sortingExtraMin, sortingExtraMax);
    }

    public int randomFlightSeconds() {
        return randomBetween(minFlightSeconds, maxFlightSeconds);
    }

    public String randomSortingNote() {
        if (sortingNotes.isEmpty()) {
            return "Обрабатывается в сортировочном центре";
        }
        return sortingNotes.get(ThreadLocalRandom.current().nextInt(sortingNotes.size()));
    }

    public boolean rollSorting() {
        return ThreadLocalRandom.current().nextDouble() < sortingChance;
    }

    // ---- getters ----

    public double sortingChance() {
        return sortingChance;
    }

    public double droneSpeed() {
        return droneSpeed;
    }

    public double cruiseHeight() {
        return cruiseHeight;
    }

    public CostType costType() {
        return costType;
    }

    public int costAmount() {
        return costAmount;
    }

    public Material costItem() {
        return costItem;
    }

    public int packingRows() {
        return packingRows;
    }

    public boolean requireOffice() {
        return requireOffice;
    }

    public double officeRadius() {
        return officeRadius;
    }

    public Material bodyMaterial() {
        return bodyMaterial;
    }

    public Material rotorMaterial() {
        return rotorMaterial;
    }

    public double droneScale() {
        return droneScale;
    }

    public Material noseMaterial() {
        return noseMaterial;
    }

    public int rotorCount() {
        return rotorCount;
    }

    public boolean faceTravel() {
        return faceTravel;
    }

    public boolean particles() {
        return particles;
    }

    public boolean sounds() {
        return sounds;
    }

    public boolean notifyOnJoin() {
        return notifyOnJoin;
    }

    public boolean updateCheck() {
        return updateCheck;
    }

    public int deliveredRetentionDays() {
        return deliveredRetentionDays;
    }

    // ---- helpers ----

    private static int randomBetween(int min, int max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static double clamp01(double v) {
        return clampDouble(v, 0.0, 1.0);
    }

    private static double clampDouble(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E def) {
        if (raw == null) {
            return def;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Неизвестное значение '" + raw + "' для " + type.getSimpleName() + ", использую " + def);
            return def;
        }
    }

    private Material parseMaterial(String raw, Material def) {
        if (raw == null) {
            return def;
        }
        Material m = Material.matchMaterial(raw.trim());
        if (m == null) {
            plugin.getLogger().log(Level.WARNING, "Неизвестный материал '" + raw + "', использую " + def);
            return def;
        }
        return m;
    }
}
