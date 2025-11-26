package github.dimazbtw.lobby.listeners;

import github.dimazbtw.lobby.Main;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ParkourListener implements Listener {

    private final Main plugin;

    public ParkourListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Detecta quando o jogador pisa em placas de pressão
     */
    @EventHandler
    public void onPressurePlate(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        switch (block.getType()) {
            case IRON_PLATE:
                plugin.getParkourManager().startParkour(player, player.getLocation());
                break;

            case WOOD_PLATE:
                if (plugin.getParkourManager().isInParkour(player)) {
                    plugin.getParkourManager().checkpoint(player, player.getLocation());
                }
                break;

            case GOLD_PLATE:
                if (plugin.getParkourManager().isInParkour(player)) {
                    plugin.getParkourManager().finish(player);
                }
                break;
        }
    }


    /**
     * Detecta cliques nos itens de parkour
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getParkourManager().isInParkour(player)) {
            return;
        }

        if (event.getItem() == null || !event.getItem().hasItemMeta()) {
            return;
        }

        String itemName = event.getItem().getItemMeta().getDisplayName();
        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);

        // Teleportar para checkpoint (ENDER_PEARL)
        if (event.getItem().getType() == Material.WOOD_PLATE) {
            String checkpointName = plugin.getLanguageManager().getMessage(playerLang, "parkour.items.checkpoint.name");
            if (itemName.equals(checkpointName.replace("&", "§"))) {
                event.setCancelled(true);
                plugin.getParkourManager().teleportToLastCheckpoint(player);
            }
        }
        // Reiniciar (WATCH)
        else if (event.getItem().getType() == Material.WATCH) {
            String restartName = plugin.getLanguageManager().getMessage(playerLang, "parkour.items.restart.name");
            if (itemName.equals(restartName.replace("&", "§"))) {
                event.setCancelled(true);
                plugin.getParkourManager().restart(player);
            }
        }
        // Cancelar (BARRIER)
        else if (event.getItem().getType() == Material.WOOD_DOOR) {
            String cancelName = plugin.getLanguageManager().getMessage(playerLang, "parkour.items.cancel.name");
            if (itemName.equals(cancelName.replace("&", "§"))) {
                event.setCancelled(true);
                plugin.getParkourManager().cancel(player);
            }
        }
    }

    /**
     * Remove jogador do parkour ao sair
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getParkourManager().removePlayer(event.getPlayer());
    }
}