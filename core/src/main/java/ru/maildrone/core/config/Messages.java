package ru.maildrone.core.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.maildrone.core.parcel.ParcelStatus;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Тексты плагина на русском в формате MiniMessage. Значения по умолчанию
 * зашиты в коде; их можно переопределить файлом {@code messages.yml}
 * (плоские строковые ключи).
 */
public final class Messages {

    private final JavaPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, String> templates = new HashMap<>();
    private String prefix;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        templates.clear();
        templates.putAll(defaults());
        File f = new File(plugin.getDataFolder(), "messages.yml");
        if (f.exists()) {
            FileConfiguration yc = YamlConfiguration.loadConfiguration(f);
            for (String key : yc.getKeys(true)) {
                if (yc.isString(key)) {
                    templates.put(key, yc.getString(key));
                }
            }
        }
        prefix = templates.getOrDefault("prefix", "");
    }

    /** Сообщение без префикса. */
    public Component get(String key, TagResolver... resolvers) {
        return mm.deserialize(templates.getOrDefault(key, key), resolvers);
    }

    /** Сообщение с префиксом плагина (для чата). */
    public Component msg(String key, TagResolver... resolvers) {
        return mm.deserialize(prefix + templates.getOrDefault(key, key), resolvers);
    }

    /** Цветной компонент статуса посылки. */
    public Component status(ParcelStatus status) {
        return mm.deserialize(status.colorTag() + status.label());
    }

    /** Плейсхолдер с пользовательским текстом (без интерпретации тегов — безопасно). */
    public static TagResolver ph(String name, String value) {
        return Placeholder.unparsed(name, value == null ? "" : value);
    }

    /** Плейсхолдер с готовым компонентом. */
    public static TagResolver phc(String name, Component value) {
        return Placeholder.component(name, value);
    }

    private static Map<String, String> defaults() {
        Map<String, String> m = new HashMap<>();
        m.put("prefix", "<gold>[</gold><yellow>Почта России</yellow><gold>]</gold> ");

        m.put("only-player", "<red>Команда доступна только игроку.");
        m.put("no-permission", "<red>У вас нет прав на это действие.");

        m.put("usage-send", "<gray>Использование: <white>/post send <ник></white>");
        m.put("usage-track", "<gray>Использование: <white>/post track <трек-номер></white>");
        m.put("player-not-found", "<red>Игрок <white><name></white> не найден.");
        m.put("cant-send-self", "<red>Нельзя отправить посылку самому себе.");
        m.put("need-office", "<red>Рядом нет почтового отделения. Подойдите к отделению, чтобы отправить посылку.");

        m.put("packing-title", "Упаковка посылки для <name>");
        m.put("packing-empty", "<red>Положите в посылку хотя бы один предмет.");
        m.put("packing-cancelled", "<yellow>Отправка отменена, предметы возвращены.");
        m.put("btn-send", "<green><bold>Отправить посылку</bold>");
        m.put("btn-send-lore", "<gray>Получатель: <white><name></white>");
        m.put("btn-send-cost", "<gray>Стоимость: <white><cost></white>");
        m.put("btn-cancel", "<red><bold>Отменить</bold>");
        m.put("btn-cancel-lore", "<gray>Закрыть и забрать предметы");
        m.put("cost-free", "бесплатно");
        m.put("cost-xp", "<amount> ур. опыта");
        m.put("cost-item", "<amount> × <item>");
        m.put("cost-money", "<amount> монет");

        m.put("cannot-afford-xp", "<red>Недостаточно опыта: нужно <white><amount></white> ур.");
        m.put("cannot-afford-item", "<red>Недостаточно предметов: нужно <white><amount> × <item></white>.");
        m.put("cannot-afford-money", "<red>Недостаточно средств: нужно <white><amount> монет</white>.");

        m.put("sent", "<green>Посылка принята! Трек-номер: <white><tracking></white>");
        m.put("drone-departed", "<gray>Дрон вылетел с отделения <white><origin></white>.");
        m.put("recipient-incoming", "<yellow><sender></yellow> <gray>отправил(а) вам посылку <white><tracking></white>. Ожидайте доставку.");
        m.put("arrived-recipient", "<green>Ваша посылка <white><tracking></white> прибыла! Заберите: <white>/post box</white>");
        m.put("arrived-title", "<green>Посылка прибыла");
        m.put("arrived-subtitle", "<white><tracking></white>");
        m.put("join-incoming", "<green>В почтомате вас ждут посылки: <white><count></white>. Откройте: <white>/post box</white>");
        m.put("sender-delivered", "<gray>Посылка <white><tracking></white> вручена получателю.");
        m.put("returned-to-sender", "<yellow>Посылку <white><tracking></white> не забрал(а) <white><recipient></white> — она возвращена вам. Заберите: <white>/post box</white>");

        m.put("track-not-found", "<red>Посылка с номером <white><tracking></white> не найдена.");
        m.put("track-header", "<gold>━━━━━ Отслеживание <white><tracking></white> <gold>━━━━━");
        m.put("track-status", "<gray>Статус: <status>");
        m.put("track-route", "<gray>Маршрут: <white><origin></white> <dark_gray>→</dark_gray> <white><dest></white>");
        m.put("track-parties", "<gray>От: <white><sender></white>  Кому: <white><recipient></white>");
        m.put("track-note", "<gray>Примечание: <white><note></white>");
        m.put("track-eta", "<gray>Ожидаемое прибытие: <white><eta></white>");
        m.put("track-items", "<gray>Вложений: <white><count></white>");

        m.put("list-header", "<gold>━━━━━ Ваши посылки ━━━━━");
        m.put("list-empty", "<gray>У вас нет активных посылок.");
        m.put("list-sent", "<dark_gray>↗</dark_gray> <white><tracking></white> <gray>кому</gray> <white><recipient></white> <dark_gray>—</dark_gray> <status>");
        m.put("list-incoming", "<dark_gray>↘</dark_gray> <white><tracking></white> <gray>от</gray> <white><sender></white> <dark_gray>—</dark_gray> <status>");

        m.put("box-title", "Почтомат — посылки для вас");
        m.put("box-empty", "<gray>Для вас нет посылок в почтомате.");
        m.put("box-icon-name", "<white><tracking></white>");
        m.put("box-icon-lore-from", "<gray>От: <white><sender></white>");
        m.put("box-icon-lore-returned", "<red>Возврат: <white><recipient></white> не забрал(а)");
        m.put("box-icon-lore-items", "<gray>Вложений: <white><count></white>");
        m.put("box-icon-lore-take", "<green>ЛКМ — забрать");
        m.put("collected", "<green>Посылка <white><tracking></white> получена!");
        m.put("inventory-full", "<yellow>Инвентарь полон — часть предметов выпала рядом с вами.");

        m.put("help-header", "<gold>━━━━━ Почта России: команды ━━━━━");
        m.put("help-send", "<yellow>/post send <ник></yellow> <gray>— отправить посылку");
        m.put("help-box", "<yellow>/post box</yellow> <gray>— открыть почтомат");
        m.put("help-track", "<yellow>/post track <номер></yellow> <gray>— отследить посылку");
        m.put("help-list", "<yellow>/post list</yellow> <gray>— список ваших посылок");
        m.put("help-admin", "<yellow>/post admin</yellow> <gray>— команды администратора");

        m.put("admin-usage", "<gray>/post admin <office|postomat|list|reload>");
        m.put("admin-not-looking", "<red>Посмотрите на блок (до 6 блоков), который хотите назначить.");
        m.put("office-added", "<green>Блок назначен отделением: <white><pos></white>");
        m.put("office-removed", "<yellow>Отделение удалено: <white><pos></white>");
        m.put("postomat-added", "<green>Блок назначен почтоматом: <white><pos></white>");
        m.put("postomat-removed", "<yellow>Почтомат удалён: <white><pos></white>");
        m.put("points-header", "<gold>━━━━━ Почтовые точки ━━━━━");
        m.put("points-empty", "<gray>Точки не настроены.");
        m.put("points-office", "<gray>Отделение: <white><pos></white>");
        m.put("points-postomat", "<gray>Почтомат: <white><pos></white>");
        m.put("reloaded", "<green>Конфигурация перезагружена.");

        m.put("office-here", "<yellow>Почтовое отделение.</yellow> <gray>Используйте <white>/post send <ник></white>, чтобы отправить посылку.");
        m.put("postomat-here", "<yellow>Почтомат.</yellow> <gray>Открываю ваши посылки...");
        m.put("anvil-title", "Кому отправить?");
        m.put("anvil-input-name", "Введите ник получателя");
        return m;
    }
}
