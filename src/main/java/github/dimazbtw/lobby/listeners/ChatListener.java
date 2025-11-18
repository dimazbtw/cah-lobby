package github.dimazbtw.lobby.listeners;

import github.dimazbtw.lobby.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;

public class ChatListener implements Listener {

    private final Main plugin;

    public ChatListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Verificar se o jogador pode usar o chat (plugin ou preferências)
        if (!plugin.getChatManager().canChat(player)) {
            String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);
            player.sendMessage(plugin.getLanguageManager().getMessage(playerLang, "chat.disabled-message"));
            event.setCancelled(true);
            return;
        }

        // Verificar se o jogador tem o chat desativado nas preferências
        if (!plugin.getPlayerDataManager().isChatEnabled(player)) {
            String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);
            player.sendMessage(plugin.getLanguageManager().getMessage(playerLang, "preferences.chat-self-disabled"));
            event.setCancelled(true);
            return;
        }

        // Verificar e censurar palavrões
        if (plugin.getChatManager().containsBlockedWord(message)) {
            message = plugin.getChatManager().censorMessage(message);
            event.setMessage(message);
        }

        // Formatar mensagem
        String formattedMessage = plugin.getChatManager().formatMessage(player, message);

        // Filtrar destinatários baseado nas preferências
        Set<Player> recipients = new HashSet<>(event.getRecipients());
        for (Player recipient : recipients) {
            if (!plugin.getPlayerDataManager().isChatEnabled(recipient)) {
                event.getRecipients().remove(recipient);
            }
        }

        // Definir formato do chat
        event.setFormat(formattedMessage.replace("%", "%%")); // Escapar % para evitar erros
    }
}