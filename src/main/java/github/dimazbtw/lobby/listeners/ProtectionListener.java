package github.dimazbtw.lobby.listeners;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.managers.PvPManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public class ProtectionListener implements Listener {

    private final Main plugin;

    public ProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Definir modo de jogo padrão
        if (plugin.getConfig().getBoolean("protection.default-gamemode.enabled", true)) {
            String gamemodeName = plugin.getConfig().getString("protection.default-gamemode.mode", "ADVENTURE");

            try {
                GameMode gameMode = GameMode.valueOf(gamemodeName.toUpperCase());
                player.setGameMode(gameMode);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Modo de jogo inválido na config: " + gamemodeName);
            }
        }

        // Desativar fome ao entrar
        if (plugin.getConfig().getBoolean("protection.disable-hunger", true)) {
            player.setFoodLevel(20);
            player.setSaturation(20F);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        // Desativar fome
        if (plugin.getConfig().getBoolean("protection.disable-hunger", true)) {
            event.setCancelled(true);

            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                player.setFoodLevel(20);
                player.setSaturation(20F);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        PvPManager pvpManager = plugin.getPvPManager();

        // PRIORIDADE 1: Se o jogador estiver no modo PvP, SEMPRE permitir dano
        if (pvpManager != null && pvpManager.isPvPEnabled(player)) {
            return; // Permite QUALQUER dano (queda, fogo, combate, etc.)
        }

        // PRIORIDADE 2: Se NÃO estiver em PvP e a proteção global estiver ativa
        if (plugin.getConfig().getBoolean("protection.disable-damage", true)) {
            event.setCancelled(true);
            player.setHealth(player.getMaxHealth());
            player.setFireTicks(0);
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = null;

        Entity damager = event.getDamager();

        // Identificar o atacante
        if (damager instanceof Player) {
            attacker = (Player) damager;
        } else if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
            }
        } else if (damager.getPassenger() instanceof Player) {
            attacker = (Player) damager.getPassenger();
        }

        PvPManager pvpManager = plugin.getPvPManager();

        // Se o atacante não é um jogador (mob/ambiente)
        if (attacker == null) {
            // Se a vítima estiver em modo PvP, permitir dano de mobs/ambiente
            if (pvpManager != null && pvpManager.isPvPEnabled(victim)) {
                return;
            }
            // Caso contrário, verificar proteção global
            if (plugin.getConfig().getBoolean("protection.disable-damage", true)) {
                event.setCancelled(true);
            }
            return;
        }

        // A partir daqui, o atacante É um jogador
        if (pvpManager == null) {
            // Se o PvPManager não existe, aplicar proteção global
            if (plugin.getConfig().getBoolean("protection.disable-damage", true)) {
                event.setCancelled(true);
            }
            return;
        }

        boolean attackerPvP = pvpManager.isPvPEnabled(attacker);
        boolean victimPvP = pvpManager.isPvPEnabled(victim);

        // PRIORIDADE MÁXIMA: Se AMBOS estiverem em modo PvP, SEMPRE permitir
        if (attackerPvP && victimPvP) {
            return; // Permite PvP INCONDICIONALMENTE
        }

        // Se algum dos dois não estiver com PvP ativo, cancelar o dano
        event.setCancelled(true);

        // Informar o atacante se ele tentou atacar alguém sem PvP
        if (attackerPvP && !victimPvP) {
            plugin.getLanguageManager().sendMessage(attacker, "pvp.target-not-in-pvp");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        // Desativar mudança de clima
        if (plugin.getConfig().getBoolean("protection.disable-weather-change", true)) {
            if (event.toWeatherState()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Desativar spawn de entidades
        if (plugin.getConfig().getBoolean("protection.disable-mob-spawn", true)) {
            // Permitir spawns customizados e de plugins
            if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM &&
                    event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        // Desativar ignição de blocos
        if (plugin.getConfig().getBoolean("protection.disable-fire-spread", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        // Desativar blocos queimando
        if (plugin.getConfig().getBoolean("protection.disable-fire-spread", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        // Desativar fogo se espalhando
        if (plugin.getConfig().getBoolean("protection.disable-fire-spread", true)) {
            if (event.getSource().getType().toString().contains("FIRE")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Desativar dano de explosões
        if (plugin.getConfig().getBoolean("protection.disable-explosion-damage", true)) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        // Desativar explosões
        if (plugin.getConfig().getBoolean("protection.disable-explosion-damage", true)) {
            event.setCancelled(true);
        }
    }
}