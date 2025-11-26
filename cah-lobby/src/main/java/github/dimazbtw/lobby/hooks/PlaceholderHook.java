package github.dimazbtw.lobby.hooks;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.database.PvPStats;
import github.dimazbtw.lobby.database.RankingEntry;
import github.dimazbtw.lobby.database.X1RankingEntry;
import github.dimazbtw.lobby.database.X1Stats;
import github.dimazbtw.lobby.managers.ParkourManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlaceholderHook extends PlaceholderExpansion {

    private final Main plugin;

    // Cache para rankings
    private List<RankingEntry> cachedArenaKills;
    private List<RankingEntry> cachedArenaKDR;
    private List<X1RankingEntry> cachedX1Wins;
    private List<X1RankingEntry> cachedX1KDR;
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 30000; // 30 segundos

    public PlaceholderHook(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "pvpstats";
    }

    @Override
    public @NotNull String getAuthor() {
        return "dimazbtw";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    private void updateCache() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > CACHE_DURATION) {
            cachedArenaKills = plugin.getStatsDatabase().getTopKills(10);
            cachedArenaKDR = plugin.getStatsDatabase().getTopKDR(10);
            cachedX1Wins = plugin.getStatsDatabase().getTopX1Wins(10);
            cachedX1KDR = plugin.getStatsDatabase().getTopX1KDR(10);
            lastCacheUpdate = now;
        }
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        updateCache();

        // ==================== STATS DO JOGADOR ====================
        if (player != null) {
            // PvP Stats (Arena)
            switch (identifier.toLowerCase()) {
                case "kills":
                    return String.valueOf(plugin.getStatsDatabase().loadStats(player).getKills());
                case "deaths":
                    return String.valueOf(plugin.getStatsDatabase().loadStats(player).getDeaths());
                case "kdr":
                    return plugin.getStatsDatabase().loadStats(player).getFormattedKDR();
                case "streak":
                case "highest_streak":
                    return String.valueOf(plugin.getStatsDatabase().loadStats(player).getHighestStreak());
                case "current_streak":
                    return String.valueOf(plugin.getPvPManager().getKillStreak(player));

                // X1 Stats
                case "x1_wins":
                    return String.valueOf(plugin.getStatsDatabase().loadX1Stats(player).getWins());
                case "x1_losses":
                    return String.valueOf(plugin.getStatsDatabase().loadX1Stats(player).getLosses());
                case "x1_winrate":
                    return plugin.getStatsDatabase().loadX1Stats(player).getFormattedWinRate();
                case "x1_kdr":
                    return plugin.getStatsDatabase().loadX1Stats(player).getFormattedKDR();
                case "x1_total":
                    return String.valueOf(plugin.getStatsDatabase().loadX1Stats(player).getTotalMatches());

                // Posições do jogador
                case "kills_position":
                    return String.valueOf(plugin.getStatsDatabase().getPlayerKillsPosition(player));
                case "kdr_position":
                    return String.valueOf(plugin.getStatsDatabase().getPlayerKDRPosition(player));

                // Status
                case "in_pvp":
                    return plugin.getPvPManager().isPvPEnabled(player) ? "Sim" : "Não";
                case "in_x1":
                    return plugin.getX1Manager().isInX1(player) ? "Sim" : "Não";
                case "in_combat":
                    return plugin.getPvPManager().isInCombat(player) ? "Sim" : "Não";
                case "in_queue":
                    return plugin.getX1Manager().isInQueue(player) ? "Sim" : "Não";
            }
        }

        // ==================== PARKOUR ====================

        if (identifier.startsWith("parkour_")) {

            if (identifier.equalsIgnoreCase("parkour_recorde_pessoal")) {

                if (player == null) return "N/A";

                Long record = plugin.getParkourManager().getPersonalRecord(player);
                if (record == null) return "N/A";

                return plugin.getParkourManager().formatTime(record);
            }

            if (identifier.startsWith("parkour_top_")) {
                try {
                    String posStr = identifier.substring("parkour_top_".length());
                    int position = Integer.parseInt(posStr);

                    ParkourManager.ParkourRecord record = plugin.getParkourManager().getTopRecord(position);
                    if (record == null) return "N/A";

                    return record.getPlayerName() + " - " + plugin.getParkourManager().formatTime(record.getTime());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }


        // ==================== TOP ARENA KILLS ====================
        if (identifier.startsWith("top_arena_") && identifier.endsWith("_name")) {
            int pos = extractPosition(identifier, "top_arena_", "_name");
            return getArenaKillsEntry(pos) != null ? getArenaKillsEntry(pos).getPlayerName() : "---";
        }
        if (identifier.startsWith("top_arena_") && identifier.endsWith("_kills")) {
            int pos = extractPosition(identifier, "top_arena_", "_kills");
            return getArenaKillsEntry(pos) != null ? String.valueOf(getArenaKillsEntry(pos).getKills()) : "0";
        }
        if (identifier.startsWith("top_arena_") && identifier.endsWith("_deaths")) {
            int pos = extractPosition(identifier, "top_arena_", "_deaths");
            return getArenaKillsEntry(pos) != null ? String.valueOf(getArenaKillsEntry(pos).getDeaths()) : "0";
        }
        if (identifier.startsWith("top_arena_") && identifier.endsWith("_kdr")) {
            int pos = extractPosition(identifier, "top_arena_", "_kdr");
            return getArenaKillsEntry(pos) != null ? getArenaKillsEntry(pos).getFormattedKDR() : "0.00";
        }

        // ==================== TOP ARENA KDR ====================
        if (identifier.startsWith("top_arena_kdr_") && identifier.endsWith("_name")) {
            int pos = extractPosition(identifier, "top_arena_kdr_", "_name");
            return getArenaKDREntry(pos) != null ? getArenaKDREntry(pos).getPlayerName() : "---";
        }
        if (identifier.startsWith("top_arena_kdr_") && identifier.endsWith("_kills")) {
            int pos = extractPosition(identifier, "top_arena_kdr_", "_kills");
            return getArenaKDREntry(pos) != null ? String.valueOf(getArenaKDREntry(pos).getKills()) : "0";
        }
        if (identifier.startsWith("top_arena_kdr_") && identifier.endsWith("_deaths")) {
            int pos = extractPosition(identifier, "top_arena_kdr_", "_deaths");
            return getArenaKDREntry(pos) != null ? String.valueOf(getArenaKDREntry(pos).getDeaths()) : "0";
        }
        if (identifier.startsWith("top_arena_kdr_") && identifier.endsWith("_kdr")) {
            int pos = extractPosition(identifier, "top_arena_kdr_", "_kdr");
            return getArenaKDREntry(pos) != null ? getArenaKDREntry(pos).getFormattedKDR() : "0.00";
        }

        // ==================== TOP X1 WINS ====================
        if (identifier.startsWith("top_x1_") && identifier.endsWith("_name")) {
            int pos = extractPosition(identifier, "top_x1_", "_name");
            return getX1WinsEntry(pos) != null ? getX1WinsEntry(pos).getPlayerName() : "---";
        }
        if (identifier.startsWith("top_x1_") && identifier.endsWith("_wins")) {
            int pos = extractPosition(identifier, "top_x1_", "_wins");
            return getX1WinsEntry(pos) != null ? String.valueOf(getX1WinsEntry(pos).getWins()) : "0";
        }
        if (identifier.startsWith("top_x1_") && identifier.endsWith("_losses")) {
            int pos = extractPosition(identifier, "top_x1_", "_losses");
            return getX1WinsEntry(pos) != null ? String.valueOf(getX1WinsEntry(pos).getLosses()) : "0";
        }
        if (identifier.startsWith("top_x1_") && identifier.endsWith("_winrate")) {
            int pos = extractPosition(identifier, "top_x1_", "_winrate");
            return getX1WinsEntry(pos) != null ? getX1WinsEntry(pos).getFormattedWinRate() : "0.0%";
        }
        if (identifier.startsWith("top_x1_") && identifier.endsWith("_kdr")) {
            int pos = extractPosition(identifier, "top_x1_", "_kdr");
            return getX1WinsEntry(pos) != null ? getX1WinsEntry(pos).getFormattedKDR() : "0.00";
        }

        // ==================== TOP X1 KDR ====================
        if (identifier.startsWith("top_x1_kdr_") && identifier.endsWith("_name")) {
            int pos = extractPosition(identifier, "top_x1_kdr_", "_name");
            return getX1KDREntry(pos) != null ? getX1KDREntry(pos).getPlayerName() : "---";
        }
        if (identifier.startsWith("top_x1_kdr_") && identifier.endsWith("_wins")) {
            int pos = extractPosition(identifier, "top_x1_kdr_", "_wins");
            return getX1KDREntry(pos) != null ? String.valueOf(getX1KDREntry(pos).getWins()) : "0";
        }
        if (identifier.startsWith("top_x1_kdr_") && identifier.endsWith("_losses")) {
            int pos = extractPosition(identifier, "top_x1_kdr_", "_losses");
            return getX1KDREntry(pos) != null ? String.valueOf(getX1KDREntry(pos).getLosses()) : "0";
        }
        if (identifier.startsWith("top_x1_kdr_") && identifier.endsWith("_winrate")) {
            int pos = extractPosition(identifier, "top_x1_kdr_", "_winrate");
            return getX1KDREntry(pos) != null ? getX1KDREntry(pos).getFormattedWinRate() : "0.0%";
        }
        if (identifier.startsWith("top_x1_kdr_") && identifier.endsWith("_kdr")) {
            int pos = extractPosition(identifier, "top_x1_kdr_", "_kdr");
            return getX1KDREntry(pos) != null ? getX1KDREntry(pos).getFormattedKDR() : "0.00";
        }

        return null;
    }

    /**
     * Extrai a posição do placeholder
     * Ex: top_arena_1_name -> 1
     */
    private int extractPosition(String identifier, String prefix, String suffix) {
        try {
            String numStr = identifier.replace(prefix, "").replace(suffix, "");
            return Integer.parseInt(numStr) - 1; // -1 porque array começa em 0
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private RankingEntry getArenaKillsEntry(int position) {
        if (cachedArenaKills == null || position < 0 || position >= cachedArenaKills.size()) {
            return null;
        }
        return cachedArenaKills.get(position);
    }

    private RankingEntry getArenaKDREntry(int position) {
        if (cachedArenaKDR == null || position < 0 || position >= cachedArenaKDR.size()) {
            return null;
        }
        return cachedArenaKDR.get(position);
    }

    private X1RankingEntry getX1WinsEntry(int position) {
        if (cachedX1Wins == null || position < 0 || position >= cachedX1Wins.size()) {
            return null;
        }
        return cachedX1Wins.get(position);
    }

    private X1RankingEntry getX1KDREntry(int position) {
        if (cachedX1KDR == null || position < 0 || position >= cachedX1KDR.size()) {
            return null;
        }
        return cachedX1KDR.get(position);
    }
}