package github.dimazbtw.lobby.views;

import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.views.Menu;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MenuManager {

    private final Main plugin;
    private final File menusFolder;
    private final Map<String, Menu> menus = new HashMap<>();

    public MenuManager(Main plugin) {
        this.plugin = plugin;
        this.menusFolder = new File(plugin.getDataFolder(), "menus");

        // Criar pasta menus se não existir
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
        }

        loadMenus();
    }

    /**
     * Carrega todos os menus da pasta menus
     */
    public void loadMenus() {
        menus.clear();

        // Criar arquivos padrão se não existirem
        createDefaultMenuFiles();

        // Carregar todos os arquivos .yml da pasta menus
        File[] files = menusFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files != null && files.length > 0) {
            for (File file : files) {
                String menuId = file.getName().replace(".yml", "");
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    Menu menu = new Menu(plugin, menuId, config);
                    menus.put(menuId, menu);
                    Bukkit.getConsoleSender().sendMessage("§a[MenuManager] Menu carregado: " + menuId);
                } catch (Exception e) {
                    Bukkit.getConsoleSender().sendMessage("§c[MenuManager] Erro ao carregar menu: " + menuId);
                    e.printStackTrace();
                }
            }
        }

        if (menus.isEmpty()) {
            Bukkit.getConsoleSender().sendMessage("§c[MenuManager] AVISO: Nenhum menu encontrado!");
        } else {
            Bukkit.getConsoleSender().sendMessage("§a[MenuManager] Total de menus carregados: " + menus.size());
        }
    }

    /**
     * Cria os arquivos de menu padrão
     */
    private void createDefaultMenuFiles() {
        createDefaultGameModesMenu();
    }

    /**
     * Cria o menu padrão de modos de jogo
     */
    private void createDefaultGameModesMenu() {
        File file = new File(menusFolder, "game-modes.yml");

        if (!file.exists()) {
            try {
                plugin.saveResource("menus/game-modes.yml", false);
                Bukkit.getConsoleSender().sendMessage("§a[MenuManager] Arquivo criado: game-modes.yml");
            } catch (IllegalArgumentException e) {
                // Arquivo não existe nos resources, criar template básico
                FileConfiguration config = new YamlConfiguration();

                // Menu em Português
                config.set("menu.pt_PT.title", "&6&lMODOS DE JOGO");
                config.set("menu.pt_PT.size", 27);
                config.set("menu.pt_PT.update-interval", 20);

                config.set("menu.pt_PT.items.rankup.slot", 11);
                config.set("menu.pt_PT.items.rankup.material", "DIAMOND");
                config.set("menu.pt_PT.items.rankup.name", "&a&lRANKUP");
                config.set("menu.pt_PT.items.rankup.lore", java.util.Arrays.asList(
                        "&7Mine, evolua e domine!",
                        "",
                        "&7Jogadores: &f{server_online_rankup}",
                        "",
                        "&eClique para conectar!"
                ));
                config.set("menu.pt_PT.items.rankup.actions", java.util.Arrays.asList(
                        "[SERVER] rankup"
                ));

                config.set("menu.pt_PT.items.bedwars.slot", 13);
                config.set("menu.pt_PT.items.bedwars.material", "BED");
                config.set("menu.pt_PT.items.bedwars.name", "&c&lBEDWARS");
                config.set("menu.pt_PT.items.bedwars.lore", java.util.Arrays.asList(
                        "&7Proteja sua cama e destrua",
                        "&7a dos seus inimigos!",
                        "",
                        "&7Jogadores: &f{server_online_bedwars}",
                        "",
                        "&eClique para conectar!"
                ));
                config.set("menu.pt_PT.items.bedwars.actions", java.util.Arrays.asList(
                        "[SERVER] bedwars"
                ));

                config.set("menu.pt_PT.items.close.slot", 22);
                config.set("menu.pt_PT.items.close.material", "BARRIER");
                config.set("menu.pt_PT.items.close.name", "&c&lFECHAR");
                config.set("menu.pt_PT.items.close.lore", java.util.Arrays.asList(
                        "&7Clique para fechar o menu"
                ));
                config.set("menu.pt_PT.items.close.actions", java.util.Arrays.asList(
                        "[CLOSE]"
                ));

                // Menu em Inglês
                config.set("menu.en_US.title", "&6&lGAME MODES");
                config.set("menu.en_US.size", 27);
                config.set("menu.en_US.update-interval", 20);

                config.set("menu.en_US.items.rankup.slot", 11);
                config.set("menu.en_US.items.rankup.material", "DIAMOND");
                config.set("menu.en_US.items.rankup.name", "&a&lRANKUP");
                config.set("menu.en_US.items.rankup.lore", java.util.Arrays.asList(
                        "&7Mine, evolve and dominate!",
                        "",
                        "&7Players: &f{server_online_rankup}",
                        "",
                        "&eClick to connect!"
                ));
                config.set("menu.en_US.items.rankup.actions", java.util.Arrays.asList(
                        "[SERVER] rankup"
                ));

                config.set("menu.en_US.items.bedwars.slot", 13);
                config.set("menu.en_US.items.bedwars.material", "BED");
                config.set("menu.en_US.items.bedwars.name", "&c&lBEDWARS");
                config.set("menu.en_US.items.bedwars.lore", java.util.Arrays.asList(
                        "&7Protect your bed and destroy",
                        "&7your enemies' beds!",
                        "",
                        "&7Players: &f{server_online_bedwars}",
                        "",
                        "&eClick to connect!"
                ));
                config.set("menu.en_US.items.bedwars.actions", java.util.Arrays.asList(
                        "[SERVER] bedwars"
                ));

                config.set("menu.en_US.items.close.slot", 22);
                config.set("menu.en_US.items.close.material", "BARRIER");
                config.set("menu.en_US.items.close.name", "&c&lCLOSE");
                config.set("menu.en_US.items.close.lore", java.util.Arrays.asList(
                        "&7Click to close the menu"
                ));
                config.set("menu.en_US.items.close.actions", java.util.Arrays.asList(
                        "[CLOSE]"
                ));

                try {
                    config.save(file);
                    Bukkit.getConsoleSender().sendMessage("§e[MenuManager] Template criado: game-modes.yml");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Abre um menu para um jogador
     */
    public void openMenu(Player player, String menuId) {
        Menu menu = menus.get(menuId);

        if (menu == null) {
            player.sendMessage("§cMenu não encontrado: " + menuId);
            return;
        }

        menu.open(player);
    }

    /**
     * Verifica se um menu existe
     */
    public boolean hasMenu(String menuId) {
        return menus.containsKey(menuId);
    }

    /**
     * Obtém um menu
     */
    public Menu getMenu(String menuId) {
        return menus.get(menuId);
    }

    /**
     * Obtém todos os menus carregados
     */
    public java.util.Collection<Menu> getAllMenus() {
        return menus.values();
    }

    /**
     * Recarrega todos os menus
     */
    public void reload() {
        // Fechar todos os menus abertos
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() != null) {
                for (Menu menu : menus.values()) {
                    if (menu.isViewing(player)) {
                        player.closeInventory();
                    }
                }
            }
        }

        loadMenus();
        Bukkit.getConsoleSender().sendMessage("§a[MenuManager] Menus recarregados!");
    }
}
