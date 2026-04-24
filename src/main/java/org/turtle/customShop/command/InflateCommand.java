package org.turtle.customShop.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.turtle.customShop.config.ConfigService;
import org.turtle.customShop.state.InflationService;

import java.util.Locale;

public class InflateCommand implements CommandExecutor {

    private final ConfigService configService;
    private final InflationService inflationService;

    public InflateCommand(ConfigService configService, InflationService inflationService) {
        this.configService = configService;
        this.inflationService = inflationService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("customshop.admin.inflate")) {
            sender.sendMessage(color("&cYou do not have permission."));
            return true;
        }

        if (!configService.getMainConfig().inflationEnabled()) {
            sender.sendMessage(color("&cInflation is disabled in config."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(color("&eUsage: /inflate <%change> <sell_multiplier>"));
            return true;
        }

        Double pct = parsePercent(args[0]);
        if (pct == null) {
            sender.sendMessage(color("&cInvalid percent format. Example: %50 or %-10"));
            return true;
        }

        double sellMultiplier;
        try {
            sellMultiplier = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(color("&cInvalid sell multiplier. Example: 0.75"));
            return true;
        }

        double buyMultiplier = Math.max(0.0D, 1.0D + pct / 100.0D);
        inflationService.apply(buyMultiplier, Math.max(0.0D, sellMultiplier));

        sender.sendMessage(color("&aInflation updated: buy x" + format(buyMultiplier) + " sell x" + format(sellMultiplier)));
        return true;
    }

    private Double parsePercent(String value) {
        if (!value.startsWith("%")) {
            return null;
        }
        try {
            return Double.parseDouble(value.substring(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String format(double value) {
        return String.format(Locale.US, "%.3f", value);
    }
}

