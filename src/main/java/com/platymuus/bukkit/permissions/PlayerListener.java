package com.platymuus.bukkit.permissions;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;

/**
 * Listen for player-based events to keep track of players and build permissions.
 */
final class PlayerListener implements Listener {

    private final PermissionsPlugin plugin;

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

        if (plugin.configLoadError && event.getPlayer().hasPermission("permissions.reload")) {
            plugin.configLoadError = false;
            event.getPlayer().sendMessage(ChatColor.RED + "[" + ChatColor.GREEN + "PermissionsBukkit" + ChatColor.RED + "] Your configuration is invalid, see the console for details.");
        }
    }

    // Unregister players when needed

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        plugin.debug("Player " + event.getPlayer().getName() + " was kicked, unregistering...");
        plugin.unregisterPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.debug("Player " + event.getPlayer().getName() + " quit, unregistering...");
        plugin.unregisterPlayer(event.getPlayer());
    }

    // Prevent doing things in the event of permissions.build: false

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (!event.getPlayer().hasPermission("permissions.interact")) {
            bother(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.getPlayer().hasPermission("permissions.build")) {
            bother(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.getPlayer().hasPermission("permissions.build")) {
            bother(event.getPlayer());
            event.setCancelled(true);
        }
    }

    private void bother(Player player) {
        if (plugin.getConfig().getString("messages/build", "").length() > 0) {
            String message = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages/build", ""));
            player.sendMessage(message);
        }
    }

}
