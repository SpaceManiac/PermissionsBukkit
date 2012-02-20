package com.platymuus.bukkit.permissions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Main class for PermissionsBukkit.
 */
public class PermissionsPlugin extends JavaPlugin {

    private PlayerListener playerListener = new PlayerListener(this);
    private PermissionsCommand commandExecutor = new PermissionsCommand(this);
    private HashMap<String, PermissionAttachment> permissions = new HashMap<String, PermissionAttachment>();
    private HashMap<String, String> lastWorld = new HashMap<String, String>();

    // -- Basic stuff
    @Override
    public void onEnable() {
        // Write some default configuration
        saveDefaultConfig();

        // Register stuff
        getCommand("permissions").setExecutor(commandExecutor);
        getServer().getPluginManager().registerEvents(playerListener, this);

        // Register everyone online right now
        for (Player p : getServer().getOnlinePlayers()) {
            registerPlayer(p);
        }

        // How are you gentlemen
        getLogger().info("Enabled successfully, " + getServer().getOnlinePlayers().length + " players registered");
    }

    @Override
    public void onDisable() {
        // Unregister everyone
        for (Player p : getServer().getOnlinePlayers()) {
            unregisterPlayer(p);
        }

        // Good day to you! I said good day!
        getLogger().info("Disabled successfully, " + getServer().getOnlinePlayers().length + " players unregistered");
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
        if (getNode("users." + playerName) != null) {
            for (String key : getNode("users." + playerName).getStringList("groups")) {
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
        if (getNode("users." + playerName) == null) {
            return null;
        } else {
            return new PermissionInfo(this, getNode("users." + playerName), "groups");
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
        setLastWorld(player.getName(), player.getWorld().getName());
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
            lastWorld.remove(player.getName());
        } else {
            debug("Unregistering " + player.getName() + ": was not registered");
        }
    }

    protected void setLastWorld(String player, String world) {
        if (permissions.containsKey(player) && (lastWorld.get(player) == null || !lastWorld.get(player).equals(world))) {
            debug("Player " + player + " moved to world " + world + ", recalculating...");
            lastWorld.put(player, world);
            calculateAttachment(getServer().getPlayer(player));
        }
    }

    protected void refreshPermissions() {
        try {
            getConfig().save(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            getLogger().warning("Failed to write changed config.yml: " + e.getMessage());
        }
        for (String player : permissions.keySet()) {
            PermissionAttachment attachment = permissions.get(player);
            for (String key : attachment.getPermissions().keySet()) {
                attachment.unsetPermission(key);
            }

            calculateAttachment(getServer().getPlayer(player));
        }
    }

    protected ConfigurationSection getNode(String node) {
        return getConfig().getConfigurationSection(node);
    }

    protected HashMap<String, Boolean> getAllPerms(String desc, String path) {
        HashMap<String, Boolean> result = new HashMap<String, Boolean>();
        ConfigurationSection node = getNode(path);
        
        int failures = 0;
        String firstFailure = "";
        
        Set<String> deep = node.getKeys(true);
        for (String key : deep) {
            if (node.isConfigurationSection(key)) {
                // weirdness due to perm nodes having '.', skip
            } else if (node.isBoolean(key)) {
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

    // -- Private stuff

    private void calculateAttachment(Player player) {
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

        for (Map.Entry<String, Boolean> entry : calculatePlayerPermissions(player.getName().toLowerCase(), lastWorld.get(player.getName())).entrySet()) {
            if (entry.getValue() != null) {
                attachment.setPermission(entry.getKey(), entry.getValue());
            } else {
                getLogger().warning("Node " + entry.getKey() + " for player " + player.getName() + " is non-Boolean");
            }
        }

        player.recalculatePermissions();
    }

    private Map<String, Boolean> calculatePlayerPermissions(String player, String world) {
        if (getNode("users." + player) == null) {
            return calculateGroupPermissions("default", world);
        }

        Map<String, Boolean> perms = getNode("users." + player + ".permissions") == null ?
                new HashMap<String, Boolean>() :
                getAllPerms("user " + player, "users." + player + ".permissions");

        if (getNode("users." + player + ".worlds." + world) != null) {
            for (Map.Entry<String, Boolean> entry : getAllPerms("user" + player, "users." + player + ".worlds." + world).entrySet()) {
                // No containskey; world overrides non-world
                perms.put(entry.getKey(), entry.getValue());
            }
        }

        for (String group : getConfig().getStringList("users." + player + ".groups")) {
            for (Map.Entry<String, Boolean> entry : calculateGroupPermissions(group, world).entrySet()) {
                if (!perms.containsKey(entry.getKey())) { // User overrides group
                    perms.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return perms;
    }

    private Map<String, Boolean> calculateGroupPermissions(String group, String world) {
        if (getNode("groups." + group) == null) {
            return new HashMap<String, Boolean>();
        }

        Map<String, Boolean> perms = getNode("groups." + group + ".permissions") == null ?
                new HashMap<String, Boolean>() :
                getAllPerms("group " + group, "groups." + group + ".permissions");
        

        if (getNode("groups." + group + ".worlds." + world) != null) {
            for (Map.Entry<String, Boolean> entry : getAllPerms("group " + group, "groups." + group + ".worlds." + world).entrySet()) {
                // No containskey; world overrides non-world
                perms.put(entry.getKey(), entry.getValue());
            }
        }

        for (String parent : getConfig().getStringList("groups." + group + ".inheritance")) {
            for (Map.Entry<String, Boolean> entry : calculateGroupPermissions(parent, world).entrySet()) {
                if (!perms.containsKey(entry.getKey())) { // Children override permissions
                    perms.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return perms;
    }

}
