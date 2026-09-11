package com.miningtop;

import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * MiningTop - 1.12.2 server-side block mining counter.
 *
 * Counts every block broken by every player (survival or creative,
 * any dimension, vanilla or modded blocks), stores the data with the
 * world save, and provides lookup / leaderboard commands.
 */
@Mod(
        modid = MiningTop.MODID,
        name = MiningTop.NAME,
        version = MiningTop.VERSION,
        // "*" means the mod works on the server even if clients
        // do not have it installed (pure server-side mod).
        acceptableRemoteVersions = "*"
)
public class MiningTop {

    public static final String MODID = "miningtop";
    public static final String NAME = "MiningTop";
    public static final String VERSION = "1.3.0";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // BreakEvent is posted on the MinecraftForge event bus.
        MinecraftForge.EVENT_BUS.register(new BlockBreakHandler());
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandMCount());
        event.registerServerCommand(new CommandMTop());
        event.registerServerCommand(new CommandMReset());
        event.registerServerCommand(new CommandMName());
        event.registerServerCommand(new CommandMNick());
        event.registerServerCommand(new CommandMSet());
    }

    /**
     * After the worlds are loaded: restore the configured display
     * slot (belowName by default) and mirror all stored totals into
     * the scoreboards, so the counts are visible right away.
     */
    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        World world = DimensionManager.getWorld(0);
        if (world != null) {
            ScoreboardSync.syncAll(MiningData.get(world));
        }
    }
}
