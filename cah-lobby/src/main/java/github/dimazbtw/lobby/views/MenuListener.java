package github.dimazbtw.lobby.views;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.views.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MenuListener implements Listener {

    private final Main plugin;

    public MenuListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Previne cliques em menus customizados
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Verificar se está em algum menu
        for (Menu menu : plugin.getMenuManager().getAllMenus()) {
            if (menu.isViewing(player)) {
                event.setCancelled(true);

                // Se clicou em um slot vazio, ignorar
                if (event.getCurrentItem() == null || event.getCurrentItem().getType().name().equals("AIR")) {
                    return;
                }

                // Executar ações do item clicado
                menu.executeActions(player, event.getSlot());
                return;
            }
        }
    }

    /**
     * Remove jogador do cache quando fecha o menu
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // Remover jogador de todos os menus
        for (Menu menu : plugin.getMenuManager().getAllMenus()) {
            if (menu.isViewing(player)) {
                menu.removePlayer(player);
            }
        }
    }
}