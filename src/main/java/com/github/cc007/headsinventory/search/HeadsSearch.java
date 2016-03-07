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
package com.github.cc007.headsinventory.search;

import com.github.cc007.headsinventory.HeadsInventory;
import com.github.cc007.headsinventory.inventory.HeadsInventoryMenu;
import com.github.cc007.headsplugin.HeadsPlugin;
import com.github.cc007.headsplugin.bukkit.HeadCreator;
import com.github.cc007.headsplugin.exceptions.AuthenticationException;
import com.github.cc007.headsplugin.utils.HeadsUtils;
import com.github.cc007.headsplugin.utils.heads.HeadsCategory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class HeadsSearch {

    public static void myHead(Player player) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (byte) SkullType.PLAYER.ordinal());
        SkullMeta skullMeta = (SkullMeta) Bukkit.getItemFactory().getItemMeta(Material.SKULL_ITEM);
        skullMeta.setOwner(player.getName());
        head.setItemMeta(skullMeta);
        putHeadInInventory(head, player);
        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + "The head is placed in your inventory.");
    }

    public static void playerHead(Player player, String[] otherPlayers) {
        for (String otherPlayerName : otherPlayers) {
            ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (byte) SkullType.PLAYER.ordinal());
            SkullMeta skullMeta = (SkullMeta) Bukkit.getItemFactory().getItemMeta(Material.SKULL_ITEM);
            //TODO check if player exists: Player otherPlayer = getPlayerByName(otherPlayerName);
            skullMeta.setOwner(otherPlayerName);
            head.setItemMeta(skullMeta);
            putHeadInInventory(head, player);
        }
        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + "The heads are placed in your inventory.");
    }

    public static void search(final Player player, final String searchString) {
        Thread t = new Thread() {

            @Override
            public void run() {
                List<ItemStack> heads = null;
                try {
                    heads = HeadCreator.getItemStacks(HeadsUtils.getInstance().getHeads(searchString));
                } catch (SocketTimeoutException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, ex.getMessage());
                    player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "Could not connect to heads database. Please try again later.");
                    return;
                } catch (MalformedURLException ex) {
                    // prob no heads found
                    Bukkit.getLogger().log(Level.WARNING, "Malformed url exception, probably caused by there not being any results to a search request");
                } catch (UnknownHostException ex){
                    Bukkit.getLogger().log(Level.WARNING, "The website returns a non-JSON format. Assume no results were found.");
                } catch (IOException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, null, ex);
                    player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "An unknown error occurred. Please contact an admin.");
                    return;
                } catch (AuthenticationException ex) {
                    HeadsInventory.getPlugin().getLogger().warning(ex.getMessage());
                    switch (HeadsPlugin.getHeadsPlugin().getAccessMode()) {
                        case LITE:
                            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "Searching through the user collection is not part of the lite version of this plugin.");
                            break;
                        case EXPIRED:
                            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "The trial for the plugin expired. Searching through the user collection is only part of the full or trial version of the plugin.");
                            break;
                        case NONE:
                            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "The plugin has not been properly configured. Please contact the server owner");
                            break;
                    }
                    return;
                }
                if (heads == null || heads.isEmpty()) {
                    player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "No heads found.");
                    return;
                }

                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + "Choose a head from the inventory...");
                showInventory(searchString, player, heads);
            }

        };
        t.start();
    }

    public static void searchFirst(final Player player, final String searchString) {
        Thread t = new Thread() {

            @Override
            public void run() {
                ItemStack head = null;
                try {
                    head = HeadCreator.getItemStack(HeadsUtils.getInstance().getHead(searchString));
                } catch (SocketTimeoutException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, null, ex);
                    player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "Could not connect to heads database. Please try again later.");
                    return;
                } catch (UnknownHostException ex){
                    Bukkit.getLogger().log(Level.WARNING, "The website returns a non-JSON format. Assume no results were found.");
                } catch (IOException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, null, ex);
                    player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "An unknown error occurred. Please contact an admin.");
                    return;
                } catch (AuthenticationException ex) {
                    HeadsInventory.getPlugin().getLogger().warning(ex.getMessage());
                    switch (HeadsPlugin.getHeadsPlugin().getAccessMode()) {
                        case LITE:
                            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "Searching through the user collection is not part of the lite version of this plugin.");
                            break;
                        case EXPIRED:
                            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "The trial for the plugin expired. Searching through the user collection is only part of the full or trial version of the plugin.");
                            break;
                        case NONE:
                            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "The plugin has not been properly configured. Please contact the server owner");
                            break;
                    }
                    return;
                }
                if (head == null) {
                    player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "No heads found.");
                    return;
                }
                putHeadInInventory(head, player);
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + "The head is placed in your inventory.");
            }

        };
        t.start();
    }

    public static boolean searchAllCategories(Player player) {
        List<ItemStack> heads = HeadCreator.getItemStacks(HeadsUtils.getInstance().getAllCategoryHeads());
        if (heads == null || heads.isEmpty()) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "No heads found.");
            return false;
        }

        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + "Choose a head from the inventory...");
        showInventory("All categories", player, heads);
        return true;

    }

    public static boolean searchCategory(Player player, String categoryName) {

        // check if given category name exists
        boolean flag = false;
        for (HeadsCategory category : HeadsUtils.getInstance().getCategories().getList()) {
            if (categoryName.equalsIgnoreCase(category.getCategoryName())) {
                flag = true;
                break;
            }
        }

        if (!flag) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "No category found with that name, possible categories: ");
            sendCategoriesList(player);
            return false;
        }

        List<ItemStack> heads = HeadCreator.getItemStacks(HeadsUtils.getInstance().getCategoryHeads(categoryName));
        if (heads == null || heads.isEmpty()) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + "No heads found.");
            return false;
        }

        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + "Choose a head from the inventory...");
        showInventory(categoryName, player, heads);
        return true;
    }

    private static void showInventory(String menuName, Player player, List<ItemStack> heads) {
        final HeadsInventoryMenu menu = new HeadsInventoryMenu(player, menuName, heads);
        Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getServer().getPluginManager().getPlugin("HeadsInventory"), new Runnable() {
            @Override
            public void run() {
                menu.getInventoryPages().get(0).open();
            }
        }, 5);
    }

    public static void putHeadInInventory(ItemStack head, Player player) {
        player.getInventory().addItem(head);
    }

    /**
     * Gets online player from name
     *
     * @param name the name of the player
     * @return the player
     */
    public static Player getPlayerByName(String name) {
        // if online
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }

        // if not online
        return Bukkit.getPlayer(name);
    }

    public static void sendCategoriesList(CommandSender sender) {
        List<HeadsCategory> categories = HeadsUtils.getInstance().getCategories().getList();
        List<String> categoryNames = new ArrayList<>();
        for (HeadsCategory category : categories) {
            categoryNames.add(category.getCategoryName());
        }
        sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GOLD + StringUtils.join(categoryNames, ", "));
    }

    public static void setItemName(ItemStack item, String name) {
        ItemMeta im = item.getItemMeta();
        im.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(im);
    }
}
