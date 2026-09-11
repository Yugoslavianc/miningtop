package com.miningtop;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.Collections;
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
    public String getName() {
        return "mreset";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return I18n.translateToLocal("commands.mreset.usage");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP only
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
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
            CommandMCount.send(sender, TextFormatting.YELLOW
                    + String.format(I18n.translateToLocal("commands.mreset.doneAll"),
                            Integer.valueOf(removed)));
            return;
        }

        String name = args[0];
        UUID playerUuid = null;
        try {
            playerUuid = CommandBase.getPlayer(server, sender, name)
                    .getGameProfile().getId();
        } catch (PlayerNotFoundException offlinePlayer) {
            playerUuid = data.findUUIDByName(name);
        }
        PlayerMiningStats stats = playerUuid == null
                ? null : data.players.get(playerUuid);
        if (playerUuid == null || !data.reset(playerUuid)) {
            throw new PlayerNotFoundException("commands.mcount.noData", name);
        }
        if (stats != null) {
            ScoreboardSync.removePlayerEntries(stats);
        }

        CommandMCount.send(sender, TextFormatting.YELLOW
                + String.format(I18n.translateToLocal("commands.mreset.done"), name));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                           String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            List<String> options = new ArrayList<String>();
            for (String user : server.getOnlinePlayerNames()) {
                options.add(user);
            }
            options.add("all");
            return getListOfStringsMatchingLastWord(args,
                    options.toArray(new String[options.size()]));
        }
        return Collections.emptyList();
    }
}
