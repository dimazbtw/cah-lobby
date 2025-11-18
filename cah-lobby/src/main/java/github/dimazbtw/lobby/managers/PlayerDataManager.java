package github.dimazbtw.lobby.managers;

import github.dimazbtw.lobby.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Main plugin;
    private final File dataFolder;
    private final Map<UUID, FileConfiguration> playerDataCache = new HashMap<>();
    private final Map<UUID, Boolean> adminModeCache = new HashMap<>();

    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Carrega os dados de um jogador
     */
    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");

        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        playerDataCache.put(uuid, config);

        // Carregar idioma salvo ou usar padrão
        if (config.contains("language")) {
            String language = config.getString("language");
            // Usar método interno que não salva, apenas define no cache
            plugin.getLanguageManager().setPlayerLanguageCache(player, language);
        }

        // Carregar admin mode (sempre falso ao entrar)
        adminModeCache.put(uuid, false);
    }

    /**
     * Salva os dados de um jogador
     */
    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        FileConfiguration config = playerDataCache.get(uuid);

        if (config == null) {
            return;
        }

        // Salvar idioma
        String language = plugin.getLanguageManager().getPlayerLanguage(player);
        config.set("language", language);

        // Salvar último login
        config.set("last-login", System.currentTimeMillis());
        config.set("player-name", player.getName());

        // Salvar no arquivo
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove os dados do cache quando o jogador sai
     */
    public void unloadPlayerData(UUID uuid) {
        playerDataCache.remove(uuid);
        adminModeCache.remove(uuid);
    }

    /**
     * Salva apenas o idioma do jogador (para atualizações imediatas)
     */
    public void savePlayerLanguage(Player player, String language) {
        UUID uuid = player.getUniqueId();
        FileConfiguration config = playerDataCache.get(uuid);

        if (config == null) {
            // Se não existe no cache, carregar
            loadPlayerData(player);
            config = playerDataCache.get(uuid);
        }

        if (config != null) {
            config.set("language", language);

            // Salvar no arquivo
            File playerFile = new File(dataFolder, uuid.toString() + ".yml");
            try {
                config.save(playerFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Verifica se o jogador está em modo admin
     */
    public boolean isAdminMode(Player player) {
        return adminModeCache.getOrDefault(player.getUniqueId(), false);
    }

    /**
     * Define o modo admin de um jogador
     */
    public void setAdminMode(Player player, boolean adminMode) {
        adminModeCache.put(player.getUniqueId(), adminMode);
    }

    /**
     * Alterna o modo admin de um jogador
     */
    public boolean toggleAdminMode(Player player) {
        boolean currentMode = isAdminMode(player);
        boolean newMode = !currentMode;
        setAdminMode(player, newMode);
        return newMode;
    }

    public boolean isChatEnabled(Player player) {
        UUID uuid = player.getUniqueId();
        FileConfiguration config = playerDataCache.get(uuid);

        if (config == null) {
            return true; // Padrão: habilitado
        }

        return config.getBoolean("preferences.chat", true);
    }

    /**
     * Define se o chat está habilitado
     */
    public void setChatEnabled(Player player, boolean enabled) {
        UUID uuid = player.getUniqueId();
        FileConfiguration config = playerDataCache.get(uuid);

        if (config == null) {
            loadPlayerData(player);
            config = playerDataCache.get(uuid);
        }

        if (config != null) {
            config.set("preferences.chat", enabled);
            savePlayerData(player);
        }
    }

    /**
     * Alterna o estado do chat
     */
    public boolean toggleChat(Player player) {
        boolean current = isChatEnabled(player);
        boolean newState = !current;
        setChatEnabled(player, newState);
        return newState;
    }

    /**
     * Verifica se mensagens privadas estão habilitadas
     */
    public boolean isPrivateMessagesEnabled(Player player) {
        UUID uuid = player.getUniqueId();
        FileConfiguration config = playerDataCache.get(uuid);

        if (config == null) {
            return true; // Padrão: habilitado
        }

        return config.getBoolean("preferences.private-messages", true);
    }

    /**
     * Define se mensagens privadas estão habilitadas
     */
    public void setPrivateMessagesEnabled(Player player, boolean enabled) {
        UUID uuid = player.getUniqueId();
        FileConfiguration config = playerDataCache.get(uuid);

        if (config == null) {
            loadPlayerData(player);
            config = playerDataCache.get(uuid);
        }

        if (config != null) {
            config.set("preferences.private-messages", enabled);
            savePlayerData(player);
        }
    }

    /**
     * Alterna o estado de mensagens privadas
     */
    public boolean togglePrivateMessages(Player player) {
        boolean current = isPrivateMessagesEnabled(player);
        boolean newState = !current;
        setPrivateMessagesEnabled(player, newState);
        return newState;
    }

    /**
     * Verifica se o modo fly está habilitado (preferência)
     */
    public boolean isFlyPreferenceEnabled(Player player) {
        UUID uuid = player.getUniqueId();
        FileConfiguration config = playerDataCache.get(uuid);

        if (config == null) {
            return false; // Padrão: desabilitado
        }

        return config.getBoolean("preferences.fly", false);
    }

    /**
     * Define se o modo fly está habilitado (preferência)
     */
    public void setFlyPreferenceEnabled(Player player, boolean enabled) {
        UUID uuid = player.getUniqueId();
        FileConfiguration config = playerDataCache.get(uuid);

        if (config == null) {
            loadPlayerData(player);
            config = playerDataCache.get(uuid);
        }

        if (config != null) {
            config.set("preferences.fly", enabled);
            savePlayerData(player);
        }
    }

    /**
     * Alterna o estado do fly (preferência)
     */
    public boolean toggleFlyPreference(Player player) {
        boolean current = isFlyPreferenceEnabled(player);
        boolean newState = !current;
        setFlyPreferenceEnabled(player, newState);
        return newState;
    }
}