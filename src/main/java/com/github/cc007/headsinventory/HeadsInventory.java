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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import net.milkbowl.vault.permission.Permission;
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

	private Plugin vault = null;
	private Permission permission = null;
    private FileConfiguration config = null;
    private File configFile = null;

	@Override
	public void onEnable() {
        /* Config stuffs */
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        
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
		getCommand("updateheads").setExecutor(hic);

		/* Register tab completers*/
        HeadsInventoryTabCompleter hitc = new HeadsInventoryTabCompleter(this);
		getCommand("headsinventory").setTabCompleter(hitc);
		getCommand("updateheads").setTabCompleter(hitc);
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
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
    }
}
