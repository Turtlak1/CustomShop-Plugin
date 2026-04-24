package org.turtle.customShop.state;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StockService {

    private final JavaPlugin plugin;
    private final File stockFile;
    private final Map<String, Map<String, Map<String, Integer>>> stockByPlayerShop = new HashMap<>();

    public StockService(JavaPlugin plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataDir.mkdirs();
        }
        this.stockFile = new File(dataDir, "stock.yml");
    }

    public void load() {
        stockByPlayerShop.clear();
        if (!stockFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(stockFile);
        ConfigurationSection root = yaml.getConfigurationSection("stock");
        if (root == null) {
            return;
        }

        for (String uuidStr : root.getKeys(false)) {
            ConfigurationSection playerSection = root.getConfigurationSection(uuidStr);
            if (playerSection == null) continue;

            Map<String, Map<String, Integer>> playerStock = new HashMap<>();
            for (String shopId : playerSection.getKeys(false)) {
                ConfigurationSection shopSection = playerSection.getConfigurationSection(shopId);
                if (shopSection == null) continue;

                Map<String, Integer> itemStock = new HashMap<>();
                for (String itemId : shopSection.getKeys(false)) {
                    itemStock.put(itemId.toLowerCase(), Math.max(0, shopSection.getInt(itemId, 0)));
                }
                playerStock.put(shopId.toLowerCase(), itemStock);
            }
            stockByPlayerShop.put(uuidStr.toLowerCase(), playerStock);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Map<String, Map<String, Integer>>> playerEntry : stockByPlayerShop.entrySet()) {
            String uuidStr = playerEntry.getKey();
            for (Map.Entry<String, Map<String, Integer>> shopEntry : playerEntry.getValue().entrySet()) {
                String shopId = shopEntry.getKey();
                for (Map.Entry<String, Integer> itemEntry : shopEntry.getValue().entrySet()) {
                    yaml.set("stock." + uuidStr + "." + shopId + "." + itemEntry.getKey(), itemEntry.getValue());
                }
            }
        }

        try {
            yaml.save(stockFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save stock.yml: " + exception.getMessage());
        }
    }

    public int getStock(org.bukkit.entity.Player player, String shopId, String itemId, int fallback) {
        return stockByPlayerShop
                .computeIfAbsent(player.getUniqueId().toString().toLowerCase(), ignored -> new HashMap<>())
                .computeIfAbsent(shopId.toLowerCase(), ignored -> new HashMap<>())
                .computeIfAbsent(itemId.toLowerCase(), ignored -> Math.max(0, fallback));
    }

    public void setStock(org.bukkit.entity.Player player, String shopId, String itemId, int amount) {
        stockByPlayerShop
                .computeIfAbsent(player.getUniqueId().toString().toLowerCase(), ignored -> new HashMap<>())
                .computeIfAbsent(shopId.toLowerCase(), ignored -> new HashMap<>())
                .put(itemId.toLowerCase(), Math.max(0, amount));
    }

    public void clearShopStock(String shopId) {
        String lowerShopId = shopId.toLowerCase();
        for (Map<String, Map<String, Integer>> playerStock : stockByPlayerShop.values()) {
            playerStock.remove(lowerShopId);
        }
    }
}

