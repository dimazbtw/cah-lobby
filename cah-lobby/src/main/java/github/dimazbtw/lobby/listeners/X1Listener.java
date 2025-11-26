package github.dimazbtw.lobby.listeners;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.managers.X1Manager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class X1Listener implements Listener {

    private final Main plugin;

    public X1Listener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        X1Manager x1Manager = plugin.getX1Manager();

        if (!x1Manager.isInX1(player)) return;

        // Se está em X1 mas não pode receber dano (countdown)
        if (!x1Manager.canTakeDamage(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        X1Manager x1Manager = plugin.getX1Manager();

        // Verificar se ambos estão no mesmo X1
        if (x1Manager.isInX1(victim) && x1Manager.isInX1(attacker)) {
            X1Manager.X1Match victimMatch = x1Manager.getMatch(victim);
            X1Manager.X1Match attackerMatch = x1Manager.getMatch(attacker);

            // Se estão na mesma partida
            if (victimMatch == attackerMatch) {
                // Permitir dano apenas se está em FIGHTING
                if (!x1Manager.canTakeDamage(victim)) {
                    event.setCancelled(true);
                }
                return;
            }
        }

        // Se apenas um está em X1, cancelar
        if (x1Manager.isInX1(victim) || x1Manager.isInX1(attacker)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        X1Manager x1Manager = plugin.getX1Manager();

        if (!x1Manager.isInX1(victim)) return;

        // Limpar drops
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        // Processar kill
        if (killer != null && x1Manager.isInX1(killer)) {
            x1Manager.handleKill(killer, victim);
        } else {
            // Morreu por outro motivo (queda, etc) - oponente ganha
            X1Manager.X1Match match = x1Manager.getMatch(victim);
            if (match != null) {
                x1Manager.forfeitMatch(victim);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Se estava em X1, o X1Manager já cuidou do teleporte
        // Definir spawn para o lobby
        if (plugin.getLobbyCommand().hasSpawn()) {
            event.setRespawnLocation(plugin.getLobbyCommand().getSpawn());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getX1Manager().handleDisconnect(player);
    }
}