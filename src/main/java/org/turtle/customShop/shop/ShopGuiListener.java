package org.turtle.customShop.shop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ShopGuiListener implements Listener {

    private final ShopService shopService;

    public ShopGuiListener(ShopService shopService) {
        this.shopService = shopService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (event.getInventory().getHolder() instanceof MainMenuInventoryHolder mainHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory() && event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                shopService.handleMainMenuClick(player, mainHolder, event.getSlot());
            }
            return;
        }

        if (event.getInventory().getHolder() instanceof TransactionInventoryHolder transactionHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory() && event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                shopService.handleTransactionClick(player, transactionHolder, event.getSlot());
            }
            return;
        }

        if (!(event.getInventory().getHolder() instanceof ShopInventoryHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
            return;
        }

        shopService.handleClick(player, holder, event.getSlot(), event.getClick());
    }
}
