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
import com.github.cc007.headsplugin.api.HeadsPluginServices;
import com.github.cc007.headsplugin.api.business.domain.Category;
import com.github.cc007.headsplugin.api.business.domain.exceptions.LockingException;
import com.github.cc007.headsplugin.api.business.services.heads.CategorySearcher;
import com.github.cc007.headsplugin.api.business.services.heads.HeadSearcher;
import com.github.cc007.headsplugin.api.business.services.heads.HeadToItemstackMapper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class CategoriesMenu implements Listener {

    private static final int CATEGORY_NAME_INDEX = 0;
    private static final int CATEGORY_HEAD_UUID_INDEX = 1;

    private static final CategorySearcher categorySearcher;
    private static final HeadSearcher headSearcher;
    private static final HeadToItemstackMapper headToItemstackMapper;
    private static final Translator translator;
    private static final HeadsInventory plugin;


    static {
        HeadsPluginServices apiServices = HeadsPluginApi.getHeadsPluginServices().orElseThrow(IllegalStateException::new);
        categorySearcher = apiServices.categorySearcher();
        headSearcher = apiServices.headSearcher();
        headToItemstackMapper = apiServices.headToItemstackMapper();
        translator = HeadsInventory.getTranslator();
        plugin = Objects.requireNonNull(HeadsInventory.getPlugin());
    }

    private final Player player;
    private final Inventory categoriesInventory;

    public CategoriesMenu(Player player) throws LockingException {
        this.player = player;
        this.categoriesInventory = createCategoriesInventory();
    }

    private Inventory createCategoriesInventory() throws LockingException {
        FileConfiguration config = plugin.getConfig();
        List<List<CategoryIcon>> rows = getMenuContents(config);
        Inventory categoriesInventory = Bukkit.createInventory(player, rows.size() * 9, ChatColor.DARK_BLUE + HeadsInventory.pluginChatPrefix(false) + translator.getText("categoriesmenu-gui-categoriestitle") + ":");

        List<Category> categories = getSortedCategories();

        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            List<CategoryIcon> cols = rows.get(rowIdx);
            for (int colIdx = 0; colIdx < cols.size(); colIdx++) {
                CategoryIcon categoryIcon = cols.get(colIdx);
                setCategoryItemFromIcon(categoriesInventory, categories, rowIdx, colIdx, categoryIcon);
            }
        }
        return categoriesInventory;
    }

    private List<Category> getSortedCategories() {
        return categorySearcher.getCategories()
                .stream()
                .sorted(Comparator.comparing(Category::getName))
                .toList();
    }

    private void setCategoryItemFromIcon(Inventory categoriesInventory, List<Category> categories, int rowIdx, int colIdx, CategoryIcon categoryIcon) {
        String categoryName = categoryIcon.categoryName();
        if (!"empty".equals(categoryName)) {
            Optional<Category> optionalCategory = categories.stream()
                    .filter((c) -> c.getName().equals(categoryName))
                    .findAny();
            if (optionalCategory.isEmpty()) {
                player.sendMessage(HeadsInventory.pluginChatPrefix(true) + ChatColor.RED + translator.getText("categoriesmenu-error-categorynotloaded"));
                HeadsInventory.getPlugin().getLogger().warning(translator.getText("categoriesmenu-warning-categorynotloaded") + "(category: " + categoryName + ")");
                return;
            }

            UUID headUuid = UUID.fromString(categoryIcon.categoryHeadUuid());
            ItemStack showHeadItem = headSearcher.getHead(headUuid)
                    .map(showHead -> {
                        showHead.setName(ChatColor.RESET + categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1));
                        return headToItemstackMapper.getItemStack(showHead);
                    })
                    .orElseGet(() -> {
                        HeadsInventory.getPlugin().getLogger().warning(translator.getText("categoriesmenu-warning-headnotavailable") + "(category: " + categoryName + ")");
                        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
                        if (headItem.getItemMeta() instanceof SkullMeta headItemMeta) {
                            headItemMeta.setDisplayName(ChatColor.RESET + categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1));
                            headItem.setItemMeta(headItemMeta);
                        }
                        return headItem;
                    });

            categoriesInventory.setItem(rowIdx * 9 + colIdx, showHeadItem);
        }
    }

    public void open() {
        registerEvents();
        player.openInventory(categoriesInventory);
    }

    @SuppressWarnings("unchecked")
    private List<List<CategoryIcon>> getMenuContents(FileConfiguration config) {
        final List<List<List<String>>> rawMenuContents = (List<List<List<String>>>) config.get("menuContents");
        return Objects.requireNonNull(rawMenuContents).stream().map(
                row -> row.stream().map(
                        col -> new CategoryIcon(col.get(CATEGORY_NAME_INDEX), col.get(CATEGORY_HEAD_UUID_INDEX))
                ).toList()
        ).toList();
    }

    public final void registerEvents() {
        Bukkit.getServer()
                .getPluginManager()
                .registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.DARK_BLUE + HeadsInventory.pluginChatPrefix(false) + translator.getText("categoriesmenu-gui-categoriestitle") + ":")) {
            return;
        }

        if (player != null && !event.getPlayer().equals(player)) {
            return;
        }

        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        FileConfiguration config = plugin.getConfig();

        if (!event.getView().getTitle().equals(ChatColor.DARK_BLUE + HeadsInventory.pluginChatPrefix(false) + translator.getText("categoriesmenu-gui-categoriestitle") + ":")) {
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
        List<List<CategoryIcon>> rows = getMenuContents(config);

        if ((slot < 0 || row > rows.size() - 1)) {
            return;
        }
        CategoryIcon categoryIcon = rows.get(row).get(col);
        String categoryName = categoryIcon.categoryName();
        if ("empty".equals(categoryName)) {
            return;
        }
        Bukkit.dispatchCommand(player, "headsinv category " + categoryName);
        player.updateInventory();
        Bukkit.getScheduler().scheduleSyncDelayedTask(HeadsInventory.getPlugin(), player::closeInventory);
    }
}
