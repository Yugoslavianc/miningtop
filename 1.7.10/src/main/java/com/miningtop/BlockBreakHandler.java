package com.miningtop;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.BlockEvent;

/**
 * Listens for block breaks and records them into the world data.
 *
 * BlockEvent.BreakEvent in 1.7.10 is fired on the server side every
 * time a player (real or fake) destroys a block, no matter which tool,
 * game mode or dimension is used. That is exactly what we need.
 */
public class BlockBreakHandler {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.world;
        if (world == null || world.isRemote) {
            return; // only count on the server
        }

        EntityPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        // Always store into the overworld storage so all dimensions
        // share one combined scoreboard.
        World storageWorld = DimensionManager.getWorld(0);
        if (storageWorld == null) {
            storageWorld = world;
        }

        String blockId = BlockUtil.idOf(event.block);

        MiningData data = MiningData.get(storageWorld);
        data.addBreak(
                player.getGameProfile().getId(),
                player.getCommandSenderName(),
                blockId);

        // Mirror the new total into the vanilla scoreboard so the
        // count is visible in belowName / sidebar / list.
        ScoreboardSync.syncPlayer(
                data, player.getGameProfile().getId());
    }
}
