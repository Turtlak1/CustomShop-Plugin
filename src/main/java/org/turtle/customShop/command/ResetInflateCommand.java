    package org.turtle.customShop.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.turtle.customShop.config.ConfigService;
import org.turtle.customShop.state.InflationService;

public class ResetInflateCommand implements CommandExecutor {

    private final ConfigService configService;
    private final InflationService inflationService;

    public ResetInflateCommand(ConfigService configService, InflationService inflationService) {
        this.configService = configService;
        this.inflationService = inflationService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("customshop.admin.resetinflate")) {
            sender.sendMessage(color("&cYou do not have permission."));
            return true;
        }

        inflationService.reset(configService.getMainConfig().defaultBuyMultiplier(), configService.getMainConfig().defaultSellMultiplier());
        String message = "&6[CustomShop] &eInflation has been reset to defaults.";
        Bukkit.broadcastMessage(color(message));
        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

