package com.miningtop;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds all mining counters and persists them into the world save
 * (file "data/miningtop_data.dat" of the overworld), so counts
 * survive server restarts. Uses a WorldSavedData - the vanilla way
 * to attach data to a world.
 */
public class MiningData extends WorldSavedData {

    public static final String DATA_NAME = "miningtop_data";

    /** Where the mining count objective is displayed. */
    public static final String SLOT_BELOWNAME = "belowName";
    public static final String SLOT_SIDEBAR = "sidebar";
    public static final String SLOT_LIST = "list";
    public static final String SLOT_OFF = "off";

    /** UUID -> stats for every player that has ever mined a block. */
    public final Map<UUID, PlayerMiningStats> players =
            new HashMap<UUID, PlayerMiningStats>();

    /** Current display slot for the count objective (see /mset). */
    public String displaySlot = SLOT_BELOWNAME;

    /**
     * Custom title of the count objective, set by an OP via /mname.
     * null = use the localized default ("挖掘数" / "Mined").
     */
    public String objectiveTitle;

    public MiningData(String name) {
        super(name);
    }

    /** Loads (or creates) the data for the given world. */
    public static MiningData get(World world) {
        if (world == null) {
            throw new IllegalArgumentException("world must not be null");
        }
        MiningData data = (MiningData) world.perWorldStorage
                .loadData(MiningData.class, DATA_NAME);
        if (data == null) {
            data = new MiningData(DATA_NAME);
            world.perWorldStorage.setData(DATA_NAME, data);
        }
        return data;
    }

    public PlayerMiningStats getStats(UUID playerUuid) {
        PlayerMiningStats stats = players.get(playerUuid);
        if (stats == null) {
            stats = new PlayerMiningStats();
            players.put(playerUuid, stats);
        }
        return stats;
    }

    /** Returns the stats (also so the caller can sync scoreboards). */
    public PlayerMiningStats addBreak(UUID playerUuid, String playerName, String blockId) {
        PlayerMiningStats stats = getStats(playerUuid);
        stats.name = playerName;
        stats.total++;

        Long currentCount = stats.perBlock.get(blockId);
        stats.perBlock.put(blockId, currentCount == null
                ? Long.valueOf(1L)
                : Long.valueOf(currentCount.longValue() + 1L));

        markDirty();
        return stats;
    }

    /** Finds the UUID of a stored player by (case-insensitive) name. */
    public UUID findUUIDByName(String playerName) {
        if (playerName == null) {
            return null;
        }
        for (Map.Entry<UUID, PlayerMiningStats> entry : players.entrySet()) {
            if (entry.getValue().name != null
                    && entry.getValue().name.equalsIgnoreCase(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * True if some other player already uses the candidate as their
     * real name or nickname (case-insensitive).
     */
    public boolean isNameTaken(String candidate, UUID selfUuid) {
        if (candidate == null) {
            return false;
        }
        for (Map.Entry<UUID, PlayerMiningStats> entry : players.entrySet()) {
            if (selfUuid != null && entry.getKey().equals(selfUuid)) {
                continue;
            }
            PlayerMiningStats stats = entry.getValue();
            if (candidate.equalsIgnoreCase(stats.name)
                    || (stats.nickname != null
                        && candidate.equalsIgnoreCase(stats.nickname))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        players.clear();
        String slot = nbt.getString("DisplaySlot");
        if (SLOT_SIDEBAR.equalsIgnoreCase(slot)) {
            displaySlot = SLOT_SIDEBAR;
        } else if (SLOT_LIST.equalsIgnoreCase(slot)) {
            displaySlot = SLOT_LIST;
        } else if (SLOT_OFF.equalsIgnoreCase(slot)) {
            displaySlot = SLOT_OFF;
        } else {
            displaySlot = SLOT_BELOWNAME;
        }
        objectiveTitle = nbt.hasKey("ObjectiveTitle")
                ? nbt.getString("ObjectiveTitle") : null;

        for (Object rawKey : nbt.getKeySet()) {
            String key = (String) rawKey;
            UUID playerUuid;
            try {
                playerUuid = UUID.fromString(key);
            } catch (IllegalArgumentException invalidUuid) {
                continue;
            }
            NBTTagCompound tag = nbt.getCompoundTag(key);
            PlayerMiningStats stats = new PlayerMiningStats();
            stats.name = tag.getString("Name");
            stats.total = tag.getLong("Total");
            if (tag.hasKey("Nickname")) {
                stats.nickname = tag.getString("Nickname");
            } else if (tag.hasKey("DisplayName")) {
                // legacy key from v1.1.0/1.2.0 saves
                stats.nickname = tag.getString("DisplayName");
            }

            NBTTagCompound blocks = tag.getCompoundTag("Blocks");
            for (Object rawBlockId : blocks.getKeySet()) {
                String blockId = (String) rawBlockId;
                stats.perBlock.put(blockId,
                        Long.valueOf(blocks.getLong(blockId)));
            }
            players.put(playerUuid, stats);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString("DisplaySlot", displaySlot);
        if (objectiveTitle != null) {
            nbt.setString("ObjectiveTitle", objectiveTitle);
        }
        for (Map.Entry<UUID, PlayerMiningStats> entry : players.entrySet()) {
            PlayerMiningStats stats = entry.getValue();
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Name", stats.name);
            tag.setLong("Total", stats.total);
            if (stats.nickname != null) {
                tag.setString("Nickname", stats.nickname);
            }

            NBTTagCompound blocks = new NBTTagCompound();
            for (Map.Entry<String, Long> blockEntry : stats.perBlock.entrySet()) {
                blocks.setLong(blockEntry.getKey(),
                        blockEntry.getValue().longValue());
            }
            tag.setTag("Blocks", blocks);

            nbt.setTag(entry.getKey().toString(), tag);
        }
    }

    /** Removes a player's stats. Returns true if something was removed. */
    public boolean reset(UUID playerUuid) {
        boolean removed = players.remove(playerUuid) != null;
        if (removed) {
            markDirty();
        }
        return removed;
    }

    /** Clears all stats. Returns the number of players removed. */
    public int resetAll() {
        int removedCount = players.size();
        players.clear();
        if (removedCount > 0) {
            markDirty();
        }
        return removedCount;
    }
}
