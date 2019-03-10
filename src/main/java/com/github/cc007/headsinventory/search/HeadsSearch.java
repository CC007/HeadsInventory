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
import com.github.cc007.headsplugin.HeadsPlugin;
import com.github.cc007.headsplugin.bukkit.HeadCreator;
import com.github.cc007.headsplugin.exceptions.AuthenticationException;
import com.github.cc007.headsplugin.utils.HeadsUtils;
import com.github.cc007.headsplugin.utils.MinecraftVersion;
import com.github.cc007.headsplugin.utils.heads.Head;
import com.github.cc007.headsplugin.utils.heads.HeadsCategory;
import com.github.cc007.headsplugin.utils.loader.FreshCoalLoader;
import com.github.cc007.headsplugin.utils.loader.MineSkinLoader;
import com.github.cc007.headsplugin.utils.loader.MinecraftHeadsLoader;
import java.io.IOException;
import java.lang.reflect.Field;
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
    private static Material skull;
    static {
        MinecraftVersion version = new MinecraftVersion();
        String skullName;
        if(version.getMinor() > 12){
            skullName = "PLAYER_HEAD";
        } else {
            skullName = "SKULL_ITEM";
        }
        try{
            Class<?> obMaterialClass = Class.forName("org.bukkit.Material");
            Field skull = obMaterialClass.getDeclaredField(skullName);
            HeadsSearch.skull = (Material) skull.get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            HeadsSearch.skull = null;
        }
    }
    
    public static void myHead(Player player) {
        Translator t = HeadsInventory.getTranslator();
        ItemStack head = new ItemStack(skull, 1, (byte) SkullType.PLAYER.ordinal());
        SkullMeta skullMeta = (SkullMeta) Bukkit.getItemFactory().getItemMeta(skull);
        skullMeta.setOwner(player.getName());
        head.setItemMeta(skullMeta);
        putHeadInInventory(head, player);
        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + t.getText("search-info-headgiven"));
    }

    public static void playerHead(Player player, String[] otherPlayers) {
        Translator t = HeadsInventory.getTranslator();
        for (String otherPlayerName : otherPlayers) {
            ItemStack head = new ItemStack(skull, 1, (byte) SkullType.PLAYER.ordinal());
            SkullMeta skullMeta = (SkullMeta) Bukkit.getItemFactory().getItemMeta(skull);
            //TODO check if player exists: Player otherPlayer = getPlayerByName(otherPlayerName);
            skullMeta.setOwner(otherPlayerName);
            head.setItemMeta(skullMeta);
            putHeadInInventory(head, player);
        }
        player.sendMessage(HeadsInventory.pluginChatPrefix(true)+ ChatColor.GREEN + t.getText("search-info-headgiven"));
    }

    public static void saveHead(Player player, String headName) {
        Translator t = HeadsInventory.getTranslator();
        Head newHead = null;
        try {
            HeadsUtils hu = HeadsUtils.getInstance();
            hu.setDatabaseLoader(new MineSkinLoader());
            newHead = hu.saveHead(player, headName);
            hu.setDatabaseLoader(HeadsPlugin.getDefaultDatabaseLoader());
        } catch (SocketTimeoutException ex) {
            Bukkit.getLogger().log(Level.SEVERE, ex.getMessage());
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("search-msg-sockettimeout"));
            return;
        } catch (MalformedURLException ex) {
            // prob no heads found
            Bukkit.getLogger().log(Level.WARNING, t.getText("search-warning-malformedurl"), ex);
        } catch (UnknownHostException ex) {
            Bukkit.getLogger().log(Level.WARNING, t.getText("search-warning-unknownhost"), ex);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, null, ex);
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("search-msg-io"));
            return;
        } catch (AuthenticationException ex) {
            //legacy exception
            Bukkit.getLogger().log(Level.SEVERE, null, ex);
            return;
        }
        if (newHead == null) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("search-error-addhead-failure"));
            return;
        }
        ItemStack newHeadStack = HeadCreator.getItemStack(newHead);
        putHeadInInventory(newHeadStack, player);
        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + t.getText("search-error-addhead-success"));
    }

    public static void search(final Player player, final String searchString, final String searchDatabase) {
        Translator t = HeadsInventory.getTranslator();
        Thread thread = new Thread(() -> {
            List<ItemStack> heads = null;
            try {
                switch (searchDatabase) {
                    case "freshcoal":
                        HeadsUtils.getInstance().setDatabaseLoader(new FreshCoalLoader());
                        break;
                    case "mineskin":
                        HeadsUtils.getInstance().setDatabaseLoader(new MineSkinLoader());
                        break;
                    case "minecraftheads":
                        HeadsUtils.getInstance().setDatabaseLoader(new MinecraftHeadsLoader(player));
                        break;
                    default:
                        HeadsUtils.getInstance().setDatabaseLoader(HeadsPlugin.getDefaultDatabaseLoader());
                }
                heads = HeadCreator.getItemStacks(HeadsUtils.getInstance().getHeads(searchString));
                HeadsUtils.getInstance().setDatabaseLoader(HeadsPlugin.getDefaultDatabaseLoader());
            } catch (SocketTimeoutException ex) {
                Bukkit.getLogger().log(Level.SEVERE, ex.getMessage());
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("search-msg-sockettimeout"));
                return;
            } catch (MalformedURLException ex) {
                // prob no heads found
                Bukkit.getLogger().log(Level.WARNING, t.getText("search-warning-malformedurl"), ex);
            } catch (UnknownHostException ex) {
                Bukkit.getLogger().log(Level.WARNING, t.getText("search-warning-unknownhost"), ex);
            } catch (IOException ex) {
                Bukkit.getLogger().log(Level.SEVERE, null, ex);
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("search-msg-io"));
                return;
            } catch (UnsupportedOperationException ex) {
                Bukkit.getLogger().log(Level.WARNING, null, ex);
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("search-msg-unsupported"));
                return;
            } catch (AuthenticationException ex) {
                //legacy exception
                Bukkit.getLogger().log(Level.SEVERE, null, ex);
                return;
            }
            if (heads == null || heads.isEmpty()) {
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GOLD + t.getText("search-msg-search-noheads"));
                return;
            }

            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + t.getText("search-msg-search-choose"));
            showInventory(searchString, player, heads);
        });
        thread.start();
    }

    public static void searchFirst(final Player player, final String searchString, final String searchDatabase) {
        Translator t = HeadsInventory.getTranslator();
        Thread thread = new Thread(() -> {
            ItemStack head = null;
            try {
                switch (searchDatabase) {
                    case "freshcoal":
                        HeadsUtils.getInstance().setDatabaseLoader(new FreshCoalLoader());
                        break;
                    case "mineskin":
                        HeadsUtils.getInstance().setDatabaseLoader(new MineSkinLoader());
                        break;
                    case "minecraftheads":
                        HeadsUtils.getInstance().setDatabaseLoader(new MinecraftHeadsLoader(player));
                        break;
                    default:
                        HeadsUtils.getInstance().setDatabaseLoader(HeadsPlugin.getDefaultDatabaseLoader());
                }
                head = HeadCreator.getItemStack(HeadsUtils.getInstance().getHead(searchString));
                HeadsUtils.getInstance().setDatabaseLoader(HeadsPlugin.getDefaultDatabaseLoader());
            } catch (SocketTimeoutException ex) {
                Bukkit.getLogger().log(Level.SEVERE, ex.getMessage());
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("search-msg-sockettimeout"));
                return;
            } catch (MalformedURLException ex) {
                // prob no heads found
                Bukkit.getLogger().log(Level.WARNING, t.getText("search-warning-malformedurl"), ex);
            } catch (UnknownHostException ex) {
                Bukkit.getLogger().log(Level.WARNING, t.getText("search-warning-unknownhost"), ex);
            } catch (IOException ex) {
                Bukkit.getLogger().log(Level.SEVERE, null, ex);
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("search-msg-io"));
                return;
            } catch (UnsupportedOperationException ex) {
                Bukkit.getLogger().log(Level.WARNING, null, ex);
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("search-msg-unsupported"));
                return;
            } catch (AuthenticationException ex) {
                //legacy exception
                Bukkit.getLogger().log(Level.SEVERE, null, ex);
                return;
            }
            if (head == null) {
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GOLD  + t.getText("search-msg-search-noheads"));
                return;
            }
            putHeadInInventory(head, player);
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + t.getText("search-info-headgiven"));
        });
        thread.start();
    }

    public static boolean searchAllCategories(Player player) {
        Translator t = HeadsInventory.getTranslator();
        List<ItemStack> heads = HeadCreator.getItemStacks(HeadsUtils.getInstance().getAllCategoryHeads());
        if (heads == null || heads.isEmpty()) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("search-msg-search-noheads"));
            return false;
        }

        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + t.getText("search-msg-search-choose"));
        showInventory(t.getText("search-gui-search-allcategories"), player, heads);
        return true;

    }

    public static boolean searchCategory(Player player, String categoryName) {
        Translator t = HeadsInventory.getTranslator();
        // check if given category name exists
        boolean flag = false;
        for (HeadsCategory category : HeadsUtils.getInstance().getCategories().getList()) {
            if (categoryName.equalsIgnoreCase(category.getCategoryName())) {
                flag = true;
                break;
            }
        }

        if (!flag) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("error-unknowncategory") + ": ");
            sendCategoriesList(player);
            return false;
        }

        List<ItemStack> heads = HeadCreator.getItemStacks(HeadsUtils.getInstance().getCategoryHeads(categoryName));
        if (heads == null || heads.isEmpty()) {
            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("search-msg-search-noheads"));
            return false;
        }

        player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + t.getText("search-msg-search-choose"));
        showInventory(categoryName, player, heads);
        return true;
    }

    private static void showInventory(String menuName, Player player, List<ItemStack> heads) {
        final HeadsInventoryMenu menu = new HeadsInventoryMenu(player, menuName, heads);
        Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getServer().getPluginManager().getPlugin("HeadsInventory"), () -> {
            menu.getInventoryPages().get(0).open();
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
