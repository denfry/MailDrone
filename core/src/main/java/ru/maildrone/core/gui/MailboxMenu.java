package ru.maildrone.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.maildrone.core.config.Messages;
import ru.maildrone.core.parcel.Parcel;
import ru.maildrone.core.parcel.ParcelStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.List.of;

/**
 * Почтомат: список прибывших посылок получателя. Клик по иконке — забрать.
 * Все клики по этому меню отменяются (это витрина, а не хранилище).
 */
public final class MailboxMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, String> slotToTracking = new HashMap<>();

    public MailboxMenu(Messages messages, List<Parcel> parcels) {
        int count = Math.min(parcels.size(), 54);
        int size = Math.max(9, Math.min(54, ((count + 8) / 9) * 9));
        this.inventory = Bukkit.createInventory(this, size, messages.get("box-title"));
        for (int i = 0; i < count; i++) {
            Parcel p = parcels.get(i);
            ItemStack icon;
            if (p.status() == ParcelStatus.RETURNED) {
                // Возврат отправителю: получатель не забрал посылку.
                icon = GuiItems.icon(Material.PAPER,
                        messages.get("box-icon-name", Messages.ph("tracking", p.tracking())),
                        of(messages.get("box-icon-lore-returned", Messages.ph("recipient", p.recipientName())),
                                messages.get("box-icon-lore-items", Messages.ph("count", String.valueOf(p.itemCount()))),
                                messages.get("box-icon-lore-take")));
            } else {
                icon = GuiItems.icon(Material.PAPER,
                        messages.get("box-icon-name", Messages.ph("tracking", p.tracking())),
                        of(messages.get("box-icon-lore-from", Messages.ph("sender", p.senderName())),
                                messages.get("box-icon-lore-items", Messages.ph("count", String.valueOf(p.itemCount()))),
                                messages.get("box-icon-lore-take")));
            }
            inventory.setItem(i, icon);
            slotToTracking.put(i, p.tracking());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Трек-номер посылки в слоте или {@code null}. */
    public String trackingAt(int slot) {
        return slotToTracking.get(slot);
    }
}
