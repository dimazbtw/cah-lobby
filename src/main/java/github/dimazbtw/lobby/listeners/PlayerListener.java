package github.dimazbtw.lobby.listeners;

import github.dimazbtw.lobby.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }



    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpar dados dos NPCs do jogador
        if (plugin.getNpcManager() != null) {
            plugin.getNpcManager().removePlayerData(event.getPlayer());
        }

        // Limpar dados dos hologramas do jogador
        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().removePlayerData(event.getPlayer());
        }
    }
}