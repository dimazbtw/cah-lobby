package github.dimazbtw.lobby.listeners;

import github.dimazbtw.lobby.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class AdditionalListener implements Listener {

    private final Main plugin;

    public AdditionalListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Carregar dados do jogador
        plugin.getPlayerDataManager().loadPlayerData(player);

        // Mensagem de entrada customizada
        if (plugin.getConfig().getBoolean("join-message.enabled", true)) {
            String message = plugin.getConfig().getString("join-message.message", "§a{player} entrou no servidor!")
                    .replace("{player}", player.getName())
                    .replace("&", "§");
            event.setJoinMessage(message);
        } else {
            event.setJoinMessage(null);
        }

        // Limpar chat ao entrar
        if (plugin.getConfig().getBoolean("clear-chat-on-join", false)) {
            for (int i = 0; i < 100; i++) {
                player.sendMessage("");
            }
        }

        // Garantir vida e fome cheias
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20F);
        player.setFireTicks(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Salvar dados do jogador
        plugin.getPlayerDataManager().savePlayerData(player);
        plugin.getPlayerDataManager().unloadPlayerData(player.getUniqueId());

        // Mensagem de saída customizada
        if (plugin.getConfig().getBoolean("quit-message.enabled", true)) {
            String message = plugin.getConfig().getString("quit-message.message", "§c{player} saiu do servidor!")
                    .replace("{player}", player.getName())
                    .replace("&", "§");
            event.setQuitMessage(message);
        } else {
            event.setQuitMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Auto respawn
        if (plugin.getConfig().getBoolean("auto-respawn", true)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.spigot().respawn();
            }, 1L);
        }

        // Limpar drops se keep-inventory estiver ativo
        if (plugin.getConfig().getBoolean("protection.keep-inventory", true)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        // Mensagem de morte customizada (opcional)
        event.setDeathMessage(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Verificar se está em modo admin
        if (!plugin.getPlayerDataManager().isAdminMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Verificar se está em modo admin
        if (!plugin.getPlayerDataManager().isAdminMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // Apenas jogadores com permissão podem dropar itens
        if (!player.hasPermission("lobby.drop")) {
            if (plugin.getConfig().getBoolean("protection.disable-damage", true)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();

        // Apenas jogadores com permissão podem pegar itens
        if (!player.hasPermission("lobby.pickup")) {
            if (plugin.getConfig().getBoolean("protection.disable-damage", true)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("commands.blocked-commands.enabled", false)) {
            return;
        }

        String command = event.getMessage().toLowerCase().split(" ")[0].replace("/", "");

        if (plugin.getConfig().getStringList("commands.blocked-commands.list").contains(command)) {
            if (!event.getPlayer().hasPermission("lobby.bypass.blockedcommands")) {
                event.setCancelled(true);
                String message = plugin.getConfig().getString("commands.blocked-message", "§cVocê não pode usar este comando!")
                        .replace("&", "§");
                event.getPlayer().sendMessage(message);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.anti-swear.enabled", false)) {
            return;
        }

        String message = event.getMessage();

        for (String word : plugin.getConfig().getStringList("chat.anti-swear.words")) {
            if (message.toLowerCase().contains(word.toLowerCase())) {
                String replacement = plugin.getConfig().getString("chat.anti-swear.replacement", "***");
                message = message.replaceAll("(?i)" + word, replacement);
            }
        }

        event.setMessage(message);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        // Garantir que jogadores mantenham vida cheia se regeneração natural estiver desativada
        if (event.getEntity() instanceof Player) {
            if (plugin.getConfig().getBoolean("protection.disable-natural-regeneration", false)) {
                if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED ||
                        event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
                    event.setCancelled(true);
                }
            }
        }
    }
}