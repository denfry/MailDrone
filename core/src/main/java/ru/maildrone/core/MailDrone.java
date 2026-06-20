package ru.maildrone.core;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.maildrone.adapter.PlatformAdapter;
import ru.maildrone.core.block.PostPointManager;
import ru.maildrone.core.command.PostCommand;
import ru.maildrone.core.config.MailConfig;
import ru.maildrone.core.config.Messages;
import ru.maildrone.core.delivery.CostService;
import ru.maildrone.core.delivery.DeliveryManager;
import ru.maildrone.core.delivery.NotificationService;
import ru.maildrone.core.gui.MailboxMenu;
import ru.maildrone.core.gui.PackingMenu;
import ru.maildrone.core.listener.BlockListener;
import ru.maildrone.core.listener.JoinListener;
import ru.maildrone.core.listener.MenuListener;
import ru.maildrone.core.parcel.Parcel;
import ru.maildrone.core.scheduler.Schedulers;
import ru.maildrone.core.storage.ParcelStore;
import ru.maildrone.core.storage.YamlParcelStore;

import java.util.List;
import java.util.UUID;

/**
 * Центральный объект плагина: создаёт и связывает все сервисы, регистрирует
 * слушатели и команду. Создаётся загрузчиком в модуле {@code plugin} с уже
 * выбранным {@link PlatformAdapter}.
 */
public final class MailDrone {

    private final JavaPlugin plugin;
    private final PlatformAdapter adapter;
    private final Schedulers schedulers;

    private MailConfig config;
    private Messages messages;
    private ParcelStore store;
    private PostPointManager postPoints;
    private CostService cost;
    private NotificationService notifications;
    private DeliveryManager delivery;

    public MailDrone(JavaPlugin plugin, PlatformAdapter adapter) {
        this.plugin = plugin;
        this.adapter = adapter;
        this.schedulers = new Schedulers(plugin);
    }

    public void enable() {
        plugin.saveDefaultConfig();
        config = new MailConfig(plugin);
        messages = new Messages(plugin);

        store = new YamlParcelStore(plugin, schedulers);
        store.load();
        int retentionDays = config.deliveredRetentionDays();
        if (retentionDays > 0) {
            store.purgeTerminalBefore(System.currentTimeMillis() - retentionDays * 86_400_000L);
        }

        postPoints = new PostPointManager(plugin, schedulers);
        postPoints.load();

        cost = new CostService(config, messages);
        notifications = new NotificationService(messages, config, schedulers, store);
        delivery = new DeliveryManager(config, messages, schedulers, store, adapter, notifications, postPoints);

        Bukkit.getPluginManager().registerEvents(new MenuListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), plugin);

        PluginCommand command = plugin.getCommand("post");
        if (command != null) {
            PostCommand handler = new PostCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        } else {
            plugin.getLogger().warning("Команда /post не объявлена в plugin.yml");
        }

        delivery.resumeAll();

        plugin.getLogger().info("MailDrone включён. Адаптер: " + adapter.name()
                + (schedulers.isFolia() ? ", режим Folia" : ", режим Paper"));
    }

    public void disable() {
        rescueOpenPackingMenus();
        try {
            if (delivery != null) {
                delivery.shutdown();
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Ошибка при остановке доставок: " + t.getMessage());
        }
        if (store != null) {
            store.flush();
        }
        if (postPoints != null) {
            postPoints.flush();
        }
    }

    /** При выключении возвращает игрокам предметы из открытых меню упаковки. */
    private void rescueOpenPackingMenus() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                if (!(p.getOpenInventory().getTopInventory().getHolder() instanceof PackingMenu pm) || pm.submitted()) {
                    continue;
                }
                List<ItemStack> left = pm.collectContents();
                pm.markSubmitted();
                pm.clearItemArea();
                for (ItemStack it : left) {
                    if (it == null || it.getType().isAir()) {
                        continue;
                    }
                    var leftover = p.getInventory().addItem(it);
                    for (ItemStack drop : leftover.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), drop);
                    }
                }
                p.closeInventory();
            } catch (Throwable ignored) {
                // best-effort на выключении
            }
        }
    }

    public void reload() {
        if (config != null) {
            config.reload();
        }
        if (messages != null) {
            messages.reload();
        }
    }

    /** Открывает игроку меню упаковки (на его региональном потоке). */
    public void openPacking(Player sender, UUID recipientId, String recipientName) {
        PackingMenu menu = new PackingMenu(config, messages, cost, sender.getUniqueId(), recipientId, recipientName);
        schedulers.onEntity(sender, () -> sender.openInventory(menu.getInventory()), null);
    }

    /** Открывает игроку почтомат с его прибывшими посылками. */
    public void openMailbox(Player player) {
        List<Parcel> incoming = store.incomingFor(player.getUniqueId());
        if (incoming.isEmpty()) {
            player.sendMessage(messages.msg("box-empty"));
            return;
        }
        MailboxMenu menu = new MailboxMenu(messages, incoming);
        schedulers.onEntity(player, () -> player.openInventory(menu.getInventory()), null);
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public PlatformAdapter adapter() {
        return adapter;
    }

    public Schedulers schedulers() {
        return schedulers;
    }

    public MailConfig config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }

    public ParcelStore store() {
        return store;
    }

    public PostPointManager postPoints() {
        return postPoints;
    }

    public CostService cost() {
        return cost;
    }

    public NotificationService notifications() {
        return notifications;
    }

    public DeliveryManager delivery() {
        return delivery;
    }
}
