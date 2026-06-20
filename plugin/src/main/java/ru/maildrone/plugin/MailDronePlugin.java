package ru.maildrone.plugin;

import org.bukkit.plugin.java.JavaPlugin;
import ru.maildrone.adapter.PlatformAdapter;
import ru.maildrone.core.MailDrone;

/**
 * Точка входа плагина. Вся логика — в модуле {@code core}; здесь только выбор
 * адаптера и проброс жизненного цикла.
 */
public final class MailDronePlugin extends JavaPlugin {

    private MailDrone mailDrone;

    @Override
    public void onEnable() {
        PlatformAdapter adapter = AdapterFactory.create(this);
        mailDrone = new MailDrone(this, adapter);
        mailDrone.enable();
        setupMetrics();
    }

    private void setupMetrics() {
        int id = getConfig().getInt("metrics.plugin-id", 0);
        if (getConfig().getBoolean("metrics.enabled", true) && id > 0) {
            new org.bstats.bukkit.Metrics(this, id);
            getLogger().info("bStats-метрика включена (id " + id + ")");
        }
    }

    @Override
    public void onDisable() {
        if (mailDrone != null) {
            mailDrone.disable();
            mailDrone = null;
        }
    }
}
