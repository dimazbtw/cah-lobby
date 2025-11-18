package github.dimazbtw.lobby.managers;

import github.andredimaz.plugin.core.utils.basics.HologramUtils;
import github.dimazbtw.lobby.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final Main plugin;
    private final File hologramsFile;
    private FileConfiguration hologramsConfig;
    private final Map<String, Location> hologramLocations = new HashMap<>();
    private final Map<UUID, Map<String, int[]>> playerHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerVisibleHolograms = new ConcurrentHashMap<>();
    private BukkitTask updateTask;
    private BukkitTask renderTask;
    private boolean placeholderApiEnabled;
    private static final double RENDER_DISTANCE = 30.0;

    public HologramManager(Main plugin) {
        this.plugin = plugin;
        this.hologramsFile = new File(plugin.getDataFolder(), "holograms.yml");
        this.placeholderApiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        loadHolograms();
        startUpdateTask();
        startRenderTask();
    }

    /**
     * Carrega os hologramas do arquivo
     */
    public void loadHolograms() {
        if (!hologramsFile.exists()) {
            try {
                hologramsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        hologramsConfig = YamlConfiguration.loadConfiguration(hologramsFile);
        hologramLocations.clear();

        if (hologramsConfig.contains("holograms")) {
            for (String name : hologramsConfig.getConfigurationSection("holograms").getKeys(false)) {
                String path = "holograms." + name + ".location.";

                if (hologramsConfig.contains(path + "world")) {
                    Location loc = new Location(
                            Bukkit.getWorld(hologramsConfig.getString(path + "world")),
                            hologramsConfig.getDouble(path + "x"),
                            hologramsConfig.getDouble(path + "y"),
                            hologramsConfig.getDouble(path + "z")
                    );
                    hologramLocations.put(name, loc);
                }
            }
        }

        plugin.getLogger().info("Carregados " + hologramLocations.size() + " hologramas!");
    }

    /**
     * Salva os hologramas no arquivo
     */
    public void saveHolograms() {
        try {
            hologramsConfig.save(hologramsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cria um novo holograma
     */
    public void createHologram(String name, Location location, List<String> lines) {
        hologramLocations.put(name, location);

        String path = "holograms." + name + ".";
        hologramsConfig.set(path + "location.world", location.getWorld().getName());
        hologramsConfig.set(path + "location.x", location.getX());
        hologramsConfig.set(path + "location.y", location.getY());
        hologramsConfig.set(path + "location.z", location.getZ());

        // Salvar linhas em todos os idiomas (começa com o padrão)
        String defaultLang = plugin.getConfig().getString("default-language", "pt_PT");
        hologramsConfig.set(path + "lines." + defaultLang, lines);

        saveHolograms();
    }

    /**
     * Define as linhas de um holograma para um idioma específico
     */
    public void setHologramLines(String name, String language, List<String> lines) {
        if (!hologramLocations.containsKey(name)) {
            return;
        }

        String path = "holograms." + name + ".lines." + language;
        hologramsConfig.set(path, lines);
        saveHolograms();

        // Atualizar para jogadores com este idioma
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);
            if (playerLang.equals(language) && isHologramVisibleToPlayer(player, name)) {
                hideHologramFromPlayer(player, name);
                showHologramToPlayer(player, name);
            }
        }
    }

    /**
     * Remove um holograma
     */
    public void deleteHologram(String name) {
        if (!hologramLocations.containsKey(name)) {
            return;
        }

        // Remover de todos os jogadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideHologramFromPlayer(player, name);
        }

        hologramLocations.remove(name);
        hologramsConfig.set("holograms." + name, null);
        saveHolograms();
    }

    /**
     * Inicia a task de renderização por distância
     */
    private void startRenderTask() {
        renderTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateHologramVisibility(player);
            }
        }, 10L, 10L);
    }

    /**
     * Atualiza a visibilidade dos hologramas para um jogador baseado na distância
     */
    private void updateHologramVisibility(Player player) {
        Set<String> visibleHolograms = playerVisibleHolograms.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());

        for (Map.Entry<String, Location> entry : hologramLocations.entrySet()) {
            String name = entry.getKey();
            Location holoLoc = entry.getValue();

            if (!holoLoc.getWorld().equals(player.getWorld())) {
                if (visibleHolograms.contains(name)) {
                    hideHologramFromPlayer(player, name);
                }
                continue;
            }

            double distance = holoLoc.distance(player.getLocation());
            boolean shouldBeVisible = distance <= RENDER_DISTANCE;
            boolean isCurrentlyVisible = visibleHolograms.contains(name);

            if (shouldBeVisible && !isCurrentlyVisible) {
                showHologramToPlayer(player, name);
            } else if (!shouldBeVisible && isCurrentlyVisible) {
                hideHologramFromPlayer(player, name);
            }
        }
    }

    /**
     * Mostra um holograma para um jogador
     */
    private void showHologramToPlayer(Player player, String name) {
        if (!hologramLocations.containsKey(name)) {
            return;
        }

        Location location = hologramLocations.get(name);
        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);

        // Obter linhas do idioma do jogador ou padrão
        List<String> lines = getHologramLines(name, playerLang);
        if (lines == null || lines.isEmpty()) {
            return;
        }

        // Processar placeholders e cores
        String[] processedLines = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            processedLines[i] = processLine(player, lines.get(i));
        }

        // Criar holograma
        int[] entityIds = HologramUtils.createMultiLineHologram(player, location, processedLines);

        // Salvar IDs das entidades
        playerHolograms.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(name, entityIds);
        playerVisibleHolograms.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(name);
    }

    /**
     * Esconde um holograma de um jogador
     */
    private void hideHologramFromPlayer(Player player, String name) {
        Map<String, int[]> holograms = playerHolograms.get(player.getUniqueId());
        if (holograms == null || !holograms.containsKey(name)) {
            return;
        }

        int[] entityIds = holograms.get(name);
        HologramUtils.removeMultiLineHologram(player, entityIds);
        holograms.remove(name);

        Set<String> visibleHolograms = playerVisibleHolograms.get(player.getUniqueId());
        if (visibleHolograms != null) {
            visibleHolograms.remove(name);
        }
    }

    /**
     * Verifica se um holograma está visível para um jogador
     */
    private boolean isHologramVisibleToPlayer(Player player, String name) {
        Set<String> visibleHolograms = playerVisibleHolograms.get(player.getUniqueId());
        return visibleHolograms != null && visibleHolograms.contains(name);
    }

    /**
     * Mostra um holograma para um jogador (método público - força exibição)
     */
    public void showHologram(Player player, String name) {
        showHologramToPlayer(player, name);
    }

    /**
     * Esconde um holograma de um jogador (método público)
     */
    public void hideHologram(Player player, String name) {
        hideHologramFromPlayer(player, name);
    }

    /**
     * Esconde todos os hologramas de um jogador
     */
    public void hideAllHolograms(Player player) {
        Map<String, int[]> holograms = playerHolograms.get(player.getUniqueId());
        if (holograms == null) {
            return;
        }

        for (Map.Entry<String, int[]> entry : holograms.entrySet()) {
            HologramUtils.removeMultiLineHologram(player, entry.getValue());
        }

        playerHolograms.remove(player.getUniqueId());
        playerVisibleHolograms.remove(player.getUniqueId());
    }

    /**
     * Remove os dados de um jogador (quando sai do servidor)
     */
    public void removePlayerData(Player player) {
        hideAllHolograms(player);
    }

    /**
     * Obtém as linhas de um holograma para um idioma específico
     */
    private List<String> getHologramLines(String name, String language) {
        String path = "holograms." + name + ".lines.";

        // Tentar idioma do jogador
        if (hologramsConfig.contains(path + language)) {
            return hologramsConfig.getStringList(path + language);
        }

        // Fallback para idioma padrão
        String defaultLang = plugin.getConfig().getString("default-language", "pt_PT");
        if (hologramsConfig.contains(path + defaultLang)) {
            return hologramsConfig.getStringList(path + defaultLang);
        }

        return new ArrayList<>();
    }

    /**
     * Obtém as linhas de um holograma para um idioma (método público)
     */
    public List<String> getLines(String name, String language) {
        return new ArrayList<>(getHologramLines(name, language));
    }

    /**
     * Processa uma linha (placeholders e cores)
     */
    private String processLine(Player player, String line) {
        // Placeholders básicos
        line = line
                .replace("{player}", player.getName())
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getServer().getMaxPlayers()));

        // PlaceholderAPI
        if (placeholderApiEnabled) {
            line = PlaceholderAPI.setPlaceholders(player, line);
        }

        // Cores
        return ChatColor.translateAlternateColorCodes('&', line);
    }

    /**
     * Inicia a task de atualização dos hologramas
     */
    private void startUpdateTask() {
        int updateInterval = plugin.getConfig().getInt("holograms.update-interval", 40);

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerHolograms(player);
            }
        }, 20L, updateInterval);
    }

    /**
     * Atualiza os hologramas de um jogador (para placeholders dinâmicos)
     */
    private void updatePlayerHolograms(Player player) {
        Map<String, int[]> holograms = playerHolograms.get(player.getUniqueId());
        if (holograms == null) {
            return;
        }

        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);

        for (Map.Entry<String, int[]> entry : holograms.entrySet()) {
            String name = entry.getKey();
            int[] entityIds = entry.getValue();

            List<String> lines = getHologramLines(name, playerLang);
            if (lines == null || lines.size() != entityIds.length) {
                continue;
            }

            for (int i = 0; i < lines.size(); i++) {
                String processedLine = processLine(player, lines.get(i));
                HologramUtils.updateHologramLine(player, entityIds[i], processedLine);
            }
        }
    }

    /**
     * Para as tasks de atualização e renderização
     */
    public void stopTasks() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (renderTask != null) {
            renderTask.cancel();
            renderTask = null;
        }
    }

    /**
     * Obtém todos os nomes de hologramas
     */
    public Set<String> getHologramNames() {
        return hologramLocations.keySet();
    }

    /**
     * Verifica se um holograma existe
     */
    public boolean hologramExists(String name) {
        return hologramLocations.containsKey(name);
    }

    /**
     * Move um holograma para uma nova localização
     */
    public void moveHologram(String name, Location newLocation) {
        if (!hologramLocations.containsKey(name)) {
            return;
        }

        // Esconder de todos os jogadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideHologramFromPlayer(player, name);
        }

        // Atualizar localização
        hologramLocations.put(name, newLocation);

        String path = "holograms." + name + ".location.";
        hologramsConfig.set(path + "world", newLocation.getWorld().getName());
        hologramsConfig.set(path + "x", newLocation.getX());
        hologramsConfig.set(path + "y", newLocation.getY());
        hologramsConfig.set(path + "z", newLocation.getZ());
        saveHolograms();
    }

    /**
     * Obtém a localização de um holograma
     */
    public Location getHologramLocation(String name) {
        return hologramLocations.get(name);
    }

    /**
     * Recarrega todos os hologramas
     */
    public void reload() {
        // Esconder todos os hologramas de todos os jogadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideAllHolograms(player);
        }

        // Recarregar configuração
        loadHolograms();
    }
}