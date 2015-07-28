/* 
 * The MIT License
 *
 * Copyright 2015 Rik Schaaf aka CC007 (http://coolcat007.nl/).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.cc007.headsinventory.commands;

import com.github.cc007.headsinventory.HeadsInventory;
import com.github.cc007.headsinventory.search.HeadsSearch;
import com.github.cc007.headsplugin.HeadsPlugin;
import com.github.cc007.headsplugin.utils.HeadsUtils;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Autom
 * @author CC007 (http://coolcat007.nl/)
 */
public class HeadsInventoryCommand implements CommandExecutor {

    private final HeadsInventory plugin;

    public HeadsInventoryCommand(HeadsInventory plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {

        // update heads
        if (command.getName().equalsIgnoreCase("updateheads")) {
            return onUpdateHeadsCommand(sender, args);
        }

        // from here on only player commands
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can perform this command");
            return false;
        }
        Player player = (Player) sender;
        return onPlayerCommand(player, command, commandLabel, args);
    }

    private boolean onPlayerCommand(Player player, Command command, String commandLabel, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "myhead":
                return onMyHeadCommand(player);
            case "playerhead":
                return onPlayerHeadCommand(player, args);
            case "headsinventory":
                return onHeadsInventoryCommand(player, args);
            default:
                plugin.getLogger().log(Level.WARNING, "Unknown command send: {0}", command.getName());
                plugin.getLogger().log(Level.WARNING, "Used alias: {0}", commandLabel);
                return false;
        }

    }

    private boolean onUpdateHeadsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("headsinv.update")) {
            sender.sendMessage("You don't have permission to update the catagorized heads.");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.GREEN + "Updating all categories...");
            HeadsUtils.getInstance().loadCategories();
            sender.sendMessage(ChatColor.GREEN + "Update complete.");
            return true;
        }

        if (HeadsPlugin.getHeadsPlugin().getCategoriesConfig().isInt("predefinedcategories." + args[1]) || HeadsPlugin.getHeadsPlugin().getCategoriesConfig().isInt("customcategories." + args[1] + ".id")) {
            sender.sendMessage(ChatColor.GREEN + "Updating all category: " + args[1] + "...");
            try {
                HeadsUtils.getInstance().loadCategory(args[1]);
            } catch (NullPointerException ex) {
                sender.sendMessage(ChatColor.RED + "Category is empty!");
                return false;
            }
            sender.sendMessage(ChatColor.GREEN + "Update complete.");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "No category found with that name, possible categories: ");
        Set<String> categoryNames = HeadsPlugin.getHeadsPlugin().getCategoriesConfig().getConfigurationSection("predefinedcategories").getKeys(false);
        categoryNames.addAll(HeadsPlugin.getHeadsPlugin().getCategoriesConfig().getConfigurationSection("customcategories").getKeys(false));
        sender.sendMessage(ChatColor.GOLD + StringUtils.join(categoryNames, ", "));
        return false;
    }

    private boolean onMyHeadCommand(Player player) {
        if (!player.hasPermission("headsinv.myhead")) {
            player.sendMessage("You don't have the permission to spawn in your own head");
            return false;
        }
        HeadsSearch.myHead(player);
        return true;
    }

    private boolean onPlayerHeadCommand(Player player, String[] args) {
        if (!player.hasPermission("headsinv.playerhead")) {
            player.sendMessage("You don't have the permission to spawn in someone elses head");
            return false;
        }
        if (args.length == 0) {
            player.sendMessage("You didn't specify a username");
            return false;
        }
        HeadsSearch.playerHead(player, args);
        return true;
    }

    private boolean onHeadsInventoryCommand(Player player, String args[]) {
        if (!player.hasPermission("headsinv.inventory")) {
            player.sendMessage("You don't have permission to look up custom heads.");
            return false;
        }

        // head search with inventory of all categories
        if (args.length == 0) {
            return HeadsSearch.searchAllCategories(player);
        }

        switch (args[0].toLowerCase()) {
            case "categories":
                //head search with inventory of the specified category
                return onCategoriesCommand(player, args);
            case "search":
                //head search with inventory
                return onSearchCommand(player, args);
            case "searchfirst":
                //return first head from search
                return onSearchFirstCommand(player, args);
            default:
                player.sendMessage("Use: \n/headsinventory categories <categoryname>\n/headsinventory search <keyword> \n/headsinventory searchfirst <keyword>");
                return true;
        }
    }

    private boolean onCategoriesCommand(Player player, String[] args) {
        if (args.length == 1) {
            //return the category names
            HeadsSearch.sendCategoriesList(player);
            return true;
        }

        if (args.length > 2) {
            player.sendMessage("Please provide only one category name.");
            return false;
        }

        if (args[1].equalsIgnoreCase("all")) {
            return HeadsSearch.searchAllCategories(player);
        }

        return HeadsSearch.searchCategory(player, args[1]);

    }

    private boolean onSearchCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "You need to specify a head.");
            return false;
        }
        if (args[1].length() < 3) {
            player.sendMessage(ChatColor.RED + "The keyword needs start with at least 3 characters.");
            return false;
        }

        String[] searchArgs = new String[args.length - 1];
        System.arraycopy(args, 1, searchArgs, 0, searchArgs.length);

        return HeadsSearch.search(player, String.join(" ", searchArgs));
    }

    private boolean onSearchFirstCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "You need to specify a head.");
            return false;
        }
        if (args[1].length() < 3) {
            player.sendMessage(ChatColor.RED + "The keyword needs start with at least 3 characters.");
            return false;
        }

        String[] searchArgs = new String[args.length - 1];
        System.arraycopy(args, 1, searchArgs, 0, searchArgs.length);

        return HeadsSearch.searchFirst(player, String.join(" ", searchArgs));
    }

}
