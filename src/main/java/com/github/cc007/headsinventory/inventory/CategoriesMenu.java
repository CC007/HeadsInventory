/*
 * The MIT License
 *
 * Copyright 2016 Rik Schaaf aka CC007 (http://coolcat007.nl/).
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
package com.github.cc007.headsinventory.inventory;

import com.github.cc007.headsinventory.HeadsInventory;
import com.github.cc007.headsplugin.bukkit.HeadCreator;
import com.github.cc007.headsplugin.utils.HeadsUtils;
import com.github.cc007.headsplugin.utils.heads.Head;
import com.github.cc007.headsplugin.utils.heads.HeadsCategories;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class CategoriesMenu implements Listener {

    private Inventory inventory;
    private Player player;

    public CategoriesMenu(Player player) {
        this.player = player;
    }

    public void open() {

        if (inventory == null) {
            List rows = (List) HeadsInventory.getPlugin().getConfig().get("menuContents");
            inventory = Bukkit.createInventory(player, rows.size() * 9, ChatColor.DARK_BLUE + HeadsInventory.pluginChatPrefix(false) + "Categories:");

            HeadsCategories categories = HeadsUtils.getInstance().getCategories();
            for (int i = 0; i < rows.size(); i++) {
                List<List<Integer>> cols = (List<List<Integer>>) rows.get(i);
                for (int j = 0; j < cols.size(); j++) {
                    int id = cols.get(j).get(0);
                    if (id != 0) {
                        if (categories.getCategory(id) == null) {
                            player.sendMessage("This command isn't available at the moment.");
                            Bukkit.getLogger().warning(HeadsInventory.pluginChatPrefix(false) + "Player tried to open the category menu, but not all categories were loaded");
                            return;
                        }
                        Head showHead = categories.getCategory(id).getList().get(cols.get(j).get(1));
                        String catName = categories.getCategory(id).getCategoryName();
                        showHead.setName(ChatColor.RESET + catName.substring(0, 1).toUpperCase() + catName.substring(1));
                        ItemStack showHeadItem = HeadCreator.getItemStack(showHead);

                        inventory.setItem(i * 9 + j, showHeadItem);
                    }
                }

            }

        }
        registerEvents();
        player.openInventory(inventory);
    }

    public final void registerEvents() {
        Bukkit.getServer().getPluginManager().registerEvents(this, HeadsInventory.getPlugin());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().getTitle().equals(ChatColor.DARK_BLUE + HeadsInventory.pluginChatPrefix(false) + "Categories:")) {
            return;
        }
        Bukkit.getLogger().fine("Menu name correct");

        if (player != null && !event.getPlayer().equals(player)) {
            return;
        }
        Bukkit.getLogger().fine("Player name correct");

        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().getTitle().equals(ChatColor.DARK_BLUE + HeadsInventory.pluginChatPrefix(false) + "Categories:")) {
            return;
        }
        Bukkit.getLogger().fine("Menu name correct");

        if (player != null && !event.getWhoClicked().equals(player)) {
            return;
        }
        Bukkit.getLogger().fine("Player name correct");
        event.setCancelled(true);

        if (event.getClick() != ClickType.LEFT) {
            return;
        }
        int slot = event.getRawSlot();

        int row = slot / 9;
        int col = slot % 9;
        List rows = (List) HeadsInventory.getPlugin().getConfig().get("menuContents");

        if ((slot < 0 || row > rows.size() - 1)) {
            return;
        }
        List<Integer> head = ((List<List<List<Integer>>>) rows).get(row).get(col);
        int id = head.get(0);
        if (id == 0) {
            return;
        }
        Bukkit.dispatchCommand(player, "headsinv category " + HeadsUtils.getInstance().getCategories().getCategory(id).getCategoryName());
        player.updateInventory();
        Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getServer().getPluginManager().getPlugin("HeadsInventory"), new Runnable() {
            @Override
            public void run() {
                player.closeInventory();
            }
        });
    }

}
