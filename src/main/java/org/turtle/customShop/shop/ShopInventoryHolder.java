package org.turtle.customShop.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

public class ShopInventoryHolder implements InventoryHolder {

    private final String shopId;
    private final int page;
    private final Map<Integer, String> slotToItemId;
    private Inventory inventory;

    public ShopInventoryHolder(String shopId, int page, Map<Integer, String> slotToItemId) {
        this.shopId = shopId;
        this.page = page;
        this.slotToItemId = slotToItemId;
    }

    public String getShopId() {
        return shopId;
    }

    public int getPage() {
        return page;
    }

    public String getItemIdAt(int slot) {
        return slotToItemId.get(slot);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
