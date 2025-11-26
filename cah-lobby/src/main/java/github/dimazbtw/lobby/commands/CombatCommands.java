package github.dimazbtw.lobby.commands;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.config.LanguageManager;
import github.dimazbtw.lobby.managers.X1Manager;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CombatCommands {

    private final Main plugin;
    private final LanguageManager languageManager;
    private File arenaFile;
    private FileConfiguration arenaConfig;

    public CombatCommands(Main plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        loadArenaConfig();
    }

    private void loadArenaConfig() {
        arenaFile = new File(plugin.getDataFolder(), "arena.yml");
        if (!arenaFile.exists()) {
            try {
                arenaFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
    }

    private void saveArenaConfig() {
        try {
            arenaConfig.save(arenaFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== ARENA FFA ====================

    @Command(
            name = "arena",
            description = "Entra no modo FFA",
            target = CommandTarget.PLAYER
    )
    public void arenaCommand(Context<Player> context) {
        Player player = context.getSender();

        if (!hasArenaSpawn()) {
            languageManager.sendMessage(player, "arena.no-spawn");
            return;
        }

        // Ativar PvP e teleportar
        plugin.getPvPManager().enablePvP(player);
        player.teleport(getArenaSpawn());

        languageManager.sendMessage(player, "arena.joined");
    }

    @Command(
            name = "arena.setspawn",
            description = "Define o spawn da arena",
            target = CommandTarget.PLAYER,
            permission = "lobby.arena.admin"
    )
    public void arenaSetSpawnCommand(Context<Player> context) {
        Player player = context.getSender();
        Location loc = player.getLocation();

        arenaConfig.set("arena.spawn.world", loc.getWorld().getName());
        arenaConfig.set("arena.spawn.x", loc.getX());
        arenaConfig.set("arena.spawn.y", loc.getY());
        arenaConfig.set("arena.spawn.z", loc.getZ());
        arenaConfig.set("arena.spawn.yaw", loc.getYaw());
        arenaConfig.set("arena.spawn.pitch", loc.getPitch());
        saveArenaConfig();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("x", String.format("%.2f", loc.getX()));
        placeholders.put("y", String.format("%.2f", loc.getY()));
        placeholders.put("z", String.format("%.2f", loc.getZ()));
        languageManager.sendMessage(player, "arena.spawn-set", placeholders);
    }

    @Command(
            name = "arena.ranking",
            aliases = {"arena.top", "arena.rank"},
            description = "Ver ranking da arena",
            target = CommandTarget.PLAYER
    )
    public void arenaRankingCommand(Context<Player> context) {
        Player player = context.getSender();
        plugin.getMenuManager().openMenu(player, "arena-ranking");
    }

    private boolean hasArenaSpawn() {
        return arenaConfig.contains("arena.spawn.world");
    }

    private Location getArenaSpawn() {
        if (!hasArenaSpawn()) return null;
        String world = arenaConfig.getString("arena.spawn.world");
        double x = arenaConfig.getDouble("arena.spawn.x");
        double y = arenaConfig.getDouble("arena.spawn.y");
        double z = arenaConfig.getDouble("arena.spawn.z");
        float yaw = (float) arenaConfig.getDouble("arena.spawn.yaw");
        float pitch = (float) arenaConfig.getDouble("arena.spawn.pitch");
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    // ==================== X1 ====================

    @Command(
            name = "x1",
            description = "Sistema de X1",
            target = CommandTarget.PLAYER
    )
    public void x1Command(Context<Player> context, @Optional String targetName) {
        Player player = context.getSender();
        X1Manager x1Manager = plugin.getX1Manager();

        if (targetName == null) {
            // Mostrar ajuda
            languageManager.sendMessage(player, "x1.help.header");
            player.sendMessage("§e/x1 <jogador> §7- Convidar para X1");
            player.sendMessage("§e/x1 fila §7- Entrar na fila");
            player.sendMessage("§e/x1 ranking §7- Ver ranking");
            languageManager.sendMessage(player, "x1.help.footer");
            return;
        }

        // Verificar se está em X1
        if (x1Manager.isInX1(player)) {
            languageManager.sendMessage(player, "x1.already-in-match");
            return;
        }

        // Verificar se está em PvP
        if (plugin.getPvPManager().isPvPEnabled(player)) {
            languageManager.sendMessage(player, "x1.in-pvp");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            languageManager.sendMessage(player, "general.player-not-found");
            return;
        }

        if (target.equals(player)) {
            languageManager.sendMessage(player, "x1.self-invite");
            return;
        }

        if (x1Manager.isInX1(target)) {
            languageManager.sendMessage(player, "x1.target-in-match");
            return;
        }

        if (plugin.getPvPManager().isPvPEnabled(target)) {
            languageManager.sendMessage(player, "x1.target-in-pvp");
            return;
        }

        // Enviar convite
        x1Manager.sendInvite(player, target);
    }

    @Command(
            name = "x1.aceitar",
            aliases = {"x1.accept"},
            description = "Aceita um convite de X1",
            target = CommandTarget.PLAYER
    )
    public void x1AcceptCommand(Context<Player> context) {
        Player player = context.getSender();
        plugin.getX1Manager().acceptInvite(player);
    }

    @Command(
            name = "x1.recusar",
            aliases = {"x1.deny", "x1.rejeitar"},
            description = "Recusa um convite de X1",
            target = CommandTarget.PLAYER
    )
    public void x1DenyCommand(Context<Player> context) {
        Player player = context.getSender();
        plugin.getX1Manager().denyInvite(player);
    }

    @Command(
            name = "x1.fila",
            aliases = {"x1.queue"},
            description = "Entra na fila de X1",
            target = CommandTarget.PLAYER
    )
    public void x1QueueCommand(Context<Player> context) {
        Player player = context.getSender();
        X1Manager x1Manager = plugin.getX1Manager();

        if (x1Manager.isInX1(player)) {
            languageManager.sendMessage(player, "x1.already-in-match");
            return;
        }

        if (plugin.getPvPManager().isPvPEnabled(player)) {
            languageManager.sendMessage(player, "x1.in-pvp");
            return;
        }

        if (x1Manager.isInQueue(player)) {
            x1Manager.leaveQueue(player);
            languageManager.sendMessage(player, "x1.left-queue");
        } else {
            x1Manager.joinQueue(player);
        }
    }

    @Command(
            name = "x1.set",
            description = "Define posições do X1",
            target = CommandTarget.PLAYER,
            permission = "lobby.x1.admin"
    )
    public void x1SetCommand(Context<Player> context, String position) {
        Player player = context.getSender();
        Location loc = player.getLocation();

        if (!position.equalsIgnoreCase("pos1") && !position.equalsIgnoreCase("pos2")) {
            languageManager.sendMessage(player, "x1.invalid-position");
            return;
        }

        String path = "x1." + position.toLowerCase() + ".";
        arenaConfig.set(path + "world", loc.getWorld().getName());
        arenaConfig.set(path + "x", loc.getX());
        arenaConfig.set(path + "y", loc.getY());
        arenaConfig.set(path + "z", loc.getZ());
        arenaConfig.set(path + "yaw", loc.getYaw());
        arenaConfig.set(path + "pitch", loc.getPitch());
        saveArenaConfig();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("position", position.toUpperCase());
        placeholders.put("x", String.format("%.2f", loc.getX()));
        placeholders.put("y", String.format("%.2f", loc.getY()));
        placeholders.put("z", String.format("%.2f", loc.getZ()));
        languageManager.sendMessage(player, "x1.position-set", placeholders);
    }

    @Command(
            name = "x1.ranking",
            aliases = {"x1.top", "x1.rank"},
            description = "Ver ranking de X1",
            target = CommandTarget.PLAYER
    )
    public void x1RankingCommand(Context<Player> context) {
        Player player = context.getSender();
        plugin.getMenuManager().openMenu(player, "x1-ranking");
    }

    @Command(
            name = "x1.sair",
            aliases = {"x1.leave"},
            description = "Sair do X1",
            target = CommandTarget.PLAYER
    )
    public void x1LeaveCommand(Context<Player> context) {
        Player player = context.getSender();
        X1Manager x1Manager = plugin.getX1Manager();

        if (!x1Manager.isInX1(player)) {
            languageManager.sendMessage(player, "x1.not-in-match");
            return;
        }

        x1Manager.forfeitMatch(player);
    }

    // Getters para as posições do X1
    public Location getX1Position1() {
        if (!arenaConfig.contains("x1.pos1.world")) return null;
        return new Location(
                Bukkit.getWorld(arenaConfig.getString("x1.pos1.world")),
                arenaConfig.getDouble("x1.pos1.x"),
                arenaConfig.getDouble("x1.pos1.y"),
                arenaConfig.getDouble("x1.pos1.z"),
                (float) arenaConfig.getDouble("x1.pos1.yaw"),
                (float) arenaConfig.getDouble("x1.pos1.pitch")
        );
    }

    public Location getX1Position2() {
        if (!arenaConfig.contains("x1.pos2.world")) return null;
        return new Location(
                Bukkit.getWorld(arenaConfig.getString("x1.pos2.world")),
                arenaConfig.getDouble("x1.pos2.x"),
                arenaConfig.getDouble("x1.pos2.y"),
                arenaConfig.getDouble("x1.pos2.z"),
                (float) arenaConfig.getDouble("x1.pos2.yaw"),
                (float) arenaConfig.getDouble("x1.pos2.pitch")
        );
    }

    public boolean hasX1Positions() {
        return arenaConfig.contains("x1.pos1.world") && arenaConfig.contains("x1.pos2.world");
    }

    public void reload() {
        loadArenaConfig();
    }
}