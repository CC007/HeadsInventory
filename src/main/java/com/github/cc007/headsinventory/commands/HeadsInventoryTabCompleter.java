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
import com.github.cc007.headsplugin.api.HeadsPluginApi;
import com.github.cc007.headsplugin.api.HeadsPluginServices;
import com.github.cc007.headsplugin.api.business.domain.Category;
import com.github.cc007.headsplugin.api.business.domain.exceptions.LockingException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Autom
 * @author CC007 (http://coolcat007.nl/)
 */
public class HeadsInventoryTabCompleter implements TabCompleter {

    private final HeadsInventory plugin;

    public HeadsInventoryTabCompleter(HeadsInventory plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        HeadsPluginServices api = HeadsPluginApi.getHeadsPluginServices().orElseThrow(IllegalStateException::new);
        if (command.getName().equalsIgnoreCase("updateheads") && args.length == 1) {
            String partialCommand = args[0];
            Set<String> commands;
            try {
                commands = api.categorySearcher().getCategories().stream().map(Category::getName).collect(Collectors.toSet());
            } catch (LockingException e) {
                commands = new HashSet<>();
            }
            commands.add("all");
            commands.add("");
            StringUtil.copyPartialMatches(partialCommand, commands, completions);
        }
        if (command.getName().equalsIgnoreCase("headsinventory") && args.length == 1) {
            String partialCommand = args[0];
            List<String> commands = Arrays.asList(
                    "cat", "category", "categories",
                    "search", "fsearch", "msearch", "mhsearch",
                    "searchfirst", "fsearchfirst", "msearchfirst", "mhsearchfirst",
                    "getfirst", "fgetfirst", "mgetfirst", "mhgetfirst",
                    "help", ""
            );
            StringUtil.copyPartialMatches(partialCommand, commands, completions);
        }
        if (command.getName().equalsIgnoreCase("headsinventory") && args.length == 2) {
            if (args[0].equalsIgnoreCase("categories") || args[0].equalsIgnoreCase("category") || args[0].equalsIgnoreCase("cat")) {
                String partialCommand = args[1];
                Set<String> commands;
                try {
                    commands = api.categorySearcher().getCategories().stream().map(Category::getName).collect(Collectors.toSet());
                } catch (LockingException e) {
                    commands = new HashSet<>();
                }
                commands.add("all");
                commands.add("");
                StringUtil.copyPartialMatches(partialCommand, commands, completions);
            }
        }

        Collections.sort(completions);

        return completions;
    }

}
