package org.turtle.customShop.shop;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public record ShopDefinition(
        String id,
        String name,
        String description,
        String type,
        String owner,
        MenuConfig menu,
        Map<String, ShopItem> items
) {
    public record MenuConfig(String type, int rows, String title) {
    }

    public record ShopItem(
            String id,
            int slot,
            Material material,
            String displayName,
            List<String> lore,
            double buyPrice,
            double sellPrice,
            int initialStock,
            int maxStack,
            String entityType,
            boolean canBuy,
            boolean canSell
    ) {
    }
}
