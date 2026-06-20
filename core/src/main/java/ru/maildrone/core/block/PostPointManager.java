package ru.maildrone.core.block;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.maildrone.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.logging.Level;

/**
 * Реестр почтовых точек: отделения (отправка) и почтоматы (получение).
 * Точка — это конкретный блок. Источник истины — ключи блоков (строки),
 * чтобы точки в выгруженных мирах не терялись при сохранении.
 */
public final class PostPointManager {

    private final JavaPlugin plugin;
    private final Schedulers schedulers;
    private final File file;
    private final Object writeLock = new Object();

    private final Set<String> offices = ConcurrentHashMap.newKeySet();
    private final Set<String> postomats = ConcurrentHashMap.newKeySet();

    public PostPointManager(JavaPlugin plugin, Schedulers schedulers) {
        this.plugin = plugin;
        this.schedulers = schedulers;
        this.file = new File(plugin.getDataFolder(), "points.yml");
    }

    public void load() {
        offices.clear();
        postomats.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yc = YamlConfiguration.loadConfiguration(file);
        offices.addAll(yc.getStringList("offices"));
        postomats.addAll(yc.getStringList("postomats"));
        plugin.getLogger().info("Почтовых точек: отделений " + offices.size() + ", почтоматов " + postomats.size());
    }

    /** @return true, если назначено; false, если снято. */
    public boolean toggleOffice(Location blockLoc) {
        String k = key(blockLoc);
        boolean added;
        if (offices.remove(k)) {
            added = false;
        } else {
            offices.add(k);
            added = true;
        }
        scheduleSave();
        return added;
    }

    public boolean togglePostomat(Location blockLoc) {
        String k = key(blockLoc);
        boolean added;
        if (postomats.remove(k)) {
            added = false;
        } else {
            postomats.add(k);
            added = true;
        }
        scheduleSave();
        return added;
    }

    public boolean isOffice(Location blockLoc) {
        return offices.contains(key(blockLoc));
    }

    public boolean isPostomat(Location blockLoc) {
        return postomats.contains(key(blockLoc));
    }

    public boolean hasOffices() {
        return !offices.isEmpty();
    }

    public boolean hasPostomats() {
        return !postomats.isEmpty();
    }

    public List<Location> officeLocations() {
        return resolve(offices);
    }

    public List<Location> postomatLocations() {
        return resolve(postomats);
    }

    public Location nearestOffice(Location from) {
        return nearest(offices, from);
    }

    public Location nearestPostomat(Location from) {
        return nearest(postomats, from);
    }

    public boolean hasOfficeNear(Location from, double radius) {
        Location o = nearestOffice(from);
        return o != null && o.getWorld() == from.getWorld()
                && o.distanceSquared(from) <= radius * radius;
    }

    public void flush() {
        writeNow();
    }

    // ---- internals ----

    private void scheduleSave() {
        schedulers.async(this::writeNow);
    }

    private void writeNow() {
        synchronized (writeLock) {
            YamlConfiguration yc = new YamlConfiguration();
            yc.set("offices", new ArrayList<>(offices));
            yc.set("postomats", new ArrayList<>(postomats));
            try {
                File dir = plugin.getDataFolder();
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                Files.write(file.toPath(), yc.saveToString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить points.yml", e);
            }
        }
    }

    private List<Location> resolve(Set<String> keys) {
        List<Location> out = new ArrayList<>();
        for (String k : keys) {
            Location loc = parse(k);
            if (loc != null) {
                out.add(loc);
            }
        }
        return out;
    }

    private Location nearest(Set<String> keys, Location from) {
        if (from.getWorld() == null) {
            return null;
        }
        Location best = null;
        double bestDist = Double.MAX_VALUE;
        for (String k : keys) {
            Location loc = parse(k);
            if (loc == null || loc.getWorld() != from.getWorld()) {
                continue;
            }
            double d = loc.distanceSquared(from);
            if (d < bestDist) {
                bestDist = d;
                best = loc;
            }
        }
        return best;
    }

    private static String key(Location loc) {
        World w = loc.getWorld();
        String world = w == null ? "null" : w.getUID().toString();
        return world + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    private static Location parse(String key) {
        String[] parts = key.split(";");
        if (parts.length != 4) {
            return null;
        }
        World world;
        try {
            world = Bukkit.getWorld(UUID.fromString(parts[0]));
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (world == null) {
            return null;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x + 0.5, y, z + 0.5);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
