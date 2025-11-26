package github.dimazbtw.lobby.managers;

import github.andredimaz.plugin.core.utils.basics.ActionBar;
import github.dimazbtw.lobby.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ParkourManager {

    private final Main plugin;
    private final File parkourFile;
    private final File recordsFile;
    private FileConfiguration parkourConfig;
    private FileConfiguration recordsConfig;

    // Jogadores ativos no parkour
    private final Map<UUID, ParkourSession> activeSessions = new ConcurrentHashMap<>();

    // Recordes pessoais: UUID -> tempo em milissegundos
    private final Map<UUID, Long> personalRecords = new HashMap<>();

    // Top 10 recordes: posição -> (nome, tempo)
    private final List<ParkourRecord> topRecords = new ArrayList<>();

    public ParkourManager(Main plugin) {
        this.plugin = plugin;
        this.parkourFile = new File(plugin.getDataFolder(), "parkour.yml");
        this.recordsFile = new File(plugin.getDataFolder(), "parkour-records.yml");

        loadConfigs();
        loadRecords();
    }

    private void loadConfigs() {
        if (!parkourFile.exists()) {
            try {
                parkourFile.createNewFile();
                parkourConfig = YamlConfiguration.loadConfiguration(parkourFile);

                // Config padrão
                parkourConfig.set("enabled", true);
                parkourConfig.set("start-material", "IRON_PLATE");
                parkourConfig.set("checkpoint-material", "WOOD_PLATE");
                parkourConfig.set("finish-material", "GOLD_PLATE");

                parkourConfig.save(parkourFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            parkourConfig = YamlConfiguration.loadConfiguration(parkourFile);
        }

        if (!recordsFile.exists()) {
            try {
                recordsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        recordsConfig = YamlConfiguration.loadConfiguration(recordsFile);
    }

    private void loadRecords() {
        personalRecords.clear();
        topRecords.clear();

        // Carregar recordes pessoais
        if (recordsConfig.contains("personal")) {
            for (String uuidStr : recordsConfig.getConfigurationSection("personal").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                long time = recordsConfig.getLong("personal." + uuidStr + ".time");
                personalRecords.put(uuid, time);
            }
        }

        // Carregar top 10
        if (recordsConfig.contains("top")) {
            for (int i = 1; i <= 10; i++) {
                String path = "top." + i;
                if (recordsConfig.contains(path)) {
                    String name = recordsConfig.getString(path + ".name");
                    long time = recordsConfig.getLong(path + ".time");
                    topRecords.add(new ParkourRecord(name, time));
                }
            }
        }
    }

    private void saveRecords() {
        try {
            // Salvar recordes pessoais
            for (Map.Entry<UUID, Long> entry : personalRecords.entrySet()) {
                String uuidStr = entry.getKey().toString();
                recordsConfig.set("personal." + uuidStr + ".time", entry.getValue());

                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null) {
                    recordsConfig.set("personal." + uuidStr + ".name", player.getName());
                }
            }

            // Salvar top 10
            recordsConfig.set("top", null); // Limpar
            for (int i = 0; i < topRecords.size() && i < 10; i++) {
                ParkourRecord record = topRecords.get(i);
                String path = "top." + (i + 1);
                recordsConfig.set(path + ".name", record.getPlayerName());
                recordsConfig.set(path + ".time", record.getTime());
            }

            recordsConfig.save(recordsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inicia uma sessão de parkour
     */
    public void startParkour(Player player, Location startLocation) {
        if (!parkourConfig.getBoolean("enabled", true)) {
            return;
        }

        // Se já está no parkour, ignorar
        if (isInParkour(player)) {
            return;
        }

        ParkourSession session = new ParkourSession(player.getUniqueId(), startLocation);
        activeSessions.put(player.getUniqueId(), session);

        // Limpar inventário
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Dar itens de parkour
        giveParkourItems(player);

        // Mensagem de início
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("checkpoints", "0");
        plugin.getLanguageManager().sendMessage(player, "parkour.started", placeholders);
    }

    /**
     * Registra um checkpoint
     */
    public void checkpoint(Player player, Location checkpointLocation) {
        ParkourSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        session.addCheckpoint(checkpointLocation);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("checkpoint", String.valueOf(session.getCheckpoints().size()));
        plugin.getLanguageManager().sendMessage(player, "parkour.checkpoint", placeholders);
    }

    /**
     * Finaliza o parkour
     */
    public void finish(Player player) {
        ParkourSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        long time = session.getElapsedTime();
        String formattedTime = formatTime(time);

        // Verificar recorde pessoal
        boolean isPersonalRecord = false;
        Long previousBest = personalRecords.get(player.getUniqueId());

        if (previousBest == null || time < previousBest) {
            personalRecords.put(player.getUniqueId(), time);
            isPersonalRecord = true;
        }

        // Verificar top 10
        boolean isTopRecord = updateTopRecords(player.getName(), time);

        // Salvar recordes
        saveRecords();

        // Limpar inventário e dar join items
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        plugin.getJoinItemsManager().giveJoinItems(player);

        // Mensagens
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("time", formattedTime);

        if (isPersonalRecord) {
            plugin.getLanguageManager().sendMessage(player, "parkour.finished-record", placeholders);
        } else {
            plugin.getLanguageManager().sendMessage(player, "parkour.finished", placeholders);
        }

        if (isTopRecord) {
            plugin.getLanguageManager().sendMessage(player, "parkour.top-record", placeholders);
        }
    }

    /**
     * Teleporta para o último checkpoint
     */
    public void teleportToLastCheckpoint(Player player) {
        ParkourSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        Location checkpoint = session.getLastCheckpoint();
        if (checkpoint != null) {
            player.teleport(checkpoint);
            plugin.getLanguageManager().sendMessage(player, "parkour.teleported-checkpoint");
        } else {
            plugin.getLanguageManager().sendMessage(player, "parkour.no-checkpoint");
        }
    }

    /**
     * Reinicia o parkour
     */
    public void restart(Player player) {
        ParkourSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        Location start = session.getStartLocation();
        player.teleport(start);

        // Criar nova sessão
        activeSessions.put(player.getUniqueId(), new ParkourSession(player.getUniqueId(), start));

        plugin.getLanguageManager().sendMessage(player, "parkour.restarted");
    }

    /**
     * Cancela o parkour
     */
    public void cancel(Player player) {
        ParkourSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        // Limpar inventário e dar join items
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        plugin.getJoinItemsManager().giveJoinItems(player);

        plugin.getLanguageManager().sendMessage(player, "parkour.cancelled");
    }

    /**
     * Dá os itens de parkour ao jogador
     */
    private void giveParkourItems(Player player) {
        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);

        // Slot 3 - Teleportar checkpoint
        ItemStack checkpoint = createItem(
                Material.WOOD_PLATE,
                plugin.getLanguageManager().getMessage(playerLang, "parkour.items.checkpoint.name"),
                plugin.getLanguageManager().getMessageList(playerLang, "parkour.items.checkpoint.lore")
        );
        player.getInventory().setItem(2, checkpoint);

        // Slot 4 - Reiniciar
        ItemStack restart = createItem(
                Material.WATCH,
                plugin.getLanguageManager().getMessage(playerLang, "parkour.items.restart.name"),
                plugin.getLanguageManager().getMessageList(playerLang, "parkour.items.restart.lore")
        );
        player.getInventory().setItem(4, restart);

        // Slot 5 - Cancelar
        ItemStack cancel = createItem(
                Material.WOOD_DOOR,
                plugin.getLanguageManager().getMessage(playerLang, "parkour.items.cancel.name"),
                plugin.getLanguageManager().getMessageList(playerLang, "parkour.items.cancel.lore")
        );
        player.getInventory().setItem(6, cancel);

        player.updateInventory();
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name.replace("&", "§"));

            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(line.replace("&", "§"));
                }
                meta.setLore(coloredLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Atualiza os top recordes
     */
    private boolean updateTopRecords(String playerName, long time) {
        ParkourRecord newRecord = new ParkourRecord(playerName, time);

        // Verificar se já existe recorde deste jogador
        topRecords.removeIf(record -> record.getPlayerName().equals(playerName));

        // Adicionar novo recorde
        topRecords.add(newRecord);

        // Ordenar por tempo (menor primeiro)
        topRecords.sort(Comparator.comparingLong(ParkourRecord::getTime));

        // Manter apenas top 10
        if (topRecords.size() > 10) {
            topRecords.subList(10, topRecords.size()).clear();
        }

        // Verificar se está no top 10
        return topRecords.contains(newRecord);
    }

    /**
     * Formata o tempo em formato legível
     */
    public String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        long millis = milliseconds % 1000;

        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    /**
     * Verifica se o jogador está no parkour
     */
    public boolean isInParkour(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Obtém o recorde pessoal de um jogador
     */
    public Long getPersonalRecord(Player player) {
        return personalRecords.get(player.getUniqueId());
    }

    /**
     * Obtém um recorde do top 10 por posição (1-10)
     */
    public ParkourRecord getTopRecord(int position) {
        if (position < 1 || position > topRecords.size()) {
            return null;
        }
        return topRecords.get(position - 1);
    }

    /**
     * Remove jogador quando sai do servidor
     */
    public void removePlayer(Player player) {
        activeSessions.remove(player.getUniqueId());
    }

    /**
     * Recarrega as configurações
     */
    public void reload() {
        // Cancelar todos os parkours ativos
        for (UUID uuid : new HashSet<>(activeSessions.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                cancel(player);
            }
        }

        activeSessions.clear();
        loadConfigs();
        loadRecords();
    }

    /**
     * Classe interna para sessão de parkour
     */
    private static class ParkourSession {
        private final UUID playerUUID;
        private final Location startLocation;
        private final List<Location> checkpoints;
        private final long startTime;

        public ParkourSession(UUID playerUUID, Location startLocation) {
            this.playerUUID = playerUUID;
            this.startLocation = startLocation;
            this.checkpoints = new ArrayList<>();
            this.startTime = System.currentTimeMillis();
        }

        public void addCheckpoint(Location location) {
            checkpoints.add(location);
        }

        public Location getLastCheckpoint() {
            if (checkpoints.isEmpty()) {
                return startLocation;
            }
            return checkpoints.get(checkpoints.size() - 1);
        }

        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }

        public Location getStartLocation() {
            return startLocation;
        }

        public List<Location> getCheckpoints() {
            return checkpoints;
        }
    }

    /**
     * Classe para armazenar recordes
     */
    public static class ParkourRecord {
        private final String playerName;
        private final long time;

        public ParkourRecord(String playerName, long time) {
            this.playerName = playerName;
            this.time = time;
        }

        public String getPlayerName() {
            return playerName;
        }

        public long getTime() {
            return time;
        }
    }

    private void startActionBarUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeSessions.isEmpty()) return;

                for (UUID uuid : activeSessions.keySet()) {
                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    ParkourSession session = activeSessions.get(uuid);
                    long elapsed = session.getElapsedTime();

                    String formatted = formatTime(elapsed);

                    ActionBar.send(player, "§e" + formatted);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // atualiza a cada 3 ticks (~0.15s)
    }

}