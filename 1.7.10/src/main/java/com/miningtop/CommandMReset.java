package com.miningtop;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /mreset <player|all>
 *
 * Admin command (permission level 2 = OP) to delete mining stats of
 * one player or of everyone.
 */
public class CommandMReset extends CommandBase {

    @Override
    public String getCommandName() {
        return "mreset";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return StatCollector.translateToLocal("commands.mreset.usage");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP only
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            throw new WrongUsageException("commands.mreset.usage");
        }

        MiningData data = MiningData.get(DimensionManager.getWorld(0));

        if (args[0].equalsIgnoreCase("all")) {
            // Clean scoreboard entries before the map is cleared.
            for (PlayerMiningStats stats : data.players.values()) {
                ScoreboardSync.removePlayerEntries(stats);
            }
            int removed = data.resetAll();
            CommandMCount.send(sender, EnumChatFormatting.YELLOW
                    + String.format(StatCollector.translateToLocal("commands.mreset.doneAll"),
                            Integer.valueOf(removed)));
            return;
        }

        String name = args[0];
        UUID uuid = null;
        try {
            uuid = CommandBase.getPlayer(sender, name).getGameProfile().getId();
        } catch (PlayerNotFoundException ignored) {
            uuid = data.findUUIDByName(name);
        }
        PlayerMiningStats stats = uuid == null ? null : data.players.get(uuid);
        if (uuid == null || !data.reset(uuid)) {
            throw new PlayerNotFoundException("commands.mcount.noData", name);
        }
        if (stats != null) {
            ScoreboardSync.removePlayerEntries(stats);
        }

        CommandMCount.send(sender, EnumChatFormatting.YELLOW
                + String.format(StatCollector.translateToLocal("commands.mreset.done"),
                        name));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<String>();
            for (String user : MinecraftServer.getServer().getAllUsernames()) {
                options.add(user);
            }
            options.add("all");
            return CommandBase.getListOfStringsMatchingLastWord(args,
                    options.toArray(new String[options.size()]));
        }
        return null;
    }
}
