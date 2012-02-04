package com.platymuus.bukkit.permissions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.ConfigurationNode;

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
        if (!new File(getDataFolder(), "config.yml").exists()) {
            getDataFolder().mkdirs();
            getLogger().info("Generating default configuration");
            writeDefaultConfiguration();
        }

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
            for (String key : getNode("groups").getKeys()) {
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
            for (String key : getNode("users." + playerName).getStringList("groups", new ArrayList<String>())) {
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
            for (String key : getNode("groups").getKeys()) {
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
        getConfiguration().save();
        for (String player : permissions.keySet()) {
            PermissionAttachment attachment = permissions.get(player);
            for (String key : attachment.getPermissions().keySet()) {
                attachment.unsetPermission(key);
            }

            calculateAttachment(getServer().getPlayer(player));
        }
    }

    protected ConfigurationNode getNode(String child) {
        return getNode("", child);
    }
    
    protected void debug(String message) {
        if (getConfiguration().getBoolean("debug", false)) {
            getLogger().info("Debug: " + message);
        }
    }

    // -- Private stuff
    
    private ConfigurationNode getNode(String parent, String child) {
        ConfigurationNode parentNode = null;
        if (child.contains(".")) {
            int index = child.lastIndexOf('.');
            parentNode = getNode("", child.substring(0, index));
            child = child.substring(index + 1);
        } else if (parent.length() == 0) {
            parentNode = getConfiguration();
        } else if (parent.contains(".")) {
            int index = parent.indexOf('.');
            parentNode = getNode(parent.substring(0, index), parent.substring(index + 1));
        } else {
            parentNode = getNode("", parent);
        }

        if (parentNode == null) {
            return null;
        }

        for (String entry : parentNode.getKeys()) {
            if (child.equalsIgnoreCase(entry)) {
                return parentNode.getNode(entry);
            }
        }
        return null;
    }

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

        for (Map.Entry<String, Object> entry : calculatePlayerPermissions(player.getName().toLowerCase(), lastWorld.get(player.getName())).entrySet()) {
            if (entry.getValue() != null && entry.getValue() instanceof Boolean) {
                attachment.setPermission(entry.getKey(), (Boolean) entry.getValue());
            } else {
                getLogger().warning("Node " + entry.getKey() + " for player " + player.getName() + " is non-Boolean");
            }
        }

        player.recalculatePermissions();
    }

    private Map<String, Object> calculatePlayerPermissions(String player, String world) {
        if (getNode("users." + player) == null) {
            return calculateGroupPermissions("default", world);
        }

        Map<String, Object> perms = getNode("users." + player + ".permissions") == null ? new HashMap<String, Object>() : getNode("users." + player + ".permissions").getAll();

        if (getNode("users." + player + ".worlds." + world) != null) {
            for (Map.Entry<String, Object> entry : getNode("users." + player + ".worlds." + world).getAll().entrySet()) {
                // No containskey; world overrides non-world
                perms.put(entry.getKey(), entry.getValue());
            }
        }

        for (String group : getNode("users." + player).getStringList("groups", new ArrayList<String>())) {
            for (Map.Entry<String, Object> entry : calculateGroupPermissions(group, world).entrySet()) {
                if (!perms.containsKey(entry.getKey())) { // User overrides group
                    perms.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return perms;
    }

    private Map<String, Object> calculateGroupPermissions(String group, String world) {
        if (getNode("groups." + group) == null) {
            return new HashMap<String, Object>();
        }

        Map<String, Object> perms = getNode("groups." + group + ".permissions") == null ? new HashMap<String, Object>() : getNode("groups." + group + ".permissions").getAll();

        if (getNode("groups." + group + ".worlds." + world) != null) {
            for (Map.Entry<String, Object> entry : getNode("groups." + group + ".worlds." + world).getAll().entrySet()) {
                // No containskey; world overrides non-world
                perms.put(entry.getKey(), entry.getValue());
            }
        }

        for (String parent : getNode("groups." + group).getStringList("inheritance", new ArrayList<String>())) {
            for (Map.Entry<String, Object> entry : calculateGroupPermissions(parent, world).entrySet()) {
                if (!perms.containsKey(entry.getKey())) { // Children override permissions
                    perms.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return perms;
    }

    private void writeDefaultConfiguration() {
        HashMap<String, Object> messages = new HashMap<String, Object>();
        HashMap<String, Object> users = new HashMap<String, Object>();
        HashMap<String, Object> user = new HashMap<String, Object>();
        HashMap<String, Object> user_permissions = new HashMap<String, Object>();
        ArrayList<String> user_groups = new ArrayList<String>();

        HashMap<String, Object> groups = new HashMap<String, Object>();
        HashMap<String, Object> group_default = new HashMap<String, Object>();
        HashMap<String, Object> group_default_permissions = new HashMap<String, Object>();

        HashMap<String, Object> group_user = new HashMap<String, Object>();
        ArrayList<String> group_user_inheritance = new ArrayList<String>();
        HashMap<String, Object> group_user_permissions = new HashMap<String, Object>();
        HashMap<String, Object> group_user_worlds = new HashMap<String, Object>();
        HashMap<String, Object> group_user_worlds_creative = new HashMap<String, Object>();

        HashMap<String, Object> group_admin = new HashMap<String, Object>();
        ArrayList<String> group_admin_inheritance = new ArrayList<String>();
        HashMap<String, Object> group_admin_permissions = new HashMap<String, Object>();

        messages.put("build", "&cYou do not have permission to build here.");

        user_permissions.put("permissions.example", true);
        user_groups.add("admin");
        user.put("permissions", user_permissions);
        user.put("groups", user_groups);
        users.put("ConspiracyWizard", user);

        group_default_permissions.put("permissions.build", false);
        group_default.put("permissions", group_default_permissions);

        group_user_inheritance.add("default");
        group_user_permissions.put("permissions.build", true);
        group_user_worlds_creative.put("coolplugin.item", true);
        group_user_worlds.put("creative", group_user_worlds_creative);
        group_user.put("inheritance", group_user_inheritance);
        group_user.put("permissions", group_user_permissions);
        group_user.put("worlds", group_user_worlds);

        group_admin_inheritance.add("user");
        group_admin_permissions.put("permissions.*", true);
        group_admin.put("inheritance", group_admin_inheritance);
        group_admin.put("permissions", group_admin_permissions);

        groups.put("default", group_default);
        groups.put("user", group_user);
        groups.put("admin", group_admin);

        getConfiguration().setProperty("messages", messages);
        getConfiguration().setProperty("users", users);
        getConfiguration().setProperty("groups", groups);

        getConfiguration().setHeader(
            "# PermissionsBukkit configuration file",
            "# ",
            "# A permission node is a string like 'permissions.build', usually starting",
            "# with the name of the plugin. Refer to a plugin's documentation for what",
            "# permissions it cares about. Each node should be followed by true to grant",
            "# that permission or false to revoke it, as in 'permissions.build: true'.",
            "# Some plugins provide permission nodes that map to a group of permissions -",
            "# for example, PermissionsBukkit has 'permissions.*', which automatically",
            "# grants all admin permissions. You can also specify false for permissions",
            "# of this type.",
            "# ",
            "# Users inherit permissions from the groups they are a part of. If a user is",
            "# not specified here, or does not have a 'groups' node, they will be in the",
            "# group 'default'. Permissions for individual users may also be specified by",
            "# using a 'permissions' node with a list of permission nodes, which will",
            "# override their group permissions. World permissions may be assigned to",
            "# users with a 'worlds:' entry.",
            "# ",
            "# Groups can be assigned to players and all their permissions will also be",
            "# assigned to those players. Groups can also inherit permissions from other",
            "# groups. Like user permissions, groups may override the permissions of their",
            "# parent group(s). Unlike users, groups do NOT automatically inherit from",
            "# default. World permissions may be assigned to groups with a 'worlds:' entry.",
            "#",
            "# The cannot-build message is configurable. If it is left blank, no message",
            "# will be displayed to the player if PermissionsBukkit prevents them from",
            "# building, digging, or interacting with a block. Use '&' characters to",
            "# signify color codes.",
            "");
        getConfiguration().save();
    }

}
