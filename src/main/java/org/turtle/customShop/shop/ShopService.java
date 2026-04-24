package org.turtle.customShop.shop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.turtle.customShop.config.ConfigService;
import org.turtle.customShop.config.MainConfig;
import org.turtle.customShop.economy.EconomyGateway;
import org.turtle.customShop.state.InflationService;
import org.turtle.customShop.state.StockService;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShopService {

    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final StockService stockService;
    private final InflationService inflationService;
    private final EconomyGateway economyGateway;

    public ShopService(
            JavaPlugin plugin,
            ConfigService configService,
            StockService stockService,
            InflationService inflationService,
            EconomyGateway economyGateway
    ) {
        this.plugin = plugin;
        this.configService = configService;
        this.stockService = stockService;
        this.inflationService = inflationService;
        this.economyGateway = economyGateway;
    }

    public Map<String, ShopDefinition> getShops() {
        return configService.getShops();
    }

    public ShopDefinition getShop(String shopId) {
        return configService.getShop(shopId);
    }

    public double processSellItem(Player player, ItemStack stack, boolean isHand) {
        if (stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0) return 0;

        ShopDefinition matchedShop = null;
        ShopDefinition.ShopItem matchedItem = null;

        outer:
        for (ShopDefinition shop : getShops().values()) {
            for (ShopDefinition.ShopItem item : shop.items().values()) {
                if (!item.canSell()) continue;
                if (item.material() != stack.getType()) continue;

                if (item.entityType() != null && !item.entityType().isEmpty() && stack.getType() == Material.SPAWNER) {
                    if (stack.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
                        if (bsm.getBlockState() instanceof org.bukkit.block.CreatureSpawner spawner) {
                            try {
                                org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(item.entityType().toUpperCase());
                                if (spawner.getSpawnedType() != type) continue;
                            } catch (Exception e) {
                                continue;
                            }
                        } else continue;
                    } else continue;
                }

                matchedShop = shop;
                matchedItem = item;
                break outer;
            }
        }

        if (matchedItem == null) {
            if (isHand) {
                player.sendMessage(colorize("&cThis item cannot be sold."));
            }
            return 0;
        }

        int amount = stack.getAmount();
        double earned = inflatedSellPrice(matchedItem) * amount;

        economyGateway.deposit(player, earned);
        stack.setAmount(0);

        boolean isUnlimited = matchedShop.type() == null || matchedShop.type().equalsIgnoreCase("unlimited");
        if (!isUnlimited) {
            int stock = getStock(player, matchedShop, matchedItem);
            stockService.setStock(player, matchedShop.id(), matchedItem.id(), stock + amount);
            stockService.save();
        }

        if (configService.getMainConfig().dynamicPricingEnabled()) {
            double currentBuyMult = inflationService.buyMultiplier();
            inflationService.setBuyMultiplier(Math.max(0.1, currentBuyMult - (0.005 * amount)));
        }

        if (isHand) {
            player.sendMessage(colorize("&aSold " + amount + "x " + prettifyMaterial(matchedItem.material()) + " for &e" + formatPrice(earned) + "&a."));
        }

        return earned;
    }

    public void sellHand(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        processSellItem(player, handItem, true);
    }

    public void sellInventory(Player player) {
        boolean soldAnything = false;
        double totalEarnings = 0;
        int totalItems = 0;

        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0) continue;

            int originalAmount = stack.getAmount();
            double earned = processSellItem(player, stack, false);
            if (earned > 0) {
                soldAnything = true;
                totalEarnings += earned;
                totalItems += originalAmount;
            }
        }

        if (soldAnything) {
            player.sendMessage(colorize("&aYou sold " + totalItems + " items for &e" + formatPrice(totalEarnings) + "&a."));
        } else {
            player.sendMessage(colorize("&cYou have no sellable items in your inventory."));
        }
    }

    public void openMainMenu(Player player) {
        MainConfig.MainMenuConfig menuConfig = configService.getMainConfig().mainMenu();
        int size = Math.max(9, Math.min(54, menuConfig.rows() * 9));

        MainMenuInventoryHolder holder = new MainMenuInventoryHolder(menuConfig.items());
        Inventory inventory = Bukkit.createInventory(holder, size, colorize(menuConfig.title()));
        holder.setInventory(inventory);

        for (Map.Entry<Integer, MainConfig.MainMenuItem> entry : menuConfig.items().entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < size) {
                MainConfig.MainMenuItem item = entry.getValue();
                ItemStack stack = new ItemStack(item.material(), 1);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(colorize(item.name()));
                    List<String> lore = new ArrayList<>();
                    for (String line : item.lore()) {
                        lore.add(colorize(line));
                    }
                    meta.setLore(lore);
                    stack.setItemMeta(meta);
                }
                inventory.setItem(slot, stack);
            }
        }

        player.openInventory(inventory);
    }

    public void handleMainMenuClick(Player player, MainMenuInventoryHolder holder, int slot) {
        MainConfig.MainMenuItem item = holder.getItemAt(slot);
        if (item != null && !item.shopId().isEmpty()) {
            if (!openShop(player, item.shopId())) {
                player.sendMessage(colorize("&cShop unavailable."));
                player.closeInventory();
            }
        }
    }

    public boolean openShop(Player player, String shopId) {
        return openShop(player, shopId, 1);
    }

    public boolean openShop(Player player, String shopId, int page) {
        ShopDefinition shop = getShop(shopId);
        if (shop == null) {
            return false;
        }

        boolean hasPerm = player.hasPermission("customshop.shop." + shopId) 
                       || player.hasPermission("customshop.shop.*") 
                       || player.hasPermission("customshop.admin");
                       
        if (!hasPerm) {
            player.sendMessage(colorize("&cYou don't have permission to open this shop."));
            return false;
        }

        int rows = shop.menu().rows();
        int size = Math.max(9, Math.min(54, rows * 9));

        // Define navigation slots
        int prevSlot = Math.max(0, size - 9);
        int backSlot = Math.max(0, size - 5);
        int nextSlot = Math.max(0, size - 1);

        Map<Integer, String> slotToItemId = new HashMap<>();
        ShopInventoryHolder holder = new ShopInventoryHolder(shop.id(), page, slotToItemId);
        String pageTitle = shop.menu().title() + " &8- Page " + page;
        Inventory inventory = Bukkit.createInventory(holder, size, colorize(pageTitle));
        holder.setInventory(inventory);

        List<ShopDefinition.ShopItem> unplaced = new ArrayList<>();
        boolean[] usedSlots = new boolean[size];

        // Reserve navigation slots
        usedSlots[prevSlot] = true;
        usedSlots[backSlot] = true;
        usedSlots[nextSlot] = true;

        int pageOffset = (page - 1) * size;
        int maxSlotNeeded = 0;

        for (ShopDefinition.ShopItem item : shop.items().values()) {
            if (item.slot() >= 0) {
                maxSlotNeeded = Math.max(maxSlotNeeded, item.slot());
                if (item.slot() >= pageOffset && item.slot() < pageOffset + size) {
                    int slotOnPage = item.slot() - pageOffset;
                    if (slotOnPage == prevSlot || slotOnPage == backSlot || slotOnPage == nextSlot) {
                        unplaced.add(item); // Conflicting with nav buttons, treat as unplaced
                    } else {
                        inventory.setItem(slotOnPage, buildDisplayItem(player, shop, item));
                        slotToItemId.put(slotOnPage, item.id());
                        usedSlots[slotOnPage] = true;
                    }
                }
            } else {
                unplaced.add(item);
            }
        }

        int nextAvailSlot = 0;
        int totalItemsProcessed = 0;

        for (ShopDefinition.ShopItem item : unplaced) {
            totalItemsProcessed++;
            // If item auto-places, determine its absolute slot
            // Since we need pagination, we calculate its page based on next available slots
            /* We skip explicit pagination tracking for unplaced items and just push them.
               Actually, a simpler way is just loop unplaced and find next slot across all pages. */
        }

        // Better unplaced logic for pagination: calculate absolute slots for all unplaced
        int unplacedIndex = 0;
        List<ShopDefinition.ShopItem> allItemsList = new ArrayList<>(shop.items().values());

        // Calculate max possible page
        int maxPage = (maxSlotNeeded / size) + 1;

        // For unplaced, assign them to next free absolute slots
        int absoluteSlotCounter = 0;
        for (ShopDefinition.ShopItem item : unplaced) {
            while (true) {
                int testPage = (absoluteSlotCounter / size) + 1;
                int testSlot = absoluteSlotCounter % size;

                // Keep away from nav buttons on EVERY page
                if (testSlot != prevSlot && testSlot != backSlot && testSlot != nextSlot) {
                    boolean conflict = false;
                    for (ShopDefinition.ShopItem testItem : allItemsList) {
                        if (testItem.slot() == absoluteSlotCounter) {
                            conflict = true;
                            break;
                        }
                    }
                    if (!conflict) {
                        maxPage = Math.max(maxPage, testPage);
                        if (testPage == page) {
                            inventory.setItem(testSlot, buildDisplayItem(player, shop, item));
                            slotToItemId.put(testSlot, item.id());
                        }
                        absoluteSlotCounter++;
                        break;
                    }
                }
                absoluteSlotCounter++;
            }
        }

        // Navigation elements
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(colorize("&ePrevious Page"));
                prev.setItemMeta(meta);
            }
            inventory.setItem(prevSlot, prev);
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(colorize("&cBack to Menu"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(backSlot, back);

        if (page < maxPage) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(colorize("&eNext Page"));
                next.setItemMeta(meta);
            }
            inventory.setItem(nextSlot, next);
        }

        player.openInventory(inventory);
        return true;
    }

    public void openTransactionMenu(Player player, ShopDefinition shop, ShopDefinition.ShopItem item) {
        TransactionInventoryHolder holder = new TransactionInventoryHolder(shop.id(), item.id());
        Inventory inventory = Bukkit.createInventory(holder, 54, colorize("&8Buy / Sell"));
        holder.setInventory(inventory);

        // Center item
        inventory.setItem(22, buildDisplayItem(player, shop, item));

        // Buy buttons (left, green)
        inventory.setItem(19, createTransactionButton(Material.LIME_STAINED_GLASS_PANE, "&aBuy +1", 1));
        inventory.setItem(20, createTransactionButton(Material.LIME_STAINED_GLASS_PANE, "&aBuy +8", 8));
        inventory.setItem(21, createTransactionButton(Material.LIME_STAINED_GLASS_PANE, "&aBuy +16", 16));
        inventory.setItem(28, createTransactionButton(Material.LIME_STAINED_GLASS_PANE, "&aBuy +32", 32));
        inventory.setItem(29, createTransactionButton(Material.LIME_STAINED_GLASS_PANE, "&aBuy +64", 64));

        // Sell buttons (right, red)
        inventory.setItem(23, createTransactionButton(Material.RED_STAINED_GLASS_PANE, "&cSell -1", 1));
        inventory.setItem(24, createTransactionButton(Material.RED_STAINED_GLASS_PANE, "&cSell -8", 8));
        inventory.setItem(25, createTransactionButton(Material.RED_STAINED_GLASS_PANE, "&cSell -16", 16));
        inventory.setItem(33, createTransactionButton(Material.RED_STAINED_GLASS_PANE, "&cSell -32", 32));
        inventory.setItem(34, createTransactionButton(Material.RED_STAINED_GLASS_PANE, "&cSell -64", 64));

        inventory.setItem(35, createTransactionButton(Material.ORANGE_STAINED_GLASS_PANE, "&eSell All", -1));

        player.openInventory(inventory);
    }

    private ItemStack createTransactionButton(Material material, String name, int amount) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleTransactionClick(Player player, TransactionInventoryHolder holder, int slot) {
        ShopDefinition shop = getShop(holder.getShopId());
        if (shop == null) return;
        ShopDefinition.ShopItem item = shop.items().get(holder.getItemId());
        if (item == null) return;

        int amount = 0;
        boolean isBuy = false;
        boolean isSellAll = false;

        switch (slot) {
            case 19: isBuy = true; amount = 1; break;
            case 20: isBuy = true; amount = 8; break;
            case 21: isBuy = true; amount = 16; break;
            case 28: isBuy = true; amount = 32; break;
            case 29: isBuy = true; amount = 64; break;
            case 23: amount = 1; break;
            case 24: amount = 8; break;
            case 25: amount = 16; break;
            case 33: amount = 32; break;
            case 34: amount = 64; break;
            case 35: isSellAll = true; break;
            default: return; // Do nothing for other slots
        }

        if (isBuy) {
            if (!item.canBuy()) {
                player.sendMessage("§cYou cannot buy this item.");
                return;
            }
            buyMultiple(player, shop, item, amount);
        } else if (isSellAll) {
            if (!item.canSell()) {
                player.sendMessage("§cYou cannot sell this item.");
                return;
            }
            sellMultiple(player, shop, item, Integer.MAX_VALUE);
        } else {
            if (!item.canSell()) {
                player.sendMessage("§cYou cannot sell this item.");
                return;
            }
            sellMultiple(player, shop, item, amount);
        }

        player.getOpenInventory().setItem(22, buildDisplayItem(player, shop, item));
    }

    public void handleClick(Player player, ShopInventoryHolder holder, int slot, ClickType clickType) {
        ShopDefinition shop = getShop(holder.getShopId());
        if (shop == null) {
            return;
        }

        int size = Math.max(9, Math.min(54, shop.menu().rows() * 9));
        int prevSlot = Math.max(0, size - 9);
        int backSlot = Math.max(0, size - 5);
        int nextSlot = Math.max(0, size - 1);

        if (slot == prevSlot && holder.getPage() > 1) {
            openShop(player, shop.id(), holder.getPage() - 1);
            return;
        }
        if (slot == backSlot) {
            openMainMenu(player);
            return;
        }
        if (slot == nextSlot) {
            openShop(player, shop.id(), holder.getPage() + 1);
            return;
        }

        String itemId = holder.getItemIdAt(slot);
        if (itemId == null) {
            return;
        }

        ShopDefinition.ShopItem item = shop.items().get(itemId);
        if (item == null) {
            return;
        }

        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            openTransactionMenu(player, shop, item);
            return;
        }

        if (clickType == ClickType.LEFT) {
            if (!item.canBuy()) {
                player.sendMessage("§cYou cannot buy this item.");
                return;
            }
            buyMultiple(player, shop, item, 1);
        } else if (clickType == ClickType.RIGHT) {
            if (!item.canSell()) {
                player.sendMessage("§cYou cannot sell this item.");
                return;
            }
            sellMultiple(player, shop, item, 1);
        }

        player.getOpenInventory().setItem(slot, buildDisplayItem(player, shop, item));
    }

    public void restockAll(boolean broadcast) {
        for (ShopDefinition shop : getShops().values()) {
            restockShop(shop.id());
        }
        stockService.save();

        if (broadcast) {
            Bukkit.broadcastMessage(colorize(configService.getMainConfig().restockBroadcastMessage()));
        }
    }

    public boolean restockShop(String shopId) {
        ShopDefinition shop = getShop(shopId);
        if (shop == null) {
            return false;
        }

        stockService.clearShopStock(shopId);
        stockService.save();
        return true;
    }

    public String formatPrice(double amount) {
        try {
            return economyGateway.format(amount);
        } catch (Exception exception) {
            MainConfig main = configService.getMainConfig();
            DecimalFormat format = new DecimalFormat(main.decimalPlaces() == 0 ? "0" : "0." + "0".repeat(main.decimalPlaces()));
            return main.currencySymbol() + format.format(amount);
        }
    }

    public double inflatedBuyPrice(ShopDefinition.ShopItem item) {
        return item.buyPrice() * inflationService.buyMultiplier();
    }

    public double inflatedSellPrice(ShopDefinition.ShopItem item) {
        return item.sellPrice() * inflationService.buyMultiplier();
    }

    public int getStock(Player player, ShopDefinition shop, ShopDefinition.ShopItem item) {
        return stockService.getStock(player, shop.id(), item.id(), item.initialStock());
    }

    public boolean setStock(Player player, String shopId, String itemId, int amount) {
        ShopDefinition shop = getShop(shopId);
        if (shop == null || !shop.items().containsKey(itemId.toLowerCase(Locale.ROOT))) {
            return false;
        }
        stockService.setStock(player, shopId.toLowerCase(Locale.ROOT), itemId.toLowerCase(Locale.ROOT), Math.max(0, amount));
        stockService.save();
        return true;
    }

    private void buyMultiple(Player player, ShopDefinition shop, ShopDefinition.ShopItem item, int amount) {
        int stock = getStock(player, shop, item);
        boolean isUnlimited = item.initialStock() < 0;

        if (!isUnlimited && stock <= 0) {
            player.sendMessage(message("out_of_stock", "&cThis item is out of stock!"));
            return;
        }

        int wantToBuy = isUnlimited ? amount : Math.min(amount, stock);
        double unitPrice = inflatedBuyPrice(item);
        
        double balance = economyGateway.getBalance(player);
        if (unitPrice > 0) {
            int affordable = (int) (balance / unitPrice);
            if (affordable < wantToBuy) {
                wantToBuy = affordable;
            }
        }

        if (wantToBuy <= 0) {
            player.sendMessage(message("insufficient_funds", "&cYou do not have enough money!"));
            return;
        }

        double totalPrice = wantToBuy * unitPrice;
        if (!economyGateway.withdraw(player, totalPrice)) {
            player.sendMessage(message("insufficient_funds", "&cYou do not have enough money!"));
            return;
        }

        ItemStack purchased = new ItemStack(item.material(), wantToBuy);

        if (item.entityType() != null && item.material() == Material.SPAWNER) {
            org.bukkit.inventory.meta.BlockStateMeta meta = (org.bukkit.inventory.meta.BlockStateMeta) purchased.getItemMeta();
            if (meta != null) {
                org.bukkit.block.CreatureSpawner spawner = (org.bukkit.block.CreatureSpawner) meta.getBlockState();
                try {
                    org.bukkit.entity.EntityType eType = org.bukkit.entity.EntityType.valueOf(item.entityType().toUpperCase(java.util.Locale.ROOT));
                    spawner.setSpawnedType(eType);
                    meta.setBlockState(spawner);
                    purchased.setItemMeta(meta);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid entity type for spawner: " + item.entityType());
                }
            }
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(purchased);
        for (ItemStack left : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), left);
        }

        if (!isUnlimited) {
            stockService.setStock(player, shop.id(), item.id(), stock - wantToBuy);
            stockService.save();
        }
        
        if (configService.getMainConfig().dynamicPricingEnabled()) {
            // Simple Dynamic Pricing Emulation: Buying increases buy price temporarily in InflationService
            inflationService.setBuyMultiplier(inflationService.buyMultiplier() + (0.01 * wantToBuy));
        }

        player.sendMessage(message("item_purchased", "&aPurchased {amount}x {item} for {price}")
                .replace("{amount}", String.valueOf(wantToBuy))
                .replace("{item}", prettifyMaterial(item.material()))
                .replace("{price}", formatPrice(totalPrice)));
    }

    private void sellMultiple(Player player, ShopDefinition shop, ShopDefinition.ShopItem item, int amount) {
        int playerHas = 0;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && invItem.getType() == item.material()) {
                playerHas += invItem.getAmount();
            }
        }

        int wantToSell = Math.min(amount, playerHas);
        if (wantToSell <= 0) {
            player.sendMessage(colorize("&cYou do not have this item to sell."));
            return;
        }

        player.getInventory().removeItem(new ItemStack(item.material(), wantToSell));

        double totalPrice = inflatedSellPrice(item) * wantToSell;
        economyGateway.deposit(player, totalPrice);

        boolean isUnlimited = item.initialStock() < 0;
        if (!isUnlimited) {
            int stock = getStock(player, shop, item);
            stockService.setStock(player, shop.id(), item.id(), stock + wantToSell);
            stockService.save();
        }
        
        if (configService.getMainConfig().dynamicPricingEnabled()) {
            // Simple Dynamic Pricing Emulation: Selling decreases buy multiplier temporarily to lower market value
            double currentBuyMult = inflationService.buyMultiplier();
            inflationService.setBuyMultiplier(Math.max(0.1, currentBuyMult - (0.005 * wantToSell)));
        }

        player.sendMessage(message("item_sold", "&aSold {amount}x {item} for {price}")
                .replace("{amount}", String.valueOf(wantToSell))
                .replace("{item}", prettifyMaterial(item.material()))
                .replace("{price}", formatPrice(totalPrice)));
    }

    private ItemStack buildDisplayItem(Player player, ShopDefinition shop, ShopDefinition.ShopItem item) {
        ItemStack stack = new ItemStack(item.material(), 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        long nextRestock = ((org.turtle.customShop.CustomShop) plugin).getNextRestockTime();
        long diff = nextRestock - System.currentTimeMillis();
        String restockTime = "Restocked";
        if (diff > 0) {
            long s = diff / 1000;
            restockTime = String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
        }

        if (item.displayName() != null && !item.displayName().isEmpty()) {
            meta.setDisplayName(colorize(item.displayName()));
        } else {
            meta.setDisplayName(colorize("&e" + prettifyMaterial(item.material())));
        }

        List<String> lore = new ArrayList<>();
        int stock = getStock(player, shop, item);
        boolean isUnlimited = item.initialStock() < 0;
        String stockDisplay = isUnlimited ? "Unlimited" : Integer.toString(stock);
        String maxStockDisplay = isUnlimited ? "Unlimited" : Integer.toString(item.initialStock());

        if (item.lore() != null && !item.lore().isEmpty()) {
            for (String line : item.lore()) {
                // If the user hasn't removed their old placeholders yet, we can skip printing them
                // to avoid duplicates, or just parse them if they want them in custom spots.
                // But generally they'll just have pure descriptive lore now.
                lore.add(colorize(line
                        .replace("{buy_price}", formatPrice(inflatedBuyPrice(item)))
                        .replace("{sell_price}", formatPrice(inflatedSellPrice(item)))
                        .replace("{current_stock}", stockDisplay)
                        .replace("{max_stock}", maxStockDisplay)));
            }
            lore.add(colorize(""));
        }

        // Built-in Donut SMP style Lore
        if (item.canBuy()) {
            lore.add(colorize("&7Buy: &a" + formatPrice(inflatedBuyPrice(item))));
        } else {
            lore.add(colorize("&7Buy: &c&mN/A"));
        }
        
        if (item.canSell()) {
            lore.add(colorize("&7Sell: &c" + formatPrice(inflatedSellPrice(item))));
        } else {
            lore.add(colorize("&7Sell: &c&mN/A"));
        }
        
        if (!isUnlimited) {
            lore.add(colorize("&7Stock: &f" + stockDisplay + "/" + maxStockDisplay));
        }

        if (!isUnlimited && stock <= 0) {
            lore.add(colorize("&cOut of stock! &7Restock in: &e" + restockTime));
        }
        lore.add(colorize(""));
        lore.add(colorize("&8Left-Click to Buy x1"));
        lore.add(colorize("&8Right-Click to Sell x1"));
        lore.add(colorize("&8Shift-Click to open Transaction Menu"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private String message(String key, String fallback) {
        return colorize(configService.getMainConfig().messages().getOrDefault(key, fallback));
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private String prettifyMaterial(Material material) {
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
