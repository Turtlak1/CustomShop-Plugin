package org.turtle.customShop.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class VaultEconomyGateway implements EconomyGateway {

    private final Economy economy;

    public VaultEconomyGateway(Economy economy) {
        this.economy = economy;
    }

    @Override
    public String getName() {
        return "Vault";
    }

    @Override
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean has(Player player, double amount) {
        return economy.has(player, Math.max(0.0D, amount));
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return economy.withdrawPlayer(player, Math.max(0.0D, amount)).transactionSuccess();
    }

    @Override
    public void deposit(Player player, double amount) {
        economy.depositPlayer(player, Math.max(0.0D, amount));
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }
}
