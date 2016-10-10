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
import com.github.cc007.headsinventory.search.HeadsSearch;
import com.github.cc007.headsplugin.HeadsPlugin;
import com.github.cc007.headsplugin.exceptions.AuthenticationException;
import com.github.cc007.headsplugin.utils.HeadsUtils;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
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
            sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "Only players can perform this command");
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
            case "heads":
                return onHeadsCommand(player);
            case "headsinventory":
                return onHeadsInventoryCommand(player, args);
            default:
                plugin.getLogger().log(Level.WARNING, "Unknown command send: {0}", command.getName());
                plugin.getLogger().log(Level.WARNING, "Used alias: {0}", commandLabel);
                return false;
        }

    }

    private boolean onUpdateHeadsCommand(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("headsinv.update")) {
            sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + "You don't have permission to update the catagorized heads.");
            return false;
        }

        if (args.length < 1) {
            if (sender instanceof Player) {
                sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + "Updating all categories...");
            }else{
                sender.sendMessage(HeadsInventory.pluginChatPrefix(false) + "Updating all categories...");
            }
            Thread t = new Thread() {

                @Override
                public void run() {
                    try {
                        HeadsUtils.getInstance().loadCategories();
                    } catch (SocketTimeoutException ex) {
                        try {
                            HeadsUtils.getInstance().loadCategories();
                        } catch (SocketTimeoutException ex2) {
                            plugin.getLogger().severe("The server did not respond. Please check if the heads website is online.");
                            plugin.getLogger().log(Level.SEVERE, null, ex2);
                        } catch (MalformedURLException ex2) {
                            plugin.getLogger().severe("The url is malformed. Please check the config file");
                            plugin.getLogger().log(Level.SEVERE, null, ex2);
                        } catch (IOException ex2) {
                            plugin.getLogger().severe("An unknown exception has occurred. Please check if the heads website is online.");
                            plugin.getLogger().log(Level.SEVERE, null, ex2);
                        }
                    } catch (MalformedURLException ex) {
                        plugin.getLogger().severe("The url is malformed. Please check the config file");
                        plugin.getLogger().log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        plugin.getLogger().severe("An unknown exception has occurred. Please check if the heads website is online.");
                        plugin.getLogger().log(Level.SEVERE, null, ex);
                    }
                    if (sender instanceof Player) {
                        sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + "Update complete.");
                    } else {
                        sender.sendMessage(HeadsInventory.pluginChatPrefix(false) + "Update complete.");
                    }
                }

            };
            t.start();
            return true;
        }

        if (HeadsPlugin.getHeadsPlugin().getCategoriesConfig().isInt("predefinedcategories." + args[0]) || HeadsPlugin.getHeadsPlugin().getCategoriesConfig().isInt("customcategories." + args[0] + ".id")) {
            if (sender instanceof Player) {
                sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + "Updating category: " + args[0] + "...");
            }
            Thread t = new Thread() {

                @Override
                public void run() {

                    try {
                        HeadsUtils.getInstance().loadCategory(args[0]);
                    } catch (SocketTimeoutException ex) {
                        plugin.getLogger().log(Level.SEVERE, null, ex);
                        sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "The heads database didn't respond in time.");
                        return;
                    } catch (IOException ex) {
                        plugin.getLogger().log(Level.SEVERE, null, ex);
                        sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "An unknown error occurred while reloading a category! Please contact an admin.");
                        return;
                    } catch (NullPointerException ex) {
                        sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "Category is empty!");
                        return;
                    } catch (AuthenticationException ex) {
                        plugin.getLogger().warning(ex.getMessage());
                        switch (HeadsPlugin.getHeadsPlugin().getAccessMode()) {
                            case LITE:
                                sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "The category that was specified is not part of the lite version of this plugin.");
                                break;
                            case EXPIRED:
                                sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "The trial for the plugin expired. The category that you tried to load is part of the full or trial version of the plugin.");
                                break;
                            case NONE:
                                sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "The plugin has not been properly configured. Please contact the server owner");
                                break;
                        }
                    }
                    if (sender instanceof Player) {
                        sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + "Update complete.");
                    } else {
                        sender.sendMessage(HeadsInventory.pluginChatPrefix(false) + "Update complete.");
                    }
                }

            };
            t.start();
            return true;
        }

        sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "No category found with that name, possible categories: ");
        Set<String> categoryNames = HeadsPlugin.getHeadsPlugin().getCategoriesConfig().getConfigurationSection("predefinedcategories").getKeys(false);
        categoryNames.addAll(HeadsPlugin.getHeadsPlugin().getCategoriesConfig().getConfigurationSection("customcategories").getKeys(false));
        sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GOLD + StringUtils.join(categoryNames, ", "));
        return false;
    }

    private boolean onMyHeadCommand(Player player) {
        if (!player.hasPermission("headsinv.myhead")) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + "You don't have the permission to spawn in your own head");
            return false;
        }
        HeadsSearch.myHead(player);
        return true;
    }

    private boolean onPlayerHeadCommand(Player player, String[] args) {
        if (!player.hasPermission("headsinv.playerhead")) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + "You don't have the permission to spawn in someone elses head");
            return false;
        }
        if (args.length == 0) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + "You didn't specify a username");
            return false;
        }
        HeadsSearch.playerHead(player, args);
        return true;
    }

    private boolean onHeadsCommand(Player player) {
        player.sendMessage(ChatColor.YELLOW + " ---- " + ChatColor.GOLD + "Heads Help" + ChatColor.YELLOW + " ---- \n"
                + ChatColor.GOLD + "/headsinv category <name>" + ChatColor.RESET + ": Display heads from a category.\n"
                + ChatColor.GOLD + "/headsinv category all" + ChatColor.RESET + ": Displays all heads from categories.\n"
                + ChatColor.GOLD + "/headsinv cat" + ChatColor.RESET + ": Displays all categories.\n"
                + ChatColor.GOLD + "/headsinv search <keyword>" + ChatColor.RESET + ": Display heads from keyword.\n"
                + ChatColor.GOLD + "/headsinv searchfirst <keyword>" + ChatColor.RESET + ": First head from keyword.\n"
                + ChatColor.GOLD + "/playerhead <playername>" + ChatColor.RESET + ": Gives you the head of a player."
                + ChatColor.GOLD + "/myhead" + ChatColor.RESET + ": Gives you your head.");
        return true;
    }

    private boolean onHeadsInventoryCommand(Player player, String args[]) {
        if (!player.hasPermission("headsinv.inventory")) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + "You don't have permission to look up custom heads.");
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
            case "search":
                //head search with inventory
                return onSearchCommand(player, args);
            case "searchfirst":
                //return first head from search
                return onSearchFirstCommand(player, args);
            default:
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "Incorrect usage.");
                player.sendMessage(ChatColor.YELLOW + " ---- " + ChatColor.GOLD + "Heads Help" + ChatColor.YELLOW + " ---- \n"
                        + ChatColor.GOLD + "/headsinv category <name>" + ChatColor.RESET + ": Display heads from a category.\n"
                        + ChatColor.GOLD + "/headsinv category all" + ChatColor.RESET + ": Displays all heads from categories.\n"
                        + ChatColor.GOLD + "/headsinv cat" + ChatColor.RESET + ": Displays all categories.\n"
                        + ChatColor.GOLD + "/headsinv search <keyword>" + ChatColor.RESET + ": Display heads from keyword.\n"
                        + ChatColor.GOLD + "/headsinv searchfirst <keyword>" + ChatColor.RESET + ": First head from keyword.\n"
                        + ChatColor.GOLD + "/playerhead <playername>" + ChatColor.RESET + ": Gives you the head of a player."
                        + ChatColor.GOLD + "/myhead" + ChatColor.RESET + ": Gives you your head.");
                return true;
        }
    }

    private boolean onCategoriesCommand(Player player, String[] args) {
        if (args.length == 1) {
            /*//return the category names
            HeadsSearch.sendCategoriesList(player);*/
            
            //open the categories ui
            CategoriesMenu menu = new CategoriesMenu(player);
            menu.open();
            return true;
        }

        if (args.length > 2) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + "Please provide only one category name.");
            return false;
        }

        if (args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("*")) {
            return HeadsSearch.searchAllCategories(player);
        }

        return HeadsSearch.searchCategory(player, args[1]);

    }

    private boolean onSearchCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "You need to specify a head.");
            return false;
        }
        if (args[1].length() < 3) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "The keyword needs start with at least 3 characters.");
            return false;
        }

        String[] searchArgs = new String[args.length - 1];
        System.arraycopy(args, 1, searchArgs, 0, searchArgs.length);

        HeadsSearch.search(player, Joiner.on(" ").join(searchArgs));
        return true;
    }

    private boolean onSearchFirstCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "You need to specify a head.");
            return false;
        }
        if (args[1].length() < 3) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "The keyword needs start with at least 3 characters.");
            return false;
        }

        String[] searchArgs = new String[args.length - 1];
        System.arraycopy(args, 1, searchArgs, 0, searchArgs.length);
        HeadsSearch.searchFirst(player, Joiner.on(" ").join(searchArgs));
        return true;
    }

}
