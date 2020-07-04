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
package com.github.cc007.headsinventory;

import com.github.cc007.headsinventory.commands.HeadsInventoryCommand;
import com.github.cc007.headsinventory.commands.HeadsInventoryTabCompleter;
import com.github.cc007.headsinventory.locale.Translator;
import com.github.cc007.headsplugin.api.HeadsPluginApi;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang.LocaleUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class HeadsInventory extends JavaPlugin {
    
    private static Translator translator = null;

    private Plugin vault = null;
    private Permission permission = null;
    private FileConfiguration config = null;
    private File configFile = null;

    @Override
    public void onLoad() {
        getLogger().info("Added class loader to HeadsPlugin springClassLoaders");
        HeadsPluginApi.addSpringClassLoader(getClassLoader());
    }

    @Override
    public void onEnable() {
        /* Config stuffs */
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        /* Configure BStats metrics */
        Metrics metrics = new Metrics(this, 5875);

        /* Setup plugin hooks */
        vault = getPlugin("Vault");
        if (vault != null) {
            setupPermissions();
        }

        /* Register commands */
        HeadsInventoryCommand hic = new HeadsInventoryCommand(this);
        getCommand("heads").setExecutor(hic);
        getCommand("headsinventory").setExecutor(hic);
        getCommand("myhead").setExecutor(hic);
        getCommand("playerhead").setExecutor(hic);
        getCommand("addhead").setExecutor(hic);

        /* Register tab completers*/
        HeadsInventoryTabCompleter hitc = new HeadsInventoryTabCompleter(this);
        getCommand("headsinventory").setTabCompleter(hitc);
    }

    @Override
    public void onDisable() {
        vault = null;
        permission = null;
    }

    /**
     * Setup permissions
     *
     * @return True: Setup correctly, Didn't setup correctly
     */
    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);

        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }

        if (permission == null) {
            getLogger().log(Level.WARNING, "Could not hook Vault!");
        } else {
            getLogger().log(Level.INFO, "Hooked Vault!");
        }

        return (permission != null);
    }

    /**
     * Get the vault
     *
     * @return the vault
     */
    public Plugin getVault() {
        return vault;
    }

    /**
     * Get the permissions
     *
     * @return the permissions
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * Gets a plugin
     *
     * @param pluginName Name of the plugin to get
     * @return The plugin from name
     */
    protected Plugin getPlugin(String pluginName) {
        if (getServer().getPluginManager().getPlugin(pluginName) != null && getServer().getPluginManager().getPlugin(pluginName).isEnabled()) {
            return getServer().getPluginManager().getPlugin(pluginName);
        } else {
            getLogger().log(Level.WARNING, "Could not find plugin \"{0}\"!", pluginName);
            return null;
        }
    }

    /**
     * get the minecraft chat prefix for this plugin
     *
     * @param colored whether or not the prefix should be colored
     * @return the minecraft chat prefix for this plugin
     */
    public static String pluginChatPrefix(boolean colored) {
        if (colored) {
            return ChatColor.DARK_AQUA + "[" + ChatColor.GOLD + "Heads" + ChatColor.GREEN + "Inventory" + ChatColor.DARK_AQUA + "]" + ChatColor.WHITE + " ";
        } else {
            return "[HeadsInventory] ";
        }
    }

    public static HeadsInventory getPlugin() {
        Plugin headsInventory = Bukkit.getServer().getPluginManager().getPlugin("HeadsInventory");
        if (headsInventory != null && headsInventory.isEnabled() && headsInventory instanceof HeadsInventory) {
            return (HeadsInventory) headsInventory;
        } else {
            Bukkit.getLogger().log(Level.WARNING, "The heads inventory has not been enabled yet");
            return null;
        }
    }

    /**
     * Method to reload the config.yml config file
     */
    @Override
    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Look for defaults in the jar
        Reader defConfigStream = null;
        try {
            defConfigStream = new InputStreamReader(this.getResource("config.yml"), "UTF8");
        } catch (UnsupportedEncodingException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            config.setDefaults(defConfig);
        }
    }

    /**
     * Method to get YML content of the config.yml config file
     *
     * @return YML content of the catagories.yml config file
     */
    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    /**
     * Method to save the config.yml config file
     */
    @Override
    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    /**
     * Method to save the default config file
     */
    @Override
    public void saveDefaultConfig() {
        String version = getConfig().getString("version", null);
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        if (!configFile.exists()) {
            saveResource("config.yml", false);
            reloadConfig();
        } else if (!getDescription().getVersion().equals(version)) {
            getLogger().log(
                    Level.WARNING, "New version detected: {0}->{1}. Saving new default config.", 
                    new Object[]{(version == null ? "(?)" : version), getDescription().getVersion()}
            );
            saveResource("config.yml", true);
            reloadConfig();
        }
    }

    public static String getHelpMessage() {
        Translator t = getTranslator();
        return ChatColor.YELLOW + " ---- " + ChatColor.GOLD + "Heads Help" + ChatColor.YELLOW + " ---- \n"
                + ChatColor.GOLD + "/headsinv category <" + t.getText("name") + ">" + ChatColor.RESET + ":\n " + t.getText("cmd-headsinv-category-name") + "\n"
                + ChatColor.GOLD + "/headsinv category all" + ChatColor.RESET + ":\n " + t.getText("cmd-headsinv-category-all") + "\n"
                + ChatColor.GOLD + "/headsinv cat" + ChatColor.RESET + ":\n " + t.getText("cmd-headsinv-cat") + "\n"
                + ChatColor.GOLD + "/headsinv search <" + t.getText("keyword") + ">" + ChatColor.RESET + ":\n " + t.getText("cmd-headsinv-search") + "\n"
                + ChatColor.GOLD + "/headsinv fsearch <" + t.getText("keyword") + ">" + ChatColor.RESET + ":\n " + t.getText("cmd-headsinv-fsearch") + "\n"
                + ChatColor.GOLD + "/headsinv msearch <" + t.getText("keyword") + ">" + ChatColor.RESET + ":\n " + t.getText("cmd-headsinv-msearch") + "\n"
                + ChatColor.GOLD + "/headsinv mhsearch <" + t.getText("keyword") + ">" + ChatColor.RESET + ":\n " + t.getText("cmd-headsinv-mhsearch") + "\n"
                + ChatColor.GOLD + "/headsinv getfirst <" + t.getText("keyword") + ">" + ChatColor.RESET + ":\n " + t.getText("cmd-headsinv-getfirst") + "\n"
                + ChatColor.GOLD + "/headsinv fgetfirst <" + t.getText("keyword") + ">" + ChatColor.RESET + ":\n " + t.getText("cmd-headsinv-fgetfirst") + "\n"
                + ChatColor.GOLD + "/headsinv mgetfirst <" + t.getText("keyword") + ">" + ChatColor.RESET + ":\n " + t.getText("cmd-headsinv-mgetfirst") + "\n"
                + ChatColor.GOLD + "/headsinv mhgetfirst <" + t.getText("keyword") + ">" + ChatColor.RESET + ":\n " + t.getText("cmd-headsinv-mhgetfirst") + "\n"
                + ChatColor.GOLD + "/playerhead <" + t.getText("playername") + ">" + ChatColor.RESET + ":\n " + t.getText("cmd-playerhead-playername") + "\n"
                + ChatColor.GOLD + "/myhead" + ChatColor.RESET + ":\n " + t.getText("cmd-myhead") + " \n"
                + ChatColor.GOLD + "/addhead <" + t.getText("name") + ">" + ChatColor.RESET + ":\n " + t.getText("cmd-addhead-name") + "\n";
    }

    public static Translator getTranslator() {
        if(translator == null) {
            String bundleName = "Translations";
            translator = new Translator(
                    bundleName, 
                    LocaleUtils.toLocale(
                            HeadsInventory.getPlugin().getConfig().getString("locale", "en_US")
                    ),
                    HeadsInventory.getPlugin().getPluginClassLoader()
            );
        }
        return translator;
    }

    /**
     * Returns the ClassLoader which holds this plugin
     *
     * @return ClassLoader holding this plugin
     */
    public ClassLoader getPluginClassLoader() {
        return getClassLoader();
    }
    
    
}
