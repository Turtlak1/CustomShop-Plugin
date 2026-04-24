package org.turtle.customShop.economy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.turtle.customShop.config.MainConfig;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LocalLedgerEconomy implements EconomyGateway {

    private final File ledgerFile;
    private final Map<UUID, Double> balances = new HashMap<>();
    private final DecimalFormat formatter;
    private final String currencySymbol;
    private final double startingBalance;

    public LocalLedgerEconomy(File dataFolder, MainConfig config) {
        File dataDir = new File(dataFolder, "data");
        if (!dataDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataDir.mkdirs();
        }
        this.ledgerFile = new File(dataDir, "ledger.yml");
        this.currencySymbol = config.currencySymbol();
        this.startingBalance = config.localLedgerStartingBalance();
        this.formatter = new DecimalFormat(buildDecimalPattern(config.decimalPlaces()));
        load();
    }

    @Override
    public String getName() {
        return "LocalLedger";
    }

    @Override
    public boolean has(Player player, double amount) {
        return getBalance(player) >= Math.max(0.0D, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        double safeAmount = Math.max(0.0D, amount);
        double balance = getBalance(player);
        if (balance < safeAmount) {
            return false;
        }
        balances.put(player.getUniqueId(), balance - safeAmount);
        return true;
    }

    @Override
    public void deposit(Player player, double amount) {
        double safeAmount = Math.max(0.0D, amount);
        balances.put(player.getUniqueId(), getBalance(player) + safeAmount);
    }

    @Override
    public String format(double amount) {
        return currencySymbol + formatter.format(amount);
    }

    @Override
    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            yaml.set("balances." + entry.getKey(), entry.getValue());
        }

        try {
            yaml.save(ledgerFile);
        } catch (IOException exception) {
            // Persist errors are non-fatal for runtime transactions.
        }
    }

    private void load() {
        balances.clear();
        if (!ledgerFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(ledgerFile);
        if (yaml.getConfigurationSection("balances") == null) {
            return;
        }

        for (String key : yaml.getConfigurationSection("balances").getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), yaml.getDouble("balances." + key, startingBalance));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed UUID keys.
            }
        }
    }

    @Override
    public double getBalance(Player player) {
        return balances.computeIfAbsent(player.getUniqueId(), ignored -> startingBalance);
    }

    private String buildDecimalPattern(int decimalPlaces) {
        int safePlaces = Math.max(0, Math.min(8, decimalPlaces));
        if (safePlaces == 0) {
            return "0";
        }
        return "0." + "0".repeat(safePlaces);
    }
}

