package github.dimazbtw.lobby.listeners;

import github.andredimaz.plugin.core.utils.basics.ActionBar;
import github.andredimaz.plugin.core.utils.basics.ColorUtils;
import github.andredimaz.plugin.core.utils.basics.TitleUtils;
import github.dimazbtw.lobby.Main;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WelcomeListener implements Listener {

    private final Main plugin;

    public WelcomeListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Verificar se o sistema de boas-vindas está ativado
        if (!plugin.getConfig().getBoolean("welcome.enabled", true)) {
            return;
        }

        // Delay para garantir que o jogador foi carregado
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            sendWelcomeMessages(player);
        }, 10L); // 10 ticks = 0.5 segundos
    }

    private void sendWelcomeMessages(Player player) {
        // Obter idioma do jogador
        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);

        // Enviar mensagens no chat
        if (plugin.getConfig().getBoolean("welcome.message.enabled", true)) {
            List<String> messages = plugin.getLanguageManager().getMessageList(playerLang, "welcome.message.lines");

            if (messages != null && !messages.isEmpty()) {
                for (String line : messages) {
                    line = replacePlaceholders(line, player);
                    player.sendMessage(ColorUtils.colorize(line));
                }
            }
        }

        // Enviar título
        if (plugin.getConfig().getBoolean("welcome.title.enabled", true)) {
            String title = plugin.getLanguageManager().getMessage(playerLang, "welcome.title.title");
            String subtitle = plugin.getLanguageManager().getMessage(playerLang, "welcome.title.subtitle");

            title = replacePlaceholders(title, player);
            subtitle = replacePlaceholders(subtitle, player);

            TitleUtils.sendTitle(player, title, subtitle, 20, 40, 20);
        }

        // Enviar action bar
        if (plugin.getConfig().getBoolean("welcome.actionbar.enabled", true)) {
            String actionbarMessage = plugin.getLanguageManager().getMessage(playerLang, "welcome.actionbar.message");
            actionbarMessage = replacePlaceholders(actionbarMessage, player);

            ActionBar.send(player, actionbarMessage);

            // Manter action bar por alguns segundos
            int duration = 5;
            for (int i = 1; i <= duration; i++) {
                final String finalMessage = actionbarMessage;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    ActionBar.send(player, finalMessage);
                }, 20L * i);
            }
        }

        // Tocar som
        if (plugin.getConfig().getBoolean("welcome.sound.enabled", true)) {
            try {
                String soundName = plugin.getConfig().getString("welcome.sound.sound", "LEVEL_UP");

                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Som inválido configurado em welcome.sound.sound: " +
                        plugin.getConfig().getString("welcome.sound.sound"));
            }
        }
    }

    private String replacePlaceholders(String text, Player player) {
        return text
                .replace("{player}", player.getName())
                .replace("{displayname}", player.getDisplayName())
                .replace("{online}", String.valueOf(plugin.getServer().getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(plugin.getServer().getMaxPlayers()));
    }
}