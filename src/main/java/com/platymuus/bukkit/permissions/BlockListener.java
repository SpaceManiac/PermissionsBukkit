package com.platymuus.bukkit.permissions;

import org.bukkit.event.block.*;

/**
 * Player listener: takes care of registering and unregistering players on join
 */
class BlockListener extends org.bukkit.event.block.BlockListener {

    private PermissionsPlugin plugin;
    private final String MESSAGE;

    public BlockListener(PermissionsPlugin plugin) {
        this.plugin = plugin;
        MESSAGE = plugin.getConfiguration().getString("messages.build", "").replaceAll("(?i)&([0-F])", "\u00A7$1");
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) { return; }
        if (!event.getPlayer().isOp() && !event.getPlayer().hasPermission("permissions.build")) {
            if (MESSAGE.length() > 0) {
                event.getPlayer().sendMessage(MESSAGE);
            }
            event.setCancelled(true);
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) { return; }
        if (!event.getPlayer().isOp() && !event.getPlayer().hasPermission("permissions.build")) {
            if (MESSAGE.length() > 0) {
                event.getPlayer().sendMessage(MESSAGE);
            }
            event.setCancelled(true);
        }
    }

}
