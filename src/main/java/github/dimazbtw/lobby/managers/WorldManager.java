package github.dimazbtw.lobby.managers;

import github.dimazbtw.lobby.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class WorldManager {

    private final Main plugin;

    public WorldManager(Main plugin) {
        this.plugin = plugin;
        setupWorlds();
    }

    /**
     * Configura as regras dos mundos de acordo com a config
     */
    public void setupWorlds() {
        // Desativar ciclo do dia em todos os mundos
        if (plugin.getConfig().getBoolean("protection.disable-day-cycle", true)) {
            for (World world : Bukkit.getWorlds()) {
                world.setGameRuleValue("doDaylightCycle", "false");

                // Definir hora do dia (meio-dia por padrão)
                long time = plugin.getConfig().getLong("protection.fixed-time", 6000);
                world.setTime(time);
            }
        }

        // Desativar clima
        if (plugin.getConfig().getBoolean("protection.disable-weather-change", true)) {
            for (World world : Bukkit.getWorlds()) {
                world.setStorm(false);
                world.setThundering(false);
                world.setWeatherDuration(0);
            }
        }

        // Desativar spawn de mobs
        if (plugin.getConfig().getBoolean("protection.disable-mob-spawn", true)) {
            for (World world : Bukkit.getWorlds()) {
                world.setSpawnFlags(false, false);
            }
        }

        // Outras configurações de mundo
        for (World world : Bukkit.getWorlds()) {
            // Desativar fogo se espalhando
            if (plugin.getConfig().getBoolean("protection.disable-fire-spread", true)) {
                world.setGameRuleValue("doFireTick", "false");
            }

            // Desativar mob griefing (creepers, enderman, etc)
            if (plugin.getConfig().getBoolean("protection.disable-mob-griefing", true)) {
                world.setGameRuleValue("mobGriefing", "false");
            }

            // Manter inventário ao morrer
            if (plugin.getConfig().getBoolean("protection.keep-inventory", true)) {
                world.setGameRuleValue("keepInventory", "true");
            }

            // Desativar regeneração natural
            if (plugin.getConfig().getBoolean("protection.disable-natural-regeneration", false)) {
                world.setGameRuleValue("naturalRegeneration", "false");
            }

            // Anunciar conquistas
            world.setGameRuleValue("announceAdvancements",
                    String.valueOf(plugin.getConfig().getBoolean("protection.announce-advancements", false)));
        }

        plugin.getLogger().info("Configurações de proteção de mundo aplicadas!");
    }

    /**
     * Atualiza as configurações de um mundo específico
     */
    public void updateWorld(World world) {
        if (plugin.getConfig().getBoolean("protection.disable-day-cycle", true)) {
            world.setGameRuleValue("doDaylightCycle", "false");
            long time = plugin.getConfig().getLong("protection.fixed-time", 6000);
            world.setTime(time);
        }

        if (plugin.getConfig().getBoolean("protection.disable-weather-change", true)) {
            world.setStorm(false);
            world.setThundering(false);
        }

        if (plugin.getConfig().getBoolean("protection.disable-mob-spawn", true)) {
            world.setSpawnFlags(false, false);
        }
    }

    /**
     * Task para manter o clima e ciclo do dia desativados
     */
    public void startMaintenanceTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                // Manter o tempo fixo
                if (plugin.getConfig().getBoolean("protection.disable-day-cycle", true)) {
                    long fixedTime = plugin.getConfig().getLong("protection.fixed-time", 6000);
                    if (world.getTime() != fixedTime) {
                        world.setTime(fixedTime);
                    }
                }

                // Manter clima desativado
                if (plugin.getConfig().getBoolean("protection.disable-weather-change", true)) {
                    if (world.hasStorm() || world.isThundering()) {
                        world.setStorm(false);
                        world.setThundering(false);
                        world.setWeatherDuration(0);
                    }
                }
            }
        }, 0L, 100L); // Executa a cada 5 segundos (100 ticks)
    }
}