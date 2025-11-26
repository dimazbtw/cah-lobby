package github.dimazbtw.lobby;

import github.dimazbtw.lobby.Main;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Set;

/**
 * API Pública do CAH-Lobby
 *
 * Permite que outros plugins acessem funcionalidades do lobby,
 * incluindo o sistema de idiomas.
 *
 * @author dimazbtw
 * @version 1.0
 */
public class LobbyAPI {

    private static Main plugin;

    /**
     * Inicializa a API
     *
     * @param main Instância do plugin principal
     */
    public static void initialize(Main main) {
        plugin = main;
    }

    /**
     * Obtém a instância do plugin CAH-Lobby
     *
     * @return Instância do Main ou null se não estiver carregado
     */
    public static Main getPlugin() {
        if (plugin == null) {
            plugin = (Main) JavaPlugin.getPlugin(Main.class);
        }
        return plugin;
    }

    /**
     * Verifica se a API está disponível
     *
     * @return true se o plugin está carregado
     */
    public static boolean isAvailable() {
        return getPlugin() != null;
    }

    // ==================== LANGUAGE API ====================

    /**
     * Obtém o idioma atual de um jogador
     *
     * @param player O jogador
     * @return Código do idioma (ex: "pt_PT", "en_US")
     */
    public static String getPlayerLanguage(Player player) {
        if (!isAvailable() || player == null) {
            return "pt_PT"; // Fallback padrão
        }
        return plugin.getLanguageManager().getPlayerLanguage(player);
    }

    public static void giveJoinItems(Player player) {
        if (!isAvailable() || player == null) {
            return;
        }
        plugin.getJoinItemsManager().giveJoinItems(player);
    }

    /**
     * Define o idioma de um jogador
     *
     * @param player O jogador
     * @param language Código do idioma (ex: "pt_PT", "en_US")
     * @return true se o idioma foi definido com sucesso
     */
    public static boolean setPlayerLanguage(Player player, String language) {
        if (!isAvailable() || player == null || language == null) {
            return false;
        }

        if (!hasLanguage(language)) {
            return false;
        }

        plugin.getLanguageManager().setPlayerLanguage(player, language);
        return true;
    }

    /**
     * Obtém todos os idiomas disponíveis
     *
     * @return Set com códigos de todos os idiomas
     */
    public static Set<String> getAvailableLanguages() {
        if (!isAvailable()) {
            return Collections.singleton("pt_PT"); // Fallback padrão
        }
        return plugin.getLanguageManager().getAvailableLanguages();
    }

    /**
     * Verifica se um idioma existe
     *
     * @param language Código do idioma
     * @return true se o idioma existe
     */
    public static boolean hasLanguage(String language) {
        if (!isAvailable() || language == null) {
            return false;
        }
        return plugin.getLanguageManager().hasLanguage(language);
    }

    /**
     * Obtém o nome de exibição de um idioma
     *
     * @param language Código do idioma
     * @return Nome de exibição (ex: "Português", "English")
     */
    public static String getLanguageDisplayName(String language) {
        if (!isAvailable() || language == null) {
            return language;
        }
        return plugin.getLanguageManager().getLanguageDisplayName(language);
    }

    /**
     * Obtém o idioma padrão do servidor
     *
     * @return Código do idioma padrão
     */
    public static String getDefaultLanguage() {
        if (!isAvailable()) {
            return "pt_PT";
        }
        return plugin.getConfig().getString("default-language", "pt_PT");
    }

    /**
     * Obtém uma mensagem traduzida para um jogador
     *
     * @param player O jogador
     * @param key Chave da mensagem
     * @return Mensagem traduzida
     */
    public static String getMessage(Player player, String key) {
        if (!isAvailable() || player == null || key == null) {
            return "§c[Missing: " + key + "]";
        }
        return plugin.getLanguageManager().getMessage(player, key);
    }

    /**
     * Envia uma mensagem traduzida para um jogador
     *
     * @param player O jogador
     * @param key Chave da mensagem
     */
    public static void sendMessage(Player player, String key) {
        if (!isAvailable() || player == null || key == null) {
            return;
        }
        plugin.getLanguageManager().sendMessage(player, key);
    }

    // ==================== PVP API ====================

    /**
     * Verifica se um jogador está em modo PvP
     *
     * @param player O jogador
     * @return true se está em modo PvP
     */
    public static boolean isPvPEnabled(Player player) {
        if (!isAvailable() || player == null) {
            return false;
        }
        return plugin.getPvPManager().isPvPEnabled(player);
    }

    /**
     * Ativa o modo PvP para um jogador
     *
     * @param player O jogador
     */
    public static void enablePvP(Player player) {
        if (!isAvailable() || player == null) {
            return;
        }
        plugin.getPvPManager().enablePvP(player);
    }

    /**
     * Desativa o modo PvP para um jogador
     *
     * @param player O jogador
     */
    public static void disablePvP(Player player) {
        if (!isAvailable() || player == null) {
            return;
        }
        plugin.getPvPManager().disablePvP(player);
    }

    /**
     * Verifica se um jogador está em combate
     *
     * @param player O jogador
     * @return true se está em combate
     */
    public static boolean isInCombat(Player player) {
        if (!isAvailable() || player == null) {
            return false;
        }
        return plugin.getPvPManager().isInCombat(player);
    }

    /**
     * Obtém o killstreak atual de um jogador
     *
     * @param player O jogador
     * @return Killstreak atual
     */
    public static int getKillStreak(Player player) {
        if (!isAvailable() || player == null) {
            return 0;
        }
        return plugin.getPvPManager().getKillStreak(player);
    }

    // ==================== PLAYER DATA API ====================

    /**
     * Verifica se o chat está habilitado para um jogador
     *
     * @param player O jogador
     * @return true se o chat está habilitado
     */
    public static boolean isChatEnabled(Player player) {
        if (!isAvailable() || player == null) {
            return true;
        }
        return plugin.getPlayerDataManager().isChatEnabled(player);
    }

    /**
     * Verifica se mensagens privadas estão habilitadas para um jogador
     *
     * @param player O jogador
     * @return true se mensagens privadas estão habilitadas
     */
    public static boolean isPrivateMessagesEnabled(Player player) {
        if (!isAvailable() || player == null) {
            return true;
        }
        return plugin.getPlayerDataManager().isPrivateMessagesEnabled(player);
    }

    /**
     * Verifica se um jogador está em modo admin
     *
     * @param player O jogador
     * @return true se está em modo admin
     */
    public static boolean isAdminMode(Player player) {
        if (!isAvailable() || player == null) {
            return false;
        }
        return plugin.getPlayerDataManager().isAdminMode(player);
    }
}