package github.dimazbtw.lobby.config;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LanguageManager {

    private final Plugin plugin;
    private final File langFolder;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private String defaultLanguage = "pt_PT";

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
        this.langFolder = new File(plugin.getDataFolder(), "lang");

        // Criar pasta lang se não existir
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        loadLanguages();
    }

    /**
     * Carrega todos os arquivos de idioma da pasta lang
     */
    public void loadLanguages() {
        languages.clear();

        // Criar arquivos padrão se não existirem
        createDefaultLanguageFiles();

        // Carregar todos os arquivos .yml da pasta lang
        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files != null && files.length > 0) {
            for (File file : files) {
                String langCode = file.getName().replace(".yml", "");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                languages.put(langCode, config);
                Bukkit.getConsoleSender().sendMessage("§a[LanguageManager] Idioma carregado: " + langCode);
            }
        }

        if (languages.isEmpty()) {
            Bukkit.getConsoleSender().sendMessage("§c[LanguageManager] ERRO: Nenhum idioma encontrado!");
            Bukkit.getConsoleSender().sendMessage("§c[LanguageManager] Verifique se os arquivos estão em: " + langFolder.getAbsolutePath());
        } else {
            Bukkit.getConsoleSender().sendMessage("§a[LanguageManager] Total de idiomas carregados: " + languages.size());
        }
    }

    /**
     * Cria os arquivos de idioma padrão (pt_PT e en_US)
     */
    private void createDefaultLanguageFiles() {
        createLanguageFile("pt_PT.yml");
        createLanguageFile("en_US.yml");
        createLanguageFile("es_ES.yml");
    }

    /**
     * Cria um arquivo de idioma a partir dos recursos do plugin
     */
    private void createLanguageFile(String fileName) {
        File file = new File(langFolder, fileName);

        if (!file.exists()) {
            // Tentar copiar do resource do plugin
            try {
                plugin.saveResource("lang/" + fileName, false);
                Bukkit.getConsoleSender().sendMessage("§a[LanguageManager] Arquivo criado: " + fileName);
            } catch (IllegalArgumentException e) {
                // Arquivo não existe nos resources, criar template básico
                YamlConfiguration config = new YamlConfiguration();
                config.set("language.name", fileName.replace(".yml", ""));
                config.set("language.display", "Language Name");
                config.set("messages.example", "Example message");

                try {
                    config.save(file);
                    Bukkit.getConsoleSender().sendMessage("§e[LanguageManager] Arquivo template criado: " + fileName);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Obtém a mensagem traduzida para o jogador
     */
    public String getMessage(Player player, String key) {
        String lang = getPlayerLanguage(player);
        return getMessage(lang, key);
    }

    /**
     * Obtém a mensagem traduzida para um idioma específico
     */
    public String getMessage(String language, String key) {
        YamlConfiguration config = languages.get(language);

        if (config == null) {
            Bukkit.getConsoleSender().sendMessage("§c[LanguageManager] Idioma não encontrado: " + language);
            // Tentar idioma padrão
            config = languages.get(defaultLanguage);
        }

        if (config == null || !config.contains(key)) {
            Bukkit.getConsoleSender().sendMessage("§c[LanguageManager] Key não encontrada: " + key + " no idioma: " + language);
            if (config != null && languages.get(defaultLanguage) != null) {
                config = languages.get(defaultLanguage);
                if (!config.contains(key)) {
                    return "§c[Missing: " + key + "]";
                }
            } else {
                return "§c[Missing: " + key + "]";
            }
        }

        return config.getString(key, "§c[Missing: " + key + "]").replace("&", "§");
    }

    /**
     * Obtém a mensagem traduzida com placeholders substituídos
     */
    public String getMessage(Player player, String key, Map<String, String> placeholders) {
        String message = getMessage(player, key);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return message;
    }

    /**
     * Obtém uma lista de mensagens traduzidas
     */
    public List<String> getMessageList(String language, String key) {
        YamlConfiguration config = languages.get(language);

        if (config == null) {
            config = languages.get(defaultLanguage);
        }

        if (config == null || !config.contains(key)) {
            return new ArrayList<>();
        }

        List<String> messages = config.getStringList(key);
        List<String> colorized = new ArrayList<>();

        for (String message : messages) {
            colorized.add(message.replace("&", "§"));
        }

        return colorized;
    }

    /**
     * Obtém uma lista de mensagens traduzidas para o jogador
     */
    public List<String> getMessageList(Player player, String key) {
        String lang = getPlayerLanguage(player);
        return getMessageList(lang, key);
    }

    /**
     * Envia uma mensagem traduzida para o jogador
     */
    public void sendMessage(Player player, String key) {
        player.sendMessage(getMessage(player, key));
    }

    /**
     * Envia uma mensagem traduzida com placeholders para o jogador
     */
    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(getMessage(player, key, placeholders));
    }

    /**
     * Define o idioma de um jogador no cache (sem salvar no arquivo)
     * Usado ao carregar dados do jogador
     */
    public void setPlayerLanguageCache(Player player, String language) {
        if (!languages.containsKey(language)) {
            // Se o idioma não existe, usar o padrão
            language = defaultLanguage;
        }

        playerLanguages.put(player.getUniqueId(), language);
    }

    /**
     * Define o idioma de um jogador
     */
    public void setPlayerLanguage(Player player, String language) {
        if (!languages.containsKey(language)) {
            return;
        }

        playerLanguages.put(player.getUniqueId(), language);

        // Salvar idioma no playerdata imediatamente
        if (plugin instanceof github.dimazbtw.lobby.Main) {
            github.dimazbtw.lobby.Main mainPlugin = (github.dimazbtw.lobby.Main) plugin;
            mainPlugin.getPlayerDataManager().savePlayerLanguage(player, language);

            // Atualizar itens do jogador
            if (mainPlugin.getJoinItemsManager() != null) {
                mainPlugin.getJoinItemsManager().giveJoinItems(player);
            }
        }
    }

    /**
     * Obtém o idioma do jogador
     */
    public String getPlayerLanguage(Player player) {
        return playerLanguages.getOrDefault(player.getUniqueId(), defaultLanguage);
    }

    /**
     * Remove o idioma do jogador (quando sai do servidor)
     */
    public void removePlayer(UUID uuid) {
        playerLanguages.remove(uuid);
    }

    /**
     * Obtém todos os idiomas disponíveis
     */
    public Set<String> getAvailableLanguages() {
        return languages.keySet();
    }

    /**
     * Verifica se um idioma existe
     */
    public boolean hasLanguage(String language) {
        return languages.containsKey(language);
    }

    /**
     * Obtém o nome de exibição do idioma
     */
    public String getLanguageDisplayName(String language) {
        if (!languages.containsKey(language)) {
            return language;
        }

        String displayName = getMessage(language, "language.display");

        // Se não encontrou ou retornou erro, usar o código do idioma
        if (displayName.startsWith("§c[Missing:")) {
            return language;
        }

        return displayName;
    }

    /**
     * Define o idioma padrão
     */
    public void setDefaultLanguage(String language) {
        if (languages.containsKey(language)) {
            this.defaultLanguage = language;
        }
    }

    /**
     * Recarrega todos os idiomas
     */
    public void reload() {
        loadLanguages();
        Bukkit.getConsoleSender().sendMessage("§a[LanguageManager] Idiomas recarregados!");
    }
}