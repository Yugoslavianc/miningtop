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
    public String getName() {
        return "mtop";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return I18n.translateToLocal("commands.mtop.usage");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // everyone
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        MiningData data = MiningData.get(DimensionManager.getWorld(0));

        List<Map.Entry<UUID, PlayerMiningStats>> entries =
                new ArrayList<Map.Entry<UUID, PlayerMiningStats>>(
                        data.players.entrySet());
        if (entries.isEmpty()) {
            CommandMCount.send(sender, TextFormatting.GRAY
                    + I18n.translateToLocal("commands.mtop.empty"));
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
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException invalidNumber) {
                throw new WrongUsageException("commands.mtop.usage");
            }
            if (page < 1 || page > pages) {
                throw new WrongUsageException("commands.mtop.usage");
            }
        } else {
            page = 1;
        }

        CommandMCount.send(sender, TextFormatting.GOLD
                + String.format(I18n.translateToLocal("commands.mtop.header"),
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
            CommandMCount.send(sender, TextFormatting.GRAY + "#" + (rank + 1) + " "
                    + TextFormatting.WHITE + display + " "
                    + TextFormatting.GREEN + CommandMCount.format(stats.total));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                           String[] args, BlockPos targetPos) {
        return Collections.emptyList();
    }
}
