package com.platymuus.bukkit.permissions;

import org.bukkit.event.block.*;

/**
 * Player listener: takes care of registering and unregistering players on join
 */
public class BlockListener extends org.bukkit.event.block.BlockListener {
    
    private PermissionsMain plugin;
    
    public BlockListener(PermissionsMain plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.getPlayer().hasPermission("permissions.build")) {
            event.setCancelled(true);
        }
    }
    
    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.getPlayer().hasPermission("permissions.build")) {
            event.setCancelled(true);
        }
    }
    
}
