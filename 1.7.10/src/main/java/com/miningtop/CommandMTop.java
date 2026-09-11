package com.miningtop;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /mtop [page]
 *
 * Server-wide mining leaderboard, 10 players per page,
 * sorted by total blocks mined.
 */
public class CommandMTop extends CommandBase {

    private static final int PER_PAGE = 10;

    @Override
    public String getCommandName() {
        return "mtop";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return StatCollector.translateToLocal("commands.mtop.usage");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // everyone
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        MiningData data = MiningData.get(DimensionManager.getWorld(0));

        List<Map.Entry<UUID, PlayerMiningStats>> entries =
                new ArrayList<Map.Entry<UUID, PlayerMiningStats>>(
                        data.players.entrySet());
        if (entries.isEmpty()) {
            CommandMCount.send(sender, EnumChatFormatting.GRAY
                    + StatCollector.translateToLocal("commands.mtop.empty"));
            return;
        }

        Collections.sort(entries, new Comparator<Map.Entry<UUID, PlayerMiningStats>>() {
            @Override
            public int compare(Map.Entry<UUID, PlayerMiningStats> first,
                               Map.Entry<UUID, PlayerMiningStats> second) {
                long difference = second.getValue().total - first.getValue().total;
                return difference > 0 ? 1 : (difference < 0 ? -1 : 0);
            }
        });

        int pages = (entries.size() + PER_PAGE - 1) / PER_PAGE;
        int page;
        if (args.length >= 1) {
            try {
                page = CommandBase.parseIntBounded(sender, args[0], 1, pages);
            } catch (NumberInvalidException e) {
                throw new WrongUsageException("commands.mtop.usage");
            }
        } else {
            page = 1;
        }

        CommandMCount.send(sender, EnumChatFormatting.GOLD
                + String.format(StatCollector.translateToLocal("commands.mtop.header"),
                        Integer.valueOf(page), Integer.valueOf(pages)));

        int firstIndex = (page - 1) * PER_PAGE;
        int lastIndex = Math.min(firstIndex + PER_PAGE, entries.size());
        for (int rank = firstIndex; rank < lastIndex; rank++) {
            PlayerMiningStats stats = entries.get(rank).getValue();
            String display;
            if (stats.nickname != null && stats.nickname.length() > 0) {
                display = stats.nickname;
            } else if (stats.name != null && stats.name.length() > 0) {
                display = stats.name;
            } else {
                display = entries.get(rank).getKey().toString();
            }
            CommandMCount.send(sender, EnumChatFormatting.GRAY + "#" + (rank + 1) + " "
                    + EnumChatFormatting.WHITE + display + " "
                    + EnumChatFormatting.GREEN + CommandMCount.format(stats.total));
        }
    }
}
