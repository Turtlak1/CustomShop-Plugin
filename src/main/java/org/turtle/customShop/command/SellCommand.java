package org.turtle.customShop.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.turtle.customShop.shop.ShopService;

public class SellCommand implements CommandExecutor {

    private final ShopService shopService;

    public SellCommand(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /sell <hand|inventory|all>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "hand":
                shopService.sellHand(player);
                break;
            case "inventory":
            case "all":
                shopService.sellInventory(player);
                break;
            default:
                player.sendMessage("§cUsage: /sell <hand|inventory|all>");
                break;
        }

        return true;
    }
}
