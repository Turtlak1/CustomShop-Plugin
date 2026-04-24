package org.turtle.customShop.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.turtle.customShop.config.MainConfig;

import java.util.Map;

public class MainMenuInventoryHolder implements InventoryHolder {

    private final Map<Integer, MainConfig.MainMenuItem> items;
    private Inventory inventory;

    public MainMenuInventoryHolder(Map<Integer, MainConfig.MainMenuItem> items) {
        this.items = items;
    }

    public MainConfig.MainMenuItem getItemAt(int slot) {
        return items.get(slot);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

