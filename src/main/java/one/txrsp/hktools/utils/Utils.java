package one.txrsp.hktools.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static one.txrsp.hktools.HKTools.LOGGER;

public class Utils {
    public static boolean isInGarden() {
        List<String> lines = getScoreboardLines();
        for (String line : lines) {
            if (line.contains("The Garden") || line.contains("Plot -")) {
                return true;
            }
        }
        return false;
    }

    public static List<String> getScoreboardLines() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return Collections.emptyList();

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (sidebar == null) return Collections.emptyList();

        List<String> lines = new ArrayList<>();
        sidebar.getScoreboard().getTeams().forEach(team -> {
            if (team.getPrefix() != null) {
                if (team.getSuffix() != null) {
                    String line = (team.getPrefix().getString() + team.getSuffix().getString()).strip();
                    if (!line.isEmpty() && !line.startsWith("[")) lines.add(stripFormatting(line));
                }
            }
        });

        return lines;
    }

    private static String stripFormatting(String input) {
        return input.replaceAll("\uD83C\uDF6B|\uD83D\uDCA3|\uD83D\uDC7D|\uD83D\uDD2E|\uD83D\uDC0D|\uD83D\uDC7E|\uD83C\uDF20|\uD83C\uDF6D|\u26BD|\uD83C\uDFC0|\uD83D\uDC79|\uD83C\uDF81|\uD83C\uDF89|\uD83C\uDF82|\uD83D\uDD2B", "");
    }
}

