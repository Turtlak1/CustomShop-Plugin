package org.turtle.customShop.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.turtle.customShop.CustomShop;
import org.turtle.customShop.config.ConfigService;
import org.turtle.customShop.shop.ShopDefinition;
import org.turtle.customShop.shop.ShopService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final CustomShop plugin;
    private final ConfigService configService;
    private final ShopService shopService;

    public ShopCommand(CustomShop plugin, ConfigService configService, ShopService shopService) {
        this.plugin = plugin;
        this.configService = configService;
        this.shopService = shopService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (player.hasPermission("customshop.player.open") || player.hasPermission("customshop.admin.open")) {
                    shopService.openMainMenu(player);
                    return true;
                }
            }
            sender.sendMessage(color("&e/shop list | info <shop> | open <shop> | reload | restock [shop] | stock <shop> <item> <amount>"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "open" -> handleOpen(sender, args);
            case "reload" -> handleReload(sender);
            case "restock" -> handleRestock(sender, args);
            case "stock" -> handleStock(sender, args);
            default -> {
                sender.sendMessage(color("&cUnknown subcommand."));
                yield true;
            }
        };
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("customshop.player.list") && !sender.hasPermission("customshop.admin.list")) {
            sender.sendMessage(color("&cYou do not have permission."));
            return true;
        }

        sender.sendMessage(color("&6Available shops:"));
        for (ShopDefinition shop : shopService.getShops().values()) {
            sender.sendMessage(color("&e- " + shop.id() + " &7(" + shop.name() + "&7)"));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customshop.player.info") && !sender.hasPermission("customshop.admin.info")) {
            sender.sendMessage(color("&cYou do not have permission."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(color("&eUsage: /shop info <shop>"));
            return true;
        }

        ShopDefinition shop = shopService.getShop(args[1]);
        if (shop == null) {
            sender.sendMessage(color("&cShop not found."));
            return true;
        }

        sender.sendMessage(color("&6" + shop.name()));
        sender.sendMessage(color("&7" + shop.description()));
        sender.sendMessage(color("&7Type: &e" + shop.type()));
        sender.sendMessage(color("&7Items: &e" + shop.items().size()));
        return true;
    }

    private boolean handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can open shops."));
            return true;
        }
        if (!sender.hasPermission("customshop.player.open") && !sender.hasPermission("customshop.admin.open")) {
            sender.sendMessage(color("&cYou do not have permission."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(color("&eUsage: /shop open <shop>"));
            return true;
        }

        if (!shopService.openShop(player, args[1])) {
            sender.sendMessage(color("&cShop not found."));
            return true;
        }

        String opened = configService.getMainConfig().messages().getOrDefault("shop_opened", "&6Shop opened: &e{shop_name}");
        sender.sendMessage(color(opened.replace("{shop_name}", args[1])));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("customshop.admin.reload")) {
            sender.sendMessage(color("&cYou do not have permission."));
            return true;
        }

        plugin.reloadAll();
        plugin.scheduleRestock();
        sender.sendMessage(color("&aCustomShop reloaded."));
        return true;
    }

    private boolean handleRestock(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customshop.admin.restock")) {
            sender.sendMessage(color("&cYou do not have permission."));
            return true;
        }

        if (args.length >= 2) {
            boolean ok = shopService.restockShop(args[1]);
            sender.sendMessage(color(ok ? "&aShop restocked." : "&cShop not found."));
            return true;
        }

        shopService.restockAll(true);
        sender.sendMessage(color("&aAll shops restocked."));
        return true;
    }

    private boolean handleStock(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customshop.admin.stock")) {
            sender.sendMessage(color("&cYou do not have permission."));
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(color("&eUsage: /shop stock <shop> <item> <amount>"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(color("&cAmount must be a whole number."));
            return true;
        }

                        boolean ok = false;
                        if (sender instanceof org.bukkit.entity.Player) {
                            ok = shopService.setStock((org.bukkit.entity.Player) sender, args[1], args[2], amount);
                        } else {
                            sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
                            return true;
                        }
        sender.sendMessage(color(ok ? "&aStock updated." : "&cInvalid shop or item."));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return partial(args[0], Arrays.asList("list", "info", "open", "reload", "restock", "stock"));
        }

        if (args.length == 2 && Arrays.asList("info", "open", "restock", "stock").contains(args[0].toLowerCase(Locale.ROOT))) {
            return partial(args[1], new ArrayList<>(shopService.getShops().keySet()));
        }

        if (args.length == 3 && "stock".equalsIgnoreCase(args[0])) {
            ShopDefinition shop = shopService.getShop(args[1]);
            if (shop == null) {
                return List.of();
            }
            return partial(args[2], new ArrayList<>(shop.items().keySet()));
        }

        return List.of();
    }

    private List<String> partial(String input, List<String> options) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
