package com.platymuus.bukkit.permissions;

import org.bukkit.event.block.*;

/**
 * Player listener: takes care of registering and unregistering players on join
 */
public class BlockListener extends org.bukkit.event.block.BlockListener {

    private PermissionsPlugin plugin;

    public BlockListener(PermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.getPlayer().isOp() && !event.getPlayer().hasPermission("permissions.build")) {
            if (plugin.getConfiguration().getString("messages.build", "").length() > 0) {
                String message = plugin.getConfiguration().getString("messages.build", "").replace('&', '\u00A7');
                event.getPlayer().sendMessage(message);
            }
            event.setCancelled(true);
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.getPlayer().isOp() && !event.getPlayer().hasPermission("permissions.build")) {
            if (plugin.getConfiguration().getString("messages.build", "").length() > 0) {
                String message = plugin.getConfiguration().getString("messages.build", "").replace('&', '\u00A7');
                event.getPlayer().sendMessage(message);
            }
            event.setCancelled(true);
        }
    }

}
