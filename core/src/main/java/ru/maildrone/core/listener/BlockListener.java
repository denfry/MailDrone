package ru.maildrone.core.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import ru.maildrone.core.MailDrone;

/** ПКМ по блоку-отделению/почтомату. */
public final class BlockListener implements Listener {

    private final MailDrone mail;

    public BlockListener(MailDrone mail) {
        this.mail = mail;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Location loc = block.getLocation();
        Player player = event.getPlayer();
        if (mail.postPoints().isPostomat(loc)) {
            event.setCancelled(true);
            player.sendMessage(mail.messages().msg("postomat-here"));
            mail.openMailbox(player);
        } else if (mail.postPoints().isOffice(loc)) {
            event.setCancelled(true);
            player.sendMessage(mail.messages().msg("office-here"));
            mail.openRecipientPrompt(player);
        }
    }
}
