package ru.maildrone.core.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/**
 * Мост к экономике Vault через рефлексию — без компайл-зависимости от Vault.
 * Если Vault или эконом-плагин отсутствуют, {@link #isAvailable()} вернёт false,
 * и оплата деньгами тихо отключается (см. {@code CostService}).
 */
public final class VaultEconomy {

    private Object economy;
    private Method hasMethod;
    private Method withdrawMethod;
    private Method successMethod;
    private boolean available;

    public VaultEconomy() {
        setup();
    }

    private void setup() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                return;
            }
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            if (rsp == null || rsp.getProvider() == null) {
                return;
            }
            economy = rsp.getProvider();
            hasMethod = economyClass.getMethod("has", OfflinePlayer.class, double.class);
            withdrawMethod = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            successMethod = Class.forName("net.milkbowl.vault.economy.EconomyResponse")
                    .getMethod("transactionSuccess");
            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!available) {
            return false;
        }
        try {
            return (Boolean) hasMethod.invoke(economy, player, amount);
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!available) {
            return false;
        }
        try {
            Object response = withdrawMethod.invoke(economy, player, amount);
            return (Boolean) successMethod.invoke(response);
        } catch (Throwable t) {
            return false;
        }
    }
}
