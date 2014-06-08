package com.platymuus.bukkit.permissions;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A class representing a permissions group.
 */
public final class Group {

    private final PermissionsPlugin plugin;
    private final String name;

    Group(PermissionsPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * @deprecated Use UUIDs instead.
     */
    @Deprecated
    public List<String> getPlayers() {
        ArrayList<String> result = new ArrayList<String>();
        if (plugin.getNode("users") != null) {
            for (String user : plugin.getNode("users").getKeys(false)) {
                ConfigurationSection node = plugin.getNode("users/" + user);
                for (String group : node.getStringList("groups")) {
                    if (name.equalsIgnoreCase(group) && !result.contains(user)) {
                        // attempt to determine the username
                        if (node.getString("name") != null) {
                            // converted node
                            result.add(node.getString("name"));
                        } else {
                            // unconverted node, or UUID node missing "name" element
                            result.add(user);
                        }
                        break;
                    }
                }
            }
        }
        return result;
    }

    public List<UUID> getPlayerUUIDs() {
        ArrayList<UUID> result = new ArrayList<UUID>();
        if (plugin.getNode("users") != null) {
            for (String user : plugin.getNode("users").getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(user);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                for (String group : plugin.getNode("users/" + user).getStringList("groups")) {
                    if (name.equalsIgnoreCase(group) && !result.contains(uuid)) {
                        result.add(uuid);
                        break;
                    }
                }
            }
        }
        return result;
    }

    public List<Player> getOnlinePlayers() {
        ArrayList<Player> result = new ArrayList<Player>();
        for (UUID uuid : getPlayerUUIDs()) {
            Player player = Bukkit.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }

    public PermissionInfo getInfo() {
        ConfigurationSection node = plugin.getNode("groups/" + name);
        if (node == null) {
            return null;
        }
        return new PermissionInfo(plugin, node, "inheritance");
    }

    @Override
    public boolean equals(Object o) {
        return !(o == null || !(o instanceof Group)) && name.equalsIgnoreCase(((Group) o).getName());
    }

    @Override
    public String toString() {
        return "Group{name=" + name + "}";
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
