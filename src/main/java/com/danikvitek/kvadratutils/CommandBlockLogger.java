package com.danikvitek.kvadratutils;

import net.minecraft.server.v1_16_R3.TileEntityCommand;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CommandBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class CommandBlockLogger implements Listener { // TODO: log command minecarts
    @EventHandler
    public void onCBPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof CommandBlock) {
            Material type = block.getType();
            boolean conditional = ((org.bukkit.block.data.type.CommandBlock) block.getBlockData()).isConditional();
            BlockFace facing = ((org.bukkit.block.data.type.CommandBlock) block.getBlockData()).getFacing();
            String command = ((CommandBlock) block.getState()).getCommand();
//            TileEntityCommand
        }
    }

    @EventHandler
    public void onCBMultiPlace(BlockMultiPlaceEvent event) {

    }

    @EventHandler
    public void onCBBreak(BlockBreakEvent event) {

    }

    @EventHandler
    public void onCBPlaceServerCommand(ServerCommandEvent event) {

    }

    @EventHandler
    public void onCBPlacePlayerCommand(PlayerCommandPreprocessEvent event) {

    }
}
