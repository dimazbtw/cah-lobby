package github.dimazbtw.lobby.commands;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.config.LanguageManager;
import github.dimazbtw.lobby.managers.NPCManager;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class NPCCommands {

    private final Main plugin;
    private final LanguageManager languageManager;
    private final NPCManager npcManager;

    public NPCCommands(Main plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.npcManager = plugin.getNpcManager();
    }

    @Command(name = "npc", target = CommandTarget.PLAYER, permission = "lobby.npc")
    public void npcCommand(Context<Player> context) {
        Player player = context.getSender();
        player.sendMessage("§6§m------------------------------");
        player.sendMessage("§6Comandos de NPC:");
        player.sendMessage("§e/npc criar <id> <tipo> §7- Cria um NPC (tipos: npc, head)");
        player.sendMessage("§e/npc delete <id> §7- Remove um NPC");
        player.sendMessage("§e/npc addcmd <id> <comando> §7- Adiciona comando ao NPC");
        player.sendMessage("§e/npc resetcmd <id> §7- Reseta comandos do NPC");
        player.sendMessage("§e/npc tphere <id> §7- Move NPC até você");
        player.sendMessage("§e/npc list §7- Lista todos os NPCs");
        player.sendMessage("§6§m------------------------------");
    }

    @Command(name = "npc.criar", target = CommandTarget.PLAYER, permission = "lobby.npc.create")
    public void createCommand(Context<Player> context, String id, String type) {
        Player player = context.getSender();

        if (!type.equalsIgnoreCase("npc") && !type.equalsIgnoreCase("head")) {
            player.sendMessage("§cTipo inválido! Use: npc ou head");
            return;
        }

        if (plugin.getNpcManager().npcExists(id)) {
            player.sendMessage("§cO NPC §f" + id + "§c já existe!");
            return;
        }

        Location location = player.getLocation();
        plugin.getNpcManager().createNPC(id, location, type);
        player.sendMessage("§aNPC §f" + id + "§a criado! (tipo: " + type + ")");
    }

    @Command(name = "npc.delete", aliases = "npc.remove", target = CommandTarget.PLAYER, permission = "lobby.npc.delete")
    public void deleteCommand(Context<Player> context, String id) {
        Player player = context.getSender();

        if (!plugin.getNpcManager().npcExists(id)) {
            player.sendMessage("§cO NPC §f" + id + "§c não existe!");
            return;
        }

        plugin.getNpcManager().deleteNPC(id);
        player.sendMessage("§aNPC §f" + id + "§a removido!");
    }

    @Command(name = "npc.addcmd", target = CommandTarget.PLAYER, permission = "lobby.npc.edit")
    public void addCmdCommand(Context<Player> context, String id, String[] args) {
        Player player = context.getSender();

        if (!plugin.getNpcManager().npcExists(id)) {
            player.sendMessage("§cO NPC §f" + id + "§c não existe!");
            return;
        }

        if (args.length == 0) {
            player.sendMessage("§cVocê precisa especificar um comando!");
            player.sendMessage("§7Exemplo: /npc addcmd " + id + " [CONSOLE] give {player} diamond 1");
            player.sendMessage("§7Use {player} para nome do jogador");
            return;
        }

        String command = String.join(" ", args);
        plugin.getNpcManager().addCommand(id, command);
        player.sendMessage("§aComando adicionado ao NPC §f" + id + "§a!");
    }

    @Command(name = "npc.resetcmd", target = CommandTarget.PLAYER, permission = "lobby.npc.edit")
    public void resetCmdCommand(Context<Player> context, String id) {
        Player player = context.getSender();

        if (!plugin.getNpcManager().npcExists(id)) {
            player.sendMessage("§cO NPC §f" + id + "§c não existe!");
            return;
        }

        plugin.getNpcManager().resetCommands(id);
        player.sendMessage("§aComandos do NPC §f" + id + "§a resetados!");
    }

    @Command(name = "npc.tphere", target = CommandTarget.PLAYER, permission = "lobby.npc.edit")
    public void tphereCommand(Context<Player> context, String id) {
        Player player = context.getSender();

        if (!plugin.getNpcManager().npcExists(id)) {
            player.sendMessage("§cO NPC §f" + id + "§c não existe!");
            return;
        }

        Location location = player.getLocation();
        plugin.getNpcManager().moveNPC(id, location);
        player.sendMessage("§aNPC §f" + id + "§a movido para sua localização!");
    }

    @Command(name = "npc.list", target = CommandTarget.PLAYER, permission = "lobby.npc")
    public void listCommand(Context<Player> context) {
        Player player = context.getSender();
        Set<String> npcs = plugin.getNpcManager().getNPCIds();

        if (npcs.isEmpty()) {
            player.sendMessage("§cNão há NPCs criados!");
            return;
        }

        player.sendMessage("§6NPCs criados:");
        for (String id : npcs) {
            List<String> commands = plugin.getNpcManager().getNPCCommands(id);
            player.sendMessage("§e• §f" + id + " §7(" + commands.size() + " comandos)");
        }
        player.sendMessage("§7Total: §f" + npcs.size() + "§7 NPC(s)");
    }

    @Command(
            name = "npc.float",
            description = "Ativa/desativa animação de flutuação",
            permission = "lobby.npc.admin"
    )
    public void npcFloatCommand(Context<Player> context, String npcId, boolean enable) {
        Player player = context.getSender();

        if (!plugin.getNpcManager().npcExists(npcId)) {
            player.sendMessage("§cNPC não encontrado!");
            return;
        }

        if (!"head".equals(npcManager.getNPCType(npcId))) {
            player.sendMessage("§cApenas NPCs do tipo 'head' suportam esta animação!");
            return;
        }

        npcManager.setFloatAnimation(npcId, enable);
        player.sendMessage(enable ?
                "§aAnimação de flutuação ativada!" :
                "§cAnimação de flutuação desativada!");
    }

    @Command(
            name = "npc.rotate",
            description = "Ativa/desativa animação de rotação",
            permission = "lobby.npc.admin"
    )
    public void npcRotateCommand(Context<Player> context, String npcId, boolean enable) {
        Player player = context.getSender();

        if (!npcManager.npcExists(npcId)) {
            player.sendMessage("§cNPC não encontrado!");
            return;
        }

        if (!"head".equals(npcManager.getNPCType(npcId))) {
            player.sendMessage("§cApenas NPCs do tipo 'head' suportam esta animação!");
            return;
        }

        npcManager.setRotateAnimation(npcId, enable);
        player.sendMessage(enable ?
                "§aAnimação de rotação ativada!" :
                "§cAnimação de rotação desativada!");
    }

    @Command(
            name = "npc.rotatespeed",
            description = "Define a velocidade de rotação",
            permission = "lobby.npc.admin"
    )
    public void npcRotateSpeedCommand(Context<Player> context, String npcId, double speed) {
        Player player = context.getSender();

        if (!npcManager.npcExists(npcId)) {
            player.sendMessage("§cNPC não encontrado!");
            return;
        }

        if (!"head".equals(npcManager.getNPCType(npcId))) {
            player.sendMessage("§cApenas NPCs do tipo 'head' suportam esta configuração!");
            return;
        }

        if (speed < 0.1 || speed > 20.0) {
            player.sendMessage("§cVelocidade deve estar entre 0.1 e 20.0!");
            return;
        }

        npcManager.setRotateSpeed(npcId, speed);
        player.sendMessage("§aVelocidade de rotação definida para: §e" + speed);
    }

    @Command(
            name = "npc.lookat",
            description = "Ativa/desativa olhar para jogador",
            permission = "lobby.npc.admin"
    )
    public void npcLookAtCommand(Context<Player> context, String npcId, boolean enable) {
        Player player = context.getSender();

        if (!npcManager.npcExists(npcId)) {
            player.sendMessage("§cNPC não encontrado!");
            return;
        }

        npcManager.setLookAtPlayer(npcId, enable);
        player.sendMessage(enable ?
                "§aNPC agora olha para os jogadores!" :
                "§cNPC não olha mais para os jogadores!");
    }

    @Command(
            name = "npc.info",
            description = "Mostra informações do NPC",
            permission = "lobby.npc.admin"
    )
    public void npcInfoCommand(Context<Player> context, String npcId) {
        Player player = context.getSender();

        if (!npcManager.npcExists(npcId)) {
            player.sendMessage("§cNPC não encontrado!");
            return;
        }

        player.sendMessage("§e§l========== NPC Info ==========");
        player.sendMessage("§7ID: §f" + npcId);
        player.sendMessage("§7Tipo: §f" + npcManager.getNPCType(npcId));
        player.sendMessage("§7Nome: §f" + npcManager.getNPCName(npcId));
        player.sendMessage("§7Skin: §f" + npcManager.getNPCSkin(npcId));
        player.sendMessage("§7Altura Holograma: §f" + npcManager.getHologramHeight(npcId));

        if ("head".equals(npcManager.getNPCType(npcId))) {
            player.sendMessage("§7Flutuação: " + (npcManager.getFloatAnimation(npcId) ? "§aAtivo" : "§cInativo"));
            player.sendMessage("§7Rotação: " + (npcManager.getRotateAnimation(npcId) ? "§aAtivo" : "§cInativo"));
            if (npcManager.getRotateAnimation(npcId)) {
                player.sendMessage("§7Velocidade: §f" + npcManager.getRotateSpeed(npcId));
            }
        }

        player.sendMessage("§7Olhar Jogador: " + (npcManager.getLookAtPlayer(npcId) ? "§aAtivo" : "§cInativo"));
        player.sendMessage("§e§l==============================");
    }

}