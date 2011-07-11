package com.platymuus.bukkit.permissions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

/**
 * A class representing a permissions group.
 */
public class Group {
    
    private PermissionsPlugin plugin;
    private String name;
    
    protected Group(PermissionsPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public List<String> getPlayers() {
        ArrayList<String> result = new ArrayList<String>();
        if (plugin.getNode("users") != null) {
            for (String user : plugin.getNode("users").getKeys()) {
                for (String group : plugin.getNode("users." + user).getStringList("groups", new ArrayList<String>())) {
                    if (name.equalsIgnoreCase(group) && !result.contains(user)) {
                        result.add(user);
                    }
                }
            }
        }
        return result;
    }
    
    public List<Player> getOnlinePlayers() {
        ArrayList<Player> result = new ArrayList<Player>();
        for (String user : getPlayers()) {
            Player player = Bukkit.getServer().getPlayer(user);
            if (player != null && player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }
    
    public PermissionInfo getInfo() {
        ConfigurationNode node = plugin.getNode("groups." + name);
        if (node == null) {
            return null;
        }
        return new PermissionInfo(plugin, node, "inheritance");
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Group))
            return false;
        return name.equalsIgnoreCase(((Group) o).getName());
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
