package com.miningtop;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.DimensionManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /mcount [player]
 *
 * Shows a player's total mined blocks and their most mined block
 * types. Without arguments a player sees their own stats; console
 * must give a player name. Works for offline players too (as long
 * as they mined something before).
 */
public class CommandMCount extends CommandBase {

    private static final int TOP_BLOCKS = 5;

    @Override
    public String getName() {
        return "mcount";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return I18n.translateToLocal("commands.mcount.usage");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // everyone
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        MiningData data = MiningData.get(DimensionManager.getWorld(0));

        EntityPlayerMP online = null;
        String name;

        if (args.length >= 1) {
            name = args[0];
            try {
                online = CommandBase.getPlayer(server, sender, args[0]);
            } catch (PlayerNotFoundException ignored) {
                // offline player - fall back to stored data below
            }
        } else if (sender instanceof EntityPlayerMP) {
            online = (EntityPlayerMP) sender;
            name = online.getName();
        } else {
            throw new WrongUsageException("commands.mcount.usage");
        }

        UUID playerUuid;
        if (online != null) {
            playerUuid = online.getGameProfile().getId();
            name = online.getName();
        } else {
            playerUuid = data.findUUIDByName(name);
        }

        if (playerUuid == null || data.getStats(playerUuid).total <= 0) {
            throw new PlayerNotFoundException("commands.mcount.noData", name);
        }

        PlayerMiningStats stats = data.getStats(playerUuid);

        send(sender, TextFormatting.GOLD
                + String.format(I18n.translateToLocal("commands.mcount.header"), name));
        send(sender, TextFormatting.YELLOW
                + String.format(I18n.translateToLocal("commands.mcount.total"),
                        format(stats.total)));

        List<Map.Entry<String, Long>> topBlocks = stats.topBlocks(TOP_BLOCKS);
        if (!topBlocks.isEmpty()) {
            send(sender, TextFormatting.GRAY
                    + I18n.translateToLocal("commands.mcount.top.header"));
            for (Map.Entry<String, Long> blockEntry : topBlocks) {
                send(sender, TextFormatting.GRAY + "  "
                        + BlockUtil.displayName(blockEntry.getKey())
                        + TextFormatting.WHITE + ": "
                        + TextFormatting.GREEN
                        + format(blockEntry.getValue().longValue()));
            }
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                           String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                    args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }

    static void send(ICommandSender sender, String text) {
        sender.sendMessage(new TextComponentString(text));
    }

    static String format(long value) {
        return String.format("%,d", Long.valueOf(value));
    }
}
