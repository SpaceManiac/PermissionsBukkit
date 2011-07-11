package com.platymuus.bukkit.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.util.config.ConfigurationNode;

/**
 * A class representing the global and world nodes attached to a player or group.
 */
public class PermissionInfo {
    
    private final PermissionsPlugin plugin;
    private final ConfigurationNode node;
    private final String groupType;
    
    protected PermissionInfo(PermissionsPlugin plugin, ConfigurationNode node, String groupType) {
        this.plugin = plugin;
        this.node = node;
        this.groupType = groupType;
    }
    
    /**
     * Gets the list of groups this group/player inherits permissions from.
     * @return The list of groups.
     */
    public List<Group> getGroups() {
        ArrayList<Group> result = new ArrayList<Group>();

        for (String key : node.getStringList(groupType, new ArrayList<String>())) {
            Group group = plugin.getGroup(key);
            if (group != null) {
                result.add(group);
            }
        }
        
        return result;
    }
    
    /**
     * Gets a map of non-world-specific permission nodes to boolean values that this group/player defines.
     * @return The map of permissions.
     */
    public Map<String, Boolean> getPermissions() {
        HashMap<String, Boolean> result = new HashMap<String, Boolean>();
        for (String key : node.getNode("permissions").getKeys()) {
            result.put(key, (Boolean) node.getNode("permissions").getProperty(key));
        }
        return result;
    }
    
    /**
     * Gets a list of worlds this group/player defines world-specific permissions for.
     */
    public List<String> getWorlds() {
        if (node.getNode("worlds") == null) {
            return new ArrayList<String>();
        }
        return node.getNode("worlds").getKeys();
    }
    
    /**
     * Gets a map of world-specific permission nodes to boolean values that this group/player defines.
     * @return The map of permissions.
     */
    public Map<String, Boolean> getWorldPermissions(String world) {
        HashMap<String, Boolean> result = new HashMap<String, Boolean>();
        if (node.getNode("worlds." + world) != null) {
            for (String key : node.getNode("worlds." + world).getKeys()) {
                result.put(key, (Boolean) node.getNode("worlds." + world).getProperty(key));
            }
        }
        return result;
    }
    
}
