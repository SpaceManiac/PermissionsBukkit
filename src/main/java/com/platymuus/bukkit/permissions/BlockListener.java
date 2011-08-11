package com.platymuus.bukkit.permissions;

import org.bukkit.event.block.*;

/**
 * Player listener: takes care of registering and unregistering players on join
 */
class BlockListener extends org.bukkit.event.block.BlockListener {

    private final String MESSAGE;

    public BlockListener(PermissionsPlugin plugin) {
        MESSAGE = plugin.getConfiguration().getString("messages.build", "").replace('&', '\u00A7');
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
