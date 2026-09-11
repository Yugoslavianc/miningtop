package com.miningtop;

import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.Map;
import java.util.UUID;

/**
 * Bridges the mining counters into the vanilla scoreboard.
 *
 * Maintains a dummy objective named "mined" (default title "挖掘数",
 * changeable by an OP via /mname)
 * and mirrors every player's total into it, so the count can be
 * shown in the vanilla display slots: belowName (default), sidebar
 * or list - see /mset. You can also use vanilla commands on the
 * objective, e.g. "/scoreboard objectives setdisplay sidebar mined".
 *
 * On 1.12.2 all dimensions share one ServerScoreboard; we still walk
 * every loaded world's scoreboard so the sync also covers setups
 * that replace scoreboards per dimension.
 */
public final class ScoreboardSync {

    public static final String OBJECTIVE = "mined";

    /** Vanilla display slot ids (see Scoreboard.setObjectiveInDisplaySlot). */
    public static final int SLOT_OFF = -1;
    public static final int SLOT_LIST = 0;
    public static final int SLOT_SIDEBAR = 1;
    public static final int SLOT_BELOWNAME = 2;

    private ScoreboardSync() {
    }

    /** Localized default title for the objective (lang key miningtop.title). */
    public static String defaultTitle() {
        return I18n.translateToLocal("miningtop.title");
    }

    /** The title /mname has configured, or the localized default. */
    public static String title(MiningData data) {
        return (data.objectiveTitle != null && data.objectiveTitle.length() > 0)
                ? data.objectiveTitle : defaultTitle();
    }

    public static int slotId(String slotName) {
        if (MiningData.SLOT_SIDEBAR.equalsIgnoreCase(slotName)) {
            return SLOT_SIDEBAR;
        }
        if (MiningData.SLOT_LIST.equalsIgnoreCase(slotName)) {
            return SLOT_LIST;
        }
        if (MiningData.SLOT_BELOWNAME.equalsIgnoreCase(slotName)) {
            return SLOT_BELOWNAME;
        }
        return SLOT_OFF;
    }

    /**
     * Gets (or lazily creates) the mining objective on a scoreboard,
     * with the currently configured title. If the objective already
     * exists but its title is outdated (after /mname), it is updated
     * in place - setDisplayName broadcasts the change to clients.
     */
    public static ScoreObjective ensureObjective(Scoreboard scoreboard, MiningData data) {
        ScoreObjective objective = scoreboard.getObjective(OBJECTIVE);
        String title = title(data);
        if (objective == null) {
            objective = scoreboard.addScoreObjective(
                    OBJECTIVE, IScoreCriteria.DUMMY);
            objective.setDisplayName(title);
        } else if (!title.equals(objective.getDisplayName())) {
            objective.setDisplayName(title);
        }
        return objective;
    }

    /** Pushes the /mname title into all loaded worlds (used by /mname). */
    public static void applyObjectiveTitle(MiningData data) {
        for (WorldServer world : DimensionManager.getWorlds()) {
            ensureObjective(world.getScoreboard(), data);
        }
    }

    /**
     * Which scoreboard entry name represents this player in the given
     * slot. Nicknames (from /mnick) only work in the sidebar -
     * belowName and the tab list are matched by the client against
     * the player's real name.
     */
    private static String entryName(int currentSlotId, PlayerMiningStats stats) {
        if (currentSlotId == SLOT_SIDEBAR
                && stats.nickname != null && stats.nickname.length() > 0) {
            return stats.nickname;
        }
        return stats.name;
    }

    /**
     * Removes OUR objective's entry for the given name without
     * touching scores other plugins may have set for that name
     * (this is the same path vanilla "/scoreboard players reset"
     * takes; it also broadcasts the removal to clients).
     */
    private static void removeEntry(Scoreboard scoreboard,
                                    ScoreObjective objective, String name) {
        if (name == null || name.length() == 0) {
            return;
        }
        Map<ScoreObjective, ?> scores = scoreboard.getObjectivesForEntity(name);
        if (scores.containsKey(objective)) {
            scoreboard.removeObjectiveFromEntity(name, objective);
        }
    }

    private static void removeFromDisplaySlots(Scoreboard scoreboard,
                                                ScoreObjective objective) {
        for (int currentSlotId = 0; currentSlotId <= 2; currentSlotId++) {
            if (scoreboard.getObjectiveInDisplaySlot(currentSlotId) == objective) {
                scoreboard.setObjectiveInDisplaySlot(currentSlotId, null);
            }
        }
    }

    /**
     * Pushes one player's total into the scoreboards of all loaded
     * worlds, cleaning up entries that are no longer used (e.g. the
     * old name after /mnick, or real-name entries while a nickname
     * is active in the sidebar).
     */
    public static void syncPlayer(MiningData data, UUID playerUuid) {
        PlayerMiningStats stats = data.players.get(playerUuid);
        if (stats == null) {
            return;
        }
        int currentSlotId = slotId(data.displaySlot);

        for (WorldServer world : DimensionManager.getWorlds()) {
            Scoreboard scoreboard = world.getScoreboard();

            if (currentSlotId == SLOT_OFF) {
                ScoreObjective objective = scoreboard.getObjective(OBJECTIVE);
                if (objective != null) {
                    removeEntry(scoreboard, objective, stats.name);
                    if (stats.nickname != null) {
                        removeEntry(scoreboard, objective, stats.nickname);
                    }
                    removeFromDisplaySlots(scoreboard, objective);
                }
                continue;
            }

            ScoreObjective objective = ensureObjective(scoreboard, data);
            String entry = entryName(currentSlotId, stats);
            if (entry == null || entry.length() == 0) {
                continue;
            }
            int points = (int) Math.min(stats.total, Integer.MAX_VALUE);
            scoreboard.getOrCreateScore(entry, objective).setScorePoints(points);

            String staleEntry = entry.equals(stats.name)
                    ? stats.nickname : stats.name;
            removeEntry(scoreboard, objective, staleEntry);
        }
    }

    /** Removes every trace of one player from all scoreboards. */
    public static void removePlayerEntries(PlayerMiningStats stats) {
        for (WorldServer world : DimensionManager.getWorlds()) {
            Scoreboard scoreboard = world.getScoreboard();
            ScoreObjective objective = scoreboard.getObjective(OBJECTIVE);
            if (objective == null) {
                continue;
            }
            removeEntry(scoreboard, objective, stats.name);
            if (stats.nickname != null) {
                removeEntry(scoreboard, objective, stats.nickname);
            }
        }
    }

    /** Applies the configured display slot to all loaded worlds. */
    public static void applyDisplay(MiningData data) {
        int currentSlotId = slotId(data.displaySlot);
        for (WorldServer world : DimensionManager.getWorlds()) {
            Scoreboard scoreboard = world.getScoreboard();
            if (currentSlotId == SLOT_OFF) {
                ScoreObjective objective = scoreboard.getObjective(OBJECTIVE);
                if (objective != null) {
                    removeFromDisplaySlots(scoreboard, objective);
                }
            } else {
                scoreboard.setObjectiveInDisplaySlot(
                        currentSlotId, ensureObjective(scoreboard, data));
            }
        }
    }

    /** Full resync: display slot + every stored player's score. */
    public static void syncAll(MiningData data) {
        applyDisplay(data);
        for (UUID playerUuid : data.players.keySet()) {
            syncPlayer(data, playerUuid);
        }
    }
}
