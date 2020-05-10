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
import com.github.cc007.headsinventory.locale.Translator;
import com.github.cc007.headsplugin.api.HeadsPluginApi;
import com.github.cc007.headsplugin.api.business.domain.Category;
import com.github.cc007.headsplugin.api.business.domain.Head;
import com.github.cc007.headsplugin.api.business.services.heads.CategorySearcher;
import com.github.cc007.headsplugin.api.business.services.heads.HeadCreator;
import com.github.cc007.headsplugin.api.business.services.heads.HeadSearcher;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
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

    private final int CATEGORY_NAME_INDEX = 0;
    private final int CATEGORY_HEAD_UUID_INDEX = 1;

    private Inventory inventory;
    private Player player;

    public CategoriesMenu(Player player) {
        this.player = player;
    }

    public void open() {
        Translator t = HeadsInventory.getTranslator();
        HeadsPluginApi api = HeadsPluginApi.getInstance();
        CategorySearcher categorySearcher = api.getCategorySearcher();
        HeadSearcher headSearcher = api.getHeadSearcher();
        HeadCreator headCreator = api.getHeadCreator();

        if (inventory == null) {
            FileConfiguration config = Objects.requireNonNull(HeadsInventory.getPlugin()).getConfig();
            List<List<List<String>>> rows = getMenuContents(config);
            inventory = Bukkit.createInventory(player, rows.size() * 9, ChatColor.DARK_BLUE + HeadsInventory.pluginChatPrefix(false) + t.getText("categoriesmenu-gui-categoriestitle") + ":");

            List<Category> categories = categorySearcher.getCategories()
                    .stream()
                    .sorted(Comparator.comparing(Category::getName))
                    .collect(Collectors.toList());

            for (int i = 0; i < rows.size(); i++) {
                List<List<String>> cols = rows.get(i);
                for (int j = 0; j < cols.size(); j++) {
                    List<String> categoryTuple = cols.get(j);
                    String categoryName = categoryTuple.get(CATEGORY_NAME_INDEX);
                    if (!"empty".equals(categoryName)) {
                        Optional<Category> optionalCategory = categories.stream()
                                .filter((c) -> c.getName().equals(categoryName))
                                .findAny();
                        if (!optionalCategory.isPresent()) {
                            player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + t.getText("categoriesmenu-error-categorynotloaded"));
                            HeadsInventory.getPlugin().getLogger().warning(t.getText("categoriesmenu-warning-categorynotloaded"));
                            return;
                        }
                        Category category = optionalCategory.get();

                        UUID headUuid = UUID.fromString(categoryTuple.get(CATEGORY_HEAD_UUID_INDEX));
                        Head showHead = headSearcher.getHead(headUuid)
                                .orElseThrow(() -> new IllegalArgumentException("UUID for category head overview not valid (category: " + categoryName + ")"));

                        showHead.setName(ChatColor.RESET + categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1));
                        ItemStack showHeadItem = headCreator.getItemStack(showHead);

                        inventory.setItem(i * 9 + j, showHeadItem);
                    }
                }

            }

        }
        registerEvents();
        player.openInventory(inventory);
    }

    @SuppressWarnings("unchecked")
    private List<List<List<String>>> getMenuContents(FileConfiguration config) {
        return (List<List<List<String>>>) config.get("menuContents");
    }

    public final void registerEvents() {
        Bukkit.getServer().getPluginManager().registerEvents(this, Objects.requireNonNull(HeadsInventory.getPlugin()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        Translator t = HeadsInventory.getTranslator();
        if (!event.getView().getTitle().equals(ChatColor.DARK_BLUE + HeadsInventory.pluginChatPrefix(false) + t.getText("categoriesmenu-gui-categoriestitle") + ":")) {
            return;
        }

        if (player != null && !event.getPlayer().equals(player)) {
            return;
        }

        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        FileConfiguration config = Objects.requireNonNull(HeadsInventory.getPlugin()).getConfig();
        Translator t = HeadsInventory.getTranslator();

        if (!event.getView().getTitle().equals(ChatColor.DARK_BLUE + HeadsInventory.pluginChatPrefix(false) + t.getText("categoriesmenu-gui-categoriestitle") + ":")) {
            return;
        }

        if (player != null && !event.getWhoClicked().equals(player)) {
            return;
        }
        event.setCancelled(true);

        if (event.getClick() != ClickType.LEFT) {
            return;
        }
        int slot = event.getRawSlot();

        int row = slot / 9;
        int col = slot % 9;
        List<List<List<String>>> rows = getMenuContents(config);

        if ((slot < 0 || row > rows.size() - 1)) {
            return;
        }
        List<String> categoryTuple = rows.get(row).get(col);
        String categoryName = categoryTuple.get(CATEGORY_NAME_INDEX);
        if ("empty".equals(categoryName)) {
            return;
        }
        Bukkit.dispatchCommand(player, "headsinv category " + categoryName);
        player.updateInventory();
        Bukkit.getScheduler().scheduleSyncDelayedTask(HeadsInventory.getPlugin(), player::closeInventory);
    }
}
