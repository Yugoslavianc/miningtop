package com.miningtop;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.DimensionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * /mnick <玩家> [名字|clear]
 *
 * OP command that sets a player's nickname for the leaderboard and
 * the sidebar. Without a nickname argument the current nickname is
 * shown. "clear" removes it again. Works for offline players too
 * (as long as they have mining data). Nicknames only show up in the
 * sidebar slot - belowName and the tab list are matched by the
 * client against the real player name.
 */
public class CommandMNick extends CommandBase {

    private static final int MAX_LENGTH = 40;

    @Override
    public String getName() {
        return "mnick";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("mnickname");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return I18n.translateToLocal("commands.mnick.usage");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP only
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException("commands.mnick.usage");
        }

        MiningData data = MiningData.get(DimensionManager.getWorld(0));

        EntityPlayerMP onlinePlayer = null;
        try {
            onlinePlayer = CommandBase.getPlayer(server, sender, args[0]);
        } catch (PlayerNotFoundException offlinePlayer) {
            // fall back to the stored data below
        }

        UUID playerUuid;
        String playerName;
        if (onlinePlayer != null) {
            playerUuid = onlinePlayer.getGameProfile().getId();
            playerName = onlinePlayer.getName();
        } else {
            playerUuid = data.findUUIDByName(args[0]);
            playerName = args[0];
        }

        PlayerMiningStats stats = playerUuid == null
                ? null : data.players.get(playerUuid);
        if (stats == null) {
            throw new PlayerNotFoundException("commands.mcount.noData", args[0]);
        }

        if (args.length == 1) {
            if (stats.nickname != null && stats.nickname.length() > 0) {
                CommandMCount.send(sender, TextFormatting.YELLOW
                        + String.format(I18n.translateToLocal("commands.mnick.current"),
                                playerName, stats.nickname));
            } else {
                CommandMCount.send(sender, TextFormatting.YELLOW
                        + String.format(I18n.translateToLocal("commands.mnick.usingDefault"),
                                playerName, stats.name));
            }
            return;
        }

        if (args.length == 2
                && (args[1].equalsIgnoreCase("clear")
                    || args[1].equalsIgnoreCase("off"))) {
            stats.nickname = null;
            data.markDirty();
            ScoreboardSync.syncPlayer(data, playerUuid);
            CommandMCount.send(sender, TextFormatting.GREEN
                    + String.format(I18n.translateToLocal("commands.mnick.cleared"),
                            playerName));
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 1; index < args.length; index++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        String nickname = builder.toString().trim();
        if (nickname.length() == 0 || nickname.length() > MAX_LENGTH) {
            throw new WrongUsageException("commands.mnick.usage");
        }
        if (data.isNameTaken(nickname, playerUuid)) {
            CommandMCount.send(sender, TextFormatting.RED
                    + String.format(I18n.translateToLocal("commands.mnick.conflict"),
                            nickname));
            return;
        }

        stats.nickname = nickname;
        data.markDirty();
        ScoreboardSync.syncPlayer(data, playerUuid);

        CommandMCount.send(sender, TextFormatting.GREEN
                + String.format(I18n.translateToLocal("commands.mnick.set"),
                        playerName, nickname));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                           String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                    args, server.getOnlinePlayerNames());
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "clear");
        }
        return Collections.emptyList();
    }
}
