package com.miningtop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Per-player mining statistics. Kept memory-resident, persisted to
 * the world save through {@link MiningData}.
 */
public class PlayerMiningStats {

    /** Last known player name (names can change, UUID is the key). */
    public String name = "";

    /**
     * Nickname set by an OP via /mnick (null = not set). Used in the
     * sidebar and in /mtop instead of the real name. Note: belowName
     * and the tab list always use the real name (the client looks the
     * score up by the player's actual name there).
     */
    public String nickname;

    /** Total number of blocks mined. */
    public long total = 0;

    /** Mined count per block id, e.g. "minecraft:stone" -> 1234. */
    public final Map<String, Long> perBlock =
            new HashMap<String, Long>();

    /** Returns the given number of most mined blocks, highest first. */
    public List<Map.Entry<String, Long>> topBlocks(int limit) {
        List<Map.Entry<String, Long>> sortedEntries =
                new ArrayList<Map.Entry<String, Long>>(perBlock.entrySet());
        Collections.sort(sortedEntries, new Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> first,
                               Map.Entry<String, Long> second) {
                long difference = second.getValue().longValue()
                        - first.getValue().longValue();
                return difference > 0 ? 1 : (difference < 0 ? -1 : 0);
            }
        });
        return new ArrayList<Map.Entry<String, Long>>(
                sortedEntries.subList(0, Math.min(limit, sortedEntries.size())));
    }
}
