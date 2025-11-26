package github.dimazbtw.lobby.managers;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.database.X1Stats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import github.andredimaz.plugin.core.utils.basics.TitleUtils;
import java.util.*;

public class X1Manager {

    private final Main plugin;
    private final Map<UUID, UUID> pendingInvites = new HashMap<>(); // invited -> inviter
    private final Map<UUID, Long> inviteExpiry = new HashMap<>();
    private final Set<UUID> queue = new LinkedHashSet<>();
    private final Map<UUID, X1Match> activeMatches = new HashMap<>(); // player -> match
    private BukkitTask queueTask;
    private BukkitTask inviteCleanupTask;
    private static final long INVITE_TIMEOUT = 30000; // 30 segundos

    public X1Manager(Main plugin) {
        this.plugin = plugin;
        startQueueTask();
        startInviteCleanupTask();
    }

    // ==================== CONVITES ====================

    public void sendInvite(Player sender, Player target) {
        // Verificar se já tem convite pendente
        if (pendingInvites.containsKey(target.getUniqueId())) {
            UUID existingInviter = pendingInvites.get(target.getUniqueId());
            if (existingInviter.equals(sender.getUniqueId())) {
                plugin.getLanguageManager().sendMessage(sender, "x1.already-invited");
                return;
            }
        }

        // Verificar se o alvo já convidou o sender (aceitar automaticamente)
        if (pendingInvites.containsKey(sender.getUniqueId()) &&
                pendingInvites.get(sender.getUniqueId()).equals(target.getUniqueId())) {
            acceptInvite(sender);
            return;
        }

        pendingInvites.put(target.getUniqueId(), sender.getUniqueId());
        inviteExpiry.put(target.getUniqueId(), System.currentTimeMillis() + INVITE_TIMEOUT);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        plugin.getLanguageManager().sendMessage(sender, "x1.invite-sent", placeholders);

        Map<String, String> targetPlaceholders = new HashMap<>();
        targetPlaceholders.put("player", sender.getName());
        plugin.getLanguageManager().sendMessage(target, "x1.invite-received", targetPlaceholders);
        target.sendMessage("§a/x1 aceitar §7- Aceitar | §c/x1 recusar §7- Recusar");
    }

    public void acceptInvite(Player player) {
        UUID inviterUUID = pendingInvites.remove(player.getUniqueId());
        inviteExpiry.remove(player.getUniqueId());

        if (inviterUUID == null) {
            plugin.getLanguageManager().sendMessage(player, "x1.no-invite");
            return;
        }

        Player inviter = Bukkit.getPlayer(inviterUUID);
        if (inviter == null || !inviter.isOnline()) {
            plugin.getLanguageManager().sendMessage(player, "x1.inviter-offline");
            return;
        }

        // Verificar posições
        if (!plugin.getCombatCommands().hasX1Positions()) {
            plugin.getLanguageManager().sendMessage(player, "x1.no-positions");
            plugin.getLanguageManager().sendMessage(inviter, "x1.no-positions");
            return;
        }

        // Iniciar partida
        startMatch(inviter, player);
    }

    public void denyInvite(Player player) {
        UUID inviterUUID = pendingInvites.remove(player.getUniqueId());
        inviteExpiry.remove(player.getUniqueId());

        if (inviterUUID == null) {
            plugin.getLanguageManager().sendMessage(player, "x1.no-invite");
            return;
        }

        Player inviter = Bukkit.getPlayer(inviterUUID);

        plugin.getLanguageManager().sendMessage(player, "x1.invite-denied");

        if (inviter != null && inviter.isOnline()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            plugin.getLanguageManager().sendMessage(inviter, "x1.invite-denied-other", placeholders);
        }
    }

    // ==================== FILA ====================

    public void joinQueue(Player player) {
        if (queue.contains(player.getUniqueId())) {
            plugin.getLanguageManager().sendMessage(player, "x1.already-in-queue");
            return;
        }

        queue.add(player.getUniqueId());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("position", String.valueOf(queue.size()));
        plugin.getLanguageManager().sendMessage(player, "x1.joined-queue", placeholders);
    }

    public void leaveQueue(Player player) {
        queue.remove(player.getUniqueId());
    }

    public boolean isInQueue(Player player) {
        return queue.contains(player.getUniqueId());
    }

