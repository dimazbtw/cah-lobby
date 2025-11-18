package github.dimazbtw.lobby.listeners;

import github.dimazbtw.lobby.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PvPVisibilityListener implements Listener {

    private final Main plugin;

    public PvPVisibilityListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Atualiza visibilidade quando um jogador entra
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay para garantir que tudo foi carregado
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getPvPManager().updatePlayerVisibility(player);
            }
        }, 20L); // 1 segundo de delay
    }

    /**
     * Limpa dados ao sair
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Se estava em PvP, remover (sem dar itens já que está saindo)
        if (plugin.getPvPManager().isPvPEnabled(player)) {
            plugin.getPvPManager().removePlayer(player);
        }
    }
}