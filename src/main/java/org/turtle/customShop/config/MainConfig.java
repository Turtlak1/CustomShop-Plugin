package org.turtle.customShop.config;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public record MainConfig(
        String currencySymbol,
        String currencyName,
        int decimalPlaces,
        List<String> shops,
        boolean restockingEnabled,
        long globalRestockIntervalSeconds,
        boolean restockAutoStart,
        String restockBroadcastMessage,
        boolean inflationEnabled,
        double defaultBuyMultiplier,
        double defaultSellMultiplier,
        boolean trackPerShopInflation,
        boolean dynamicPricingEnabled,
        long decreaseIntervalSeconds,
        Map<String, String> messages,
        double localLedgerStartingBalance,
        MainMenuConfig mainMenu
) {
    public record MainMenuConfig(
            int rows,
            String title,
            Map<Integer, MainMenuItem> items
    ) {}

    public record MainMenuItem(
            String shopId,
            Material material,
            String name,
            List<String> lore
    ) {}
}
