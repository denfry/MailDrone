package ru.maildrone.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.maildrone.core.config.MailConfig;
import ru.maildrone.core.config.Messages;
import ru.maildrone.core.delivery.CostService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.List.of;

/**
 * Меню упаковки посылки: верхняя область — свободные слоты под предметы,
 * нижний ряд — кнопки «Отправить»/«Отменить» и информация. Слоты-кнопки заняты
 * предметами, поэтому shift-click из инвентаря не «протекает» в служебный ряд.
 */
public final class PackingMenu implements InventoryHolder {

    private final UUID sender;
    private final UUID recipient;
    private final String recipientName;
    private final int controlStart;
    private final int size;
    private final int sendSlot;
    private final int cancelSlot;
    private final Inventory inventory;

    private boolean submitted;

    public PackingMenu(MailConfig config, Messages messages, CostService cost,
                       UUID sender, UUID recipient, String recipientName) {
        this.sender = sender;
        this.recipient = recipient;
        this.recipientName = recipientName;

        int rows = config.packingRows();
        this.controlStart = rows * 9;
        this.size = controlStart + 9;
        this.cancelSlot = controlStart;
        this.sendSlot = controlStart + 8;

        this.inventory = Bukkit.createInventory(this, size,
                messages.get("packing-title", Messages.ph("name", recipientName)));

        // Служебный ряд: серый филлер + кнопки.
        ItemStack filler = GuiItems.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = controlStart; i < size; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(sendSlot, GuiItems.icon(Material.LIME_CONCRETE,
                messages.get("btn-send"),
                of(messages.get("btn-send-lore", Messages.ph("name", recipientName)),
                        messages.get("btn-send-cost", Messages.phc("cost", cost.describe())))));
        inventory.setItem(cancelSlot, GuiItems.icon(Material.RED_CONCRETE,
                messages.get("btn-cancel"),
                of(messages.get("btn-cancel-lore"))));
        inventory.setItem(controlStart + 4, GuiItems.icon(Material.PAPER,
                messages.get("box-icon-name", Messages.ph("tracking", "—")),
                of(messages.get("btn-send-lore", Messages.ph("name", recipientName)))));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID sender() {
        return sender;
    }

    public UUID recipient() {
        return recipient;
    }

    public String recipientName() {
        return recipientName;
    }

    public int sendSlot() {
        return sendSlot;
    }

    public int cancelSlot() {
        return cancelSlot;
    }

    public boolean submitted() {
        return submitted;
    }

    public void markSubmitted() {
        this.submitted = true;
    }

    public boolean isItemSlot(int slot) {
        return slot >= 0 && slot < controlStart;
    }

    public boolean isControlSlot(int slot) {
        return slot >= controlStart && slot < size;
    }

    /** Предметы из области упаковки. */
    public List<ItemStack> collectContents() {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < controlStart; i++) {
            ItemStack it = inventory.getItem(i);
            if (it != null && !it.getType().isAir()) {
                out.add(it.clone());
            }
        }
        return out;
    }

    public void clearItemArea() {
        for (int i = 0; i < controlStart; i++) {
            inventory.setItem(i, null);
        }
    }
}
