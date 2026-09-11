package com.miningtop;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.DimensionManager;

import java.util.List;

/**
 * /mname [标题|reset]
 *
 * OP command that renames the mining objective (the title shown at
 * the top of the sidebar / in front of belowName scores). Without
 * arguments shows the current title, "reset" restores the localized
 * default ("挖掘数" / "Mined"). The title is stored with the world
 * save and pushed into every loaded world's scoreboard.
 */
public class CommandMName extends CommandBase {

    private static final int MAX_LENGTH = 40;

    @Override
    public String getCommandName() {
        return "mname";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return StatCollector.translateToLocal("commands.mname.usage");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP only
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        MiningData data = MiningData.get(DimensionManager.getWorld(0));

        if (args.length == 0) {
            if (data.objectiveTitle != null && data.objectiveTitle.length() > 0) {
                CommandMCount.send(sender, EnumChatFormatting.YELLOW
                        + String.format(StatCollector.translateToLocal(
                                "commands.mname.current"), data.objectiveTitle));
            } else {
                CommandMCount.send(sender, EnumChatFormatting.YELLOW
                        + String.format(StatCollector.translateToLocal(
                                "commands.mname.usingDefault"),
                                ScoreboardSync.defaultTitle()));
            }
            return;
        }

        if (args.length == 1
                && (args[0].equalsIgnoreCase("reset")
                    || args[0].equalsIgnoreCase("clear")
                    || args[0].equalsIgnoreCase("default"))) {
            data.objectiveTitle = null;
            data.markDirty();
            ScoreboardSync.applyObjectiveTitle(data);
            CommandMCount.send(sender, EnumChatFormatting.GREEN
                    + String.format(StatCollector.translateToLocal(
                            "commands.mname.reset"),
                            ScoreboardSync.defaultTitle()));
            return;
        }

        String title = CommandBase.joinNiceString(args).trim();
        if (title.length() == 0 || title.length() > MAX_LENGTH) {
            throw new WrongUsageException("commands.mname.usage");
        }

        data.objectiveTitle = title;
        data.markDirty();
        ScoreboardSync.applyObjectiveTitle(data);

        CommandMCount.send(sender, EnumChatFormatting.GREEN
                + String.format(StatCollector.translateToLocal(
                        "commands.mname.set"), title));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(
                    args, "reset");
        }
        return null;
    }
}