    private void startQueueTask() {
        queueTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (queue.size() < 2) return;

            // Verificar posições
            if (!plugin.getCombatCommands().hasX1Positions()) return;

            Iterator<UUID> iterator = queue.iterator();
            UUID first = iterator.next();
            iterator.remove();
            UUID second = iterator.next();
            iterator.remove();

            Player player1 = Bukkit.getPlayer(first);
            Player player2 = Bukkit.getPlayer(second);

            if (player1 == null || !player1.isOnline()) {
                if (player2 != null) queue.add(second);
                return;
            }

            if (player2 == null || !player2.isOnline()) {
                queue.add(first);
                return;
            }

            startMatch(player1, player2);
        }, 40L, 40L); // Verificar a cada 2 segundos
    }

    private void startInviteCleanupTask() {
        inviteCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, Long>> it = inviteExpiry.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<UUID, Long> entry = it.next();
                if (now >= entry.getValue()) {
                    UUID targetUUID = entry.getKey();
                    UUID inviterUUID = pendingInvites.remove(targetUUID);
                    it.remove();

                    Player target = Bukkit.getPlayer(targetUUID);
                    Player inviter = inviterUUID != null ? Bukkit.getPlayer(inviterUUID) : null;

                    if (target != null && target.isOnline()) {
                        plugin.getLanguageManager().sendMessage(target, "x1.invite-expired");
                    }
                    if (inviter != null && inviter.isOnline()) {
                        plugin.getLanguageManager().sendMessage(inviter, "x1.invite-expired-sender");
                    }
                }
            }
        }, 20L, 20L);
    }

    // ==================== PARTIDA ====================

    public void startMatch(Player player1, Player player2) {
        // Remover da fila se estiverem
        queue.remove(player1.getUniqueId());
        queue.remove(player2.getUniqueId());

        // Remover convites pendentes
        pendingInvites.remove(player1.getUniqueId());
        pendingInvites.remove(player2.getUniqueId());
        inviteExpiry.remove(player1.getUniqueId());
        inviteExpiry.remove(player2.getUniqueId());

        Location pos1 = plugin.getCombatCommands().getX1Position1();
        Location pos2 = plugin.getCombatCommands().getX1Position2();

        X1Match match = new X1Match(player1.getUniqueId(), player2.getUniqueId());
        activeMatches.put(player1.getUniqueId(), match);
        activeMatches.put(player2.getUniqueId(), match);

        // Limpar inventários
        player1.getInventory().clear();
        player1.getInventory().setArmorContents(null);
        player2.getInventory().clear();
        player2.getInventory().setArmorContents(null);

        // Dar kit
        giveX1Kit(player1);
        giveX1Kit(player2);

        // Teleportar
        player1.teleport(pos1);
        player2.teleport(pos2);

        // Restaurar vida
        player1.setHealth(player1.getMaxHealth());
        player1.setFoodLevel(20);
        player1.setSaturation(20F);
        player2.setHealth(player2.getMaxHealth());
        player2.setFoodLevel(20);
        player2.setSaturation(20F);

        // Mensagens
        Map<String, String> p1Placeholders = new HashMap<>();
        p1Placeholders.put("opponent", player2.getName());
        plugin.getLanguageManager().sendMessage(player1, "x1.match-started", p1Placeholders);

        Map<String, String> p2Placeholders = new HashMap<>();
        p2Placeholders.put("opponent", player1.getName());
        plugin.getLanguageManager().sendMessage(player2, "x1.match-started", p2Placeholders);

        // Contagem regressiva
        startCountdown(match);
    }

    private void startCountdown(X1Match match) {
        match.setState(X1Match.State.COUNTDOWN);

        Player p1 = Bukkit.getPlayer(match.getPlayer1());
        Player p2 = Bukkit.getPlayer(match.getPlayer2());

        for (int i = 3; i >= 1; i--) {
            final int count = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p1 != null && p1.isOnline()) TitleUtils.sendTitle(p1, "§e" + count, "§7Prepare-se!", 0, 20, 0);
                if (p2 != null && p2.isOnline()) TitleUtils.sendTitle(p2, "§e" + count, "§7Prepare-se!", 0, 20, 0);
            }, (3 - i) * 20L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (match.getState() == X1Match.State.COUNTDOWN) {
                match.setState(X1Match.State.FIGHTING);
                if (p1 != null && p1.isOnline()) TitleUtils.sendTitle(p1, "§a§lLUTE!", "", 0, 20, 10);
                if (p2 != null && p2.isOnline()) TitleUtils.sendTitle(p2, "§a§lLUTE!", "", 0, 20, 10);
            }
        }, 60L);
    }

    private void giveX1Kit(Player player) {
        // Armadura
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);
        ItemStack chestplate = new ItemStack(Material.IRON_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.IRON_LEGGINGS);
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        // Espada
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName("§c§lEspada X1");
        sword.setItemMeta(swordMeta);
        player.getInventory().setItem(0, sword);

        // Golden Apples
        ItemStack apples = new ItemStack(Material.GOLDEN_APPLE, 3);
        player.getInventory().setItem(1, apples);

        player.updateInventory();
    }

    public void handleKill(Player killer, Player victim) {
        X1Match match = activeMatches.get(killer.getUniqueId());
        if (match == null || match.getState() != X1Match.State.FIGHTING) return;

        endMatch(match, killer.getUniqueId(), victim.getUniqueId());
    }

    public void forfeitMatch(Player player) {
        X1Match match = activeMatches.get(player.getUniqueId());
        if (match == null) return;

        UUID opponentUUID = match.getPlayer1().equals(player.getUniqueId()) ?
                match.getPlayer2() : match.getPlayer1();

        endMatch(match, opponentUUID, player.getUniqueId());
    }

    private void endMatch(X1Match match, UUID winnerUUID, UUID loserUUID) {
        match.setState(X1Match.State.ENDED);
        activeMatches.remove(match.getPlayer1());
        activeMatches.remove(match.getPlayer2());

        Player winner = Bukkit.getPlayer(winnerUUID);
        Player loser = Bukkit.getPlayer(loserUUID);

        // Salvar estatísticas
        if (winner != null) {
            plugin.getStatsDatabase().addX1Win(winner);
        }
        if (loser != null) {
            plugin.getStatsDatabase().addX1Loss(loser);
        }

        // Mensagens
        String winnerName = winner != null ? winner.getName() : "Desconhecido";
        String loserName = loser != null ? loser.getName() : "Desconhecido";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("winner", winnerName);
        placeholders.put("loser", loserName);

        if (winner != null && winner.isOnline()) {
            TitleUtils.sendTitle(winner, "§a§lVITÓRIA!", "§7Você venceu o X1!", 0, 60, 20);
            plugin.getLanguageManager().sendMessage(winner, "x1.you-won", placeholders);

            // Restaurar
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (winner.isOnline()) {
                    winner.getInventory().clear();
                    winner.getInventory().setArmorContents(null);
                    plugin.getJoinItemsManager().giveJoinItems(winner);
                    if (plugin.getLobbyCommand().hasSpawn()) {
                        winner.teleport(plugin.getLobbyCommand().getSpawn());
                    }
                }
            }, 60L);
        }

        if (loser != null && loser.isOnline()) {
            TitleUtils.sendTitle(loser, "§c§lDERROTA!", "§7Você perdeu o X1!", 0, 60, 20);
            plugin.getLanguageManager().sendMessage(loser, "x1.you-lost", placeholders);

            // Restaurar
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (loser.isOnline()) {
                    loser.getInventory().clear();
                    loser.getInventory().setArmorContents(null);
                    loser.setHealth(loser.getMaxHealth());
                    plugin.getJoinItemsManager().giveJoinItems(loser);
                    if (plugin.getLobbyCommand().hasSpawn()) {
                        loser.teleport(plugin.getLobbyCommand().getSpawn());
                    }
                }
            }, 60L);
        }

        // Broadcast
        for (Player online : Bukkit.getOnlinePlayers()) {
            plugin.getLanguageManager().sendMessage(online, "x1.match-result", placeholders);
        }
    }

    public void handleDisconnect(Player player) {
        // Remover da fila
        queue.remove(player.getUniqueId());

        // Remover convites
        pendingInvites.remove(player.getUniqueId());
        inviteExpiry.remove(player.getUniqueId());

        // Se estava em partida, o oponente ganha
        X1Match match = activeMatches.get(player.getUniqueId());
        if (match != null) {
            UUID opponentUUID = match.getPlayer1().equals(player.getUniqueId()) ?
                    match.getPlayer2() : match.getPlayer1();
            endMatch(match, opponentUUID, player.getUniqueId());
        }
    }

    public boolean isInX1(Player player) {
        return activeMatches.containsKey(player.getUniqueId());
    }

    public X1Match getMatch(Player player) {
        return activeMatches.get(player.getUniqueId());
    }

    public boolean canTakeDamage(Player player) {
        X1Match match = activeMatches.get(player.getUniqueId());
        return match != null && match.getState() == X1Match.State.FIGHTING;
    }

    public void stopTasks() {
        if (queueTask != null) queueTask.cancel();
        if (inviteCleanupTask != null) inviteCleanupTask.cancel();
    }

    // ==================== CLASSE INTERNA ====================

    public static class X1Match {
        public enum State { COUNTDOWN, FIGHTING, ENDED }

        private final UUID player1;
        private final UUID player2;
        private State state;
        private final long startTime;

        public X1Match(UUID player1, UUID player2) {
            this.player1 = player1;
            this.player2 = player2;
            this.state = State.COUNTDOWN;
            this.startTime = System.currentTimeMillis();
        }

        public UUID getPlayer1() { return player1; }
        public UUID getPlayer2() { return player2; }
        public State getState() { return state; }
        public void setState(State state) { this.state = state; }
        public long getStartTime() { return startTime; }
    }
}