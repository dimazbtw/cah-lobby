package github.dimazbtw.lobby.managers;

import github.andredimaz.plugin.core.utils.basics.ScoreboardUtils;
import github.dimazbtw.lobby.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScoreboardManager {

    private final Main plugin;
    private BukkitTask updateTask;
    private boolean placeholderApiEnabled;
    private final Random random = new Random();

    public ScoreboardManager(Main plugin) {
        this.plugin = plugin;
        this.placeholderApiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        if (placeholderApiEnabled) {
            plugin.getLogger().info("PlaceholderAPI detectado! Placeholders habilitados nas scoreboards.");
        }
    }

    /**
     * Inicia a task de atualização das scoreboards
     */
    public void startUpdateTask() {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            return;
        }

        int updateInterval = plugin.getConfig().getInt("scoreboard.update-interval", 20);

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 20L, updateInterval);

        plugin.getLogger().info("Sistema de scoreboard iniciado! (Atualização a cada " + updateInterval + " ticks)");
    }

    /**
     * Para a task de atualização
     */
    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Remover todas as scoreboards
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeScoreboard(player);
        }
    }

    /**
     * Cria/atualiza a scoreboard de um jogador
     */
    public void updateScoreboard(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            return;
        }

        // Obter idioma do jogador
        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);

        // Obter título do idioma
        String title = plugin.getLanguageManager().getMessage(playerLang, "scoreboard.title");

        // Obter linhas do idioma
        List<String> lines = plugin.getLanguageManager().getMessageList(playerLang, "scoreboard.lines");

        // Processar placeholders
        title = replacePlaceholders(player, title);
        lines = replacePlaceholders(player, lines);

        // Adicionar cores aleatórias às linhas vazias para evitar duplicação
        lines = addRandomColorsToEmptyLines(lines);

        // Inverter linhas (scoreboard começa de baixo)
        List<String> reversedLines = new ArrayList<>();
        for (int i = lines.size() - 1; i >= 0; i--) {
            reversedLines.add(lines.get(i));
        }

        // Definir scoreboard
        ScoreboardUtils.setScoreboard(player, title, reversedLines);
    }

    /**
     * Remove a scoreboard de um jogador
     */
    public void removeScoreboard(Player player) {
        ScoreboardUtils.removeScoreboard(player);
    }

    /**
     * Adiciona cores aleatórias e invisíveis às linhas vazias para torná-las únicas
     */
    private List<String> addRandomColorsToEmptyLines(List<String> lines) {
        List<String> processed = new ArrayList<>();

        for (String line : lines) {
            // Se a linha estiver vazia ou for apenas espaços
            if (line == null || line.trim().isEmpty()) {
                // Adicionar cores aleatórias invisíveis
                processed.add(getRandomInvisibleColors());
            } else {
                processed.add(line);
            }
        }

        return processed;
    }

    /**
     * Gera uma string com cores aleatórias invisíveis para tornar linhas vazias únicas
     */
    private String getRandomInvisibleColors() {
        StringBuilder colors = new StringBuilder();

        // Adicionar 2-4 códigos de cor aleatórios
        int colorCount = random.nextInt(3) + 2;

        ChatColor[] allColors = ChatColor.values();
        for (int i = 0; i < colorCount; i++) {
            ChatColor color = allColors[random.nextInt(allColors.length)];
            colors.append(color);
        }

        return colors.toString();
    }

    /**
     * Substitui placeholders em uma linha
     */
    private String replacePlaceholders(Player player, String text) {
        if (text == null) {
            return "";
        }

        // Placeholders internos
        text = text
                .replace("{player}", player.getName())
                .replace("{displayname}", player.getDisplayName())
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getServer().getMaxPlayers()))
                .replace("{world}", player.getWorld().getName())
                .replace("{ping}", String.valueOf(getPing(player)))
                .replace("{health}", String.format("%.1f", player.getHealth()))
                .replace("{food}", String.valueOf(player.getFoodLevel()))
                .replace("{level}", String.valueOf(player.getLevel()))
                .replace("{exp}", String.valueOf(player.getExp()));

        // PlaceholderAPI
        if (placeholderApiEnabled) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }

    /**
     * Substitui placeholders em uma lista de linhas
     */
    private List<String> replacePlaceholders(Player player, List<String> lines) {
        List<String> processed = new ArrayList<>();
        for (String line : lines) {
            processed.add(replacePlaceholders(player, line));
        }
        return processed;
    }

    /**
     * Obtém o ping do jogador
     */
    private int getPing(Player player) {
        try {
            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return (int) craftPlayer.getClass().getField("ping").get(craftPlayer);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Recarrega as configurações
     */
    public void reload() {
        stopUpdateTask();

        if (plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            startUpdateTask();
        }
    }
}