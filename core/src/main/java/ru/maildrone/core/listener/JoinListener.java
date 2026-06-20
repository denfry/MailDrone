package ru.maildrone.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.maildrone.core.MailDrone;

/** Уведомление о посылках при входе. */
public final class JoinListener implements Listener {

    private final MailDrone mail;

    public JoinListener(MailDrone mail) {
        this.mail = mail;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        mail.notifications().onJoin(event.getPlayer());
    }
}
