package com.platymuus.bukkit.permissions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Main class for PermissionsBukkit.
 */
public class PermissionsPlugin extends JavaPlugin {

    private PlayerListener playerListener = new PlayerListener(this);
    private PermissionsCommand commandExecutor = new PermissionsCommand(this);
    private HashMap<String, PermissionAttachment> permissions = new HashMap<String, PermissionAttachment>();
    
    private File configFile;
    private YamlConfiguration config;

    // -- Basic stuff
    @Override
    public void onEnable() {
        // Take care of configuration
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        reloadConfig();

        // Register stuff
        getCommand("permissions").setExecutor(commandExecutor);
        getServer().getPluginManager().registerEvents(playerListener, this);

        // Register everyone online right now
        for (Player p : getServer().getOnlinePlayers()) {
            registerPlayer(p);
        }

        // How are you gentlemen
        int count = getServer().getOnlinePlayers().length;
        if (count > 0) {
            getLogger().info("Enabled successfully, " + count + " online players registered");
        } else {
            // "0 players registered" sounds too much like an error
            getLogger().info("Enabled successfully");
        }
    }

    @Override
    public FileConfiguration getConfig() {
        return config;
    }

    @Override
    public void reloadConfig() {
        config = new YamlConfiguration();
        config.options().pathSeparator('/');
        try {
            config.load(configFile);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to load configuration", ex);
        }
    }

    @Override
    public void saveConfig() {
        // If there's no keys (such as in the event of a load failure) don't save
        if (config.getKeys(true).size() > 0) {
            try {
                config.save(configFile);
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Failed to save configuration", ex);
            }
        }
    }

    @Override
    public void onDisable() {
        // Unregister everyone
        for (Player p : getServer().getOnlinePlayers()) {
            unregisterPlayer(p);
        }

        // Good day to you! I said good day!
        int count = getServer().getOnlinePlayers().length;
        if (count > 0) {
            getLogger().info("Disabled successfully, " + count + " online players unregistered");
        }
    }

    // -- External API
    /**
     * Get the group with the given name.
     * @param groupName The name of the group.
     * @return A Group if it exists or null otherwise.
     */
    public Group getGroup(String groupName) {
        if (getNode("groups") != null) {
            for (String key : getNode("groups").getKeys(false)) {
                if (key.equalsIgnoreCase(groupName)) {
                    return new Group(this, key);
                }
            }
        }
        return null;
    }

    /**
     * Returns a list of groups a player is in.
     * @param playerName The name of the player.
     * @return The groups this player is in. May be empty.
     */
    public List<Group> getGroups(String playerName) {
        ArrayList<Group> result = new ArrayList<Group>();
        if (getNode("users/" + playerName) != null) {
            for (String key : getNode("users/" + playerName).getStringList("groups")) {
                result.add(new Group(this, key));
            }
        } else {
            result.add(new Group(this, "default"));
        }
        return result;
    }

    /**
     * Returns permission info on the given player.
     * @param playerName The name of the player.
     * @return A PermissionsInfo about this player.
     */
    public PermissionInfo getPlayerInfo(String playerName) {
        if (getNode("users/" + playerName) == null) {
            return null;
        } else {
            return new PermissionInfo(this, getNode("users/" + playerName), "groups");
        }
    }

    /**
     * Returns a list of all defined groups.
     * @return The list of groups.
     */
    public List<Group> getAllGroups() {
        ArrayList<Group> result = new ArrayList<Group>();
        if (getNode("groups") != null) {
            for (String key : getNode("groups").getKeys(false)) {
                result.add(new Group(this, key));
            }
        }
        return result;
    }

    // -- Plugin stuff
    
    protected void registerPlayer(Player player) {
        if (permissions.containsKey(player.getName())) {
            debug("Registering " + player.getName() + ": was already registered");
            unregisterPlayer(player);
        }
        PermissionAttachment attachment = player.addAttachment(this);
        permissions.put(player.getName(), attachment);
        calculateAttachment(player);
    }

    protected void unregisterPlayer(Player player) {
        if (permissions.containsKey(player.getName())) {
            try {
                player.removeAttachment(permissions.get(player.getName()));
            }
            catch (IllegalArgumentException ex) {
                debug("Unregistering " + player.getName() + ": player did not have attachment");
            }
            permissions.remove(player.getName());
        } else {
            debug("Unregistering " + player.getName() + ": was not registered");
        }
    }

    protected void refreshPermissions() {
        saveConfig();
        for (String player : permissions.keySet()) {
            PermissionAttachment attachment = permissions.get(player);
            for (String key : attachment.getPermissions().keySet()) {
                attachment.unsetPermission(key);
            }

            calculateAttachment(getServer().getPlayer(player));
        }
    }
    
