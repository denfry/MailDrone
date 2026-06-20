package ru.maildrone.core.delivery;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.maildrone.core.config.MailConfig;
import ru.maildrone.core.config.Messages;
import ru.maildrone.core.economy.VaultEconomy;

/**
 * Оплата отправки: бесплатно / уровни опыта / предметы / деньги (Vault).
 * Если выбран VAULT, но экономика недоступна — отправка становится бесплатной.
 */
public final class CostService {

    private final MailConfig config;
    private final Messages messages;
    private final VaultEconomy vault;

    public CostService(MailConfig config, Messages messages, VaultEconomy vault) {
        this.config = config;
        this.messages = messages;
        this.vault = vault;
    }

    public boolean canAfford(Player player) {
        switch (config.costType()) {
            case NONE:
                return true;
            case XP_LEVELS:
                return player.getLevel() >= config.costAmount();
            case ITEM:
                return config.costAmount() == 0
                        || player.getInventory().containsAtLeast(new ItemStack(config.costItem()), config.costAmount());
            case VAULT:
                return !vault.isAvailable() || vault.has(player, config.costAmount());
            default:
                return true;
        }
    }

    /** Списывает стоимость. Возвращает {@code false}, если средств не хватило. */
    public boolean charge(Player player) {
        if (!canAfford(player)) {
            return false;
        }
        switch (config.costType()) {
            case XP_LEVELS:
                if (config.costAmount() > 0) {
                    player.setLevel(Math.max(0, player.getLevel() - config.costAmount()));
                }
                return true;
            case ITEM:
                if (config.costAmount() > 0) {
                    player.getInventory().removeItem(new ItemStack(config.costItem(), config.costAmount()));
                }
                return true;
            case VAULT:
                return !vault.isAvailable() || vault.withdraw(player, config.costAmount());
            case NONE:
            default:
                return true;
        }
    }

    /** Краткое описание стоимости для кнопки/сообщений. */
    public Component describe() {
        switch (config.costType()) {
            case XP_LEVELS:
                return messages.get("cost-xp", Messages.ph("amount", String.valueOf(config.costAmount())));
            case ITEM:
                return messages.get("cost-item",
                        Messages.ph("amount", String.valueOf(config.costAmount())),
                        Messages.ph("item", itemName()));
            case VAULT:
                if (vault.isAvailable()) {
                    return messages.get("cost-money", Messages.ph("amount", String.valueOf(config.costAmount())));
                }
                return messages.get("cost-free");
            case NONE:
            default:
                return messages.get("cost-free");
        }
    }

    public Component cannotAffordMessage() {
        switch (config.costType()) {
            case XP_LEVELS:
                return messages.msg("cannot-afford-xp", Messages.ph("amount", String.valueOf(config.costAmount())));
            case ITEM:
                return messages.msg("cannot-afford-item",
                        Messages.ph("amount", String.valueOf(config.costAmount())),
                        Messages.ph("item", itemName()));
            case VAULT:
                return messages.msg("cannot-afford-money", Messages.ph("amount", String.valueOf(config.costAmount())));
            case NONE:
            default:
                return Component.empty();
        }
    }

    private String itemName() {
        return config.costItem().name().toLowerCase(java.util.Locale.ROOT);
    }
}
