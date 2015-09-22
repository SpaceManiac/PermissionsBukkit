package com.platymuus.bukkit.permissions;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;

/**
 * Listen for player-based events to keep track of players and build permissions.
 */
class PlayerListener implements Listener {

    private PermissionsPlugin plugin;

    public PlayerListener(PermissionsPlugin plugin) {
        this.plugin = plugin;
    }
    
    // Keep track of player's world

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.calculateAttachment(event.getPlayer());
    }
    
    // Register players when needed

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerJoinEvent event) {
        plugin.debug("Player " + event.getPlayer().getName() + " joined, registering...");
        plugin.registerPlayer(event.getPlayer());
    }

    // Unregister players when needed

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.isCancelled()) return;
        plugin.debug("Player " + event.getPlayer().getName() + " was kicked, unregistering...");
        plugin.unregisterPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.debug("Player " + event.getPlayer().getName() + " quit, unregistering...");
        plugin.unregisterPlayer(event.getPlayer());
    }
    
    // Prevent doing things in the event of permissions.build: false

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (!event.getPlayer().hasPermission("permissions.build")) {
            bother(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (!event.getPlayer().hasPermission("permissions.build")) {
            bother(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        if (!event.getPlayer().hasPermission("permissions.build")) {
            bother(event.getPlayer());
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if ((event.getDamager() instanceof Player) && (!((Player)event.getDamager()).hasPermission("permissions.build"))) {
            bother((Player)event.getDamager());
            event.setCancelled(true);
        }
    }

    private void bother(Player player) {
        if (plugin.getConfig().getString("messages.build", "").length() > 0) {
            String message = plugin.getConfig().getString("messages.build", "").replace('&', '\u00A7');
            player.sendMessage(message);
        }
    }

}