    protected ConfigurationSection getNode(String node) {
        for (String entry : getConfig().getKeys(true)) {
            if (node.equalsIgnoreCase(entry) && getConfig().isConfigurationSection(entry)) {
                return getConfig().getConfigurationSection(entry);
            }
        }
        return null;
    }

    protected void createNode(String node) {
        ConfigurationSection sec = getConfig();
        for (String piece : node.split("/")) {
            ConfigurationSection sec2 = getNode(sec == getConfig() ? piece : sec.getCurrentPath() + "/" + piece);
            if (sec2 == null) {
                sec2 = sec.createSection(piece);
            }
            sec = sec2;
        }
    }

    protected HashMap<String, Boolean> getAllPerms(String desc, String path) {
        HashMap<String, Boolean> result = new HashMap<String, Boolean>();
        ConfigurationSection node = getNode(path);
        
        int failures = 0;
        String firstFailure = "";

        // Make an attempt to autofix incorrect nesting
        boolean fixed = false, fixedNow = true;
        while (fixedNow) {
            fixedNow = false;
            for (String key : node.getKeys(true)) {
                if (node.isBoolean(key) && key.contains("/")) {
                    node.set(key.replace("/", "."), node.getBoolean(key));
                    node.set(key, null);
                    fixed = fixedNow = true;
                } else if (node.isConfigurationSection(key) && node.getConfigurationSection(key).getKeys(true).size() == 0) {
                    node.set(key, null);
                    fixed = fixedNow = true;
                }
            }
        }
        if (fixed) {
            getLogger().info("Fixed broken nesting in " + desc + ".");
            saveConfig();
        }

        // Do the actual getting of permissions
        for (String key : node.getKeys(false)) {
            if (node.isBoolean(key)) {
                result.put(key, node.getBoolean(key));
            } else {
                ++failures;
                if (firstFailure.length() == 0) {
                    firstFailure = key;
                }
            }
        }
        
        if (failures == 1) {
            getLogger().warning("In " + desc + ": " + firstFailure + " is non-boolean.");
        } else if (failures > 1) {
            getLogger().warning("In " + desc + ": " + firstFailure + " is non-boolean (+" + (failures-1) + " more).");
        }
        
        return result;
    }
    
    protected void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("Debug: " + message);
        }
    }

    protected void calculateAttachment(Player player) {
        if (player == null) {
            return;
        }
        PermissionAttachment attachment = permissions.get(player.getName());
        if (attachment == null) {
            debug("Calculating permissions on " + player.getName() + ": attachment was null");
            return;
        }

        for (String key : attachment.getPermissions().keySet()) {
            attachment.unsetPermission(key);
        }

        for (Map.Entry<String, Boolean> entry : calculatePlayerPermissions(player.getName().toLowerCase(), player.getWorld().getName()).entrySet()) {
            attachment.setPermission(entry.getKey(), entry.getValue());
        }

        player.recalculatePermissions();
    }

    // -- Private stuff

    private Map<String, Boolean> calculatePlayerPermissions(String player, String world) {
        if (getNode("users/" + player) == null) {
            return calculateGroupPermissions("default", world);
        }

        Map<String, Boolean> perms = getNode("users/" + player + "/permissions") == null ?
                new HashMap<String, Boolean>() :
                getAllPerms("user " + player, "users/" + player + "/permissions");

        if (getNode("users/" + player + "/worlds/" + world) != null) {
            for (Map.Entry<String, Boolean> entry : getAllPerms("user " + player + " world " + world, "users/" + player + "/worlds/" + world).entrySet()) {
                // No containskey; world overrides non-world
                perms.put(entry.getKey(), entry.getValue());
            }
        }

        for (String group : getNode("users/" + player).getStringList("groups")) {
            for (Map.Entry<String, Boolean> entry : calculateGroupPermissions(group, world).entrySet()) {
                if (!perms.containsKey(entry.getKey())) { // User overrides group
                    perms.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return perms;
    }

    private Map<String, Boolean> calculateGroupPermissions(String group, String world) {
        if (getNode("groups/" + group) == null) {
            return new HashMap<String, Boolean>();
        }

        Map<String, Boolean> perms = getNode("groups/" + group + "/permissions") == null ?
                new HashMap<String, Boolean>() :
                getAllPerms("group " + group, "groups/" + group + "/permissions");
        

        if (getNode("groups/" + group + "/worlds/" + world) != null) {
            for (Map.Entry<String, Boolean> entry : getAllPerms("group " + group + " world " + world, "groups/" + group + "/worlds/" + world).entrySet()) {
                // No containskey; world overrides non-world
                perms.put(entry.getKey(), entry.getValue());
            }
        }

        for (String parent : getNode("groups/" + group).getStringList("inheritance")) {
            for (Map.Entry<String, Boolean> entry : calculateGroupPermissions(parent, world).entrySet()) {
                if (!perms.containsKey(entry.getKey())) { // Children override permissions
                    perms.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return perms;
    }

}
