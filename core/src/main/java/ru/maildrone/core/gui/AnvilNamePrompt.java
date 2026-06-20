package ru.maildrone.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.maildrone.core.config.Messages;

/**
 * Меню-наковальня для ввода ника получателя у блока-отделения. Игрок печатает
 * ник в поле переименования; результат читается из {@code AnvilInventory}.
 */
public final class AnvilNamePrompt implements InventoryHolder {

    /** Слот результата в наковальне. */
    public static final int RESULT_SLOT = 2;

    private final Inventory inventory;

    public AnvilNamePrompt(Messages messages) {
        this.inventory = Bukkit.createInventory(this, InventoryType.ANVIL, messages.get("anvil-title"));
        ItemStack paper = GuiItems.icon(Material.PAPER, messages.get("anvil-input-name"), null);
        inventory.setItem(0, paper);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
