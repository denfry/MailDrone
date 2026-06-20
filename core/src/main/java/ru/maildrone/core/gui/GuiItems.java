package ru.maildrone.core.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Фабрика декоративных предметов для меню. */
public final class GuiItems {

    private GuiItems() {
    }

    public static ItemStack icon(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            if (lore != null && !lore.isEmpty()) {
                List<Component> clean = new ArrayList<>(lore.size());
                for (Component c : lore) {
                    clean.add(c.decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(clean);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack filler(Material material) {
        return icon(material, Component.empty(), null);
    }
}
