// Atualize a classe Main.java:

package github.dimazbtw.lobby;

import github.dimazbtw.lobby.commands.LobbyCommand;
import github.dimazbtw.lobby.commands.registry.CommandRegistry;
import github.dimazbtw.lobby.config.LanguageManager;
import github.dimazbtw.lobby.database.Database; // ADICIONE ESTE IMPORT
import github.dimazbtw.lobby.hooks.PlaceholderHook;
import github.dimazbtw.lobby.listeners.*;
import github.dimazbtw.lobby.managers.*;
import github.dimazbtw.lobby.utils.PacketAS;
import github.dimazbtw.lobby.views.MenuListener;
import github.dimazbtw.lobby.views.MenuManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Main extends JavaPlugin {

    private LanguageManager languageManager;
    private WorldManager worldManager;
    private PlayerDataManager playerDataManager;
    private ScoreboardManager scoreboardManager;
    private TabManager tabManager;
    private JoinItemsManager joinItemsManager;
    private ChatManager chatManager;
    private HologramManager hologramManager;
    private LobbyCommand lobbyCommand;
    private NPCManager npcManager;
    private PvPManager pvPManager;
    private Database statsDatabase; // ADICIONE ESTA LINHA
    private PacketAS packetAS;
    private MenuManager menuManager;
    private TellManager tellManager;

    @Override
    public void onEnable() {
        // Salvar configuração padrão
        saveDefaultConfig();

        getLogger().info("=== Iniciando Lobby Plugin ===");

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Inicializar Database ANTES de tudo
        getLogger().info("Inicializando Database...");
        statsDatabase = new Database(this); // ADICIONE ESTA LINHA

        // Inicializar LanguageManager
        getLogger().info("Carregando sistema de idiomas...");
        languageManager = new LanguageManager(this);
        String defaultLang = getConfig().getString("default-language", "pt_PT");
        languageManager.setDefaultLanguage(defaultLang);
        getLogger().info("Idioma padrão definido: " + defaultLang);
        getLogger().info("Idiomas disponíveis: " + languageManager.getAvailableLanguages());

        // Inicializar PlayerDataManager
        getLogger().info("Inicializando PlayerDataManager...");
        playerDataManager = new PlayerDataManager(this);

        // Inicializar ChatManager
        getLogger().info("Inicializando ChatManager...");
        chatManager = new ChatManager(this);

        // Inicializar JoinItemsManager
        getLogger().info("Inicializando JoinItemsManager...");
        joinItemsManager = new JoinItemsManager(this);

        // Inicializar HologramManager
        getLogger().info("Inicializando HologramManager...");
        hologramManager = new HologramManager(this);

        // Inicializar ScoreboardManager
        getLogger().info("Inicializando ScoreboardManager...");
        scoreboardManager = new ScoreboardManager(this);
        scoreboardManager.startUpdateTask();

        // Inicializar TabManager
        getLogger().info("Inicializando TabManager...");
        tabManager = new TabManager(this);
        tabManager.startUpdateTask();

        // Inicializar LobbyCommand
        getLogger().info("Inicializando LobbyCommand...");
        lobbyCommand = new LobbyCommand(this);

        // Inicializar WorldManager
        getLogger().info("Inicializando WorldManager...");
        worldManager = new WorldManager(this);
        worldManager.startMaintenanceTask();

        getLogger().info("Inicializando NPCManager...");
        npcManager = new NPCManager(this);

        getLogger().info("Inicializando PvPManager...");
        pvPManager = new PvPManager(this);

        getLogger().info("Inicializando MenuManager...");
        menuManager = new MenuManager(this);

        getLogger().info("Inicializando TellManager...");
        tellManager = new TellManager(this);

        packetAS = new PacketAS(this);

        new PlaceholderHook(this).register();

        // Registrar comandos
        getLogger().info("Registrando comandos...");
        new CommandRegistry(this);

        // Registrar listeners
        getLogger().info("Registrando listeners...");
        registerListeners();

        getLogger().info("=== Plugin carregado com sucesso! ===");
        getLogger().info("Sistema de proteção ativado!");
    }

    @Override
    public void onDisable() {
        // Fechar conexão com banco de dados
        if (statsDatabase != null) {
            statsDatabase.close(); // ADICIONE ESTA LINHA
        }

        // Parar scoreboard
        if (scoreboardManager != null) {
            scoreboardManager.stopUpdateTask();
        }

        // Parar tab
        if (tabManager != null) {
            tabManager.stopUpdateTask();
        }

        // Salvar dados de todos os jogadores online
        if (playerDataManager != null) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                playerDataManager.savePlayerData(player);
                playerDataManager.unloadPlayerData(player.getUniqueId());
            });
        }

        getLogger().info("Plugin descarregado!");
    }

    /**
     * Registra todos os listeners do plugin
     */
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AdditionalListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SpawnListener(this, lobbyCommand), this);
        Bukkit.getPluginManager().registerEvents(new WelcomeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ScoreboardListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TabListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinItemsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NPCInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PvPListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PvPVisibilityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
    }
}