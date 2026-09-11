package com.miningtop;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

public final class BlockUtil {

    private BlockUtil() {
    }

    /**
     * Unique string id for a block, e.g. "minecraft:stone",
     * "minecraft:log" (metadata is ignored so all log variants
     * count together), or "buildcraft:blockMine" for modded blocks.
     */
    public static String idOf(Block block) {
        if (block == null) {
            return "unknown";
        }
        try {
            ResourceLocation name = Block.REGISTRY.getNameForObject(block);
            return name == null ? "unknown" : name.toString();
        } catch (Throwable ignored) {
        }
        return "unknown";
    }

    /**
     * Localized display name of a block id ("minecraft:stone" ->
     * "Stone" / "石头"), falling back to the raw id.
     */
    public static String displayName(String id) {
        if (id == null) {
            return "unknown";
        }
        try {
            Block block = Block.REGISTRY.getObject(new ResourceLocation(id));
            if (block != null) {
                String local = block.getLocalizedName();
                // An unregistered translation returns the raw key,
                // e.g. "tile.stone.name" - prefer the id in that case.
                if (local != null && local.length() > 0
                        && !local.startsWith("tile.")) {
                    return local;
                }
            }
        } catch (Throwable ignored) {
        }
        return id;
    }
}
