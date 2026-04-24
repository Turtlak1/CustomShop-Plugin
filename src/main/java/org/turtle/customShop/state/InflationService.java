package org.turtle.customShop.state;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class InflationService {

    private final JavaPlugin plugin;
    private final File inflationFile;
    private double buyMultiplier = 1.0D;
    private double sellMultiplier = 0.75D;
    private long updatedAt = System.currentTimeMillis();

    public InflationService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.inflationFile = new File(plugin.getDataFolder(), "inflation.yml");
    }

    public void load(double defaultBuyMultiplier, double defaultSellMultiplier) {
        this.buyMultiplier = defaultBuyMultiplier;
        this.sellMultiplier = defaultSellMultiplier;
        this.updatedAt = System.currentTimeMillis();

        if (!inflationFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(inflationFile);
        this.buyMultiplier = Math.max(0.0D, yaml.getDouble("inflation.buy_multiplier", defaultBuyMultiplier));
        this.sellMultiplier = Math.max(0.0D, yaml.getDouble("inflation.sell_multiplier", defaultSellMultiplier));
        this.updatedAt = yaml.getLong("inflation.timestamp", System.currentTimeMillis());
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("inflation.buy_multiplier", buyMultiplier);
        yaml.set("inflation.sell_multiplier", sellMultiplier);
        yaml.set("inflation.timestamp", updatedAt);

        try {
            yaml.save(inflationFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save inflation.yml: " + exception.getMessage());
        }
    }

    public void apply(double newBuyMultiplier, double newSellMultiplier) {
        this.buyMultiplier = Math.max(0.0D, newBuyMultiplier);
        this.sellMultiplier = Math.max(0.0D, newSellMultiplier);
        this.updatedAt = System.currentTimeMillis();
        save();
    }

    public void reset(double defaultBuyMultiplier, double defaultSellMultiplier) {
        apply(defaultBuyMultiplier, defaultSellMultiplier);
    }

    public double buyMultiplier() {
        return buyMultiplier;
    }

    public double sellMultiplier() {
        return sellMultiplier;
    }

    public void setBuyMultiplier(double multiplier) {
        this.buyMultiplier = multiplier;
    }

    public void setSellMultiplier(double multiplier) {
        this.sellMultiplier = multiplier;
    }
}
