package org.turtle.customShop.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class TransactionInventoryHolder implements InventoryHolder {
    private Inventory inventory;
    private final String shopId;
    private final String itemId;

    public TransactionInventoryHolder(String shopId, String itemId) {
        this.shopId = shopId;
        this.itemId = itemId;
    }

    public String getShopId() {
        return shopId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
