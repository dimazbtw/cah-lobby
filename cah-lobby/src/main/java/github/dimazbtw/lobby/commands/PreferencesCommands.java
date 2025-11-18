package github.dimazbtw.lobby.commands;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.config.LanguageManager;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PreferencesCommands {

    private final Main plugin;
    private final LanguageManager languageManager;

    public PreferencesCommands(Main plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Command(
            name = "preferences",
            aliases = "prefs",
            description = "Alterna o chat",
            target = CommandTarget.PLAYER
    )
    public void PreferencesHelp(Context<Player> context){
        context.sendMessage("Ola");
    }

    @Command(
            name = "preferences.chat.toggle",
            aliases = "prefs.chat",
            description = "Alterna o chat",
            target = CommandTarget.PLAYER
    )
    public void toggleChatCommand(Context<Player> context) {
        Player player = context.getSender();
        boolean newState = plugin.getPlayerDataManager().toggleChat(player);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("state", newState ? languageManager.getMessage(player, "preferences.enabled")
                : languageManager.getMessage(player, "preferences.disabled"));

        languageManager.sendMessage(player, "preferences.chat-toggled", placeholders);

        // Atualizar menu se estiver aberto
        updatePreferencesMenu(player);
    }

    @Command(
            name = "preferences.tell.toggle",
            aliases = "prefs.tell",
            description = "Alterna mensagens privadas",
            target = CommandTarget.PLAYER
    )
    public void toggleTellCommand(Context<Player> context) {
        Player player = context.getSender();
        boolean newState = plugin.getPlayerDataManager().togglePrivateMessages(player);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("state", newState ? languageManager.getMessage(player, "preferences.enabled")
                : languageManager.getMessage(player, "preferences.disabled"));

        languageManager.sendMessage(player, "preferences.pm-toggled", placeholders);

        // Atualizar menu se estiver aberto
        updatePreferencesMenu(player);
    }

    @Command(
            name = "preferences.fly.toggle",
            aliases = "prefs.fly",
            description = "Alterna modo fly",
            target = CommandTarget.PLAYER,
            permission = "lobby.fly"
    )
    public void toggleFlyCommand(Context<Player> context) {
        Player player = context.getSender();
        boolean newState = plugin.getPlayerDataManager().toggleFlyPreference(player);

        // Aplicar o estado do fly
        if (player.hasPermission("lobby.fly") || player.hasPermission("admin")) {
            player.setAllowFlight(newState);
            player.setFlying(newState);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("state", newState ? languageManager.getMessage(player, "preferences.enabled")
                : languageManager.getMessage(player, "preferences.disabled"));

        languageManager.sendMessage(player, "preferences.fly-toggled", placeholders);

        // Atualizar menu se estiver aberto
        updatePreferencesMenu(player);
    }

    /**
     * Atualiza o menu de preferências se estiver aberto
     */
    private void updatePreferencesMenu(Player player) {
        // Verificar se o jogador está com o menu de preferências aberto
        if (plugin.getMenuManager().getMenu("preferences") != null &&
                plugin.getMenuManager().getMenu("preferences").isViewing(player)) {

            // Fechar e reabrir o menu para atualizar
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.closeInventory();
                plugin.getMenuManager().openMenu(player, "preferences");
            }, 1L);
        }
    }
}