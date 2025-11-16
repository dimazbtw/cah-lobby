package github.dimazbtw.lobby.listeners;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.commands.LobbyCommand;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class SpawnListener implements Listener {

    private final Main plugin;
    private final LobbyCommand lobbyCommand;

    public SpawnListener(Main plugin, LobbyCommand lobbyCommand) {
        this.plugin = plugin;
        this.lobbyCommand = lobbyCommand;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Teleportar para o spawn ao entrar se configurado
        if (plugin.getConfig().getBoolean("spawn.teleport-on-join", true)) {
            if (lobbyCommand.hasSpawn()) {
                Location spawn = lobbyCommand.getSpawn();

                // Usar runTaskLater para garantir que o jogador foi completamente carregado
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.teleport(spawn);
                }, 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Teleportar para o spawn ao respawnar se configurado
        if (plugin.getConfig().getBoolean("spawn.teleport-on-respawn", true)) {
            if (lobbyCommand.hasSpawn()) {
                Location spawn = lobbyCommand.getSpawn();
                event.setRespawnLocation(spawn);
            }
        }
    }
}