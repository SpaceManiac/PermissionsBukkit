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
public class PermissionsMain extends JavaPlugin {
    
    private BlockListener blockListener = new BlockListener(this);
    private PlayerListener playerListener = new PlayerListener(this);
    private PermissionsCommand commandExecutor = new PermissionsCommand(this);
    
    private HashMap<Player, PermissionAttachment> permissions = new HashMap<Player, PermissionAttachment>();

    @Override
    public void onEnable() {
        // Write some default configuration
        if (!new File(getDataFolder(), "config.yml").exists()) {
            writeDefaultConfiguration();
        }
        
        // Commands
        getCommand("permissions").setExecutor(commandExecutor); 
        
        // Events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_KICK, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_PLACE, blockListener, Priority.Normal, this);
        
        // Register everyone online right now
        for (Player p : getServer().getOnlinePlayers()) {
            registerPlayer(p);
        }
        
        // How are you gentlemen
        getServer().getLogger().info(getDescription().getFullName() + " is now enabled");
    }

    @Override
    public void onDisable() {
        // Unregister everyone
        for (Player p : getServer().getOnlinePlayers()) {
            unregisterPlayer(p);
        }
        
        // Good day to you! I said good day!
        getServer().getLogger().info(getDescription().getFullName() + " is now disabled");
    }
    
    public void registerPlayer(Player player) {
        PermissionAttachment attachment = player.addAttachment(this);
        permissions.put(player, attachment);
        calculateAttachment(player);
    }

    public void unregisterPlayer(Player player) {
        player.removeAttachment(permissions.get(player));
        permissions.remove(player);
    }

    public void refreshPermissions() {
        getConfiguration().save();
        for (Player player : permissions.keySet()) {
            PermissionAttachment attachment = permissions.get(player);
            for (String key : attachment.getPermissions().keySet()) {
                attachment.unsetPermission(key);
            }
            
            calculateAttachment(player);
        }
    }
    
    private void calculateAttachment(Player player) {
        PermissionAttachment attachment = permissions.get(player);
            
        for (Map.Entry<String, Object> entry : calculatePlayerPermissions(player.getName(), player.getWorld().getName()).entrySet()) {
            if (entry.getValue() != null && entry.getValue() instanceof Boolean) {
                attachment.setPermission(entry.getKey(), (Boolean) entry.getValue());
            } else {
                getServer().getLogger().warning("[PermissionsBukkit] Node " + entry.getKey() + " for player " + player.getName() + " is non-Boolean");
            }
        }
        
        player.recalculatePermissions();
    }
    
    private Map<String, Object> calculatePlayerPermissions(String player, String world) {
        ConfigurationNode node = getConfiguration().getNode("users." + player);
        if (node == null) {
            return calculateGroupPermissions("default", world);
        }
        
        Map<String, Object> perms = node.getNode("permissions") == null ? new HashMap<String, Object>() : node.getNode("permissions").getAll();
        
        if (node.getNode("worlds." + world) != null) {
            for (Map.Entry<String, Object> entry : node.getNode("worlds." + world).getAll().entrySet()) {
                // No containskey; world overrides non-world
                perms.put(entry.getKey(), entry.getValue());
            }
        }
        
        for (String group : node.getStringList("groups", new ArrayList<String>())) {
            for (Map.Entry<String, Object> entry : calculateGroupPermissions(group, world).entrySet()) {
                if (!perms.containsKey(entry.getKey())) { // User overrides group
                    perms.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        return perms;
    }
    
    private Map<String, Object> calculateGroupPermissions(String group, String world) {
        ConfigurationNode node = getConfiguration().getNode("groups." + group);
        if (node == null) {
            return new HashMap<String, Object>();
        }
        
        Map<String, Object> perms = node.getNode("permissions") == null ? new HashMap<String, Object>() : node.getNode("permissions").getAll();
        
        if (node.getNode("worlds." + world) != null) {
            for (Map.Entry<String, Object> entry : node.getNode("worlds." + world).getAll().entrySet()) {
                // No containskey; world overrides non-world
                perms.put(entry.getKey(), entry.getValue());
            }
        }
        
        for (String parent : node.getStringList("inherits", new ArrayList<String>())) {
            for (Map.Entry<String, Object> entry : calculateGroupPermissions(parent, world).entrySet()) {
                if (!perms.containsKey(entry.getKey())) { // Children override permissions
                    perms.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        return perms;
    }

    private void writeDefaultConfiguration() {
        HashMap<String, Object> users = new HashMap<String, Object>();
        HashMap<String, Object> user = new HashMap<String, Object>();
        HashMap<String, Object> user_permissions = new HashMap<String, Object>();
        ArrayList<String> user_groups = new ArrayList<String>();
        
        HashMap<String, Object> groups = new HashMap<String, Object>();
        HashMap<String, Object> group_default = new HashMap<String, Object>();
        HashMap<String, Object> group_default_permissions = new HashMap<String, Object>();
        
        HashMap<String, Object> group_user = new HashMap<String, Object>();
        ArrayList<String> group_user_inherits = new ArrayList<String>();
        HashMap<String, Object> group_user_permissions = new HashMap<String, Object>();
        HashMap<String, Object> group_user_worlds = new HashMap<String, Object>();
        HashMap<String, Object> group_user_worlds_creative = new HashMap<String, Object>();
        
        HashMap<String, Object> group_admin = new HashMap<String, Object>();
        ArrayList<String> group_admin_inherits = new ArrayList<String>();
        HashMap<String, Object> group_admin_permissions = new HashMap<String, Object>();
        
        user_permissions.put("permissions.example", true);
        user_groups.add("admin");
        user.put("permissions", user_permissions);
        user.put("groups", user_groups);
        users.put("ConspiracyWizard", user);
        
        group_default_permissions.put("permissions.build", false);
        group_default.put("permissions", group_default_permissions);
        
        group_user_inherits.add("default");
        group_user_permissions.put("permissions.build", true);
        group_user_worlds_creative.put("coolplugin.item", true);
        group_user_worlds.put("creative", group_user_worlds_creative);
        group_user.put("inherits", group_user_inherits);
        group_user.put("permissions", group_user_permissions);
        group_user.put("worlds", group_user_worlds);
        
        group_admin_inherits.add("user");
        group_admin_permissions.put("permissions.*", true);
        group_admin.put("inherits", group_admin_inherits);
        group_admin.put("permissions", group_admin_permissions);
        
        groups.put("default", group_default);
        groups.put("user", group_user);
        groups.put("admin", group_admin);
        
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
            ""
        );
        getConfiguration().save();
    }
    
}
