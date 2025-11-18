package github.dimazbtw.lobby.hooks;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.database.PvPStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderHook extends PlaceholderExpansion {

    private final Main plugin;

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
        return true; // Mantém o registro mesmo após reload
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        PvPStats stats = plugin.getStatsDatabase().loadStats(player);

        switch (identifier.toLowerCase()) {
            case "kills":
                return String.valueOf(stats.getKills());
            case "deaths":
                return String.valueOf(stats.getDeaths());
            case "kdr":
                return stats.getFormattedKDR();
            default:
                return null; // Placeholder não reconhecido
        }
    }
}
