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
package com.github.cc007.headsinventory.inventory;

import com.github.cc007.headsinventory.HeadsInventory;
import com.github.cc007.headsinventory.search.HeadsSearch;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class HeadsInventoryPage implements Listener {

    private Inventory inventory;
    private final HeadsInventoryMenu menu;
    private final int pageNr;
    private final Map<Integer, ItemStack> items;

    public HeadsInventoryPage(HeadsInventoryMenu menu, int pageNr) {
        this.menu = menu;
        this.pageNr = pageNr;
        this.items = new HashMap<>();
        Bukkit.getServer().getPluginManager().registerEvents(this, Bukkit.getServer().getPluginManager().getPlugin("HeadsInventory"));
    }

    public void putHead(int index, ItemStack head) {
        this.items.put(index, head);
    }

    public void setLeftArrow() {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (byte) SkullType.PLAYER.ordinal());
        SkullMeta skullMeta2 = (SkullMeta) Bukkit.getItemFactory().getItemMeta(Material.SKULL_ITEM);
        skullMeta2.setOwner("MHF_ArrowLeft");
        head.setItemMeta(skullMeta2);
        HeadsSearch.setItemName(head, "Previous");
        items.put(menu.getRowCount() * 9, head);
    }

    public void setRightArrow() {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (byte) SkullType.PLAYER.ordinal());
        SkullMeta skullMeta1 = (SkullMeta) Bukkit.getItemFactory().getItemMeta(Material.SKULL_ITEM);
        skullMeta1.setOwner("MHF_ArrowRight");
        head.setItemMeta(skullMeta1);
        HeadsSearch.setItemName(head, "Next");
        items.put(((menu.getRowCount() + 1) * 9) - 1, head);
    }

    public void open() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(menu.getPlayer(), (menu.getRowCount() + 1) * 9, menu.getMenuName() + " page " + pageNr + "/" + menu.getPageCount());
            for (Map.Entry<Integer, ItemStack> entrySet : items.entrySet()) {
                if (entrySet.getValue() != null) {
                    inventory.setItem(entrySet.getKey(), entrySet.getValue());
                }
            }
        }
        menu.getPlayer().openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Bukkit.getLogger().fine("Inventory click");
        if (event.getInventory().getTitle().equals(menu.getMenuName() + " page " + pageNr + "/" + menu.getPageCount())) {
            Bukkit.getLogger().fine("Menu name correct");
            if (menu.getPlayer() == null || event.getWhoClicked() == menu.getPlayer()) {
                Bukkit.getLogger().fine("Player name correct");
                event.setCancelled(true);
                if (event.getClick() != ClickType.LEFT) {
                    return;
                }
                int slot = event.getRawSlot();
                if (slot >= 0 && slot < menu.getInventorySize() && items.containsKey(slot)) {
                    if (slot / 9 != menu.getRowCount()) {
                        menu.getPlayer().getInventory().addItem(items.get(slot));
                        menu.getPlayer().sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + "Here's the skull");
                        return;
                    }
                    if (slot == menu.getRowCount() * 9) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getServer().getPluginManager().getPlugin("HeadsInventory"), new Runnable() {
                            @Override
                            public void run() {
                                menu.getInventoryPages().get(pageNr - 2).open();
                            }
                        }, 5);
                        return;
                    }
                    if (slot == ((menu.getRowCount() + 1) * 9) - 1) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getServer().getPluginManager().getPlugin("HeadsInventory"), new Runnable() {
                            @Override
                            public void run() {
                                menu.getInventoryPages().get(pageNr).open();
                            }
                        }, 5);
                    }
                    menu.getPlayer().updateInventory();
                    Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getServer().getPluginManager().getPlugin("HeadsInventory"), new Runnable() {
                        @Override
                        public void run() {
                            menu.getPlayer().closeInventory();
                        }
                    });
                }
            }
        }
    }
}
