package github.dimazbtw.lobby.managers;

import github.dimazbtw.lobby.Main;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PvPManager {

    private final Main plugin;
    private final Set<UUID> pvpPlayers = new HashSet<>();
    private final Map<UUID, Integer> killStreak = new HashMap<>();
    private final Map<UUID, Long> combatTag = new HashMap<>(); // Timestamp do último combate
    private static final long COMBAT_TIME = 5000; // 5 segundos em milissegundos

    public PvPManager(Main plugin) {
        this.plugin = plugin;
        startCombatTagChecker();
    }

    /**
     * Inicia a task que verifica e remove combat tags expirados
     */
    private void startCombatTagChecker() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();

            // Verificar todos os jogadores com combat tag
            for (UUID uuid : new HashSet<>(combatTag.keySet())) {
                long lastCombat = combatTag.get(uuid);

                // Se passou mais de 5 segundos, remover o tag
                if ((currentTime - lastCombat) >= COMBAT_TIME) {
                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        removeCombatTag(player);
                    } else {
                        combatTag.remove(uuid);
                    }
                }
            }
        }, 20L, 20L); // Verifica a cada segundo
    }

    /**
     * Ativa o modo PvP para um jogador
     */
    public void enablePvP(Player player) {
        if (isPvPEnabled(player)) {
            plugin.getLanguageManager().sendMessage(player, "pvp.already-enabled");
            return;
        }

        // Limpar inventário
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Dar kit PvP
        giveKit(player);

        // Adicionar à lista de PvP
        pvpPlayers.add(player.getUniqueId());
        killStreak.put(player.getUniqueId(), 0);

        // Mensagem de ativação
        plugin.getLanguageManager().sendMessage(player, "pvp.enabled");

        // Atualizar visibilidade
        updatePlayerVisibility(player);

        plugin.getLogger().info(player.getName() + " entrou no modo PvP");
    }

    /**
     * Desativa o modo PvP para um jogador
     */
    public void disablePvP(Player player) {
        if (!isPvPEnabled(player)) {
            return;
        }

        // Verificar se está em combate
        if (isInCombat(player)) {
            long timeLeft = getRemainingCombatTime(player);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(timeLeft / 1000));
            plugin.getLanguageManager().sendMessage(player, "pvp.combat-tag", placeholders);
            return;
        }

        // Remover da lista de PvP
        pvpPlayers.remove(player.getUniqueId());

        // Obter kill streak antes de remover
        int streak = killStreak.getOrDefault(player.getUniqueId(), 0);
        killStreak.remove(player.getUniqueId());

        // Remover combat tag
        combatTag.remove(player.getUniqueId());

        // Se o jogador está vivo (saiu por comando), dar itens e mensagem
        if (player.isOnline() && player.getHealth() > 0) {
            // Limpar inventário
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);

            // Dar items de join novamente
            plugin.getJoinItemsManager().giveJoinItems(player);

            // Mensagem de desativação
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("streak", String.valueOf(streak));
            plugin.getLanguageManager().sendMessage(player, "pvp.disabled", placeholders);
        }
        // Se o jogador está morto, o PvPListener cuidará do respawn e itens

        // Atualizar visibilidade
        updatePlayerVisibility(player);

        plugin.getLogger().info(player.getName() + " saiu do modo PvP (streak: " + streak + ")");
    }

    /**
     * Verifica se o jogador está em modo PvP
     */
    public boolean isPvPEnabled(Player player) {
        return pvpPlayers.contains(player.getUniqueId());
    }

    /**
     * Dá o kit PvP completo ao jogador
     */
    public void giveKit(Player player) {
        // Armadura de diamante completa
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);

        // Adicionar encantamentos básicos
        helmet.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        chestplate.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        leggings.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        boots.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        // Espada de diamante
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);

        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName("§c§lEspada de Combate");
        swordMeta.setLore(Arrays.asList(
                "§7Use para atacar seus oponentes",
                "§7e dominar o lobby!"
        ));
        sword.setItemMeta(swordMeta);

        player.getInventory().setItem(0, sword);

        // Maçã dourada
        giveGoldenApple(player);

        // Atualizar inventário
        player.updateInventory();
    }

    /**
     * Dá uma maçã dourada ao jogador
     */
    public void giveGoldenApple(Player player) {
        ItemStack apple = new ItemStack(Material.GOLDEN_APPLE, 1);

        ItemMeta appleMeta = apple.getItemMeta();
        appleMeta.setDisplayName("§6§lMaçã Dourada");
        appleMeta.setLore(Arrays.asList(
                "§7Use para recuperar vida",
                "§7durante o combate!"
        ));
        apple.setItemMeta(appleMeta);

        player.getInventory().addItem(apple);
    }

    /**
     * Processa um kill no PvP
     */
    public void handleKill(Player killer, Player victim) {
        if (!isPvPEnabled(killer) || !isPvPEnabled(victim)) {
            return;
        }

        // Incrementar kill streak do killer
        int streak = killStreak.getOrDefault(killer.getUniqueId(), 0) + 1;
        killStreak.put(killer.getUniqueId(), streak);

        // Dar maçã dourada ao killer
        giveGoldenApple(killer);

        // Resetar kill streak da vítima
        int victimStreak = killStreak.getOrDefault(victim.getUniqueId(), 0);
        killStreak.put(victim.getUniqueId(), 0);

        // Salvar estatísticas no banco de dados
        plugin.getStatsDatabase().addKill(killer, streak);
        plugin.getStatsDatabase().addDeath(victim);

        // Mensagens
        Map<String, String> killerPlaceholders = new HashMap<>();
        killerPlaceholders.put("victim", victim.getName());
        killerPlaceholders.put("streak", String.valueOf(streak));
        plugin.getLanguageManager().sendMessage(killer, "pvp.kill", killerPlaceholders);

        Map<String, String> victimPlaceholders = new HashMap<>();
        victimPlaceholders.put("killer", killer.getName());
        victimPlaceholders.put("streak", String.valueOf(victimStreak));
        plugin.getLanguageManager().sendMessage(victim, "pvp.death", victimPlaceholders);

        plugin.getLogger().info(killer.getName() + " matou " + victim.getName() + " no PvP (streak: " + streak + ")");
    }

    /**
     * Obtém o kill streak de um jogador
     */
    public int getKillStreak(Player player) {
        return killStreak.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Obtém todos os jogadores em modo PvP
     */
    public Set<UUID> getPvPPlayers() {
        return new HashSet<>(pvpPlayers);
    }

    /**
     * Limpa os dados de um jogador (quando sai do servidor)
     */
    public void removePlayer(Player player) {
        pvpPlayers.remove(player.getUniqueId());
        killStreak.remove(player.getUniqueId());
        combatTag.remove(player.getUniqueId());
    }

    /**
     * Marca um jogador como em combate
     */
    public void tagCombat(Player player) {
        if (!isPvPEnabled(player)) {
            return;
        }

        // Se o jogador já está em combate, não enviar a mensagem novamente
        boolean alreadyInCombat = isInCombat(player);

        combatTag.put(player.getUniqueId(), System.currentTimeMillis());

        if (!alreadyInCombat) {
            // Enviar mensagem apenas na primeira vez até o tag expirar
            plugin.getLanguageManager().sendMessage(player, "pvp.combat-start");
        }
    }

    /**
     * Verifica se um jogador está em combate
     */
    public boolean isInCombat(Player player) {
        if (!combatTag.containsKey(player.getUniqueId())) {
            return false;
        }

        long lastCombat = combatTag.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();

        return (currentTime - lastCombat) < COMBAT_TIME;
    }

    /**
     * Obtém o tempo restante de combate em milissegundos
     */
    public long getRemainingCombatTime(Player player) {
        if (!combatTag.containsKey(player.getUniqueId())) {
            return 0;
        }

        long lastCombat = combatTag.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastCombat;

        return Math.max(0, COMBAT_TIME - elapsed);
    }

    /**
     * Remove o combat tag de um jogador
     */
    public void removeCombatTag(Player player) {
        if (combatTag.remove(player.getUniqueId()) != null) {
            plugin.getLanguageManager().sendMessage(player, "pvp.combat-end");
        }
    }

    /**
     * Atualiza a visibilidade de um jogador em relação aos outros
     */
    public void updatePlayerVisibility(Player player) {
        boolean playerInPvP = isPvPEnabled(player);

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.equals(player)) continue;

            boolean onlineInPvP = isPvPEnabled(online);

            // Se ambos estão em PvP OU ambos estão fora do PvP = podem se ver
            if (playerInPvP == onlineInPvP) {
                player.showPlayer(online);
                online.showPlayer(player);
            } else {
                // Se um está em PvP e o outro não = não podem se ver
                player.hidePlayer(online);
                online.hidePlayer(player);
            }
        }
    }

    /**
     * Atualiza a visibilidade de TODOS os jogadores
     * Útil ao recarregar o plugin ou ao iniciar
     */
    public void updateAllPlayersVisibility() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerVisibility(player);
        }
    }

    /**
     * Recarrega as configurações
     */
    public void reload() {
        // Desativar PvP de todos os jogadores
        for (UUID uuid : new HashSet<>(pvpPlayers)) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disablePvP(player);
            }
        }

        pvpPlayers.clear();
        killStreak.clear();
    }

    public void forceDisablePvP(Player player) {
        pvpPlayers.remove(player.getUniqueId());
        killStreak.remove(player.getUniqueId());
        combatTag.remove(player.getUniqueId());
        updatePlayerVisibility(player);
    }

}