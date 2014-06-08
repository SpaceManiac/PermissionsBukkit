package com.platymuus.bukkit.permissions;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * A class representing the global and world nodes attached to a player or group.
 */
public final class PermissionInfo {

    private final PermissionsPlugin plugin;
    private final ConfigurationSection node;
    private final String groupType;

    PermissionInfo(PermissionsPlugin plugin, ConfigurationSection node, String groupType) {
        this.plugin = plugin;
        this.node = node;
        this.groupType = groupType;
    }

    /**
     * Gets the list of groups this group/player inherits permissions from.
     *
     * @return The list of groups.
     */
    public List<Group> getGroups() {
        ArrayList<Group> result = new ArrayList<Group>();

        for (String key : node.getStringList(groupType)) {
            Group group = plugin.getGroup(key);
            if (group != null) {
                result.add(group);
            }
        }

        return result;
    }

    /**
     * Gets a map of non-world-specific permission nodes to boolean values that this group/player defines.
     *
     * @return The map of permissions.
     */
    public Map<String, Boolean> getPermissions() {
        return plugin.getAllPerms(node.getName(), node.getCurrentPath());
    }

    /**
     * Gets a list of worlds this group/player defines world-specific permissions for.
     *
     * @return The list of worlds.
     */
    public Set<String> getWorlds() {
        if (node.getConfigurationSection("worlds") == null) {
            return new HashSet<String>();
        }
        return node.getConfigurationSection("worlds").getKeys(false);
    }

    /**
     * Gets a map of world-specific permission nodes to boolean values that this group/player defines.
     *
     * @param world The name of the world.
     * @return The map of permissions.
     */
    public Map<String, Boolean> getWorldPermissions(String world) {
        return plugin.getAllPerms(node.getName() + ":" + world, node.getName() + "/world/" + world);
    }

}
