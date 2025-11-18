package github.dimazbtw.lobby.managers;

import github.dimazbtw.lobby.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TellManager {

    private final Main plugin;
    private final Map<UUID, UUID> lastMessages = new HashMap<>();
    private boolean placeholderApiEnabled;

    public TellManager(Main plugin) {
        this.plugin = plugin;
        this.placeholderApiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Envia uma mensagem privada
     */
    public boolean sendPrivateMessage(Player sender, Player target, String message) {
        // Verificar se o remetente pode enviar mensagens
        if (!plugin.getPlayerDataManager().isPrivateMessagesEnabled(sender)) {
            plugin.getLanguageManager().sendMessage(sender, "tell.self-disabled");
            return false;
        }

        // Verificar se o destinatário pode receber mensagens
        if (!plugin.getPlayerDataManager().isPrivateMessagesEnabled(target)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            plugin.getLanguageManager().sendMessage(sender, "tell.target-disabled", placeholders);
            return false;
        }

        // Verificar se o jogador está tentando enviar mensagem para si mesmo
        if (sender.equals(target)) {
            plugin.getLanguageManager().sendMessage(sender, "tell.self-message");
            return false;
        }

        // Formatar mensagens
        String senderFormat = formatMessage(sender, target, message, true);
        String targetFormat = formatMessage(sender, target, message, false);

        // Enviar mensagens
        sender.sendMessage(senderFormat);
        target.sendMessage(targetFormat);

        // Tocar som para o destinatário
        if (plugin.getConfig().getBoolean("tell.sound.enabled", true)) {
            try {
                String soundName = plugin.getConfig().getString("tell.sound.sound", "ORB_PICKUP");
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
                target.playSound(target.getLocation(), sound, 1.0f, 1.0f);
            } catch (Exception e) {
                // Som inválido, ignorar
            }
        }

        // Salvar última conversa
        lastMessages.put(sender.getUniqueId(), target.getUniqueId());
        lastMessages.put(target.getUniqueId(), sender.getUniqueId());

        // Log (se habilitado)
        if (plugin.getConfig().getBoolean("tell.log-messages", true)) {
            plugin.getLogger().info("[TELL] " + sender.getName() + " -> " + target.getName() + ": " + message);
        }

        return true;
    }

    /**
     * Responde a última mensagem recebida
     */
    public boolean replyToLastMessage(Player sender, String message) {
        UUID lastSenderUUID = lastMessages.get(sender.getUniqueId());

        if (lastSenderUUID == null) {
            plugin.getLanguageManager().sendMessage(sender, "tell.no-previous");
            return false;
        }

        Player target = Bukkit.getPlayer(lastSenderUUID);

        if (target == null || !target.isOnline()) {
            plugin.getLanguageManager().sendMessage(sender, "tell.target-offline");
            lastMessages.remove(sender.getUniqueId());
            return false;
        }

        return sendPrivateMessage(sender, target, message);
    }

    /**
     * Formata a mensagem privada
     */
    private String formatMessage(Player sender, Player target, String message, boolean isSender) {
        String format;

        if (isSender) {
            format = plugin.getConfig().getString("tell.format.sender", "&7[&cVocê &7-> &f{target}&7] &f{message}");
        } else {
            format = plugin.getConfig().getString("tell.format.receiver", "&7[&f{sender} &7-> &cVocê&7] &f{message}");
        }

        // Substituir placeholders básicos
        format = format
                .replace("{sender}", sender.getName())
                .replace("{target}", target.getName())
                .replace("{message}", message);

        // PlaceholderAPI
        if (placeholderApiEnabled) {
            format = PlaceholderAPI.setPlaceholders(sender, format);
        }

        // Colorir
        return ChatColor.translateAlternateColorCodes('&', format);
    }

    /**
     * Obtém o último jogador que enviou mensagem
     */
    public Player getLastMessager(Player player) {
        UUID lastUUID = lastMessages.get(player.getUniqueId());
        if (lastUUID == null) {
            return null;
        }
        return Bukkit.getPlayer(lastUUID);
    }

    /**
     * Verifica se o jogador tem histórico de mensagens
     */
    public boolean hasLastMessager(Player player) {
        return lastMessages.containsKey(player.getUniqueId());
    }

    /**
     * Remove o histórico de um jogador
     */
    public void removePlayer(Player player) {
        lastMessages.remove(player.getUniqueId());

        // Remover das conversas de outros jogadores também
        lastMessages.entrySet().removeIf(entry -> entry.getValue().equals(player.getUniqueId()));
    }

    /**
     * Limpa todo o histórico
     */
    public void clearAll() {
        lastMessages.clear();
    }
}