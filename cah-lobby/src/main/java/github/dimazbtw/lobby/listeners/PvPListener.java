package github.dimazbtw.lobby.listeners;

import github.dimazbtw.lobby.Main;
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
     * Previne dano entre jogadores que não estão em modo PvP
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Verificar se é PvP (jogador atacando jogador)
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        boolean attackerInPvP = plugin.getPvPManager().isPvPEnabled(attacker);
        boolean victimInPvP = plugin.getPvPManager().isPvPEnabled(victim);

        // Ambos devem estar em modo PvP
        if (!attackerInPvP || !victimInPvP) {
            event.setCancelled(true);

            if (attackerInPvP && !victimInPvP) {
                plugin.getLanguageManager().sendMessage(attacker, "pvp.target-not-in-pvp");
            }
        }
    }

    /**
     * Processa morte no PvP
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Verificar se é morte no PvP
        if (killer == null || !plugin.getPvPManager().isPvPEnabled(victim)) {
            return;
        }

        // Processar kill
        if (plugin.getPvPManager().isPvPEnabled(killer)) {
            plugin.getPvPManager().handleKill(killer, victim);
        }

        // Limpar drops no lobby
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Customizar mensagem de morte
        String deathMessage = plugin.getLanguageManager()
                .getMessage(plugin.getLanguageManager().getPlayerLanguage(victim), "pvp.death-broadcast")
                .replace("{victim}", victim.getName())
                .replace("{killer}", killer.getName())
                .replace("{streak}", String.valueOf(plugin.getPvPManager().getKillStreak(killer)));

        event.setDeathMessage(deathMessage);
    }

    /**
     * Respawn rápido no lobby
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Se estava em PvP, respawnar no spawn do lobby
        if (plugin.getPvPManager().isPvPEnabled(player)) {
            // Tentar teleportar para o spawn (se existir)
            if (plugin.getLobbyCommand() != null && plugin.getLobbyCommand().hasSpawn()) {
                event.setRespawnLocation(plugin.getLobbyCommand().getSpawn());
            }
        }
    }
}