package org.turtle.customShop;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.turtle.customShop.command.InflateCommand;
import org.turtle.customShop.command.ResetInflateCommand;
import org.turtle.customShop.command.SellCommand;
import org.turtle.customShop.command.ShopCommand;
import org.turtle.customShop.config.ConfigService;
import org.turtle.customShop.economy.EconomyGateway;
import org.turtle.customShop.economy.EconomyProvider;
import org.turtle.customShop.shop.ShopGuiListener;
import org.turtle.customShop.shop.ShopService;
import org.turtle.customShop.state.InflationService;
import org.turtle.customShop.state.StockService;

public final class CustomShop extends JavaPlugin {

    private ConfigService configService;
    private StockService stockService;
    private InflationService inflationService;
    private EconomyGateway economyGateway;
    private ShopService shopService;
    private int restockTaskId = -1;
    private long nextRestockTime = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("shops/blocks.yml");
        saveResourceIfMissing("shops/farming.yml");
        saveResourceIfMissing("shops/ores.yml");
        saveResourceIfMissing("shops/combat.yml");
        saveResourceIfMissing("shops/lumber.yml");
        saveResourceIfMissing("shops/redstone.yml");
        saveResourceIfMissing("shops/spawners.yml");

        this.configService = new ConfigService(this);
        this.stockService = new StockService(this);
        this.inflationService = new InflationService(this);

        reloadAll();

        this.economyGateway = EconomyProvider.resolve(this, configService.getMainConfig());
        this.shopService = new ShopService(this, configService, stockService, inflationService, economyGateway);

        ShopCommand shopCommand = new ShopCommand(this, configService, shopService);
        InflateCommand inflateCommand = new InflateCommand(configService, inflationService);
        ResetInflateCommand resetInflateCommand = new ResetInflateCommand(configService, inflationService);
        SellCommand sellCommand = new SellCommand(shopService);

        getCommand("shop").setExecutor(shopCommand);
        getCommand("shop").setTabCompleter(shopCommand);
        getCommand("inflate").setExecutor(inflateCommand);
        getCommand("resetinflate").setExecutor(resetInflateCommand);
        getCommand("sell").setExecutor(sellCommand);

        if (configService.getMainConfig().restockingEnabled()) {
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                long now = System.currentTimeMillis();
                if (now >= nextRestockTime) {
                    shopService.restockAll(true);
                    nextRestockTime = now + (configService.getMainConfig().globalRestockIntervalSeconds() * 1000L);
                }
            }, 20L, 20L * 60); // Check every minute
        }

        // Auto-Normalization matching Donut SMP
        long normalizeIntervalTicks = configService.getMainConfig().decreaseIntervalSeconds() * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            double currentBuy = inflationService.buyMultiplier();
            if (currentBuy > 1.0) {
                inflationService.setBuyMultiplier(Math.max(1.0, currentBuy - 0.005));
            } else if (currentBuy < 1.0) {
                inflationService.setBuyMultiplier(Math.min(1.0, currentBuy + 0.005));
            }
            
            double currentSell = inflationService.sellMultiplier();
            double defaultSell = configService.getMainConfig().defaultSellMultiplier();
            if (currentSell < defaultSell) {
                inflationService.setSellMultiplier(Math.min(defaultSell, currentSell + 0.002));
            } else if (currentSell > defaultSell) {
                inflationService.setSellMultiplier(Math.max(defaultSell, currentSell - 0.002));
            }
        }, normalizeIntervalTicks, normalizeIntervalTicks);

        getServer().getPluginManager().registerEvents(new ShopGuiListener(shopService), this);
        getLogger().info("CustomShop has been enabled!");
    }

    @Override
    public void onDisable() {
        cancelRestockTask();
        if (stockService != null) {
            stockService.save();
        }
        if (inflationService != null) {
            inflationService.save();
        }
        if (economyGateway != null) {
            economyGateway.save();
        }
    }

    public void reloadAll() {
        reloadConfig();
        configService.load();
        stockService.load();
        inflationService.load(configService.getMainConfig().defaultBuyMultiplier(), configService.getMainConfig().defaultSellMultiplier());
    }

    public void scheduleRestock() {
        cancelRestockTask();

        if (!configService.getMainConfig().restockingEnabled()) {
            return;
        }

        if (configService.getMainConfig().restockAutoStart()) {
            shopService.restockAll(true);
        }

        long seconds = Math.max(1, configService.getMainConfig().globalRestockIntervalSeconds());
        long ticks = seconds * 20L;

        nextRestockTime = System.currentTimeMillis() + (seconds * 1000L);

        this.restockTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            shopService.restockAll(true);
            nextRestockTime = System.currentTimeMillis() + (seconds * 1000L);
        }, ticks, ticks);
    }

    public long getNextRestockTime() {
        return nextRestockTime;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public ShopService getShopService() {
        return shopService;
    }

    private void cancelRestockTask() {
        if (restockTaskId != -1) {
            Bukkit.getScheduler().cancelTask(restockTaskId);
            restockTaskId = -1;
        }
    }

    private void saveResourceIfMissing(String resourcePath) {
        java.io.File file = new java.io.File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            saveResource(resourcePath, false);
        }
    }
}
