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
import com.github.cc007.headsinventory.inventory.CategoriesMenu;
import com.github.cc007.headsinventory.locale.Translator;
import com.github.cc007.headsinventory.search.HeadsSearch;
import com.github.cc007.headsinventory.utils.func.TriConsumer;
import com.github.cc007.headsplugin.api.business.domain.exceptions.LockingException;

import com.google.common.base.Joiner;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
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
        Translator t = HeadsInventory.getTranslator();

        // update heads
        if (command.getName().equalsIgnoreCase("updateheads")) {
            return onUpdateHeadsCommand(sender, args);
        }

        // from here on only player commands
        if (!(sender instanceof Player)) {
            sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-player-command"));
            return false;
        }
        Player player = (Player) sender;
        return onPlayerCommand(player, command, commandLabel, args);
    }

    private boolean onPlayerCommand(Player player, Command command, String commandLabel, String[] args) {
        Translator t = HeadsInventory.getTranslator();

        switch (command.getName().toLowerCase()) {
            case "myhead":
                return onMyHeadCommand(player);
            case "playerhead":
                return onPlayerHeadCommand(player, args);
            case "heads":
                return onHeadsCommand(player);
            case "addhead":
                return onAddHeadCommand(player, args);
            case "headsinventory":
                return onHeadsInventoryCommand(player, args);
            default:
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-incorrect-usage"));
                player.sendMessage(HeadsInventory.getHelpMessage());
                return true;
        }
    }

    private boolean onUpdateHeadsCommand(final CommandSender sender, final String[] args) {
        // From now on handled by HeadsPluginAPI
        String updateCommandMoved = "Use /headspluginapi update " + String.join(", ", args) + " instead.";
        HeadsInventory.getPlugin().getLogger().info(updateCommandMoved.replaceAll("\\s+", " "));
        return true;
    }

    private boolean onMyHeadCommand(Player player) {
        Translator t = HeadsInventory.getTranslator();
        if (!player.hasPermission("headsinv.myhead")) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-myhead-nopermission"));
            return false;
        }
        HeadsSearch.myHead(player);
        return true;
    }

    private boolean onPlayerHeadCommand(Player player, String[] args) {
        Translator t = HeadsInventory.getTranslator();
        if (!player.hasPermission("headsinv.playerhead")) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-playerhead-nopermission"));
            return false;
        }
        if (args.length == 0) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-playerhead-noplayername"));
            return false;
        }
        HeadsSearch.playerHead(player, args);
        return true;
    }

    private boolean onHeadsCommand(Player player) {
        player.sendMessage(HeadsInventory.getHelpMessage());
        return true;
    }

    private boolean onAddHeadCommand(Player player, String[] args) {
        Translator t = HeadsInventory.getTranslator();
        if (!player.hasPermission("headsinv.add")) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-addhead-nopermission"));
            return false;
        }

        if (args.length < 1) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-addhead-noheadname"));
            return false;
        }
        //TODO check name for edge cases

        HeadsSearch.saveHead(player, Joiner.on(" ").join(args));
        return true;
    }

    private boolean onHeadsInventoryCommand(Player player, String[] args) {
        Translator t = HeadsInventory.getTranslator();

        if (!player.hasPermission("headsinv.inventory")) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-headsinv-nopermission"));
            return false;
        }

        // head search with inventory of all categories
        if (args.length == 0) {
            return HeadsSearch.searchAllCategories(player);
        }

        switch (args[0].toLowerCase()) {
            case "categories":
            case "category":
            case "cat":
                //head search with inventory of the specified category
                return onCategoriesCommand(player, args);
            case "mhsearch":
            case "msearch":
            case "fsearch":
            case "search":
                //head search with inventory
                return onSearchCommand(player, args);
            case "mhsearchfirst":
            case "msearchfirst":
            case "fsearchfirst":
            case "searchfirst":
            case "mhgetfirst":
            case "mgetfirst":
            case "fgetfirst":
            case "getfirst":
                //return first head from search
                return onSearchFirstCommand(player, args);
            case "help":
                player.sendMessage(HeadsInventory.getHelpMessage());
                return true;
            default:
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-incorrect-usage"));
                player.sendMessage(HeadsInventory.getHelpMessage());
                return true;
        }
    }

    private boolean onCategoriesCommand(Player player, String[] args) {
        Translator t = HeadsInventory.getTranslator();
        if (args.length == 1) {
            // open the categories ui
            try {
                CategoriesMenu menu = new CategoriesMenu(player);
                menu.open();
                return true;
            } catch (LockingException ex) {
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("categoriesmenu-error-categorynotloaded"));
                return true;
            }
        }

        if (args.length > 2) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-headsinv-categories-multiplecategories"));
            return false;
        }

        try {
            if (args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("*")) {
                return HeadsSearch.searchAllCategories(player);
            }

            return HeadsSearch.searchCategory(player, args[1]);
        } catch (LockingException ex) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("categoriesmenu-error-categoriesnotloaded"));
            return true;
        }
    }

    private boolean onSearchCommand(Player player, String[] args) {
        return validateArgsAndSearch(player, args, HeadsSearch::search);
    }

    private boolean onSearchFirstCommand(Player player, String[] args) {
        return validateArgsAndSearch(player, args, HeadsSearch::searchFirst);
    }

    private boolean validateArgsAndSearch(Player player, String[] args, TriConsumer<Player, String, String> searchConsumer) {
        Translator t = HeadsInventory.getTranslator();
        if (args.length < 2) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-headsinv-search-nosearchterm"));
            return false;
        }
        if (args[1].length() < 3) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-headsinv-search-shortsearchterm"));
            return false;
        }

        String searchDatabase = args[0].startsWith("mh") ? "minecraftheads" : args[0].startsWith("m") ? "mineskin" : args[0].startsWith("f") ? "freshcoal" : "default";
        String[] searchArgs = new String[args.length - 1];
        System.arraycopy(args, 1, searchArgs, 0, searchArgs.length);
        searchConsumer.accept(player, Joiner.on(" ").join(searchArgs), searchDatabase);
        return true;
    }

}
