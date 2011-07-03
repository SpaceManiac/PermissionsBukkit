package com.platymuus.bukkit.permissions;

import org.bukkit.event.player.*;

/**
 * Player listener: takes care of registering and unregistering players on join
 */
public class PlayerListener extends org.bukkit.event.player.PlayerListener {
    
    private PermissionsMain plugin;
    
    public PlayerListener(PermissionsMain plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.registerPlayer(event.getPlayer());
    }
    
    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.unregisterPlayer(event.getPlayer());
    }
    
    @Override
    public void onPlayerKick(PlayerKickEvent event) {
        plugin.unregisterPlayer(event.getPlayer());
    }
    
}
