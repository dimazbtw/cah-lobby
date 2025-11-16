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

        // Verificar proteção global (Totalmente desativar danos)
        if (plugin.getConfig().getBoolean("protection.disable-damage", true)) {
            event.setCancelled(true);
            player.setHealth(player.getMaxHealth());
            player.setFireTicks(0);
            return;
        }

        // Se o jogador NÃO estiver no modo PvP, cancelar o dano
        if (!pvpManager.isPvPEnabled(player)) {
            event.setCancelled(true);
            return;
        }

        // Caso contrário, permitir dano normal
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = null;

        Entity damager = event.getDamager();

        // Se for dano direto entre jogadores
        if (damager instanceof Player) {
            attacker = (Player) damager;
        }
        // Se o atacante usar projétil (ex: flecha)
        else if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
            }
        }
        // Se o atacante estiver montado em algo (Suporte, por ex.)
        else if (damager.getPassenger() instanceof Player) {
            attacker = (Player) damager.getPassenger();
        }

        // Se o atacante não é um jogador, não bloqueamos
        if (attacker == null) {
            return;
        }

        // Se a proteção geral de PvP no lobby estiver ativada
        if (plugin.getConfig().getBoolean("protection.disable-damage", true)) {
            event.setCancelled(true);
            return;
        }

        // Usar PvPManager para decidir
        PvPManager pvpManager = plugin.getPvPManager();

        boolean attackerPvP = pvpManager.isPvPEnabled(attacker);
        boolean victimPvP = pvpManager.isPvPEnabled(victim);

        // Se algum dos dois não estiver com PvP ativo, cancelar o dano
        if (!attackerPvP || !victimPvP) {
            event.setCancelled(true);
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