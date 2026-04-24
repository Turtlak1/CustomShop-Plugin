package org.turtle.customShop.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.turtle.customShop.config.MainConfig;

public final class EconomyProvider {

    private EconomyProvider() {
    }

    public static EconomyGateway resolve(JavaPlugin plugin, MainConfig config) {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            EconomyGateway vault = resolveVault(plugin);
            if (vault != null) {
                return vault;
            }
        }
        
        plugin.getLogger().warning("Vault economy provider not found or not registered, using local ledger fallback.");
        return new LocalLedgerEconomy(plugin.getDataFolder(), config);
    }

    private static EconomyGateway resolveVault(JavaPlugin plugin) {
        try {
            RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (registration != null && registration.getProvider() != null) {
                return new VaultEconomyGateway(registration.getProvider());
            }
        } catch (NoClassDefFoundError | Exception exception) {
            plugin.getLogger().warning("Vault bridge initialization exception: " + exception.getMessage());
        }
        return null;
    }
}
