package github.dimazbtw.lobby.listeners;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.managers.X1Manager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PvPListener implements Listener {

    private final Main plugin;

    public PvPListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Marca jogadores em combate quando há PvP
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvPDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // Se estão em X1, não marcar combat tag (X1 tem sua própria lógica)
        if (plugin.getX1Manager().isInX1(attacker) && plugin.getX1Manager().isInX1(victim)) {
            return;
        }

        // Verificar se ambos estão em modo PvP (Arena)
        if (!plugin.getPvPManager().isPvPEnabled(attacker) || !plugin.getPvPManager().isPvPEnabled(victim)) {
            return;
        }

        // Marcar ambos como em combate
        plugin.getPvPManager().tagCombat(attacker);
        plugin.getPvPManager().tagCombat(victim);
    }

    /**
     * Processa morte no PvP
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // ========== VERIFICAR X1 PRIMEIRO ==========
        X1Manager x1Manager = plugin.getX1Manager();
        if (x1Manager.isInX1(victim)) {
            // Limpar drops
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setDeathMessage(null);

            // Processar kill do X1
            if (killer != null && x1Manager.isInX1(killer)) {
                x1Manager.handleKill(killer, victim);
            } else {
                // Morreu por outro motivo (queda, etc) - oponente ganha
                x1Manager.forfeitMatch(victim);
            }
            return;
        }

        // ========== VERIFICAR PVP ARENA ==========
        if (!plugin.getPvPManager().isPvPEnabled(victim)) {
            return;
        }

        // Limpar drops e exp sempre que morrer em PvP
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Verificar se foi morte por outro jogador em PvP
        if (killer != null && plugin.getPvPManager().isPvPEnabled(killer)) {
            // Processar kill
            plugin.getPvPManager().handleKill(killer, victim);

            // Customizar mensagem de morte PvP
            String deathMessage = plugin.getLanguageManager()
                    .getMessage(plugin.getLanguageManager().getPlayerLanguage(victim), "pvp.death-broadcast")
                    .replace("{victim}", victim.getName())
                    .replace("{killer}", killer.getName())
                    .replace("{streak}", String.valueOf(plugin.getPvPManager().getKillStreak(killer)));

            event.setDeathMessage(deathMessage);
        } else {
            // Morte por ambiente (queda, fogo, etc) no modo PvP
            String deathMessage = plugin.getLanguageManager()
                    .getMessage(plugin.getLanguageManager().getPlayerLanguage(victim), "pvp.death-environment")
                    .replace("{victim}", victim.getName());

            event.setDeathMessage(deathMessage);
        }

        // DESATIVAR PvP da vítima
        plugin.getPvPManager().forceDisablePvP(victim);
    }

    /**
     * Respawn no spawn do lobby com itens normais de join
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Teleportar para o spawn se existir
        if (plugin.getLobbyCommand() != null && plugin.getLobbyCommand().hasSpawn()) {
            event.setRespawnLocation(plugin.getLobbyCommand().getSpawn());
        }

        // Dar itens de join e restaurar status após 1 tick
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // Garantir que inventário está limpo
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);

                // Dar itens de join normais
                plugin.getJoinItemsManager().giveJoinItems(player);

                // Restaurar vida e fome
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setSaturation(20F);
                player.setFireTicks(0);

                // Atualizar visibilidade
                plugin.getPvPManager().updatePlayerVisibility(player);
            }
        }, 1L);
    }
}