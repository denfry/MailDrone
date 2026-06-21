package ru.maildrone.core.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.maildrone.core.parcel.Parcel;
import ru.maildrone.core.parcel.ParcelStatus;
import ru.maildrone.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Хранилище на основе одного файла {@code parcels.yml}.
 *
 * <p>Источник истины — карта в памяти. Содержимое посылок сериализуется в
 * base64 на потоке вызывающего ({@link #put}); запись файла идёт асинхронно
 * (только I/O, без обращения к API мира) — это безопасно на Folia.
 */
public final class YamlParcelStore implements ParcelStore {

    private final JavaPlugin plugin;
    private final Schedulers schedulers;
    private final File file;

    private final ConcurrentHashMap<String, Parcel> parcels = new ConcurrentHashMap<>();
    /** Кэш сериализованного (base64) содержимого: считается на безопасном потоке. */
    private final ConcurrentHashMap<String, String> contentCache = new ConcurrentHashMap<>();
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);
    private final Object writeLock = new Object();

    public YamlParcelStore(JavaPlugin plugin, Schedulers schedulers) {
        this.plugin = plugin;
        this.schedulers = schedulers;
        this.file = new File(plugin.getDataFolder(), "parcels.yml");
    }

    @Override
    public void load() {
        parcels.clear();
        contentCache.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        for (String key : loaded.getKeys(false)) {
            ConfigurationSection sec = loaded.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            try {
                Parcel p = readParcel(key, sec);
                if (p != null) {
                    contentCache.put(p.tracking(), sec.getString("contents", ""));
                    parcels.put(p.tracking(), p);
                }
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Не удалось прочитать посылку " + key, e);
            }
        }
        plugin.getLogger().info("Загружено посылок: " + parcels.size());
    }

    @Override
    public void put(Parcel parcel) {
        // Сначала публикуем содержимое, затем сам объект: асинхронный сейв,
        // увидев посылку в parcels, гарантированно увидит и её contentCache.
        contentCache.put(parcel.tracking(), serializeContents(parcel.contents()));
        parcels.put(parcel.tracking(), parcel);
        scheduleSave();
    }

    @Override
    public void remove(String tracking) {
        parcels.remove(tracking);
        contentCache.remove(tracking);
        scheduleSave();
    }

    @Override
    public Parcel get(String tracking) {
        return parcels.get(tracking);
    }

    @Override
    public boolean exists(String tracking) {
        return parcels.containsKey(tracking);
    }

    @Override
    public Collection<Parcel> all() {
        return new ArrayList<>(parcels.values());
    }

    @Override
    public List<Parcel> bySender(UUID sender) {
        List<Parcel> out = new ArrayList<>();
        for (Parcel p : parcels.values()) {
            if (p.sender().equals(sender)) {
                out.add(p);
            }
        }
        return out;
    }

    @Override
    public List<Parcel> incomingFor(UUID player) {
        List<Parcel> out = new ArrayList<>();
        for (Parcel p : parcels.values()) {
            if (p.collected()) {
                continue;
            }
            boolean incoming = p.recipient().equals(player) && p.status() == ParcelStatus.ARRIVED;
            boolean returned = p.sender().equals(player) && p.status() == ParcelStatus.RETURNED;
            if (incoming || returned) {
                out.add(p);
            }
        }
        return out;
    }

    @Override
    public List<Parcel> activeFor(UUID player) {
        List<Parcel> out = new ArrayList<>();
        for (Parcel p : parcels.values()) {
            boolean mine = p.sender().equals(player) || p.recipient().equals(player);
            if (mine && !(p.status().isTerminal() && p.collected())) {
                out.add(p);
            }
        }
        return out;
    }

    @Override
    public List<Parcel> inDelivery() {
        List<Parcel> out = new ArrayList<>();
        for (Parcel p : parcels.values()) {
            if (p.status().inDelivery()) {
                out.add(p);
            }
        }
        return out;
    }

    @Override
    public void flush() {
        saveScheduled.set(false);
        writeFile(buildYaml());
    }

    @Override
    public void purgeTerminalBefore(long cutoffMillis) {
        int removed = 0;
        for (Parcel p : parcels.values()) {
            if (p.status().isTerminal() && p.updatedAt() < cutoffMillis) {
                parcels.remove(p.tracking());
                contentCache.remove(p.tracking());
                removed++;
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Удалено старых посылок: " + removed);
            scheduleSave();
        }
    }

    // ---- internals ----

    private void scheduleSave() {
        if (saveScheduled.compareAndSet(false, true)) {
            schedulers.asyncLater(() -> {
                saveScheduled.set(false);
                writeFile(buildYaml());
            }, 2000L);
        }
    }

    private String buildYaml() {
        YamlConfiguration out = new YamlConfiguration();
        for (Parcel p : parcels.values()) {
            String t = p.tracking();
            out.set(t + ".sender", p.sender().toString());
            out.set(t + ".senderName", p.senderName());
            out.set(t + ".recipient", p.recipient().toString());
            out.set(t + ".recipientName", p.recipientName());
            out.set(t + ".contents", contentCache.getOrDefault(t, ""));
            out.set(t + ".status", p.status().name());
            out.set(t + ".createdAt", p.createdAt());
            out.set(t + ".updatedAt", p.updatedAt());
            out.set(t + ".deliverAt", p.deliverAt());
            out.set(t + ".note", p.note());
            out.set(t + ".originName", p.originName());
            out.set(t + ".destName", p.destName());
            out.set(t + ".collected", p.collected());
            writeLoc(out, t + ".origin", p.origin());
            writeLoc(out, t + ".destination", p.destination());
        }
        return out.saveToString();
    }

    private void writeFile(String yaml) {
        synchronized (writeLock) {
            try {
                File dir = plugin.getDataFolder();
                if (!dir.exists() && !dir.mkdirs()) {
                    plugin.getLogger().warning("Не удалось создать папку данных плагина");
                }
                File tmp = new File(dir, "parcels.yml.tmp");
                Files.write(tmp.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
                Files.move(tmp.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                // Некоторые ФС не поддерживают ATOMIC_MOVE — пробуем обычную запись.
                try {
                    Files.write(file.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить parcels.yml", e);
                }
            }
        }
    }

    private Parcel readParcel(String tracking, ConfigurationSection sec) {
        UUID sender = UUID.fromString(sec.getString("sender", ""));
        UUID recipient = UUID.fromString(sec.getString("recipient", ""));
        String senderName = sec.getString("senderName", "?");
        String recipientName = sec.getString("recipientName", "?");
        List<ItemStack> contents = deserializeContents(sec.getString("contents", ""));
        long createdAt = sec.getLong("createdAt", System.currentTimeMillis());

        Parcel p = new Parcel(tracking, sender, senderName, recipient, recipientName, contents, createdAt);
        try {
            p.status(ParcelStatus.valueOf(sec.getString("status", "CREATED")));
        } catch (IllegalArgumentException ignored) {
            p.status(ParcelStatus.CREATED);
        }
        p.updatedAt(sec.getLong("updatedAt", createdAt));
        p.deliverAt(sec.getLong("deliverAt", 0L));
        p.note(sec.getString("note", ""));
        p.originName(sec.getString("originName", ""));
        p.destName(sec.getString("destName", ""));
        p.collected(sec.getBoolean("collected", false));
        p.origin(readLoc(sec, "origin"));
        p.destination(readLoc(sec, "destination"));
        return p;
    }

    private static String serializeContents(List<ItemStack> contents) {
        List<ItemStack> clean = new ArrayList<>();
        for (ItemStack it : contents) {
            if (it != null && !it.getType().isAir()) {
                clean.add(it);
            }
        }
        if (clean.isEmpty()) {
            return "";
        }
        byte[] bytes = ItemStack.serializeItemsAsBytes(clean.toArray(new ItemStack[0]));
        return Base64.getEncoder().encodeToString(bytes);
    }

    private List<ItemStack> deserializeContents(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            ItemStack[] items = ItemStack.deserializeItemsFromBytes(bytes);
            List<ItemStack> list = new ArrayList<>();
            for (ItemStack it : items) {
                if (it != null) {
                    list.add(it);
                }
            }
            return list;
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось десериализовать содержимое посылки", e);
            return new ArrayList<>();
        }
    }

    private static void writeLoc(YamlConfiguration out, String path, Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        out.set(path + ".world", loc.getWorld().getUID().toString());
        out.set(path + ".x", loc.getX());
        out.set(path + ".y", loc.getY());
        out.set(path + ".z", loc.getZ());
        out.set(path + ".yaw", loc.getYaw());
        out.set(path + ".pitch", loc.getPitch());
    }

    private static Location readLoc(ConfigurationSection sec, String path) {
        String worldId = sec.getString(path + ".world");
        if (worldId == null) {
            return null;
        }
        World world;
        try {
            world = Bukkit.getWorld(UUID.fromString(worldId));
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (world == null) {
            return null;
        }
        return new Location(world,
                sec.getDouble(path + ".x"),
                sec.getDouble(path + ".y"),
                sec.getDouble(path + ".z"),
                (float) sec.getDouble(path + ".yaw"),
                (float) sec.getDouble(path + ".pitch"));
    }
}
