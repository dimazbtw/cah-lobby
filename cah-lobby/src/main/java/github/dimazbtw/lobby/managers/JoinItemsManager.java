package github.dimazbtw.lobby.managers;

import github.dimazbtw.lobby.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinItemsManager {

    private final Main plugin;
    private final Map<Player, Boolean> playerVisibility = new HashMap<>();

    public JoinItemsManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Dá os itens de entrada ao jogador
     */
    public void giveJoinItems(Player player) {
        if (!plugin.getConfig().getBoolean("join-items.enabled", true)) {
            return;
        }

        // Limpar inventário
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Obter idioma do jogador
        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);

        // Slot 0 - Modos de jogo (COMPASS)
        ItemStack gamemodes = createItem(
                Material.COMPASS,
                0,
                plugin.getLanguageManager().getMessage(playerLang, "join-items.gamemodes.name"),
                plugin.getLanguageManager().getMessageList(playerLang, "join-items.gamemodes.lore")
        );
        player.getInventory().setItem(4, gamemodes);

        // Slot 1 - Perfil (Cabeça do jogador)
        ItemStack profile = createPlayerHead(
                player,
                plugin.getLanguageManager().getMessage(playerLang, "join-items.profile.name"),
                plugin.getLanguageManager().getMessageList(playerLang, "join-items.profile.lore")
        );
        player.getInventory().setItem(1, profile);

        // Slot 4 - PvP (ESPADA)
        ItemStack pvp = createItem(
                Material.DIAMOND_SWORD,
                0,
                plugin.getLanguageManager().getMessage(playerLang, "join-items.pvp.name"),
                plugin.getLanguageManager().getMessageList(playerLang, "join-items.pvp.lore")
        );
        player.getInventory().setItem(0, pvp);

        // Slot 7 - Alternar visibilidade (DYE - GREEN por padrão)
        boolean visibility = playerVisibility.getOrDefault(player, true);
        ItemStack visibilityItem = createVisibilityItem(player, playerLang, visibility);
        player.getInventory().setItem(7, visibilityItem);

        // Slot 8 - Lobbies (NETHER_STAR)
        ItemStack lobbies = createItem(
                Material.NETHER_STAR,
                0,
                plugin.getLanguageManager().getMessage(playerLang, "join-items.lobbies.name"),
                plugin.getLanguageManager().getMessageList(playerLang, "join-items.lobbies.lore")
        );
        player.getInventory().setItem(8, lobbies);

        // Atualizar inventário
        player.updateInventory();
    }

    /**
     * Cria um item customizado
     */
    private ItemStack createItem(Material material, int data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, (short) data);
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
     * Cria a cabeça do jogador
     */
    private ItemStack createPlayerHead(Player player, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3); // 3 = Player head
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwner(player.getName());
            meta.setDisplayName(name.replace("&", "§").replace("{player}", player.getName()));

            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(line.replace("&", "§").replace("{player}", player.getName()));
                }
                meta.setLore(coloredLore);
            }

            head.setItemMeta(meta);
        }

        return head;
    }

    /**
     * Cria o item de visibilidade
     */
    private ItemStack createVisibilityItem(Player player, String playerLang, boolean visible) {
        Material material = Material.INK_SACK; // Dye na 1.8
        int data = visible ? 10 : 8; // 10 = Lime dye (verde), 8 = Gray dye (cinza)

        String nameKey = visible ? "join-items.visibility.name-on" : "join-items.visibility.name-off";
        String loreKey = visible ? "join-items.visibility.lore-on" : "join-items.visibility.lore-off";

        return createItem(
                material,
                data,
                plugin.getLanguageManager().getMessage(playerLang, nameKey),
                plugin.getLanguageManager().getMessageList(playerLang, loreKey)
        );
    }

    /**
     * Alterna a visibilidade de jogadores
     */
    public void toggleVisibility(Player player) {
        boolean currentVisibility = playerVisibility.getOrDefault(player, true);
        boolean newVisibility = !currentVisibility;

        playerVisibility.put(player, newVisibility);

        // Atualizar item no slot 7
        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);
        ItemStack visibilityItem = createVisibilityItem(player, playerLang, newVisibility);
        player.getInventory().setItem(7, visibilityItem);
        player.updateInventory();

        // Aplicar visibilidade
        if (newVisibility) {
            // Mostrar todos os jogadores DO MESMO MODO (PvP ou Lobby)
            boolean playerInPvP = plugin.getPvPManager().isPvPEnabled(player);

            for (Player target : plugin.getServer().getOnlinePlayers()) {
                if (target.equals(player)) continue;

                boolean targetInPvP = plugin.getPvPManager().isPvPEnabled(target);

                // Só mostrar se ambos estiverem no mesmo "modo"
                if (playerInPvP == targetInPvP) {
                    player.showPlayer(target);
                }
            }
        } else {
            // Esconder todos os jogadores
            for (Player target : plugin.getServer().getOnlinePlayers()) {
                if (!target.equals(player)) {
                    player.hidePlayer(target);
                }
            }
        }
    }

    /**
     * Obtém o estado de visibilidade de um jogador
     */
    public boolean getVisibility(Player player) {
        return playerVisibility.getOrDefault(player, true);
    }

    /**
     * Remove o jogador do cache
     */
    public void removePlayer(Player player) {
        playerVisibility.remove(player);
    }
}