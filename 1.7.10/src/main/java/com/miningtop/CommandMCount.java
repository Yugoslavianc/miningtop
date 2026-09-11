package com.miningtop;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.DimensionManager;

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
    public String getCommandName() {
        return "mcount";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return StatCollector.translateToLocal("commands.mcount.usage");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // everyone
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        MiningData data = MiningData.get(DimensionManager.getWorld(0));

        EntityPlayerMP online = null;
        String name;

        if (args.length >= 1) {
            name = args[0];
            try {
                online = CommandBase.getPlayer(sender, args[0]);
            } catch (PlayerNotFoundException ignored) {
                // offline player - fall back to stored data below
            }
        } else if (sender instanceof EntityPlayerMP) {
            online = (EntityPlayerMP) sender;
            name = online.getCommandSenderName();
        } else {
            throw new WrongUsageException("commands.mcount.usage");
        }

        UUID uuid;
        if (online != null) {
            uuid = online.getGameProfile().getId();
            name = online.getCommandSenderName();
        } else {
            uuid = data.findUUIDByName(name);
        }

        if (uuid == null || data.getStats(uuid).total <= 0) {
            throw new PlayerNotFoundException("commands.mcount.noData", name);
        }

        PlayerMiningStats stats = data.getStats(uuid);

        send(sender, EnumChatFormatting.GOLD
                + String.format(StatCollector.translateToLocal("commands.mcount.header"), name));
        send(sender, EnumChatFormatting.YELLOW
                + String.format(StatCollector.translateToLocal("commands.mcount.total"),
                        format(stats.total)));

        List<Map.Entry<String, Long>> topBlocks = stats.topBlocks(TOP_BLOCKS);
        if (!topBlocks.isEmpty()) {
            send(sender, EnumChatFormatting.GRAY
                    + StatCollector.translateToLocal("commands.mcount.top.header"));
            for (Map.Entry<String, Long> blockEntry : topBlocks) {
                send(sender, EnumChatFormatting.GRAY + "  "
                        + BlockUtil.displayName(blockEntry.getKey())
                        + EnumChatFormatting.WHITE + ": "
                        + EnumChatFormatting.GREEN
                        + format(blockEntry.getValue().longValue()));
            }
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(
                    args, MinecraftServer.getServer().getAllUsernames());
        }
        return null;
    }

    static void send(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }

    static String format(long value) {
        return String.format("%,d", Long.valueOf(value));
    }
}
