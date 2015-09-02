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

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class HeadsInventoryMenu {

    private int pageCount;
    private final String menuName;
    private final List<HeadsInventoryPage> inventoryPages;
    private final Player player;
    private final int rowCount = 5;

    public HeadsInventoryMenu(Player player, String menuName, List<ItemStack> heads) {
        this.pageCount = 0;
        this.menuName = menuName;
        this.inventoryPages = new ArrayList<>();
        this.player = player;
        HeadsInventoryPage currentPage = null;
        int i = 0;
        for (ItemStack head : heads) {
            if (i == 0) {
                pageCount++;
                if (pageCount > 1) {
                    currentPage.setRightArrow();
                }
                currentPage = new HeadsInventoryPage(this, pageCount);
                inventoryPages.add(currentPage);
                currentPage.setDownArrow();
                if (pageCount > 1) {
                    currentPage.setLeftArrow();
                }
            }
            currentPage.putHead(i, head);
            i = (i + 1) % (9 * rowCount);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public String getMenuName() {
        return menuName;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getInventorySize() {
        return (rowCount + 1) * 9;
    }

    public List<HeadsInventoryPage> getInventoryPages() {
        return inventoryPages;
    }

}
