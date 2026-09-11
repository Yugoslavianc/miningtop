package com.miningtop;

import net.minecraft.block.Block;

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
            Object name = Block.blockRegistry.getNameForObject(block);
            if (name instanceof String) {
                return (String) name;
            }
        } catch (Throwable ignored) {
        }
        return "unknown";
    }

    /**
     * Localized display name of a block id ("minecraft:stone" ->
     * "Stone" / "Stone"), falling back to the raw id.
     */
    public static String displayName(String id) {
        if (id == null) {
            return "unknown";
        }
        try {
            Object block = Block.blockRegistry.getObject(id);
            if (block instanceof Block) {
                String local = ((Block) block).getLocalizedName();
                if (local != null && local.length() > 0) {
                    return local;
                }
            }
        } catch (Throwable ignored) {
        }
        return id;
    }
}
