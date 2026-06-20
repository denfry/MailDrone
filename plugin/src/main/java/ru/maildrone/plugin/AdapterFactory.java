package ru.maildrone.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.maildrone.adapter.PlatformAdapter;
import ru.maildrone.v1_21.V1_21Adapter;
import ru.maildrone.v26.V26Adapter;

/**
 * Выбирает версионный адаптер по версии сервера. Классы адаптеров грузятся
 * лениво: на сервере 1.21.x код {@code new V26Adapter()} не выполняется, и
 * наоборот — поэтому несовместимых обращений к API не возникает.
 */
final class AdapterFactory {

    private AdapterFactory() {
    }

    static PlatformAdapter create(JavaPlugin plugin) {
        String version = serverVersion();
        int major = majorOf(version);
        PlatformAdapter adapter = major >= 26 ? new V26Adapter() : new V1_21Adapter();
        plugin.getLogger().info("Версия сервера: " + version + " → адаптер " + adapter.name());
        return adapter;
    }

    private static String serverVersion() {
        try {
            return Bukkit.getServer().getMinecraftVersion();
        } catch (Throwable t) {
            // На всякий случай: вытащим из getBukkitVersion(), напр. "1.21.11-R0.1-SNAPSHOT".
            String bv = Bukkit.getBukkitVersion();
            int dash = bv.indexOf('-');
            return dash > 0 ? bv.substring(0, dash) : bv;
        }
    }

    private static int majorOf(String version) {
        try {
            String first = version.split("\\.")[0];
            return Integer.parseInt(first);
        } catch (RuntimeException e) {
            // Неизвестный формат — берём безопасный baseline 1.21.x.
            return 1;
        }
    }
}
