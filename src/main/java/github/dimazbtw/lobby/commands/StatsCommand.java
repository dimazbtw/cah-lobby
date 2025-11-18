package github.dimazbtw.lobby.commands;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.config.LanguageManager;
import github.dimazbtw.lobby.database.PvPStats;
import github.dimazbtw.lobby.database.RankingEntry;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsCommand {

    private final Main plugin;
    private final LanguageManager languageManager;

    public StatsCommand(Main plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Command(
            name = "stats",
            aliases = {"pvpstats", "estatisticas"},
            description = "Ver estatísticas de PvP",
            target = CommandTarget.PLAYER
    )
    public void statsCommand(Context<Player> context, @Optional String targetName) {
        Player player = context.getSender();
        Player target = targetName != null ? Bukkit.getPlayer(targetName) : player;

        if (target == null) {
            languageManager.sendMessage(player, "general.player-not-found");
            return;
        }

        PvPStats stats = plugin.getStatsDatabase().loadStats(target);
        int killsPosition = plugin.getStatsDatabase().getPlayerKillsPosition(target);
        int kdrPosition = plugin.getStatsDatabase().getPlayerKDRPosition(target);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("kills", String.valueOf(stats.getKills()));
        placeholders.put("deaths", String.valueOf(stats.getDeaths()));
        placeholders.put("kdr", stats.getFormattedKDR());
        placeholders.put("streak", String.valueOf(stats.getHighestStreak()));
        placeholders.put("kills_position", String.valueOf(killsPosition));
        placeholders.put("kdr_position", String.valueOf(kdrPosition));

        languageManager.sendMessage(player, "stats.header");
        languageManager.sendMessage(player, "stats.title", placeholders);
        player.sendMessage("");
        languageManager.sendMessage(player, "stats.kills", placeholders);
        languageManager.sendMessage(player, "stats.deaths", placeholders);
        languageManager.sendMessage(player, "stats.kdr", placeholders);
        languageManager.sendMessage(player, "stats.highest-streak", placeholders);
        player.sendMessage("");
        languageManager.sendMessage(player, "stats.kills-rank", placeholders);
        languageManager.sendMessage(player, "stats.kdr-rank", placeholders);
        languageManager.sendMessage(player, "stats.footer");
    }

    @Command(
            name = "ranking",
            aliases = {"top", "leaderboard", "rank"},
            description = "Ver ranking de PvP",
            target = CommandTarget.PLAYER
    )
    public void rankingCommand(Context<Player> context, @Optional String type) {
        Player player = context.getSender();

        if (type == null) {
            type = "kills";
        }

        switch (type.toLowerCase()) {
            case "kills":
            case "k":
                showKillsRanking(player);
                break;
            case "kdr":
            case "ratio":
                showKDRRanking(player);
                break;
            case "streak":
            case "s":
                showStreakRanking(player);
                break;
            default:
                languageManager.sendMessage(player, "ranking.invalid-type");
                break;
        }
    }

    /**
     * Mostra o ranking de kills
     */
    private void showKillsRanking(Player player) {
        List<RankingEntry> ranking = plugin.getStatsDatabase().getTopKills(10);

        if (ranking.isEmpty()) {
            languageManager.sendMessage(player, "ranking.empty");
            return;
        }

        languageManager.sendMessage(player, "ranking.kills-header");
        player.sendMessage("");

        for (RankingEntry entry : ranking) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("position", entry.getFormattedPosition());
            placeholders.put("player", entry.getPlayerName());
            placeholders.put("kills", String.valueOf(entry.getKills()));
            placeholders.put("deaths", String.valueOf(entry.getDeaths()));
            placeholders.put("kdr", entry.getFormattedKDR());

            languageManager.sendMessage(player, "ranking.kills-line", placeholders);
        }

        player.sendMessage("");
        languageManager.sendMessage(player, "ranking.footer");
    }

    /**
     * Mostra o ranking de KDR
     */
    private void showKDRRanking(Player player) {
        List<RankingEntry> ranking = plugin.getStatsDatabase().getTopKDR(10);

        if (ranking.isEmpty()) {
            languageManager.sendMessage(player, "ranking.empty");
            return;
        }

        languageManager.sendMessage(player, "ranking.kdr-header");
        player.sendMessage("");

        for (RankingEntry entry : ranking) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("position", entry.getFormattedPosition());
            placeholders.put("player", entry.getPlayerName());
            placeholders.put("kills", String.valueOf(entry.getKills()));
            placeholders.put("deaths", String.valueOf(entry.getDeaths()));
            placeholders.put("kdr", entry.getFormattedKDR());

            languageManager.sendMessage(player, "ranking.kdr-line", placeholders);
        }

        player.sendMessage("");
        languageManager.sendMessage(player, "ranking.footer");
    }

    /**
     * Mostra o ranking de killstreak
     */
    private void showStreakRanking(Player player) {
        List<RankingEntry> ranking = plugin.getStatsDatabase().getTopStreak(10);

        if (ranking.isEmpty()) {
            languageManager.sendMessage(player, "ranking.empty");
            return;
        }

        languageManager.sendMessage(player, "ranking.streak-header");
        player.sendMessage("");

        for (RankingEntry entry : ranking) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("position", entry.getFormattedPosition());
            placeholders.put("player", entry.getPlayerName());
            placeholders.put("streak", String.valueOf(entry.getStreak()));
            placeholders.put("kills", String.valueOf(entry.getKills()));
            placeholders.put("deaths", String.valueOf(entry.getDeaths()));

            languageManager.sendMessage(player, "ranking.streak-line", placeholders);
        }

        player.sendMessage("");
        languageManager.sendMessage(player, "ranking.footer");
    }
}