package github.dimazbtw.lobby.views;

import github.andredimaz.plugin.core.utils.basics.MaterialUtils;
import github.dimazbtw.lobby.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Menu {

    private final Main plugin;
    private final String menuId;
    private final FileConfiguration config;
    private final Map<UUID, Inventory> playerInventories = new HashMap<>();
    private final Map<UUID, BukkitTask> updateTasks = new HashMap<>();
    private final Map<String, MenuData> menuDataCache = new HashMap<>();
    private boolean placeholderApiEnabled;

    public Menu(Main plugin, String menuId, FileConfiguration config) {
        this.plugin = plugin;
        this.menuId = menuId;
        this.config = config;
        this.placeholderApiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        loadMenuData();
    }

    /**
     * Carrega os dados dos menus de todos os idiomas
     */
    private void loadMenuData() {
        menuDataCache.clear();

        ConfigurationSection menuSection = config.getConfigurationSection("menu");
        if (menuSection == null) {
            plugin.getLogger().warning("Menu " + menuId + " não tem seção 'menu'!");
            return;
        }

        for (String language : menuSection.getKeys(false)) {
            String path = "menu." + language + ".";

            String title = config.getString(path + "title", "Menu");
            int size = config.getInt(path + "size", 27);
            int updateInterval = config.getInt(path + "update-interval", 20);

            MenuData menuData = new MenuData(title, size, updateInterval);

            // Carregar itens
            ConfigurationSection itemsSection = config.getConfigurationSection(path + "items");
            if (itemsSection != null) {
                for (String itemKey : itemsSection.getKeys(false)) {
                    String itemPath = path + "items." + itemKey + ".";

                    int slot = config.getInt(itemPath + "slot", 0);
                    String material = config.getString(itemPath + "material", "STONE");
                    String name = config.getString(itemPath + "name", "");
                    List<String> lore = config.getStringList(itemPath + "lore");
                    List<String> actions = config.getStringList(itemPath + "actions");

                    MenuItem menuItem = new MenuItem(slot, material, name, lore, actions);
                    menuData.addItem(menuItem);
                }
            }

            menuDataCache.put(language, menuData);
        }

        plugin.getLogger().info("Menu " + menuId + " carregado com " + menuDataCache.size() + " idiomas");
    }

    /**
     * Abre o menu para um jogador
     */
    public void open(Player player) {
        String language = plugin.getLanguageManager().getPlayerLanguage(player);
        MenuData menuData = menuDataCache.get(language);

        if (menuData == null) {
            // Fallback para idioma padrão
            String defaultLang = plugin.getConfig().getString("default-language", "pt_PT");
            menuData = menuDataCache.get(defaultLang);

            if (menuData == null) {
                player.sendMessage("§cErro ao abrir menu: dados não encontrados!");
                return;
            }
        }

        // Criar inventário
        String title = processPlaceholders(player, menuData.getTitle());
        Inventory inventory = Bukkit.createInventory(null, menuData.getSize(), title);

        // Adicionar itens
        for (MenuItem item : menuData.getItems()) {
            ItemStack itemStack = createItemStack(player, item);
            if (itemStack != null && item.getSlot() < menuData.getSize()) {
                inventory.setItem(item.getSlot(), itemStack);
            }
        }

        // Abrir inventário
        player.openInventory(inventory);
        playerInventories.put(player.getUniqueId(), inventory);

        // Iniciar task de atualização se necessário
        if (menuData.getUpdateInterval() > 0) {
            startUpdateTask(player, menuData);
        }
    }

    /**
     * Cria um ItemStack a partir de um MenuItem
     */
    private ItemStack createItemStack(Player player, MenuItem menuItem) {
        try {
            // Verificar se o material deve ser modificado baseado em preferências (menu de preferências)
            String material = menuItem.getMaterial();
            String name = menuItem.getName();
            List<String> lore = new ArrayList<>(menuItem.getLore());

            // Sistema de estados para menu de preferências
            if (menuId.equals("preferences")) {
                material = processPreferenceState(player, menuItem, material, lore);
            }

            ItemStack item = MaterialUtils.parseMaterial(material, player);

            if (item == null) {
                plugin.getLogger().warning("Material inválido no menu " + menuId + ": " + menuItem.getMaterial());
                return null;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return item;
            }

            // Nome
            name = processPlaceholders(player, name);
            meta.setDisplayName(name);

            // Lore
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(processPlaceholders(player, line));
            }
            meta.setLore(processedLore);

            item.setItemMeta(meta);
            return item;

        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao criar item do menu " + menuId + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Processa o estado das preferências e modifica material/lore
     */
    private String processPreferenceState(Player player, MenuItem menuItem, String material, List<String> lore) {
        String itemName = menuItem.getName().toLowerCase();
        boolean enabled = false;

        // Detectar tipo de preferência pelo nome do item
        if (itemName.contains("chat") && !itemName.contains("privada")) {
            enabled = plugin.getPlayerDataManager().isChatEnabled(player);
        } else if (itemName.contains("privada") || itemName.contains("direct") || itemName.contains("tell")) {
            enabled = plugin.getPlayerDataManager().isPrivateMessagesEnabled(player);
        } else if (itemName.contains("fly") || itemName.contains("voo")) {
            enabled = plugin.getPlayerDataManager().isFlyPreferenceEnabled(player);
        }

        // Modificar material baseado no estado
        if (material.startsWith("351:")) { // Dye/Lapis
            material = enabled ? "351:10" : "351:8"; // 10 = Verde, 8 = Cinza
        } else if (material.equals("WOOL") || material.startsWith("WOOL:")) {
            material = enabled ? "WOOL:5" : "WOOL:14"; // 5 = Verde, 14 = Vermelho
        }

        // Modificar lore para mostrar estado atual
        String language = plugin.getLanguageManager().getPlayerLanguage(player);
        String statusKey = enabled ? "preferences.status-enabled" : "preferences.status-disabled";
        String status = plugin.getLanguageManager().getMessage(language, statusKey);

        lore.add("");
        lore.add(status);

        return material;
    }

    /**
     * Processa placeholders em uma string
     */
    private String processPlaceholders(Player player, String text) {
        if (text == null) {
            return "";
        }

        // PlaceholderAPI
        if (placeholderApiEnabled) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        // Placeholders internos
        text = text
                .replace("{player}", player.getName())
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getServer().getMaxPlayers()));

        // Cores
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Inicia a task de atualização do menu
     */
    private void startUpdateTask(Player player, MenuData menuData) {
        // Cancelar task anterior se existir
        stopUpdateTask(player);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !isViewing(player)) {
                stopUpdateTask(player);
                return;
            }

            Inventory inventory = playerInventories.get(player.getUniqueId());
            if (inventory == null) {
                stopUpdateTask(player);
                return;
            }

            // Atualizar itens
            for (MenuItem item : menuData.getItems()) {
                ItemStack itemStack = createItemStack(player, item);
                if (itemStack != null && item.getSlot() < menuData.getSize()) {
                    inventory.setItem(item.getSlot(), itemStack);
                }
            }

        }, menuData.getUpdateInterval(), menuData.getUpdateInterval());

        updateTasks.put(player.getUniqueId(), task);
    }

    /**
     * Para a task de atualização do menu
     */
    private void stopUpdateTask(Player player) {
        BukkitTask task = updateTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Executa as ações de um item
     */
    public void executeActions(Player player, int slot) {
        String language = plugin.getLanguageManager().getPlayerLanguage(player);
        MenuData menuData = menuDataCache.get(language);

        if (menuData == null) {
            String defaultLang = plugin.getConfig().getString("default-language", "pt_PT");
            menuData = menuDataCache.get(defaultLang);
        }

        if (menuData == null) {
            return;
        }

        // Encontrar item pelo slot
        MenuItem menuItem = null;
        for (MenuItem item : menuData.getItems()) {
            if (item.getSlot() == slot) {
                menuItem = item;
                break;
            }
        }

        if (menuItem == null || menuItem.getActions().isEmpty()) {
            return;
        }

        // Executar ações
        for (String action : menuItem.getActions()) {
            executeAction(player, action);
        }
    }

    /**
     * Executa uma ação específica
     */
    private void executeAction(Player player, String action) {
        action = processPlaceholders(player, action);

        if (action.startsWith("[CONSOLE]")) {
            String command = action.substring(9).trim();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else if (action.startsWith("[PLAYER]")) {
            String command = action.substring(8).trim();
            player.performCommand(command);
        } else if (action.startsWith("[SERVER]")) {
            String serverName = action.substring(8).trim();
            sendPlayerToServer(player, serverName);
        } else if (action.startsWith("[MESSAGE]")) {
            String message = action.substring(9).trim();
            player.sendMessage(message);
        } else if (action.startsWith("[MENU]")) {
            String menuId = action.substring(6).trim();
            // Fechar menu atual e abrir o novo com delay
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getMenuManager().openMenu(player, menuId);
            }, 1L);
        } else if (action.equalsIgnoreCase("[CLOSE]")) {
            player.closeInventory();
        } else if (action.startsWith("[SOUND]")) {
            String soundData = action.substring(7).trim();
            playSound(player, soundData);
        } else {
            // Comando padrão (executado pelo jogador)
            player.performCommand(action);
        }
    }

    /**
     * Toca um som para o jogador
     */
    private void playSound(Player player, String soundData) {
        try {
            String[] parts = soundData.split(":");
            String soundName = parts[0];
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;

            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao tocar som: " + soundData);
        }
    }

    /**
     * Envia um jogador para um servidor BungeeCord
     */
    private void sendPlayerToServer(Player player, String serverName) {
        try {
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(b);

            out.writeUTF("Connect");
            out.writeUTF(serverName);

            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            player.sendMessage("§aConectando ao servidor §f" + serverName + "§a...");
        } catch (Exception e) {
            player.sendMessage("§cErro ao conectar ao servidor!");
            e.printStackTrace();
        }
    }

    /**
     * Verifica se um jogador está visualizando este menu
     */
    public boolean isViewing(Player player) {
        return playerInventories.containsKey(player.getUniqueId());
    }

    /**
     * Remove o jogador do cache quando fecha o menu
     */
    public void removePlayer(Player player) {
        playerInventories.remove(player.getUniqueId());
        stopUpdateTask(player);
    }

    /**
     * Obtém o ID do menu
     */
    public String getMenuId() {
        return menuId;
    }
}