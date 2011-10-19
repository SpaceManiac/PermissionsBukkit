package com.platymuus.bukkit.permissions;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;

/**
 * Player listener: takes care of registering and unregistering players on join
 */
class PlayerListener extends org.bukkit.event.player.PlayerListener {

    private PermissionsPlugin plugin;

    public PlayerListener(PermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.debug("Player " + event.getPlayer().getName() + " joined, registering...");
        plugin.registerPlayer(event.getPlayer());
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.debug("Player " + event.getPlayer().getName() + " quit, unregistering...");
        plugin.unregisterPlayer(event.getPlayer());
    }

    @Override
    public void onPlayerKick(PlayerKickEvent event) {
        plugin.debug("Player " + event.getPlayer().getName() + " was kicked, unregistering...");
        plugin.unregisterPlayer(event.getPlayer());
    }
    
    @Override
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.calculateAttachment(event.getPlayer());
    }
    
    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (!event.getPlayer().isOp() && !event.getPlayer().hasPermission("permissions.build")) {
            if (event.getAction() != Action.PHYSICAL && plugin.getConfiguration().getString("messages.build", "").length() > 0) {
                String message = plugin.getConfiguration().getString("messages.build", "").replace('&', '\u00A7');
                event.getPlayer().sendMessage(message);
            }
            event.setCancelled(true);
        }
    }

}
