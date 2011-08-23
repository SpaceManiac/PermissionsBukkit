package com.platymuus.bukkit.permissions;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;

/**
 * Player listener: takes care of registering and unregistering players on join
 */
class PlayerListener extends org.bukkit.event.player.PlayerListener {

    private PermissionsPlugin plugin;
    private final String MESSAGE;

    public PlayerListener(PermissionsPlugin plugin) {
        this.plugin = plugin;
        MESSAGE = plugin.getConfiguration().getString("messages.build", "").replaceAll("(?i)&([0-F])", "\u00A7$1");
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.debug("Player " + event.getPlayer().getName() + " joined, registering...");
        plugin.registerPlayer(event.getPlayer().getName());
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.debug("Player " + event.getPlayer().getName() + " quit, unregistering...");
        plugin.unregisterPlayer(event.getPlayer().getName());
    }

    @Override
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.isCancelled()) { return; }
        plugin.debug("Player " + event.getPlayer().getName() + " was kicked, unregistering...");
        plugin.unregisterPlayer(event.getPlayer().getName());
    }
    
    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) { return; }
        plugin.setLastWorld(event.getPlayer().getName(), event.getTo().getWorld().getName());
    }
    
    @Override
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) { return; }
        plugin.setLastWorld(event.getPlayer().getName(), event.getTo().getWorld().getName());
    }
    
    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) { return; }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (!event.getPlayer().isOp() && !event.getPlayer().hasPermission("permissions.build")) {
            if (event.getAction() != Action.PHYSICAL && MESSAGE.length() > 0) {
                event.getPlayer().sendMessage(MESSAGE);
            }
            event.setCancelled(true);
        }
    }

}
