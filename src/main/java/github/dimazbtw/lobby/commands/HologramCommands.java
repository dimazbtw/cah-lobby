package github.dimazbtw.lobby.commands;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.config.LanguageManager;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class HologramCommands {

    private final Main plugin;
    private final LanguageManager languageManager;

    public HologramCommands(Main plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Command(
            name = "hologram",
            aliases = "holo",
            description = "Gerencia hologramas",
            target = CommandTarget.PLAYER,
            permission = "lobby.hologram"
    )
    public void hologramCommand(Context<Player> context) {
        Player player = context.getSender();
        languageManager.sendMessage(player, "hologram.help.header");
        player.sendMessage("§e/hologram create <nome> §7- Cria um holograma");
        player.sendMessage("§e/hologram delete <nome> §7- Remove um holograma");
        player.sendMessage("§e/hologram setline <nome> <linha> <texto> §7- Define uma linha");
        player.sendMessage("§e/hologram addline <nome> <texto> §7- Adiciona uma linha");
        player.sendMessage("§e/hologram insertline <nome> <posição> <texto> §7- Insere uma linha");
        player.sendMessage("§e/hologram removeline <nome> <linha> §7- Remove uma linha");
        player.sendMessage("§e/hologram list §7- Lista todos os hologramas");
        player.sendMessage("§e/hologram teleport <nome> §7- Teleporta para um holograma");
        player.sendMessage("§e/hologram tphere <nome> §7- Move um holograma até você");
        languageManager.sendMessage(player, "hologram.help.footer");
    }

    @Command(
            name = "hologram.create",
            description = "Cria um holograma",
            target = CommandTarget.PLAYER,
            permission = "lobby.hologram.create",
            usage = "/hologram create <nome>"
    )
    public void createCommand(Context<Player> context, String name) {
        Player player = context.getSender();

        if (plugin.getHologramManager().hologramExists(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            languageManager.sendMessage(player, "hologram.already-exists", placeholders);
            return;
        }

        Location location = player.getLocation();
        List<String> defaultLines = Arrays.asList(
                "&7Holograma criado!",
                "&eUse &f/hologram addline " + name + " <texto>",
                "&epara adicionar linhas!"
        );

        plugin.getHologramManager().createHologram(name, location, defaultLines);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        languageManager.sendMessage(player, "hologram.created", placeholders);
    }

    @Command(
            name = "hologram.delete",
            description = "Remove um holograma",
            target = CommandTarget.PLAYER,
            permission = "lobby.hologram.delete",
            usage = "/hologram delete <nome>"
    )
    public void deleteCommand(Context<Player> context, String name) {
        Player player = context.getSender();

        if (!plugin.getHologramManager().hologramExists(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            languageManager.sendMessage(player, "hologram.not-found", placeholders);
            return;
        }

        plugin.getHologramManager().deleteHologram(name);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        languageManager.sendMessage(player, "hologram.deleted", placeholders);
    }

    @Command(
            name = "hologram.addline",
            description = "Adiciona uma linha ao holograma",
            target = CommandTarget.PLAYER,
            permission = "lobby.hologram.edit",
            usage = "/hologram addline <nome> <texto>"
    )
    public void addLineCommand(Context<Player> context, String name, String[] args) {
        Player player = context.getSender();

        if (!plugin.getHologramManager().hologramExists(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            languageManager.sendMessage(player, "hologram.not-found", placeholders);
            return;
        }

        if (args.length == 0) {
            languageManager.sendMessage(player, "hologram.no-text");
            return;
        }

        String text = String.join(" ", args);
        String playerLang = languageManager.getPlayerLanguage(player);

        // Obter linhas atuais
        List<String> currentLines = getCurrentLines(name, playerLang);
        currentLines.add(text);

        plugin.getHologramManager().setHologramLines(name, playerLang, currentLines);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        placeholders.put("line", String.valueOf(currentLines.size()));
        languageManager.sendMessage(player, "hologram.line-added", placeholders);
    }

    @Command(
            name = "hologram.setline",
            description = "Define uma linha do holograma",
            target = CommandTarget.PLAYER,
            permission = "lobby.hologram.edit",
            usage = "/hologram setline <nome> <linha> <texto>"
    )
    public void setLineCommand(Context<Player> context, String name, String lineStr, String[] args) {
        Player player = context.getSender();

        if (!plugin.getHologramManager().hologramExists(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            languageManager.sendMessage(player, "hologram.not-found", placeholders);
            return;
        }

        int line;
        try {
            line = Integer.parseInt(lineStr) - 1;
        } catch (NumberFormatException e) {
            languageManager.sendMessage(player, "hologram.invalid-line");
            return;
        }

        if (args.length == 0) {
            languageManager.sendMessage(player, "hologram.no-text");
            return;
        }

        String text = String.join(" ", args);
        String playerLang = languageManager.getPlayerLanguage(player);

        List<String> currentLines = getCurrentLines(name, playerLang);

        if (line < 0 || line >= currentLines.size()) {
            languageManager.sendMessage(player, "hologram.invalid-line");
            return;
        }

        currentLines.set(line, text);
        plugin.getHologramManager().setHologramLines(name, playerLang, currentLines);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        placeholders.put("line", String.valueOf(line + 1));
        languageManager.sendMessage(player, "hologram.line-set", placeholders);
    }

    @Command(
            name = "hologram.insertline",
            description = "Insere uma linha no holograma",
            target = CommandTarget.PLAYER,
            permission = "lobby.hologram.edit",
            usage = "/hologram insertline <nome> <posição> <texto>"
    )
    public void insertLineCommand(Context<Player> context, String name, String posStr, String[] args) {
        Player player = context.getSender();

        if (!plugin.getHologramManager().hologramExists(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            languageManager.sendMessage(player, "hologram.not-found", placeholders);
            return;
        }

        int position;
        try {
            position = Integer.parseInt(posStr) - 1;
        } catch (NumberFormatException e) {
            languageManager.sendMessage(player, "hologram.invalid-line");
            return;
        }

        if (args.length == 0) {
            languageManager.sendMessage(player, "hologram.no-text");
            return;
        }

        String text = String.join(" ", args);
        String playerLang = languageManager.getPlayerLanguage(player);

        List<String> currentLines = getCurrentLines(name, playerLang);

        if (position < 0 || position > currentLines.size()) {
            languageManager.sendMessage(player, "hologram.invalid-line");
            return;
        }

        currentLines.add(position, text);
        plugin.getHologramManager().setHologramLines(name, playerLang, currentLines);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        placeholders.put("line", String.valueOf(position + 1));
        languageManager.sendMessage(player, "hologram.line-inserted", placeholders);
    }

    @Command(
            name = "hologram.removeline",
            description = "Remove uma linha do holograma",
            target = CommandTarget.PLAYER,
            permission = "lobby.hologram.edit",
            usage = "/hologram removeline <nome> <linha>"
    )
    public void removeLineCommand(Context<Player> context, String name, String lineStr) {
        Player player = context.getSender();

        if (!plugin.getHologramManager().hologramExists(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            languageManager.sendMessage(player, "hologram.not-found", placeholders);
            return;
        }

        int line;
        try {
            line = Integer.parseInt(lineStr) - 1;
        } catch (NumberFormatException e) {
            languageManager.sendMessage(player, "hologram.invalid-line");
            return;
        }

        String playerLang = languageManager.getPlayerLanguage(player);
        List<String> currentLines = getCurrentLines(name, playerLang);

        if (line < 0 || line >= currentLines.size()) {
            languageManager.sendMessage(player, "hologram.invalid-line");
            return;
        }

        currentLines.remove(line);
        plugin.getHologramManager().setHologramLines(name, playerLang, currentLines);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        placeholders.put("line", String.valueOf(line + 1));
        languageManager.sendMessage(player, "hologram.line-removed", placeholders);
    }

    @Command(
            name = "hologram.list",
            description = "Lista todos os hologramas",
            target = CommandTarget.PLAYER,
            permission = "lobby.hologram"
    )
    public void listCommand(Context<Player> context) {
        Player player = context.getSender();

        Set<String> holograms = plugin.getHologramManager().getHologramNames();

        if (holograms.isEmpty()) {
            languageManager.sendMessage(player, "hologram.list-empty");
            return;
        }

        languageManager.sendMessage(player, "hologram.list-header");
        for (String name : holograms) {
            player.sendMessage("§e• §f" + name);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(holograms.size()));
        languageManager.sendMessage(player, "hologram.list-footer", placeholders);
    }

    @Command(
            name = "hologram.teleport",
            aliases = "hologram.tp",
            description = "Teleporta para um holograma",
            target = CommandTarget.PLAYER,
            permission = "lobby.hologram.teleport",
            usage = "/hologram teleport <nome>"
    )
    public void teleportCommand(Context<Player> context, String name) {
        Player player = context.getSender();

        if (!plugin.getHologramManager().hologramExists(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            languageManager.sendMessage(player, "hologram.not-found", placeholders);
            return;
        }

        Location location = plugin.getHologramManager().getHologramLocation(name);
        player.teleport(location);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        languageManager.sendMessage(player, "hologram.teleported", placeholders);
    }

    @Command(
            name = "hologram.tphere",
            description = "Move um holograma para sua localização",
            target = CommandTarget.PLAYER,
            permission = "lobby.hologram.edit",
            usage = "/hologram tphere <nome>"
    )
    public void tphereCommand(Context<Player> context, String name) {
        Player player = context.getSender();

        if (!plugin.getHologramManager().hologramExists(name)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("name", name);
            languageManager.sendMessage(player, "hologram.not-found", placeholders);
            return;
        }

        Location location = player.getLocation();
        plugin.getHologramManager().moveHologram(name, location);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", name);
        languageManager.sendMessage(player, "hologram.moved", placeholders);
    }

    /**
     * Obtém as linhas atuais de um holograma
     */
    private List<String> getCurrentLines(String name, String language) {
        return plugin.getHologramManager().getLines(name, language);
    }
}