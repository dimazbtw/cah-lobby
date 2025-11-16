package github.dimazbtw.lobby.commands.registry;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.commands.HologramCommands;
import github.dimazbtw.lobby.commands.LobbyCommand;
import github.dimazbtw.lobby.commands.NPCCommands;
import github.dimazbtw.lobby.commands.UtilityCommands;
import me.saiintbrisson.bukkit.command.BukkitFrame;
import me.saiintbrisson.minecraft.command.message.MessageHolder;
import me.saiintbrisson.minecraft.command.message.MessageType;

public class CommandRegistry {
    public CommandRegistry(Main plugin){
        BukkitFrame frame = new BukkitFrame(plugin);
        MessageHolder messageHolder = frame.getMessageHolder();

        frame.registerCommands(
                new UtilityCommands(plugin.getLanguageManager()),
                new LobbyCommand(plugin),
                new HologramCommands(plugin),
                new NPCCommands(plugin)
        );

        messageHolder.setMessage(MessageType.ERROR, "§cOcorreu um erro durante a execução deste comando.");
        messageHolder.setMessage(MessageType.INCORRECT_TARGET, "§cEste comando é destinado apenas a jogadores.");
        messageHolder.setMessage(MessageType.INCORRECT_USAGE, "§cUso correto: {usage}.");
        messageHolder.setMessage(MessageType.NO_PERMISSION, "§cVocê não tem permissão para executar esse comando.");
    }
}

