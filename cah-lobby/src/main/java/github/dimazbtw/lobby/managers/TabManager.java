package github.dimazbtw.lobby.managers;

import github.dimazbtw.lobby.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
import java.util.List;

public class TabManager {

    private final Main plugin;
    private BukkitTask updateTask;
    private boolean placeholderApiEnabled;
    private boolean luckPermsEnabled;
    private LuckPerms luckPerms;

    public TabManager(Main plugin) {
        this.plugin = plugin;
        this.placeholderApiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        // Verificar se LuckPerms está disponível
        try {
            this.luckPerms = LuckPermsProvider.get();
            this.luckPermsEnabled = true;
            plugin.getLogger().info("LuckPerms detectado! Ordenação de Tab habilitada.");
        } catch (IllegalStateException | NoClassDefFoundError e) {
            this.luckPermsEnabled = false;
            plugin.getLogger().info("LuckPerms não encontrado. Ordenação de Tab desabilitada.");
        }

        if (placeholderApiEnabled) {
            plugin.getLogger().info("PlaceholderAPI detectado para Tab!");
        }
    }

    /**
     * Inicia a task de atualização da tab
     */
    public void startUpdateTask() {
        if (!plugin.getConfig().getBoolean("tab.enabled", true)) {
            return;
        }

        int updateInterval = plugin.getConfig().getInt("tab.update-interval", 20);

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateTab(player);
                updateTabList(player);
            }
        }, 20L, updateInterval);

        plugin.getLogger().info("Sistema de tab iniciado! (Atualização a cada " + updateInterval + " ticks)");
    }

    /**
     * Para a task de atualização
     */
    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Limpar tab de todos os jogadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearTab(player);
            clearTabList(player);
        }
    }

    /**
     * Atualiza a tab de um jogador
     */
    public void updateTab(Player player) {
        if (!plugin.getConfig().getBoolean("tab.enabled", true)) {
            return;
        }

        // Obter idioma do jogador
        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);

        // Obter header do idioma
        List<String> headerLines = plugin.getLanguageManager().getMessageList(playerLang, "tab.header");
        String header = joinLines(headerLines);

        // Obter footer do idioma
        List<String> footerLines = plugin.getLanguageManager().getMessageList(playerLang, "tab.footer");
        String footer = joinLines(footerLines);

        // Processar placeholders
        header = replacePlaceholders(player, header);
        footer = replacePlaceholders(player, footer);

        // Enviar tab
        sendTabList(player, header, footer);
    }

    /**
     * Atualiza a lista de jogadores na tab com ordenação por grupo
     */
    public void updateTabList(Player player) {
        if (!plugin.getConfig().getBoolean("tab.sort-by-group", true)) {
            return;
        }

        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            String teamName = getTeamName(target);
            Team team = scoreboard.getTeam(teamName);

            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }

            // Definir prefix do grupo
            String prefix = getPlayerPrefix(target);
            if (prefix.length() > 16) {
                prefix = prefix.substring(0, 16);
            }
            team.setPrefix(ChatColor.translateAlternateColorCodes('&', prefix));

            // Adicionar jogador ao time
            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }
        }
    }

    /**
     * Obtém o nome do time baseado na prioridade do grupo
     */
    private String getTeamName(Player player) {
        if (!luckPermsEnabled) {
            return "999_default";
        }

        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return "999_default";
            }

            String primaryGroup = user.getPrimaryGroup();
            int weight = getGroupWeight(primaryGroup);

            // Inverter peso (quanto menor o peso, maior a prioridade na tab)
            // Adicionar zeros à esquerda para ordenação correta
            String paddedWeight = String.format("%03d", 999 - weight);

            return paddedWeight + "_" + primaryGroup;
        } catch (Exception e) {
            return "999_default";
        }
    }

    /**
     * Obtém o peso/prioridade do grupo
     */
    private int getGroupWeight(String groupName) {
        if (!luckPermsEnabled) {
            return 0;
        }

        try {
            return luckPerms.getGroupManager().getGroup(groupName)
                    .getNodes().stream()
                    .filter(node -> node.getKey().startsWith("weight."))
                    .findFirst()
                    .map(node -> {
                        try {
                            return Integer.parseInt(node.getKey().substring(7));
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .orElse(0);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Obtém o prefix do jogador via PlaceholderAPI ou LuckPerms
     */
    private String getPlayerPrefix(Player player) {
        // Tentar PlaceholderAPI primeiro
        if (placeholderApiEnabled) {
            String prefix = PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
            if (prefix != null && !prefix.isEmpty() && !prefix.equals("%luckperms_prefix%")) {
                return prefix;
            }
        }

        // Fallback para LuckPerms direto
        if (luckPermsEnabled) {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (prefix != null && !prefix.isEmpty()) {
                        return prefix + " ";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "";
    }

    /**
     * Limpa a lista de jogadores da tab
     */
    public void clearTabList(Player player) {
        Scoreboard scoreboard = player.getScoreboard();

        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }
    }

    /**
     * Limpa a tab de um jogador
     */
    public void clearTab(Player player) {
        sendTabList(player, "", "");
        clearTabList(player);
    }

    /**
     * Envia o header e footer da tab para o jogador
     */
    private void sendTabList(Player player, String header, String footer) {
        try {
            IChatBaseComponent headerComponent = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + header + "\"}");
            IChatBaseComponent footerComponent = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + footer + "\"}");

            PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();

            Field headerField = packet.getClass().getDeclaredField("a");
            headerField.setAccessible(true);
            headerField.set(packet, headerComponent);

            Field footerField = packet.getClass().getDeclaredField("b");
            footerField.setAccessible(true);
            footerField.set(packet, footerComponent);

            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Junta múltiplas linhas em uma string com quebras de linha
     */
    private String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            builder.append(lines.get(i));
            if (i < lines.size() - 1) {
                builder.append("\\n");
            }
        }
        return builder.toString();
    }

    /**
     * Substitui placeholders em uma string
     */
    private String replacePlaceholders(Player player, String text) {
        if (text == null) {
            return "";
        }

        // Colorir antes de substituir placeholders
        text = ChatColor.translateAlternateColorCodes('&', text);

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
                .replace("{exp}", String.format("%.0f%%", player.getExp() * 100));

        // PlaceholderAPI
        if (placeholderApiEnabled) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
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

        if (plugin.getConfig().getBoolean("tab.enabled", true)) {
            startUpdateTask();
        }
    }
}