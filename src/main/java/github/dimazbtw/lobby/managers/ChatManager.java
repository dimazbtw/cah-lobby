package github.dimazbtw.lobby.managers;

import github.dimazbtw.lobby.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ChatManager {

    private final Main plugin;
    private boolean placeholderApiEnabled;
    private LuckPerms luckPerms;
    private boolean luckPermsEnabled;

    public ChatManager(Main plugin) {
        this.plugin = plugin;
        this.placeholderApiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        if (placeholderApiEnabled) {
            plugin.getLogger().info("PlaceholderAPI detectado para Chat!");
        }

        try {
            this.luckPerms = LuckPermsProvider.get();
            this.luckPermsEnabled = true;
            plugin.getLogger().info("LuckPerms detectado! Ordenação de Tab habilitada.");
        } catch (IllegalStateException | NoClassDefFoundError e) {
            this.luckPermsEnabled = false;
            plugin.getLogger().info("LuckPerms não encontrado. Ordenação de Tab desabilitada.");
        }
    }

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
     * Formata a mensagem do chat
     */
    public String formatMessage(Player player, String message) {
        String format = plugin.getConfig().getString("chat.format", "{prefix}{player}§f: {message}");
        String prefix = getPlayerPrefix(player);

        // Substituir placeholders básicos
        format = format
                .replace("{player}", player.getName())
                .replace("{displayname}", player.getDisplayName())
                .replace("{message}", message)
                .replace("{prefix}", prefix)
                .replace("{world}", player.getWorld().getName());

        // PlaceholderAPI (para {prefix}, {suffix}, etc)
        if (placeholderApiEnabled) {
            format = PlaceholderAPI.setPlaceholders(player, format);
        } else {
            // Se não tem PlaceholderAPI, remover {prefix} e {suffix}
            format = format.replace("{prefix}", "").replace("{suffix}", "");
        }

        // Colorir mensagem se tiver permissão
        if (player.hasPermission("lobby.chat.color")) {
            format = ChatColor.translateAlternateColorCodes('&', format);
        } else {
            // Apenas colorir o formato, não a mensagem do jogador
            String[] parts = format.split("\\{message\\}", 2);
            if (parts.length > 0) {
                parts[0] = ChatColor.translateAlternateColorCodes('&', parts[0]);
            }
            format = String.join(message, parts);
        }

        return format;
    }

    /**
     * Verifica se a mensagem contém palavras bloqueadas
     */
    public boolean containsBlockedWord(String message) {
        if (!plugin.getConfig().getBoolean("chat.anti-swear.enabled", false)) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        for (String word : plugin.getConfig().getStringList("chat.anti-swear.words")) {
            if (lowerMessage.contains(word.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Censura palavras bloqueadas
     */
    public String censorMessage(String message) {
        if (!plugin.getConfig().getBoolean("chat.anti-swear.enabled", false)) {
            return message;
        }

        String replacement = plugin.getConfig().getString("chat.anti-swear.replacement", "***");

        for (String word : plugin.getConfig().getStringList("chat.anti-swear.words")) {
            message = message.replaceAll("(?i)" + word, replacement);
        }

        return message;
    }

    /**
     * Verifica se o jogador pode usar o chat
     */
    public boolean canChat(Player player) {
        // Verificar se o chat está desativado
        if (plugin.getConfig().getBoolean("chat.disabled", false)) {
            if (!player.hasPermission("lobby.chat.bypass")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifica se PlaceholderAPI está ativo
     */
    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }
}