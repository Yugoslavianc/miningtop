package com.miningtop;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.DimensionManager;

import java.util.List;

/**
 * /mset <belowName|sidebar|list|off>
 *
 * Chooses where the mining count is displayed:
 *   belowName - under every player's name tag (default)
 *   sidebar   - the sidebar on the right side of the screen
 *              (this is the slot where /mnick nicknames show)
 *   list      - next to the player's name in the tab list
 *   off       - not displayed anywhere
 * Without arguments the current setting is shown.
 */
public class CommandMSet extends CommandBase {

    private static final String[] OPTIONS = {
            MiningData.SLOT_BELOWNAME, MiningData.SLOT_SIDEBAR,
            MiningData.SLOT_LIST, MiningData.SLOT_OFF
    };

    @Override
    public String getCommandName() {
        return "mset";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return StatCollector.translateToLocal("commands.mset.usage");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP only
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        MiningData data = MiningData.get(DimensionManager.getWorld(0));

        if (args.length == 0) {
            CommandMCount.send(sender, EnumChatFormatting.YELLOW
                    + String.format(StatCollector.translateToLocal(
                            "commands.mset.current"), data.displaySlot));
            return;
        }

        String slot = null;
        for (String option : OPTIONS) {
            if (option.equalsIgnoreCase(args[0])) {
                slot = option;
                break;
            }
        }
        if (slot == null) {
            throw new WrongUsageException("commands.mset.usage");
        }

        data.displaySlot = slot;
        data.markDirty();
        ScoreboardSync.syncAll(data);

        CommandMCount.send(sender, EnumChatFormatting.GREEN
                + String.format(StatCollector.translateToLocal(
                        "commands.mset.set"), slot));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(args, OPTIONS);
        }
        return null;
    }
}
