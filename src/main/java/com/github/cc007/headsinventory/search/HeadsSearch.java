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
import com.github.cc007.headsinventory.locale.Translator;
import com.github.cc007.headsplugin.api.HeadsPluginApi;
import com.github.cc007.headsplugin.api.business.domain.Category;
import com.github.cc007.headsplugin.api.business.domain.Head;
import com.github.cc007.headsplugin.api.business.services.heads.CategorySearcher;
import com.github.cc007.headsplugin.api.business.services.heads.HeadCreator;
import com.github.cc007.headsplugin.api.business.services.heads.HeadSearcher;
import com.github.cc007.headsplugin.api.business.services.heads.HeadToItemstackMapper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class HeadsSearch {

    public static void saveHead(Player player, String headName) {
        Translator t = HeadsInventory.getTranslator();
        HeadsPluginApi api = HeadsPluginApi.getInstance();
        HeadCreator headCreator = api.getHeadCreator();
        HeadToItemstackMapper headToItemstackMapper = api.getHeadToItemstackMapper();

        Map<String, Head> newHeadsMap = headCreator.createHead(player, headName);
        if (newHeadsMap.size() == 0) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("addhead-msg-failure"));
            return;
        }

        for (Head newHead : newHeadsMap.values()) {
            ItemStack newHeadStack = headToItemstackMapper.getItemStack(newHead);
            putHeadInInventory(newHeadStack, player);
        }
        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + t.getText("addhead-msg-success"));
    }

    public static void myHead(Player player) {
        Translator t = HeadsInventory.getTranslator();
        ItemStack headStack = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) Bukkit.getItemFactory().getItemMeta(Material.PLAYER_HEAD);
        Objects.requireNonNull(skullMeta).setOwningPlayer(player);
        headStack.setItemMeta(skullMeta);
        putHeadInInventory(headStack, player);
        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + t.getText("search-info-headgiven"));
    }

    public static void playerHead(Player player, String[] otherPlayers) {
        Translator t = HeadsInventory.getTranslator();
        for (String otherPlayerName : otherPlayers) {
            ItemStack headStack = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta skullMeta = (SkullMeta) Bukkit.getItemFactory().getItemMeta(Material.PLAYER_HEAD);
            OfflinePlayer otherPlayer = getPlayer(otherPlayerName);
            Objects.requireNonNull(skullMeta).setOwningPlayer(otherPlayer);
            headStack.setItemMeta(skullMeta);
            putHeadInInventory(headStack, player);
        }
        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + t.getText("search-info-headgiven"));
    }

    /**
     * Gets online player from name, or if the player isn't online, get the offline player
     *
     * @param name the name of the player
     * @return the player
     */
    public static OfflinePlayer getPlayer(String name) {
        // if online
        Optional<? extends Player> onlinePlayer = Bukkit.getServer().getOnlinePlayers()
                .stream()
                .filter((p) -> name.equalsIgnoreCase(p.getName()))
                .findAny();
        if (onlinePlayer.isPresent()) {
            return onlinePlayer.get();
        } else {
            return Bukkit.getServer().getOfflinePlayer(name);
        }
    }

    public static void searchFirst(final Player player, final String searchString, final String searchDatabase) {
        //TODO support searchdatabase
        Translator t = HeadsInventory.getTranslator();
        HeadsPluginApi api = HeadsPluginApi.getInstance();
        HeadToItemstackMapper headToItemstackMapper = api.getHeadToItemstackMapper();
        HeadSearcher headSearcher = api.getHeadSearcher();
        Thread thread = new Thread(() -> {
            ItemStack headStack = headToItemstackMapper.getItemStack(headSearcher.getHeads(searchString).get(0));

            if (headStack == null) {
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GOLD + t.getText("search-msg-search-noheads"));
                return;
            }

            putHeadInInventory(headStack, player);
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + t.getText("search-info-headgiven"));
        });
        thread.start();
    }

    public static void search(final Player player, final String searchString, final String searchDatabase) {
        //TODO support searchdatabase
        HeadSearcher headSearcher = HeadsPluginApi.getInstance()
                .getHeadSearcher();

        Thread thread = new Thread(() -> {
            List<Head> heads = headSearcher.getHeads(searchString);
            showInventory(searchString, player, heads, ChatColor.GOLD);
        });
        thread.start();
    }

    public static boolean searchAllCategories(Player player) {
        Translator t = HeadsInventory.getTranslator();
        CategorySearcher categorySearcher = HeadsPluginApi.getInstance()
                .getCategorySearcher();

        List<Head> allCategoriesHeads = categorySearcher.getCategories()
                .stream()
                .flatMap(category -> categorySearcher.getCategoryHeads(category).stream())
                .collect(Collectors.toList());
        //TODO order list

        showInventory(t.getText("search-gui-search-allcategories"), player, allCategoriesHeads, ChatColor.RED);
        return true;

    }

    public static boolean searchCategory(Player player, String categoryName) {
        Translator t = HeadsInventory.getTranslator();
        CategorySearcher categorySearcher = HeadsPluginApi.getInstance()
                .getCategorySearcher();

        // check if given category name exists
        boolean categoryExists = false;
        for (Category category : categorySearcher.getCategories()) {
            if (categoryName.equalsIgnoreCase(category.getName())) {
                categoryName = category.getName();
                categoryExists = true;
                break;
            }
        }

        if (!categoryExists) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-unknowncategory") + ": ");
            sendCategoriesList(player);
            return false;
        }

        List<Head> categoryHeads = new ArrayList<>(categorySearcher.getCategoryHeads(categoryName));
        //TODO order list

        return showInventory(categoryName, player, categoryHeads, ChatColor.RED);
    }

    private static boolean showInventory(String menuName, Player player, List<Head> categoryHeads, ChatColor noHeadsColor) {
        Translator t = HeadsInventory.getTranslator();
        HeadsPluginApi api = HeadsPluginApi.getInstance();
        HeadToItemstackMapper headToItemstackMapper = api.getHeadToItemstackMapper();

        List<ItemStack> headStacks = headToItemstackMapper.getItemStacks(categoryHeads);
        if (headStacks == null || headStacks.isEmpty()) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + noHeadsColor + t.getText("search-msg-search-noheads"));
            return false;
        }

        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + t.getText("search-msg-search-choose"));

        final HeadsInventoryMenu menu = new HeadsInventoryMenu(player, menuName, headStacks);
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                Objects.requireNonNull(HeadsInventory.getPlugin()),
                () -> menu.getInventoryPages()
                        .get(0)
                        .open(),
                5
        );
        return true;
    }

    public static void putHeadInInventory(ItemStack head, Player player) {
        player.getInventory().addItem(head);
    }

    public static void sendCategoriesList(CommandSender sender) {
        CategorySearcher categorySearcher = HeadsPluginApi.getInstance()
                .getCategorySearcher();

        String categoryNames = categorySearcher.getCategories()
                .stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));

        sender.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GOLD + categoryNames);
    }

    public static void setItemName(ItemStack item, String name) {
        ItemMeta im = item.getItemMeta();
        Objects.requireNonNull(im).setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(im);
    }
}
