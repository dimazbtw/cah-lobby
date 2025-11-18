package github.dimazbtw.lobby.commands;

import github.dimazbtw.lobby.config.LanguageManager;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class UtilityCommands {

    private final LanguageManager languageManager;

    public UtilityCommands(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    @Command(
            name = "gamemode",
            aliases = "gm",
            description = "Altere o modo de jogo",
            target = CommandTarget.ALL,
            permission = "admin",
            async = true
    )
    public void changeGamemode(Context<CommandSender> context, String mode, @Optional Player target) {
        CommandSender sender = context.getSender();
        Player player = target != null ? target : (sender instanceof Player ? (Player) sender : null);

        if (player == null) {
            if (sender instanceof Player) {
                languageManager.sendMessage((Player) sender, "gamemode.console-specify");
            } else {
                sender.sendMessage("§cVocê deve especificar um jogador!");
            }
            return;
        }

        GameMode gameMode = null;
        switch (mode.toLowerCase()) {
            case "0":
            case "s":
            case "survival":
            case "sobrevivencia":
                gameMode = GameMode.SURVIVAL;
                break;
            case "1":
            case "c":
            case "creative":
            case "criativo":
                gameMode = GameMode.CREATIVE;
                break;
            case "2":
            case "a":
            case "adventure":
            case "aventura":
                gameMode = GameMode.ADVENTURE;
                break;
            case "3":
            case "sp":
            case "spectator":
            case "espectador":
                gameMode = GameMode.SPECTATOR;
                break;
            default:
                if (sender instanceof Player) {
                    languageManager.sendMessage((Player) sender, "gamemode.invalid-mode");
                } else {
                    sender.sendMessage("§cModo de jogo inválido!");
                }
                return;
        }

        player.setGameMode(gameMode);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("mode", gameMode.name());
        placeholders.put("player", player.getName());

        if (target != null && !target.equals(sender)) {
            if (sender instanceof Player) {
                languageManager.sendMessage((Player) sender, "gamemode.changed-other", placeholders);
            } else {
                sender.sendMessage("§aModo de jogo de §f" + player.getName() + "§a alterado para §f" + gameMode.name());
            }
            languageManager.sendMessage(target, "gamemode.changed-self", placeholders);
        } else if (sender instanceof Player) {
            languageManager.sendMessage((Player) sender, "gamemode.changed-self", placeholders);
        }
    }

    @Command(
            name = "fly",
            aliases = "voar",
            description = "Alterna o modo de voo",
            target = CommandTarget.ALL,
            permission = "admin",
            async = true
    )
    public void changeFly(Context<CommandSender> context, @Optional Player target) {
        CommandSender sender = context.getSender();
        Player player = target != null ? target : (sender instanceof Player ? (Player) sender : null);

        if (player == null) {
            if (sender instanceof Player) {
                languageManager.sendMessage((Player) sender, "fly.console-specify");
            } else {
                sender.sendMessage("§cVocê deve especificar um jogador!");
            }
            return;
        }

        boolean newFlyState = !player.getAllowFlight();
        player.setAllowFlight(newFlyState);
        player.setFlying(newFlyState);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());

        String messageKey = newFlyState ? (target != null && !target.equals(sender) ? "fly.enabled-other" : "fly.enabled-self")
                : (target != null && !target.equals(sender) ? "fly.disabled-other" : "fly.disabled-self");

        if (sender instanceof Player) {
            languageManager.sendMessage((Player) sender, messageKey, placeholders);
        } else {
            sender.sendMessage((newFlyState ? "§a" : "§c") + "Modo de voo " +
                    (newFlyState ? "ativado" : "desativado") + " para " + player.getName());
        }

        if (target != null && !target.equals(sender)) {
            languageManager.sendMessage(target, newFlyState ? "fly.enabled-self" : "fly.disabled-self");
        }
    }

    @Command(
            name = "broadcast",
            aliases = {"bc", "alerta"},
            description = "Envia uma mensagem para todos os jogadores",
            target = CommandTarget.ALL,
            permission = "admin",
            async = true
    )
    public void broadcast(Context<CommandSender> context, String[] args) throws ExecutionException, InterruptedException {
        CommandSender sender = context.getSender();

        if (args.length == 0) {
            if (sender instanceof Player) {
                languageManager.sendMessage((Player) sender, "broadcast.usage");
            } else {
                sender.sendMessage("§cUso: /broadcast <mensagem>");
            }
            return;
        }

        String message = String.join(" ", args).replace("&", "§");

        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("message", message);

            player.sendMessage("");
            player.sendMessage(languageManager.getMessage(player, "broadcast.format", placeholders));
            player.sendMessage("");
        }

        if (sender instanceof Player) {
            languageManager.sendMessage((Player) sender, "broadcast.sent");
        } else {
            sender.sendMessage("§aMensagem enviada!");
        }
    }

    @Command(
            name = "playerinfo",
            aliases = "pinfo",
            description = "Mostra informações detalhadas sobre um jogador",
            target = CommandTarget.ALL,
            permission = "admin",
            async = true
    )
    public void playerInfo(Context<CommandSender> context, @Optional String playerName) throws ExecutionException, InterruptedException {
        CommandSender sender = context.getSender();
        Player target;

        if (playerName == null) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cVocê deve especificar um jogador!");
                return;
            }
        } else {
            target = Bukkit.getPlayer(playerName);
            if (target == null) {
                if (sender instanceof Player) {
                    languageManager.sendMessage((Player) sender, "general.player-not-found");
                } else {
                    sender.sendMessage("§cJogador não encontrado!");
                }
                return;
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String lastEntry = dateFormat.format(target.getFirstPlayed());

        if (sender instanceof Player) {
            Player player = (Player) sender;
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            placeholders.put("nick", target.getName());
            placeholders.put("uuid", target.getUniqueId().toString());
            placeholders.put("ip", target.getAddress().getAddress().getHostAddress());
            placeholders.put("date", lastEntry);
            placeholders.put("gamemode", target.getGameMode().name());
            placeholders.put("health", String.format("%.1f", target.getHealth()));
            placeholders.put("food", String.valueOf(target.getFoodLevel()));
            placeholders.put("level", String.valueOf(target.getLevel()));
            placeholders.put("world", target.getWorld().getName());

            languageManager.sendMessage(player, "playerinfo.header");
            languageManager.sendMessage(player, "playerinfo.title", placeholders);
            player.sendMessage("");
            languageManager.sendMessage(player, "playerinfo.nick", placeholders);
            languageManager.sendMessage(player, "playerinfo.uuid", placeholders);
            languageManager.sendMessage(player, "playerinfo.ip", placeholders);
            languageManager.sendMessage(player, "playerinfo.first-login", placeholders);
            languageManager.sendMessage(player, "playerinfo.gamemode", placeholders);
            languageManager.sendMessage(player, target.isOnline() ? "playerinfo.status-online" : "playerinfo.status-offline");
            languageManager.sendMessage(player, "playerinfo.health", placeholders);
            languageManager.sendMessage(player, "playerinfo.food", placeholders);
            languageManager.sendMessage(player, "playerinfo.level", placeholders);
            languageManager.sendMessage(player, "playerinfo.world", placeholders);
            languageManager.sendMessage(player, "playerinfo.footer");
        } else {
            sender.sendMessage("§6§m------------------------------");
            sender.sendMessage("§6Informações de §f" + target.getName());
            sender.sendMessage("");
            sender.sendMessage("§eNick: §f" + target.getName());
            sender.sendMessage("§eUUID: §f" + target.getUniqueId());
            sender.sendMessage("§eIP: §f" + target.getAddress().getAddress().getHostAddress());
            sender.sendMessage("§ePrimeiro login: §f" + lastEntry);
            sender.sendMessage("§eGamemode: §f" + target.getGameMode().name());
            sender.sendMessage("§eStatus: §a" + (target.isOnline() ? "Online" : "Offline"));
            sender.sendMessage("§eVida: §f" + String.format("%.1f", target.getHealth()) + "/20");
            sender.sendMessage("§eFome: §f" + target.getFoodLevel() + "/20");
            sender.sendMessage("§eNível: §f" + target.getLevel());
            sender.sendMessage("§eMundo: §f" + target.getWorld().getName());
            sender.sendMessage("§6§m------------------------------");
        }
    }

    @Command(
            name = "speed",
            description = "Altera a velocidade de movimento do jogador",
            target = CommandTarget.PLAYER,
            permission = "admin",
            async = true
    )
    public void speed(Context<CommandSender> context, String speedArg) throws ExecutionException, InterruptedException {
        Player player = (Player) context.getSender();

        if (speedArg.equalsIgnoreCase("normal")) {
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            languageManager.sendMessage(player, "speed.reset");
            return;
        }

        try {
            float speed = Float.parseFloat(speedArg);

            if (speed < 0 || speed > 10) {
                languageManager.sendMessage(player, "speed.range");
                return;
            }

            float normalizedSpeed = speed / 10f;

            if (player.isFlying()) {
                player.setFlySpeed(normalizedSpeed);
            } else {
                player.setWalkSpeed(normalizedSpeed);
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("speed", String.valueOf(speed));
            languageManager.sendMessage(player, "speed.changed", placeholders);

        } catch (NumberFormatException e) {
            languageManager.sendMessage(player, "speed.invalid");
        }
    }

    @Command(
            name = "tp",
            description = "Teleporta para um jogador, coordenadas ou mundo",
            target = CommandTarget.PLAYER,
            permission = "admin",
            usage = "/tp <jogador> | /tp <x> <y> <z> | /tp <x> <y> <z> <mundo>"
    )
    public void teleport(Context<CommandSender> context, String[] args) {
        Player player = (Player) context.getSender();

        if (args.length == 0) {
            languageManager.sendMessage(player, "tp.usage");
            return;
        }

        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                languageManager.sendMessage(player, "general.player-not-found");
                return;
            }

            if (target.equals(player)) {
                languageManager.sendMessage(player, "tp.self-teleport");
                return;
            }

            player.teleport(target);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            languageManager.sendMessage(player, "tp.teleported-player", placeholders);

        } else if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);

                Location location = new Location(player.getWorld(), x, y, z);
                player.teleport(location);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("x", String.format("%.2f", x));
                placeholders.put("y", String.format("%.2f", y));
                placeholders.put("z", String.format("%.2f", z));
                placeholders.put("world", player.getWorld().getName());
                languageManager.sendMessage(player, "tp.teleported-coords", placeholders);

            } catch (NumberFormatException e) {
                languageManager.sendMessage(player, "tp.invalid-coords");
            }

        } else if (args.length == 4) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                String worldName = args[3];

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    languageManager.sendMessage(player, "tp.world-not-found");
                    return;
                }

                Location location = new Location(world, x, y, z);
                player.teleport(location);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("x", String.format("%.2f", x));
                placeholders.put("y", String.format("%.2f", y));
                placeholders.put("z", String.format("%.2f", z));
                placeholders.put("world", world.getName());
                languageManager.sendMessage(player, "tp.teleported-coords", placeholders);

            } catch (NumberFormatException e) {
                languageManager.sendMessage(player, "tp.invalid-coords");
            }
        } else {
            languageManager.sendMessage(player, "tp.usage");
        }
    }

    @Command(
            name = "tphere",
            description = "Teleporta um jogador para sua localização",
            target = CommandTarget.PLAYER,
            permission = "essentials.tphere",
            usage = "/tphere <jogador>"
    )
    public void teleportHere(Context<CommandSender> context, String targetName) {
        Player player = (Player) context.getSender();
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            languageManager.sendMessage(player, "general.player-not-found");
            return;
        }

        if (target.equals(player)) {
            languageManager.sendMessage(player, "tphere.self-teleport");
            return;
        }

        target.teleport(player);

        Map<String, String> placeholdersToTarget = new HashMap<>();
        placeholdersToTarget.put("player", player.getName());
        languageManager.sendMessage(target, "tphere.teleported-to-you", placeholdersToTarget);

        Map<String, String> placeholdersToSender = new HashMap<>();
        placeholdersToSender.put("player", target.getName());
        languageManager.sendMessage(player, "tphere.teleported-player", placeholdersToSender);
    }

    @Command(
            name = "tpall",
            description = "Teleporta todos os jogadores para sua localização",
            target = CommandTarget.PLAYER,
            permission = "essentials.tpall"
    )
    public void teleportAll(Context<CommandSender> context) {
        Player player = (Player) context.getSender();
        Location location = player.getLocation();

        int teleportedCount = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(player)) {
                target.teleport(location);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                languageManager.sendMessage(target, "tpall.teleported-to-you", placeholders);

                teleportedCount++;
            }
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(teleportedCount));
        languageManager.sendMessage(player, "tpall.teleported-players", placeholders);
    }

    @Command(
            name = "online",
            description = "Mostra o número de jogadores online",
            target = CommandTarget.PLAYER
    )
    public void online(Context<Player> context) {
        Player player = context.getSender();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(Bukkit.getOnlinePlayers().size()));
        languageManager.sendMessage(player, "online.message", placeholders);
    }

    @Command(
            name = "lista",
            description = "Lista todos os jogadores online",
            target = CommandTarget.PLAYER
    )
    public void lista(Context<Player> context) {
        Player player = context.getSender();
        String players = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.joining("§f, §a"));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(Bukkit.getOnlinePlayers().size()));
        placeholders.put("players", players);
        languageManager.sendMessage(player, "lista.message", placeholders);
    }

    @Command(
            name = "ping",
            description = "Mostra o ping de um jogador",
            target = CommandTarget.PLAYER
    )
    public void ping(Context<Player> context, @Optional Player target) {
        Player player = context.getSender();
        Player checkPlayer = target != null ? target : player;

        if (checkPlayer == null) {
            languageManager.sendMessage(player, "general.player-not-found");
            return;
        }

        try {
            Object craftPlayer = checkPlayer.getClass().getMethod("getHandle").invoke(checkPlayer);
            int ping = (int) craftPlayer.getClass().getField("ping").get(craftPlayer);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("ping", String.valueOf(ping));
            placeholders.put("player", checkPlayer.getName());

            if (target != null && !target.equals(player)) {
                languageManager.sendMessage(player, "ping.other", placeholders);
            } else {
                languageManager.sendMessage(player, "ping.self", placeholders);
            }
        } catch (Exception e) {
            languageManager.sendMessage(player, "ping.error");
            e.printStackTrace();
        }
    }

    @Command(
            name = "inv",
            description = "Abre e edita o inventário de outro jogador",
            target = CommandTarget.PLAYER,
            permission = "admin"
    )
    public void inv(Context<Player> context, Player target) {
        Player player = context.getSender();

        if (target == null) {
            languageManager.sendMessage(player, "general.player-not-found");
            return;
        }

        player.openInventory(target.getInventory());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        languageManager.sendMessage(player, "inv.opened", placeholders);
    }

    @Command(
            name = "sudo",
            permission = "essentials.sudo",
            description = "Força um jogador a executar um comando",
            target = CommandTarget.PLAYER
    )
    public void sudo(Context<Player> context, Player target, String[] args) {
        Player player = context.getSender();

        if (target == null) {
            languageManager.sendMessage(player, "general.player-not-found");
            return;
        }

        if (args.length == 0) {
            languageManager.sendMessage(player, "sudo.usage");
            return;
        }

        String command = String.join(" ", args);

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        target.performCommand(command);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("command", command);
        placeholders.put("player", target.getName());
        languageManager.sendMessage(player, "sudo.executed", placeholders);
    }

    @Command(
            name = "lang",
            aliases = "idioma",
            description = "Altera o idioma do jogador",
            target = CommandTarget.PLAYER
    )
    public void handleLang(Context<Player> context, @Optional String langCode) {
        Player player = context.getSender();

        if (langCode == null) {
            // Mostrar idiomas disponíveis usando o idioma atual do jogador
            String currentLang = languageManager.getPlayerLanguage(player);

            player.sendMessage(languageManager.getMessage(player, "lang.header"));
            player.sendMessage(languageManager.getMessage(player, "lang.title"));
            player.sendMessage("");

            for (String code : languageManager.getAvailableLanguages()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("code", code);
                placeholders.put("name", languageManager.getLanguageDisplayName(code));
                player.sendMessage(languageManager.getMessage(player, "lang.available", placeholders));
            }

            player.sendMessage("");
            Map<String, String> currentPlaceholders = new HashMap<>();
            currentPlaceholders.put("language", languageManager.getLanguageDisplayName(currentLang));
            player.sendMessage(languageManager.getMessage(player, "lang.current", currentPlaceholders));
            player.sendMessage(languageManager.getMessage(player, "lang.footer"));
        } else {
            // Alterar idioma
            if (!languageManager.hasLanguage(langCode)) {
                languageManager.sendMessage(player, "lang.invalid");
                return;
            }

            // Define o idioma (isso já salva automaticamente no PlayerDataManager)
            languageManager.setPlayerLanguage(player, langCode);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("language", languageManager.getLanguageDisplayName(langCode));
            languageManager.sendMessage(player, "lang.changed", placeholders);
        }
    }
}