package github.dimazbtw.lobby.listeners;

import github.dimazbtw.lobby.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class JoinItemsListener implements Listener {

    private final Main plugin;

    public JoinItemsListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay para garantir que tudo foi carregado
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getJoinItemsManager().giveJoinItems(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getJoinItemsManager().removePlayer(player);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        String itemName = item.getItemMeta().getDisplayName();
        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);

        // Verificar qual item foi clicado
        // Slot 0 - Modos de jogo (COMPASS)
        if (item.getType() == Material.COMPASS) {
            String gamemodesName = plugin.getLanguageManager().getMessage(playerLang, "join-items.gamemodes.name");
            if (itemName.equals(gamemodesName.replace("&", "§"))) {
                event.setCancelled(true);
                // TODO: Abrir menu de modos de jogo
                plugin.getMenuManager().openMenu(player, "game-modes");
                return;
            }
        }

        // Slot 1 - Perfil (SKULL_ITEM)
        if (item.getType() == Material.SKULL_ITEM) {
            String profileName = plugin.getLanguageManager().getMessage(playerLang, "join-items.profile.name");
            if (itemName.contains(profileName.replace("&", "§").split(" ")[0])) {
                event.setCancelled(true);
                // TODO: Abrir menu de perfil
                plugin.getMenuManager().openMenu(player, "profile");
                return;
            }
        }

        // Slot 4 - PvP (DIAMOND_SWORD) - Abre o menu de PvP
        if (item.getType() == Material.DIAMOND_SWORD) {
            String pvpName = plugin.getLanguageManager().getMessage(playerLang, "join-items.pvp.name");
            if (itemName.equals(pvpName.replace("&", "§"))) {
                event.setCancelled(true);
                plugin.getMenuManager().openMenu(player, "pvp-menu");
                return;
            }
        }

        // Slot 7 - Alternar visibilidade (INK_SACK/DYE)
        if (item.getType() == Material.INK_SACK) {
            event.setCancelled(true);
            plugin.getJoinItemsManager().toggleVisibility(player);

            boolean visibility = plugin.getJoinItemsManager().getVisibility(player);
            String messageKey = visibility ? "join-items.visibility.toggled-on" : "join-items.visibility.toggled-off";
            plugin.getLanguageManager().sendMessage(player, messageKey);
            return;
        }

        // Slot 8 - Lobbies (NETHER_STAR)
        if (item.getType() == Material.NETHER_STAR) {
            String lobbiesName = plugin.getLanguageManager().getMessage(playerLang, "join-items.lobbies.name");
            if (itemName.equals(lobbiesName.replace("&", "§"))) {
                event.setCancelled(true);
                // TODO: Abrir menu de lobbies
                plugin.getMenuManager().openMenu(player, "lobbies");
                return;
            }
        }
    }
}