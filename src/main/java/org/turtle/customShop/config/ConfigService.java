package org.turtle.customShop.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.turtle.customShop.shop.ShopDefinition;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfigService {

    private final JavaPlugin plugin;
    private MainConfig mainConfig;
    private Map<String, ShopDefinition> shops = new LinkedHashMap<>();

    public ConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.mainConfig = readMainConfig();
        this.shops = readShops(mainConfig.shops());
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public Map<String, ShopDefinition> getShops() {
        return Collections.unmodifiableMap(shops);
    }

    public ShopDefinition getShop(String shopId) {
        return shops.get(shopId.toLowerCase());
    }

    private MainConfig readMainConfig() {
        String currencySymbol = plugin.getConfig().getString("server.currency_symbol", "$");
        String currencyName = plugin.getConfig().getString("server.currency_name", "Coins");
        int decimalPlaces = Math.max(0, plugin.getConfig().getInt("server.decimal_places", 2));

        List<String> shops = plugin.getConfig().getStringList("shops");

        boolean restockingEnabled = plugin.getConfig().getBoolean("restocking.enabled", true);
        long globalInterval = Math.max(1L, plugin.getConfig().getLong("restocking.global_interval", 86400L));
        boolean autoStart = plugin.getConfig().getBoolean("restocking.auto_start", true);
        String restockMessage = plugin.getConfig().getString("restocking.broadcast_message", "&6[CustomShop] &eAll shops have been restocked!");

        boolean inflationEnabled = plugin.getConfig().getBoolean("inflation.enabled", true);
        double defaultBuyMultiplier = Math.max(0.0D, plugin.getConfig().getDouble("inflation.buy_multiplier", 1.0D));
        double defaultSellMultiplier = Math.max(0.0D, plugin.getConfig().getDouble("inflation.sell_multiplier", 0.75D));
        boolean trackPerShopInflation = plugin.getConfig().getBoolean("inflation.track_per_shop", false);
        boolean dynamicPricingEnabled = plugin.getConfig().getBoolean("inflation.dynamic_pricing", true);
        long decreaseIntervalSeconds = Math.max(1L, plugin.getConfig().getLong("inflation.decrease_interval_seconds", 3600L));

        Map<String, String> messages = new HashMap<>();
        ConfigurationSection msgSection = plugin.getConfig().getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                messages.put(key, Objects.toString(msgSection.getString(key), ""));
            }
        }

        double startingBalance = plugin.getConfig().getDouble("economy.local_ledger_starting_balance", 0.0D);

        MainConfig.MainMenuConfig mainMenu = readMainMenuConfig();

        return new MainConfig(
                currencySymbol,
                currencyName,
                decimalPlaces,
                shops,
                restockingEnabled,
                globalInterval,
                autoStart,
                restockMessage,
                inflationEnabled,
                defaultBuyMultiplier,
                defaultSellMultiplier,
                trackPerShopInflation,
                dynamicPricingEnabled,
                decreaseIntervalSeconds,
                messages,
                startingBalance,
                mainMenu
        );
    }

    private MainConfig.MainMenuConfig readMainMenuConfig() {
        ConfigurationSection menuSection = plugin.getConfig().getConfigurationSection("main_menu");
        if (menuSection == null) {
            Map<Integer, MainConfig.MainMenuItem> defaultItems = new HashMap<>();
            defaultItems.put(10, new MainConfig.MainMenuItem("blocks", Material.STONE, "&eBlocks", List.of("&7Buy and sell building blocks")));
            defaultItems.put(12, new MainConfig.MainMenuItem("farming", Material.WHEAT, "&eFarming", List.of("&7Buy and sell crops and food")));
            defaultItems.put(14, new MainConfig.MainMenuItem("ores", Material.DIAMOND, "&bOres", List.of("&7Buy and sell valuable ores")));
            defaultItems.put(16, new MainConfig.MainMenuItem("combat", Material.DIAMOND_SWORD, "&cCombat", List.of("&7Buy and sell weapons and armor")));
            return new MainConfig.MainMenuConfig(3, "&8Server Shops", defaultItems);
        }

        int rows = Math.max(1, Math.min(6, menuSection.getInt("rows", 3)));
        String title = menuSection.getString("title", "&8Shops");
        Map<Integer, MainConfig.MainMenuItem> items = new HashMap<>();

        ConfigurationSection itemsSection = menuSection.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ConfigurationSection itemData = itemsSection.getConfigurationSection(key);
                    if (itemData != null) {
                        String shopId = itemData.getString("shop", "");
                        Material material = Material.matchMaterial(itemData.getString("material", "CHEST"));
                        String name = itemData.getString("name", "&e" + shopId);
                        List<String> lore = itemData.getStringList("lore");
                        items.put(slot, new MainConfig.MainMenuItem(shopId, material != null ? material : Material.CHEST, name, lore));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new MainConfig.MainMenuConfig(rows, title, items);
    }

    private Map<String, ShopDefinition> readShops(List<String> shopIds) {
        Map<String, ShopDefinition> loaded = new LinkedHashMap<>();
        File shopsDir = new File(plugin.getDataFolder(), "shops");

        for (String rawId : shopIds) {
            String id = rawId.toLowerCase();
            File file = new File(shopsDir, id + ".yml");
            if (!file.exists()) {
                plugin.getLogger().warning("Missing shop config: " + file.getName());
                continue;
            }

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection shopSection = yaml.getConfigurationSection("shop");
            ConfigurationSection itemsSection = yaml.getConfigurationSection("items");

            if (shopSection == null || itemsSection == null) {
                plugin.getLogger().warning("Invalid shop config (missing sections): " + file.getName());
                continue;
            }

            String name = shopSection.getString("name", id);
            String description = shopSection.getString("description", "");
            String type = shopSection.getString("type", "GUI");
            String owner = shopSection.getString("owner", "");

            ConfigurationSection menuSection = shopSection.getConfigurationSection("menu");
            ShopDefinition.MenuConfig menuConfig = new ShopDefinition.MenuConfig(
                    menuSection != null ? menuSection.getString("type", "CHEST") : "CHEST",
                    Math.max(1, Math.min(6, menuSection != null ? menuSection.getInt("rows", 6) : 6)),
                    menuSection != null ? menuSection.getString("title", name) : name
            );

            Map<String, ShopDefinition.ShopItem> items = new LinkedHashMap<>();
            for (String itemId : itemsSection.getKeys(false)) {
                ConfigurationSection section = itemsSection.getConfigurationSection(itemId);
                if (section == null) {
                    continue;
                }

                Material material = Material.matchMaterial(section.getString("material", "STONE"));
                if (material == null) {
                    plugin.getLogger().warning("Invalid material for " + id + ":" + itemId + ", skipping");
                    continue;
                }

                List<String> lore = section.getStringList("lore");
                double buyPrice = Math.max(0.0D, section.getDouble("buy_price", 1.0D));
                double sellPrice = Math.max(0.0D, section.getDouble("sell_price", buyPrice * 0.75D));

                int initialStock = section.contains("initial_stock") ? Math.max(-1, section.getInt("initial_stock", -1)) : -1;
                int maxStack = Math.max(1, Math.min(64, section.getInt("max_stack", 64)));
                int slot = section.contains("slot") ? section.getInt("slot") : section.getInt("pos", -1);

                String entityType = section.getString("entity_type", null);
                boolean canBuy = section.getBoolean("can_buy", true);
                boolean canSell = section.getBoolean("can_sell", true);

                String displayNameStr = section.getString("display_name", null);

                items.put(itemId.toLowerCase(), new ShopDefinition.ShopItem(
                        itemId.toLowerCase(),
                        slot,
                        material,
                        displayNameStr,
                        lore,
                        buyPrice,
                        sellPrice,
                        initialStock,
                        maxStack,
                        entityType,
                        canBuy,
                        canSell
                ));
            }

            loaded.put(id, new ShopDefinition(id, name, description, type, owner, menuConfig, items));
        }

        return loaded;
    }
}
