package com.miningtop;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.DimensionManager;

import java.util.Collections;
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
    public String getName() {
        return "mset";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return I18n.translateToLocal("commands.mset.usage");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP only
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        MiningData data = MiningData.get(DimensionManager.getWorld(0));

        if (args.length == 0) {
            CommandMCount.send(sender, TextFormatting.YELLOW
                    + String.format(I18n.translateToLocal("commands.mset.current"),
                            data.displaySlot));
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

        CommandMCount.send(sender, TextFormatting.GREEN
                + String.format(I18n.translateToLocal("commands.mset.set"), slot));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                           String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, OPTIONS);
        }
        return Collections.emptyList();
    }
}
