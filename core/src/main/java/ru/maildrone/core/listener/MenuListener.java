package ru.maildrone.core.listener;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import ru.maildrone.core.MailDrone;
import ru.maildrone.core.config.Messages;
import ru.maildrone.core.gui.AnvilNamePrompt;
import ru.maildrone.core.gui.MailboxMenu;
import ru.maildrone.core.gui.PackingMenu;
import ru.maildrone.core.parcel.Parcel;
import ru.maildrone.core.parcel.ParcelStatus;

import java.util.List;

/** Обработка кликов/перетаскивания/закрытия в меню упаковки и почтомата. */
public final class MenuListener implements Listener {

    private final MailDrone mail;

    public MenuListener(MailDrone mail) {
        this.mail = mail;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof PackingMenu pm) {
            handlePacking(event, pm);
        } else if (holder instanceof MailboxMenu mm) {
            handleMailbox(event, mm);
        } else if (holder instanceof AnvilNamePrompt) {
            handleAnvil(event);
        }
    }

    private void handleAnvil(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() != AnvilNamePrompt.RESULT_SLOT) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String name = "";
        if (event.getView() instanceof AnvilView anvil && anvil.getRenameText() != null) {
            name = anvil.getRenameText().trim();
        }
        if (name.isBlank()) {
            return;
        }
        final String target = name;
        mail.schedulers().onEntity(player, () -> {
            player.closeInventory();
            mail.startSendFlow(player, target);
        }, null);
    }

    private void handlePacking(InventoryClickEvent event, PackingMenu pm) {
        if (pm.submitted()) {
            // После подтверждения меню заблокировано — никаких перемещений предметов.
            event.setCancelled(true);
            return;
        }
        int raw = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (raw < 0 || raw >= topSize) {
            // Клик в инвентаре игрока: разрешаем (служебный ряд занят, утечки нет).
            return;
        }
        if (!pm.isControlSlot(raw)) {
            // Слот области упаковки — разрешаем класть/забирать предметы.
            return;
        }
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (raw == pm.sendSlot()) {
            finalizeSend(player, pm);
        } else if (raw == pm.cancelSlot()) {
            mail.schedulers().onEntity(player, player::closeInventory, null);
        }
    }

    private void finalizeSend(Player player, PackingMenu pm) {
        if (pm.submitted()) {
            return;
        }
        List<ItemStack> contents = pm.collectContents();
        if (contents.isEmpty()) {
            player.sendMessage(mail.messages().msg("packing-empty"));
            return;
        }
        if (!mail.cost().canAfford(player)) {
            player.sendMessage(mail.cost().cannotAffordMessage());
            return;
        }
        // Снимок снят — забираем реальные предметы из меню СИНХРОННО (в этом же тике)
        // и блокируем меню (submitted), чтобы предметы нельзя было вытащить до отправки.
        pm.markSubmitted();
        pm.clearItemArea();
        mail.schedulers().onEntity(player, () -> {
            if (!mail.cost().charge(player)) {
                returnItems(player, contents);
                player.closeInventory();
                player.sendMessage(mail.cost().cannotAffordMessage());
                return;
            }
            player.closeInventory();
            String tracking = mail.delivery().createAndSend(player, pm.recipient(), pm.recipientName(), contents);
            player.sendMessage(mail.messages().msg("sent", Messages.ph("tracking", tracking)));
        }, null);
    }

    private void handleMailbox(InventoryClickEvent event, MailboxMenu mm) {
        event.setCancelled(true);
        int raw = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (raw < 0 || raw >= topSize) {
            return;
        }
        String tracking = mm.trackingAt(raw);
        if (tracking == null) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Parcel parcel = mail.store().get(tracking);
        if (parcel == null || parcel.status() != ParcelStatus.ARRIVED || parcel.collected()
                || !parcel.recipient().equals(player.getUniqueId())) {
            return;
        }
        mail.schedulers().onEntity(player, () -> {
            if (mail.delivery().collect(player, parcel)) {
                player.sendMessage(mail.messages().msg("collected", Messages.ph("tracking", tracking)));
                player.closeInventory();
                if (!mail.store().incomingFor(player.getUniqueId()).isEmpty()) {
                    mail.openMailbox(player);
                }
            }
        }, null);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        int topSize = event.getView().getTopInventory().getSize();
        if (holder instanceof PackingMenu pm) {
            if (pm.submitted()) {
                event.setCancelled(true);
                return;
            }
            for (int raw : event.getRawSlots()) {
                if (raw < topSize && pm.isControlSlot(raw)) {
                    event.setCancelled(true);
                    return;
                }
            }
        } else if (holder instanceof MailboxMenu || holder instanceof AnvilNamePrompt) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof PackingMenu pm)) {
            return;
        }
        if (pm.submitted()) {
            return;
        }
        HumanEntity human = event.getPlayer();
        if (!(human instanceof Player player)) {
            return;
        }
        List<ItemStack> left = pm.collectContents();
        if (left.isEmpty()) {
            return;
        }
        pm.clearItemArea();
        returnItems(player, left);
        player.sendMessage(mail.messages().msg("packing-cancelled"));
    }

    private void returnItems(Player player, List<ItemStack> items) {
        for (ItemStack it : items) {
            if (it == null || it.getType().isAir()) {
                continue;
            }
            var leftover = player.getInventory().addItem(it);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }
}
