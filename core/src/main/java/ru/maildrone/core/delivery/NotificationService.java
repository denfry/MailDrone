package ru.maildrone.core.delivery;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.maildrone.core.config.MailConfig;
import ru.maildrone.core.config.Messages;
import ru.maildrone.core.parcel.Parcel;
import ru.maildrone.core.scheduler.Schedulers;
import ru.maildrone.core.storage.ParcelStore;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Уведомления игрокам о событиях посылок. Все обращения к игроку выполняются
 * на его региональном потоке (Folia-безопасно).
 */
public final class NotificationService {

    private static final Sound ARRIVE_SOUND =
            Sound.sound(Key.key("minecraft:block.note_block.bell"), Sound.Source.MASTER, 1.0f, 1.4f);
    private static final Sound DEPART_SOUND =
            Sound.sound(Key.key("minecraft:entity.bee.loop"), Sound.Source.MASTER, 0.7f, 1.0f);

    private final Messages messages;
    private final MailConfig config;
    private final Schedulers schedulers;
    private final ParcelStore store;

    public NotificationService(Messages messages, MailConfig config, Schedulers schedulers, ParcelStore store) {
        this.messages = messages;
        this.config = config;
        this.schedulers = schedulers;
        this.store = store;
    }

    public void announceIncoming(Parcel parcel) {
        Player r = Bukkit.getPlayer(parcel.recipient());
        if (r == null) {
            return;
        }
        schedulers.onEntity(r, () -> r.sendMessage(
                messages.msg("recipient-incoming",
                        Messages.ph("sender", parcel.senderName()),
                        Messages.ph("tracking", parcel.tracking()))), null);
    }

    public void announceDeparture(Parcel parcel) {
        Player s = Bukkit.getPlayer(parcel.sender());
        if (s == null) {
            return;
        }
        schedulers.onEntity(s, () -> {
            s.sendMessage(messages.msg("drone-departed", Messages.ph("origin", parcel.originName())));
            if (config.sounds()) {
                s.playSound(DEPART_SOUND);
            }
        }, null);
    }

    public void announceArrival(Parcel parcel) {
        Player r = Bukkit.getPlayer(parcel.recipient());
        if (r == null) {
            return;
        }
        schedulers.onEntity(r, () -> {
            r.sendMessage(messages.msg("arrived-recipient", Messages.ph("tracking", parcel.tracking())));
            r.showTitle(Title.title(
                    messages.get("arrived-title"),
                    messages.get("arrived-subtitle", Messages.ph("tracking", parcel.tracking())),
                    Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(2500), Duration.ofMillis(800))));
            if (config.sounds()) {
                r.playSound(ARRIVE_SOUND);
            }
        }, null);
    }

    public void announceDelivered(Parcel parcel) {
        Player s = Bukkit.getPlayer(parcel.sender());
        if (s == null) {
            return;
        }
        schedulers.onEntity(s, () -> s.sendMessage(
                messages.msg("sender-delivered", Messages.ph("tracking", parcel.tracking()))), null);
    }

    public void onJoin(Player player) {
        if (!config.notifyOnJoin()) {
            return;
        }
        UUID id = player.getUniqueId();
        List<Parcel> incoming = store.incomingFor(id);
        if (incoming.isEmpty()) {
            return;
        }
        schedulers.onEntity(player, () -> player.sendMessage(
                messages.msg("join-incoming", Messages.ph("count", String.valueOf(incoming.size())))), null);
    }
}
