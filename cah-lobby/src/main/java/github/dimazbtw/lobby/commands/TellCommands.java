package github.dimazbtw.lobby.commands;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.config.LanguageManager;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class TellCommands {

    private final Main plugin;
    private final LanguageManager languageManager;

    public TellCommands(Main plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Command(
            name = "tell",
            aliases = {"msg", "whisper", "w", "t"},
            description = "Envia uma mensagem privada",
            target = CommandTarget.PLAYER,
            usage = "/tell <jogador> <mensagem>"
    )
    public void tellCommand(Context<Player> context, String targetName, String[] args) {
        Player sender = context.getSender();

        // Validar argumentos
        if (args.length == 0) {
            languageManager.sendMessage(sender, "tell.usage");
            return;
        }

        // Encontrar jogador
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            languageManager.sendMessage(sender, "tell.target-not-found", placeholders);
            return;
        }

        // Juntar mensagem
        String message = String.join(" ", args);

        // Enviar mensagem
        plugin.getTellManager().sendPrivateMessage(sender, target, message);
    }

    @Command(
            name = "reply",
            aliases = {"r", "responder"},
            description = "Responde a última mensagem privada",
            target = CommandTarget.PLAYER,
            usage = "/reply <mensagem>"
    )
    public void replyCommand(Context<Player> context, String[] args) {
        Player sender = context.getSender();

        // Validar argumentos
        if (args.length == 0) {
            languageManager.sendMessage(sender, "tell.reply-usage");
            return;
        }

        // Verificar se há mensagem anterior
        if (!plugin.getTellManager().hasLastMessager(sender)) {
            languageManager.sendMessage(sender, "tell.no-previous");
            return;
        }

        // Juntar mensagem
        String message = String.join(" ", args);

        // Enviar resposta
        plugin.getTellManager().replyToLastMessage(sender, message);
    }

    @Command(
            name = "msgtoggle",
            aliases = {"togglemsg", "togglepm"},
            description = "Alterna recebimento de mensagens privadas",
            target = CommandTarget.PLAYER
    )
    public void msgToggleCommand(Context<Player> context) {
        Player player = context.getSender();
        boolean newState = plugin.getPlayerDataManager().togglePrivateMessages(player);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("state", newState ? languageManager.getMessage(player, "preferences.enabled")
                : languageManager.getMessage(player, "preferences.disabled"));

        languageManager.sendMessage(player, "preferences.pm-toggled", placeholders);
    }
}