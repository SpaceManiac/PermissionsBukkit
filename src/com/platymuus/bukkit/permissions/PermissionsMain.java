package com.platymuus.bukkit.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        PermissionAttachment attachment = player.addAttachment();
        permissions.put(player, attachment);
        
        for (String permission : calculatePlayerPermissions(player.getName())) {
            if (permission.startsWith("-")) {
                attachment.setPermission(permission.substring(1), false);
            } else {
                attachment.setPermission(permission, true);
            }
        }
    }

    public void unregisterPlayer(Player player) {
        player.removeAttachment(permissions.get(player));
        permissions.remove(player);
    }

    void refreshPermissions() {
        getConfiguration().save();
        for (Player player : permissions.keySet()) {
            PermissionAttachment attachment = permissions.get(player);
            for (String key : attachment.getPermissions().keySet()) {
                attachment.unsetPermission(key);
            }
            
            for (String permission : calculatePlayerPermissions(player.getName())) {
                if (permission.startsWith("-")) {
                    attachment.setPermission(permission.substring(1), false);
                } else {
                    attachment.setPermission(permission, true);
                }
            }
            
            player.recalculatePermissions();
        }
    }
    
    private List<String> calculatePlayerPermissions(String player) {
        System.out.println("Player " + player + "...");
        ConfigurationNode node = getConfiguration().getNode("users." + player);
        if (node == null) {
            return calculateGroupPermissions("default");
        }
        
        List<String> perms = node.getStringList("permissions", new ArrayList<String>());
        
        for (String group : node.getStringList("groups", new ArrayList<String>())) {
            for (String permission : calculateGroupPermissions(group)) {
                if (!perms.contains(permission)) {
                    perms.add(permission);
                }
            }
        }
        
        System.out.println("Player " + player + " has " + perms.size());
        
        return perms;
    }
    
    private List<String> calculateGroupPermissions(String group) {
        System.out.println("Group " + group + "...");
        ConfigurationNode node = getConfiguration().getNode("groups." + group);
        if (node == null) {
            return new ArrayList<String>();
        }
        
        List<String> perms = node.getStringList("permissions", new ArrayList<String>());
        
        for (String parent : node.getStringList("inherits", new ArrayList<String>())) {
            for (String permission : calculateGroupPermissions(parent)) {
                if (!perms.contains(permission)) {
                    perms.add(permission);
                }
            }
        }
        
        System.out.println("Group " + group + " has " + perms.size());
        
        return perms;
    }
    
}
