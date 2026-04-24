package org.turtle.customShop.economy;

import org.bukkit.entity.Player;

public interface EconomyGateway {

    String getName();

    double getBalance(Player player);

    boolean has(Player player, double amount);

    boolean withdraw(Player player, double amount);

    void deposit(Player player, double amount);

    String format(double amount);

    default void save() {
    }
}
