package github.dimazbtw.lobby.commands;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.config.LanguageManager;
import github.dimazbtw.lobby.managers.PvPManager;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LobbyCommand {

    private final Main plugin;
    private final LanguageManager languageManager;
    private File spawnFile;
    private FileConfiguration spawnConfig;

    public LobbyCommand(Main plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        loadSpawnConfig();
    }

    /**
     * Carrega o arquivo de configuração do spawn
     */
    private void loadSpawnConfig() {
        spawnFile = new File(plugin.getDataFolder(), "spawn.yml");

        if (!spawnFile.exists()) {
            try {
                spawnFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);
    }

    /**
     * Salva o arquivo de configuração do spawn
     */
    private void saveSpawnConfig() {
        try {
            spawnConfig.save(spawnFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Command(
            name = "lobby",
            aliases = {"spawn", "hub"},
            description = "Comandos do lobby",
            target = CommandTarget.PLAYER
    )
    public void lobbyCommand(Context<Player> context) {
        Player player = context.getSender();

        // Se não tiver argumentos, teleporta para o spawn
        if (hasSpawn()) {
            Location spawn = getSpawn();
            player.teleport(spawn);
            languageManager.sendMessage(player, "lobby.teleported");
        } else {
            languageManager.sendMessage(player, "lobby.no-spawn-set");
        }
    }

    @Command(
            name = "lobby.setspawn",
            aliases = {"setspawn", "spawn.set"},
            description = "Define o spawn do servidor",
            target = CommandTarget.PLAYER,
            permission = "lobby.setspawn"
    )
    public void setSpawnCommand(Context<Player> context) {
        Player player = context.getSender();
        Location location = player.getLocation();

        // Salvar spawn
        spawnConfig.set("spawn.world", location.getWorld().getName());
        spawnConfig.set("spawn.x", location.getX());
        spawnConfig.set("spawn.y", location.getY());
        spawnConfig.set("spawn.z", location.getZ());
        spawnConfig.set("spawn.yaw", location.getYaw());
        spawnConfig.set("spawn.pitch", location.getPitch());
        saveSpawnConfig();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("x", String.format("%.2f", location.getX()));
        placeholders.put("y", String.format("%.2f", location.getY()));
        placeholders.put("z", String.format("%.2f", location.getZ()));
        placeholders.put("world", location.getWorld().getName());

        languageManager.sendMessage(player, "lobby.spawn-set", placeholders);
    }

    @Command(
            name = "lobby.delspawn",
            aliases = {"delspawn", "spawn.delete"},
            description = "Remove o spawn do servidor",
            target = CommandTarget.PLAYER,
            permission = "lobby.delspawn"
    )
    public void delSpawnCommand(Context<Player> context) {
        Player player = context.getSender();

        if (!hasSpawn()) {
            languageManager.sendMessage(player, "lobby.no-spawn-set");
            return;
        }

        spawnConfig.set("spawn", null);
        saveSpawnConfig();

        languageManager.sendMessage(player, "lobby.spawn-deleted");
    }

    @Command(
            name = "lobby.reload",
            aliases = {"lobbyreload"},
            description = "Recarrega as configurações do plugin",
            target = CommandTarget.PLAYER,
            permission = "lobby.reload"
    )
    public void reloadCommand(Context<Player> context) {
        Player player = context.getSender();

        // Recarregar config
        plugin.reloadConfig();

        // Recarregar spawn
        loadSpawnConfig();

        // Recarregar idiomas
        languageManager.reload();

        // Recarregar NPCs
        if (plugin.getNpcManager() != null) {
            plugin.getNpcManager().reload();
        }

        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().reload();
        }

        plugin.getPvPManager().disablePvP(player);
        languageManager.sendMessage(player, "lobby.reloaded");
        plugin.getMenuManager().reload();
    }

    @Command(
            name = "sair",
            aliases = {"pvp.sair"},
            description = "Sair do modo de pvp",
            target = CommandTarget.PLAYER
    )
    public void leavePvP(Context<Player> context) {
        Player player = context.getSender();
        PvPManager pvpManager = plugin.getPvPManager();

        // Verificar se está em combate
        if (pvpManager.isInCombat(player)) {
            long timeLeft = pvpManager.getRemainingCombatTime(player);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(timeLeft / 1000));

            plugin.getLanguageManager().sendMessage(player, "pvp.combat-tag", placeholders);
            return;
        }

        // Desativar PvP normalmente se não estiver em combate
        pvpManager.disablePvP(player);
    }

    /**
     * Verifica se existe um spawn definido
     */
    public boolean hasSpawn() {
        return spawnConfig.contains("spawn.world") &&
                spawnConfig.contains("spawn.x") &&
                spawnConfig.contains("spawn.y") &&
                spawnConfig.contains("spawn.z");
    }

    /**
     * Obtém a localização do spawn
     */
    public Location getSpawn() {
        if (!hasSpawn()) {
            return null;
        }

        String worldName = spawnConfig.getString("spawn.world");
        double x = spawnConfig.getDouble("spawn.x");
        double y = spawnConfig.getDouble("spawn.y");
        double z = spawnConfig.getDouble("spawn.z");
        float yaw = (float) spawnConfig.getDouble("spawn.yaw", 0);
        float pitch = (float) spawnConfig.getDouble("spawn.pitch", 0);

        return new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch);
    }
}