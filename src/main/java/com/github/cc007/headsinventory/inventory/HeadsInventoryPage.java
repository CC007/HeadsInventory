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
import com.github.cc007.headsinventory.events.HeadGivenEvent;
import com.github.cc007.headsinventory.locale.Translator;
import com.github.cc007.headsinventory.search.HeadsSearch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class HeadsInventoryPage implements Listener {
    private static final Material PLAYER_HEAD = Material.PLAYER_HEAD;

    private Inventory inventory;
    private final HeadsInventoryMenu menu;
    private final int pageNr;
    private final Map<Integer, ItemStack> items;

    public HeadsInventoryPage(HeadsInventoryMenu menu, int pageNr) {
        this.menu = menu;
        this.pageNr = pageNr;
        this.items = new HashMap<>();
    }

    public final void registerEvents() {
        Bukkit.getServer().getPluginManager().registerEvents(this, Objects.requireNonNull(HeadsInventory.getPlugin()));
    }

    public void putHead(int index, ItemStack head) {
        this.items.put(index, head);
    }

    public void setLeftArrow() {
        Translator t = HeadsInventory.getTranslator();
        ItemStack head = new ItemStack(PLAYER_HEAD, 1);
        SkullMeta skullMeta2 = (SkullMeta) Bukkit.getItemFactory().getItemMeta(PLAYER_HEAD);
        skullMeta2.setOwner("MHF_ArrowLeft");
        head.setItemMeta(skullMeta2);
        HeadsSearch.setItemName(head, t.getText("headsinvpage-gui-previous"));
        items.put(menu.getRowCount() * 9, head);
    }

    public void setRightArrow() {
        Translator t = HeadsInventory.getTranslator();
        ItemStack head = new ItemStack(PLAYER_HEAD, 1);
        SkullMeta skullMeta1 = (SkullMeta) Bukkit.getItemFactory().getItemMeta(PLAYER_HEAD);
        skullMeta1.setOwner("MHF_ArrowRight");
        head.setItemMeta(skullMeta1);
        HeadsSearch.setItemName(head, t.getText("headsinvpage-gui-next"));
        items.put(menu.getRowCount() * 9 + 8, head);
    }

    public void setDownArrow() {
        Translator t = HeadsInventory.getTranslator();
        ItemStack head = new ItemStack(PLAYER_HEAD, 1);
        SkullMeta skullMeta1 = (SkullMeta) Bukkit.getItemFactory().getItemMeta(PLAYER_HEAD);
        skullMeta1.setOwner("MHF_ArrowDown");
        head.setItemMeta(skullMeta1);
        HeadsSearch.setItemName(head, t.getText("headsinvpage-gui-close"));
        items.put(menu.getRowCount() * 9 + 4, head);
    }

    public void open() {
        Translator t = HeadsInventory.getTranslator();
        if (inventory == null) {
            inventory = Bukkit.createInventory(menu.getPlayer(), (menu.getRowCount() + 1) * 9, menu.getMenuName() + " " + t.getText("headsinvpage-gui-page") + " " + pageNr + "/" + menu.getPageCount());
            for (Map.Entry<Integer, ItemStack> entrySet : items.entrySet()) {
                if (entrySet.getValue() != null) {
                    inventory.setItem(entrySet.getKey(), entrySet.getValue());
                }
            }
        }
        registerEvents();
        menu.getPlayer().openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        Translator t = HeadsInventory.getTranslator();
        if (!event.getView().getTitle().equals(menu.getMenuName() + " " + t.getText("headsinvpage-gui-page") + " " + pageNr + "/" + menu.getPageCount())) {
            return;
        }

        if (menu.getPlayer() != null && !event.getPlayer().equals(menu.getPlayer())) {
            return;
        }

        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Translator t = HeadsInventory.getTranslator();
        // if the click is for a different inventory view, ignore event
        if (!event.getView().getTitle().equals(menu.getMenuName() + " " + t.getText("headsinvpage-gui-page") + " " + pageNr + "/" + menu.getPageCount())) {
            return;
        }

        // if a different player clicked, ignore event
        if (menu.getPlayer() != null && !event.getWhoClicked().equals(menu.getPlayer())) {
            return;
        }
        // now we know that this is the right inventory, so you shouldn't be able to pick up any item in the inventory
        event.setCancelled(true);

        // ignore anything but left-clicks
        if (event.getClick() != ClickType.LEFT) {
            return;
        }

        // get the slot that was clicked and check if it is in the right part of the inventory
        // and not in the player's own inventory
        int slot = event.getRawSlot();
        if ((slot < 0 || slot >= menu.getInventorySize()) || !items.containsKey(slot)) {
            return;
        }

        // check if it is any of the slots that contain heads and if so give that head to the player
        if (slot / 9 != menu.getRowCount()) {
            menu.getPlayer().getInventory().addItem(items.get(slot));
            Bukkit.getServer().getPluginManager().callEvent(new HeadGivenEvent(menu.getPlayer(), items.get(slot), menu.getPlayer().getWorld(), new Date()));
            menu.getPlayer().sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.GREEN + t.getText("search-info-headgiven"));
            return;
        }

        // check if the previous button was clicked and if so open the previous page
        if (slot == menu.getRowCount() * 9) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(Objects.requireNonNull(HeadsInventory.getPlugin()), () -> {
                menu.getInventoryPages().get(pageNr - 2).open();
            }, 5);
            HandlerList.unregisterAll(this);
            return;
        }
        // check if the next button was clicked and if so open the next page
        if (slot == ((menu.getRowCount() + 1) * 9) - 1) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(Objects.requireNonNull(HeadsInventory.getPlugin()), () -> {
                menu.getInventoryPages().get(pageNr).open();
            }, 5);
            HandlerList.unregisterAll(this);
            return;
        }
        // only other button could be the close button, so close the inventory
        menu.getPlayer().updateInventory();
        Bukkit.getScheduler().scheduleSyncDelayedTask(Objects.requireNonNull(HeadsInventory.getPlugin()), () -> {
            menu.getPlayer().closeInventory();
        });
    }
}
