package github.dimazbtw.lobby.managers;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import github.andredimaz.plugin.core.utils.basics.HologramUtils;
import github.andredimaz.plugin.core.utils.effects.ParticleAnimation;
import github.andredimaz.plugin.core.utils.effects.ParticleEffect;
import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.utils.PacketAS;
import me.clip.placeholderapi.PlaceholderAPI;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCManager {

    private final Main plugin;
    private final File npcsFile;
    private FileConfiguration npcsConfig;
    private final Map<String, Location> npcLocations = new HashMap<>();
    private final Map<String, List<String>> npcCommands = new HashMap<>();
    private final Map<String, Map<String, List<String>>> npcHolograms = new HashMap<>(); // id -> lang -> lines
    private final Map<String, String> npcTypes = new HashMap<>();
    private final Map<String, Map<UUID, Integer>> npcEntities = new HashMap<>(); // id -> playerUUID -> entityId
    private final Map<String, String> npcNames = new HashMap<>();
    private final Map<String, Double> npcHologramHeights = new HashMap<>();
    private final Map<String, String> npcSkins = new HashMap<>();
    private final Map<String, Boolean> npcLookAtPlayer = new HashMap<>();
    private final Map<String, Boolean> npcFloatAnimation = new HashMap<>();
    private final Map<String, Boolean> npcRotateAnimation = new HashMap<>();
    private final Map<String, Double> npcRotateSpeed = new HashMap<>();
    private final Map<String, Double> npcFloatAmplitude = new HashMap<>();
    private final Map<String, Boolean> npcParticleEnabled = new HashMap<>();
    private final Map<String, String> npcParticleType = new HashMap<>();
    private final Map<String, String> npcParticleAnimation = new HashMap<>();
    private final Map<String, Double> npcParticleRadius = new HashMap<>();
    private final Map<String, Double> npcParticleHeight = new HashMap<>();
    private final Map<String, Integer> npcParticleAmount = new HashMap<>();
    private final Map<String, Float> npcParticleSpeed = new HashMap<>();
    private final Map<String, Long> npcParticleInterval = new HashMap<>();
    private final Map<String, ParticleAnimation> npcParticleAnimations = new HashMap<>();
    private final Map<UUID, Map<String, int[]>> playerNPCHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerVisibleNPCs = new ConcurrentHashMap<>();
    private final PacketAS packetAS;
    private BukkitTask updateTask;
    private BukkitTask lookAtTask;
    private BukkitTask renderTask;
    private boolean placeholderApiEnabled;
    private static final double RENDER_DISTANCE = 30.0;

    public NPCManager(Main plugin) {
        this.plugin = plugin;
        this.npcsFile = new File(plugin.getDataFolder(), "npcs.yml");
        this.packetAS = new PacketAS(plugin);
        this.placeholderApiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        loadNPCs();
        startHologramUpdateTask();
        startLookAtTask();
        startRenderTask();
    }

    private void loadNPCs() {
        if (!npcsFile.exists()) {
            try {
                npcsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        npcsConfig = YamlConfiguration.loadConfiguration(npcsFile);

        if (npcsConfig.contains("npcs")) {
            for (String id : npcsConfig.getConfigurationSection("npcs").getKeys(false)) {
                String path = "npcs." + id + ".";

                Location loc = new Location(
                        Bukkit.getWorld(npcsConfig.getString(path + "location.world")),
                        npcsConfig.getDouble(path + "location.x"),
                        npcsConfig.getDouble(path + "location.y"),
                        npcsConfig.getDouble(path + "location.z"),
                        (float) npcsConfig.getDouble(path + "location.yaw"),
                        (float) npcsConfig.getDouble(path + "location.pitch")
                );
                npcLocations.put(id, loc);

                npcTypes.put(id, npcsConfig.getString(path + "type", "npc"));
                npcNames.put(id, npcsConfig.getString(path + "name", "§e§lNPC"));
                npcHologramHeights.put(id, npcsConfig.getDouble(path + "hologram-height", 2.3));
                npcSkins.put(id, npcsConfig.getString(path + "skin", "MHF_Question"));
                npcLookAtPlayer.put(id, npcsConfig.getBoolean(path + "look-at-player", false));
                npcFloatAnimation.put(id, npcsConfig.getBoolean(path + "float-animation", false));
                npcRotateAnimation.put(id, npcsConfig.getBoolean(path + "rotate-animation", false));
                npcRotateSpeed.put(id, npcsConfig.getDouble(path + "rotate-speed", 5.0));
                npcFloatAmplitude.put(id, npcsConfig.getDouble(path + "float-amplitude", 0.3));

                // Carregar configurações de partículas
                npcParticleEnabled.put(id, npcsConfig.getBoolean(path + "particle.enabled", false));
                npcParticleType.put(id, npcsConfig.getString(path + "particle.type", "FLAME"));
                npcParticleAnimation.put(id, npcsConfig.getString(path + "particle.animation", "CIRCLE"));
                npcParticleRadius.put(id, npcsConfig.getDouble(path + "particle.radius", 1.0));
                npcParticleHeight.put(id, npcsConfig.getDouble(path + "particle.height", 2.0));
                npcParticleAmount.put(id, npcsConfig.getInt(path + "particle.amount", 1));
                npcParticleSpeed.put(id, (float) npcsConfig.getDouble(path + "particle.speed", 0.1));
                npcParticleInterval.put(id, npcsConfig.getLong(path + "particle.interval", 2L));

                npcCommands.put(id, npcsConfig.getStringList(path + "commands"));

                // Carregar hologramas multi-linguagem
                Map<String, List<String>> hologramsByLang = new HashMap<>();
                if (npcsConfig.contains(path + "hologram")) {
                    // Verificar se é o formato antigo (lista simples) ou novo (por idioma)
                    if (npcsConfig.isList(path + "hologram")) {
                        // Formato antigo - adicionar ao idioma padrão
                        String defaultLang = plugin.getConfig().getString("default-language", "pt_PT");
                        hologramsByLang.put(defaultLang, npcsConfig.getStringList(path + "hologram"));
                    } else if (npcsConfig.isConfigurationSection(path + "hologram")) {
                        // Formato novo - multi-linguagem
                        for (String lang : npcsConfig.getConfigurationSection(path + "hologram").getKeys(false)) {
                            hologramsByLang.put(lang, npcsConfig.getStringList(path + "hologram." + lang));
                        }
                    }
                }
                npcHolograms.put(id, hologramsByLang);
            }
        }

        plugin.getLogger().info("Carregados " + npcLocations.size() + " NPCs!");
    }

    private void saveNPCs() {
        try {
            npcsConfig.save(npcsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createNPC(String id, Location location, String type) {
        npcLocations.put(id, location);
        npcTypes.put(id, type.toLowerCase());
        npcNames.put(id, "§e§lNPC");
        npcHologramHeights.put(id, 2.3);
        npcSkins.put(id, "MHF_Question");
        npcLookAtPlayer.put(id, false);
        npcFloatAnimation.put(id, false);
        npcRotateAnimation.put(id, false);
        npcRotateSpeed.put(id, 5.0);
        npcFloatAmplitude.put(id, 0.3);
        npcParticleEnabled.put(id, false);
        npcParticleType.put(id, "FLAME");
        npcParticleAnimation.put(id, "CIRCLE");
        npcParticleRadius.put(id, 1.0);
        npcParticleHeight.put(id, 2.0);
        npcParticleAmount.put(id, 1);
        npcParticleSpeed.put(id, 0.1f);
        npcParticleInterval.put(id, 2L);
        npcCommands.put(id, new ArrayList<>());

        String defaultLang = plugin.getConfig().getString("default-language", "pt_PT");
        List<String> defaultHologram = Arrays.asList("&e&lNPC", "&7Clique para interagir");
        Map<String, List<String>> hologramsByLang = new HashMap<>();
        hologramsByLang.put(defaultLang, defaultHologram);
        npcHolograms.put(id, hologramsByLang);

        String path = "npcs." + id + ".";
        npcsConfig.set(path + "type", type.toLowerCase());
        npcsConfig.set(path + "name", "§e§lNPC");
        npcsConfig.set(path + "hologram-height", 2.3);
        npcsConfig.set(path + "skin", "MHF_Question");
        npcsConfig.set(path + "look-at-player", false);
        npcsConfig.set(path + "float-animation", false);
        npcsConfig.set(path + "rotate-animation", false);
        npcsConfig.set(path + "rotate-speed", 5.0);
        npcsConfig.set(path + "float-amplitude", 0.3);
        npcsConfig.set(path + "particle.enabled", false);
        npcsConfig.set(path + "particle.type", "FLAME");
        npcsConfig.set(path + "particle.animation", "CIRCLE");
        npcsConfig.set(path + "particle.radius", 1.0);
        npcsConfig.set(path + "particle.height", 2.0);
        npcsConfig.set(path + "particle.amount", 1);
        npcsConfig.set(path + "particle.speed", 0.1);
        npcsConfig.set(path + "particle.interval", 2);
        npcsConfig.set(path + "location.world", location.getWorld().getName());
        npcsConfig.set(path + "location.x", location.getX());
        npcsConfig.set(path + "location.y", location.getY());
        npcsConfig.set(path + "location.z", location.getZ());
        npcsConfig.set(path + "location.yaw", location.getYaw());
        npcsConfig.set(path + "location.pitch", location.getPitch());
        npcsConfig.set(path + "commands", new ArrayList<>());
        npcsConfig.set(path + "hologram." + defaultLang, defaultHologram);

        saveNPCs();
    }

    public void deleteNPC(String id) {
        if (!npcLocations.containsKey(id)) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            hideNPCFromPlayer(player, id);
        }

        npcLocations.remove(id);
        npcTypes.remove(id);
        npcNames.remove(id);
        npcCommands.remove(id);
        npcSkins.remove(id);
        npcHolograms.remove(id);
        npcHologramHeights.remove(id);
        npcLookAtPlayer.remove(id);
        npcEntities.remove(id);

        npcsConfig.set("npcs." + id, null);
        saveNPCs();
    }

    public void addCommand(String id, String command) {
        if (!npcCommands.containsKey(id)) {
            return;
        }

        List<String> commands = npcCommands.get(id);
        commands.add(command);

        npcsConfig.set("npcs." + id + ".commands", commands);
        saveNPCs();
    }

    public void resetCommands(String id) {
        if (!npcCommands.containsKey(id)) {
            return;
        }

        npcCommands.put(id, new ArrayList<>());
        npcsConfig.set("npcs." + id + ".commands", new ArrayList<>());
        saveNPCs();
    }

    public void setNPCHologramLines(String id, String language, List<String> lines) {
        if (!npcLocations.containsKey(id)) return;

        Map<String, List<String>> hologramsByLang = npcHolograms.get(id);
        if (hologramsByLang == null) {
            hologramsByLang = new HashMap<>();
            npcHolograms.put(id, hologramsByLang);
        }

        hologramsByLang.put(language, lines);
        npcsConfig.set("npcs." + id + ".hologram." + language, lines);
        saveNPCs();

        // Atualizar para jogadores com este idioma
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);
            if (playerLang.equals(language) && isNPCVisibleToPlayer(player, id)) {
                hideNPCHologram(player, id);
                showNPCHologram(player, id);
            }
        }
    }

    public void setNPCName(String id, String name) {
        if (!npcLocations.containsKey(id)) return;

        npcNames.put(id, ChatColor.translateAlternateColorCodes('&', name));
        npcsConfig.set("npcs." + id + ".name", name);
        saveNPCs();

        // Respawn NPC para todos os jogadores que o veem
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isNPCVisibleToPlayer(player, id)) {
                hideNPCFromPlayer(player, id);
                showNPCToPlayer(player, id);
            }
        }
    }

    public void setNPCSkin(String id, String skin) {
        if (!npcLocations.containsKey(id)) return;

        npcSkins.put(id, skin);
        npcsConfig.set("npcs." + id + ".skin", skin);
        saveNPCs();

        // Respawn NPC para todos os jogadores que o veem
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isNPCVisibleToPlayer(player, id)) {
                hideNPCFromPlayer(player, id);
                showNPCToPlayer(player, id);
            }
        }
    }

    public void setHologramHeight(String id, double height) {
        if (!npcLocations.containsKey(id)) return;

        npcHologramHeights.put(id, height);
        npcsConfig.set("npcs." + id + ".hologram-height", height);
        saveNPCs();

        // Atualizar holograma para todos os jogadores que o veem
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isNPCVisibleToPlayer(player, id)) {
                hideNPCHologram(player, id);
                showNPCHologram(player, id);
            }
        }
    }

    public void setLookAtPlayer(String id, boolean lookAtPlayer) {
        if (!npcLocations.containsKey(id)) return;

        npcLookAtPlayer.put(id, lookAtPlayer);
        npcsConfig.set("npcs." + id + ".look-at-player", lookAtPlayer);
        saveNPCs();
    }

    public void setFloatAnimation(String id, boolean floatAnimation) {
        if (!npcLocations.containsKey(id)) return;

        npcFloatAnimation.put(id, floatAnimation);
        npcsConfig.set("npcs." + id + ".float-animation", floatAnimation);
        saveNPCs();

        // Respawn para aplicar animação
        if ("head".equals(npcTypes.get(id))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isNPCVisibleToPlayer(player, id)) {
                    hideNPCFromPlayer(player, id);
                    showNPCToPlayer(player, id);
                }
            }
        }
    }

    public void setRotateAnimation(String id, boolean rotateAnimation) {
        if (!npcLocations.containsKey(id)) return;

        npcRotateAnimation.put(id, rotateAnimation);
        npcsConfig.set("npcs." + id + ".rotate-animation", rotateAnimation);
        saveNPCs();

        // Respawn para aplicar animação
        if ("head".equals(npcTypes.get(id))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isNPCVisibleToPlayer(player, id)) {
                    hideNPCFromPlayer(player, id);
                    showNPCToPlayer(player, id);
                }
            }
        }
    }

    public void setRotateSpeed(String id, double speed) {
        if (!npcLocations.containsKey(id)) return;

        npcRotateSpeed.put(id, speed);
        npcsConfig.set("npcs." + id + ".rotate-speed", speed);
        saveNPCs();

        // Respawn para aplicar nova velocidade (se rotação está ativa)
        if ("head".equals(npcTypes.get(id)) && npcRotateAnimation.getOrDefault(id, false)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isNPCVisibleToPlayer(player, id)) {
                    hideNPCFromPlayer(player, id);
                    showNPCToPlayer(player, id);
                }
            }
        }
    }

    public void setFloatAmplitude(String id, double amplitude) {
        if (!npcLocations.containsKey(id)) return;

        npcFloatAmplitude.put(id, amplitude);
        npcsConfig.set("npcs." + id + ".float-amplitude", amplitude);
        saveNPCs();

        // Respawn para aplicar nova amplitude (se flutuação está ativa)
        if ("head".equals(npcTypes.get(id)) && npcFloatAnimation.getOrDefault(id, false)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isNPCVisibleToPlayer(player, id)) {
                    hideNPCFromPlayer(player, id);
                    showNPCToPlayer(player, id);
                }
            }
        }
    }

    public void setParticleEnabled(String id, boolean enabled) {
        if (!npcLocations.containsKey(id)) return;

        npcParticleEnabled.put(id, enabled);
        npcsConfig.set("npcs." + id + ".particle.enabled", enabled);
        saveNPCs();

        if (enabled) {
            startParticleAnimation(id);
        } else {
            stopParticleAnimation(id);
        }
    }

    public void setParticleType(String id, String type) {
        if (!npcLocations.containsKey(id)) return;

        npcParticleType.put(id, type.toUpperCase());
        npcsConfig.set("npcs." + id + ".particle.type", type.toUpperCase());
        saveNPCs();

        if (npcParticleEnabled.getOrDefault(id, false)) {
            stopParticleAnimation(id);
            startParticleAnimation(id);
        }
    }

    public void setParticleAnimation(String id, String animation) {
        if (!npcLocations.containsKey(id)) return;

        npcParticleAnimation.put(id, animation.toUpperCase());
        npcsConfig.set("npcs." + id + ".particle.animation", animation.toUpperCase());
        saveNPCs();

        if (npcParticleEnabled.getOrDefault(id, false)) {
            stopParticleAnimation(id);
            startParticleAnimation(id);
        }
    }

    public void setParticleRadius(String id, double radius) {
        if (!npcLocations.containsKey(id)) return;

        npcParticleRadius.put(id, radius);
        npcsConfig.set("npcs." + id + ".particle.radius", radius);
        saveNPCs();

        if (npcParticleEnabled.getOrDefault(id, false)) {
            stopParticleAnimation(id);
            startParticleAnimation(id);
        }
    }

    public void setParticleHeight(String id, double height) {
        if (!npcLocations.containsKey(id)) return;

        npcParticleHeight.put(id, height);
        npcsConfig.set("npcs." + id + ".particle.height", height);
        saveNPCs();

        if (npcParticleEnabled.getOrDefault(id, false)) {
            stopParticleAnimation(id);
            startParticleAnimation(id);
        }
    }

    public void setParticleAmount(String id, int amount) {
        if (!npcLocations.containsKey(id)) return;

        npcParticleAmount.put(id, amount);
        npcsConfig.set("npcs." + id + ".particle.amount", amount);
        saveNPCs();

        if (npcParticleEnabled.getOrDefault(id, false)) {
            stopParticleAnimation(id);
            startParticleAnimation(id);
        }
    }

    public void setParticleSpeed(String id, float speed) {
        if (!npcLocations.containsKey(id)) return;

        npcParticleSpeed.put(id, speed);
        npcsConfig.set("npcs." + id + ".particle.speed", speed);
        saveNPCs();

        if (npcParticleEnabled.getOrDefault(id, false)) {
            stopParticleAnimation(id);
            startParticleAnimation(id);
        }
    }

    public void setParticleInterval(String id, long interval) {
        if (!npcLocations.containsKey(id)) return;

        npcParticleInterval.put(id, interval);
        npcsConfig.set("npcs." + id + ".particle.interval", interval);
        saveNPCs();

        if (npcParticleEnabled.getOrDefault(id, false)) {
            stopParticleAnimation(id);
            startParticleAnimation(id);
        }
    }

    private void startParticleAnimation(String id) {
        Location location = npcLocations.get(id);
        if (location == null) return;

        try {
            ParticleEffect effect = ParticleEffect.valueOf(npcParticleType.getOrDefault(id, "FLAME"));
            ParticleAnimation.AnimationType animationType = ParticleAnimation.AnimationType.valueOf(
                    npcParticleAnimation.getOrDefault(id, "CIRCLE")
            );

            ParticleAnimation animation = new ParticleAnimation(
                    plugin,
                    effect,
                    animationType,
                    Long.MAX_VALUE, // Duração infinita
                    npcParticleInterval.getOrDefault(id, 2L)
            );

            animation.setRadius(npcParticleRadius.getOrDefault(id, 1.0));
            animation.setHeight(npcParticleHeight.getOrDefault(id, 2.0));
            animation.setAmount(npcParticleAmount.getOrDefault(id, 1));
            animation.setSpeed(npcParticleSpeed.getOrDefault(id, 0.1f));
            animation.setSteps(20);

            animation.start(location, 50.0);
            npcParticleAnimations.put(id, animation);

        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Partícula ou animação inválida para NPC " + id + ": " + e.getMessage());
        }
    }

    private void stopParticleAnimation(String id) {
        ParticleAnimation animation = npcParticleAnimations.remove(id);
        if (animation != null && animation.isRunning()) {
            animation.stop();
        }
    }

    public void moveNPC(String id, Location newLocation) {
        if (!npcLocations.containsKey(id)) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            hideNPCFromPlayer(player, id);
        }

        npcLocations.put(id, newLocation);

        String path = "npcs." + id + ".location.";
        npcsConfig.set(path + "world", newLocation.getWorld().getName());
        npcsConfig.set(path + "x", newLocation.getX());
        npcsConfig.set(path + "y", newLocation.getY());
        npcsConfig.set(path + "z", newLocation.getZ());
        npcsConfig.set(path + "yaw", newLocation.getYaw());
        npcsConfig.set(path + "pitch", newLocation.getPitch());
        saveNPCs();
    }

    private void startRenderTask() {
        renderTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateNPCVisibility(player);
            }
        }, 10L, 10L);
    }

    private void updateNPCVisibility(Player player) {
        Set<String> visibleNPCs = playerVisibleNPCs.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());

        for (Map.Entry<String, Location> entry : npcLocations.entrySet()) {
            String id = entry.getKey();
            Location npcLoc = entry.getValue();

            if (!npcLoc.getWorld().equals(player.getWorld())) {
                if (visibleNPCs.contains(id)) {
                    hideNPCFromPlayer(player, id);
                }
                continue;
            }

            double distance = npcLoc.distance(player.getLocation());
            boolean shouldBeVisible = distance <= RENDER_DISTANCE;
            boolean isCurrentlyVisible = visibleNPCs.contains(id);

            if (shouldBeVisible && !isCurrentlyVisible) {
                showNPCToPlayer(player, id);
            } else if (!shouldBeVisible && isCurrentlyVisible) {
                hideNPCFromPlayer(player, id);
            }
        }
    }

    private void showNPCToPlayer(Player player, String id) {
        Location location = npcLocations.get(id);
        String type = npcTypes.get(id);

        if ("head".equals(type)) {
            spawnHeadNPCForPlayer(player, id, location);
        } else {
            spawnPlayerNPCForPlayer(player, id, location);
        }

        showNPCHologram(player, id);
        playerVisibleNPCs.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(id);

        // Iniciar animação de partículas se habilitada e ainda não iniciada
        if (npcParticleEnabled.getOrDefault(id, false) && !npcParticleAnimations.containsKey(id)) {
            startParticleAnimation(id);
        }
    }

    private void hideNPCFromPlayer(Player player, String id) {
        hideNPCHologram(player, id);

        Map<UUID, Integer> entities = npcEntities.get(id);
        if (entities != null) {
            Integer entityId = entities.remove(player.getUniqueId());
            if (entityId != null) {
                if ("head".equals(npcTypes.get(id))) {
                    PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(entityId);
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(destroyPacket);
                } else {
                    removePlayerNPCForPlayer(player, entityId);
                }
            }
        }

        Set<String> visibleNPCs = playerVisibleNPCs.get(player.getUniqueId());
        if (visibleNPCs != null) {
            visibleNPCs.remove(id);
        }
    }

    private boolean isNPCVisibleToPlayer(Player player, String id) {
        Set<String> visibleNPCs = playerVisibleNPCs.get(player.getUniqueId());
        return visibleNPCs != null && visibleNPCs.contains(id);
    }

    private void spawnHeadNPCForPlayer(Player player, String id, Location location) {
        String skin = npcSkins.getOrDefault(id, "MHF_Question");

        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (skin.startsWith("http://") || skin.startsWith("https://")) {
            applySkullTexture(meta, skin);
        } else {
            meta.setOwner(skin);
        }

        skull.setItemMeta(meta);

        // Verificar animações
        boolean rotate = npcRotateAnimation.getOrDefault(id, false);
        boolean floatAnim = npcFloatAnimation.getOrDefault(id, false);
        double speed = npcRotateSpeed.getOrDefault(id, 5.0);
        double amplitude = npcFloatAmplitude.getOrDefault(id, 0.3);

        // Se tiver look-at-player ativo, desabilitar rotação automática
        boolean lookAt = npcLookAtPlayer.getOrDefault(id, false);
        if (lookAt) {
            rotate = false;
        }

        int entityId = packetAS.spawnForPlayer(player, location, skull, true, rotate ? speed : 0.0, floatAnim, amplitude);
        npcEntities.computeIfAbsent(id, k -> new HashMap<>()).put(player.getUniqueId(), entityId);
    }

    private void applySkullTexture(SkullMeta meta, String url) {
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            String textureJson = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", url);
            String encodedData = Base64.getEncoder().encodeToString(textureJson.getBytes());
            profile.getProperties().put("textures", new Property("textures", encodedData));

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spawnPlayerNPCForPlayer(Player player, String id, Location location) {
        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer nmsWorld = ((CraftWorld) location.getWorld()).getHandle();

        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), ChatColor.translateAlternateColorCodes('&', npcNames.get(id)));

        String skin = npcSkins.get(id);
        if (skin != null) {
            if (skin.startsWith("http://") || skin.startsWith("https://")) {
                String textureJson = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", skin);
                String encodedData = Base64.getEncoder().encodeToString(textureJson.getBytes());
                gameProfile.getProperties().put("textures", new Property("textures", encodedData));
            } else {
                Property skinProperty = getSkinProperty(skin);
                if (skinProperty != null) gameProfile.getProperties().put("textures", skinProperty);
            }
        }

        EntityPlayer npc = new EntityPlayer(nmsServer, nmsWorld, gameProfile, new PlayerInteractManager(nmsWorld));
        npc.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        PacketPlayOutPlayerInfo addPacket = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc);
        PacketPlayOutNamedEntitySpawn spawnPacket = new PacketPlayOutNamedEntitySpawn(npc);

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(addPacket);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(spawnPacket);

        npcEntities.computeIfAbsent(id, k -> new HashMap<>()).put(player.getUniqueId(), npc.getId());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PacketPlayOutPlayerInfo removePacket = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npc);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(removePacket);
        }, 20L);
    }

    private void removePlayerNPCForPlayer(Player player, int entityId) {
        PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(entityId);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(destroyPacket);
    }

    public void showNPCHologram(Player player, String id) {
        Location location = npcLocations.get(id);
        if (location == null) return;

        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);
        Map<String, List<String>> hologramsByLang = npcHolograms.get(id);

        if (hologramsByLang == null || hologramsByLang.isEmpty()) return;

        List<String> lines = hologramsByLang.get(playerLang);
        if (lines == null || lines.isEmpty()) {
            String defaultLang = plugin.getConfig().getString("default-language", "pt_PT");
            lines = hologramsByLang.get(defaultLang);
        }

        if (lines == null || lines.isEmpty()) return;

        double height = npcHologramHeights.getOrDefault(id, 2.3);
        String[] processedLines = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) processedLines[i] = processLine(player, lines.get(i));

        Location holoLoc = location.clone().add(0, height, 0);
        int[] entityIds = HologramUtils.createMultiLineHologram(player, holoLoc, processedLines);
        playerNPCHolograms.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(id, entityIds);
    }

    public void hideNPCHologram(Player player, String id) {
        Map<String, int[]> holograms = playerNPCHolograms.get(player.getUniqueId());
        if (holograms == null || !holograms.containsKey(id)) return;

        HologramUtils.removeMultiLineHologram(player, holograms.get(id));
        holograms.remove(id);
    }

    public void hideAllNPCHolograms(Player player) {
        Map<String, int[]> holograms = playerNPCHolograms.get(player.getUniqueId());
        if (holograms == null) return;

        for (int[] entityIds : holograms.values()) HologramUtils.removeMultiLineHologram(player, entityIds);
        playerNPCHolograms.remove(player.getUniqueId());
    }

    public void removePlayerData(Player player) {
        UUID uuid = player.getUniqueId();

        Set<String> visibleNPCs = playerVisibleNPCs.remove(uuid);
        if (visibleNPCs != null) {
            for (String id : visibleNPCs) {
                Map<UUID, Integer> entities = npcEntities.get(id);
                if (entities != null) {
                    entities.remove(uuid);
                }
            }
        }

        hideAllNPCHolograms(player);
    }

    private void startHologramUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) updatePlayerNPCHolograms(player);
        }, 20L, 40L);
    }

    private void startLookAtTask() {
        lookAtTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Set<String> visibleNPCs = playerVisibleNPCs.get(player.getUniqueId());
                if (visibleNPCs == null) continue;

                for (String id : visibleNPCs) {
                    if (!npcLookAtPlayer.getOrDefault(id, false)) continue;

                    Location npcLoc = npcLocations.get(id);
                    if (npcLoc == null || !npcLoc.getWorld().equals(player.getWorld())) continue;
                    if (npcLoc.distance(player.getLocation()) > 10) continue;

                    Map<UUID, Integer> entities = npcEntities.get(id);
                    if (entities == null) continue;

                    Integer entityId = entities.get(player.getUniqueId());
                    if (entityId == null) continue;

                    Location playerLoc = player.getLocation();
                    double dx = playerLoc.getX() - npcLoc.getX();
                    double dz = playerLoc.getZ() - npcLoc.getZ();
                    double dy = playerLoc.getY() - npcLoc.getY();

                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    float pitch = (float) Math.toDegrees(Math.atan(-dy / Math.sqrt(dx * dx + dz * dz)));

                    if ("head".equals(npcTypes.get(id))) {
                        PacketPlayOutEntityTeleport teleportPacket = new PacketPlayOutEntityTeleport(
                                entityId,
                                (int)(npcLoc.getX() * 32),
                                (int)(npcLoc.getY() * 32),
                                (int)(npcLoc.getZ() * 32),
                                (byte)((yaw * 256.0F) / 360.0F),
                                (byte)((pitch * 256.0F) / 360.0F),
                                false
                        );
                        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(teleportPacket);
                    } else {
                        PacketPlayOutEntityHeadRotation headRotation = new PacketPlayOutEntityHeadRotation();
                        try {
                            Field entityIdField = headRotation.getClass().getDeclaredField("a");
                            entityIdField.setAccessible(true);
                            entityIdField.set(headRotation, entityId);

                            Field yawField = headRotation.getClass().getDeclaredField("b");
                            yawField.setAccessible(true);
                            yawField.set(headRotation, (byte)((yaw * 256.0F) / 360.0F));

                            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(headRotation);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, 1L, 5L);
    }

    private void updatePlayerNPCHolograms(Player player) {
        Map<String, int[]> holograms = playerNPCHolograms.get(player.getUniqueId());
        if (holograms == null) return;

        String playerLang = plugin.getLanguageManager().getPlayerLanguage(player);

        for (Map.Entry<String, int[]> entry : holograms.entrySet()) {
            String id = entry.getKey();
            int[] entityIds = entry.getValue();

            Map<String, List<String>> hologramsByLang = npcHolograms.get(id);
            if (hologramsByLang == null) continue;

            List<String> lines = hologramsByLang.get(playerLang);
            if (lines == null) {
                String defaultLang = plugin.getConfig().getString("default-language", "pt_PT");
                lines = hologramsByLang.get(defaultLang);
            }

            if (lines == null || lines.size() != entityIds.length) continue;

            for (int i = 0; i < lines.size(); i++) {
                HologramUtils.updateHologramLine(player, entityIds[i], processLine(player, lines.get(i)));
            }
        }
    }

    private String processLine(Player player, String line) {
        line = line
                .replace("{player}", player.getName())
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(Bukkit.getServer().getMaxPlayers()));

        if (placeholderApiEnabled) {
            line = PlaceholderAPI.setPlaceholders(player, line);
        }

        return ChatColor.translateAlternateColorCodes('&', line);
    }

    public void reload() {
        // Remover todos os NPCs de todos os jogadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            Set<String> visibleNPCs = playerVisibleNPCs.get(player.getUniqueId());
            if (visibleNPCs != null) {
                for (String id : new HashSet<>(visibleNPCs)) {
                    hideNPCFromPlayer(player, id);
                }
            }
        }

        // Limpar todos os dados
        npcLocations.clear();
        npcCommands.clear();
        npcHolograms.clear();
        npcTypes.clear();
        npcEntities.clear();
        npcNames.clear();
        npcHologramHeights.clear();
        npcSkins.clear();
        npcLookAtPlayer.clear();
        npcFloatAnimation.clear();
        npcRotateAnimation.clear();
        npcRotateSpeed.clear();
        npcFloatAmplitude.clear();
        playerVisibleNPCs.clear();

        // Recarregar arquivo
        npcsConfig = YamlConfiguration.loadConfiguration(npcsFile);
        loadNPCs();
    }

    public void stopTasks() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (lookAtTask != null) {
            lookAtTask.cancel();
            lookAtTask = null;
        }
        if (renderTask != null) {
            renderTask.cancel();
            renderTask = null;
        }

        // Parar todas as animações de partículas
        for (String id : new HashSet<>(npcParticleAnimations.keySet())) {
            stopParticleAnimation(id);
        }
    }

    public void executeCommands(String id, Player player) {
        List<String> commands = npcCommands.get(id);
        if (commands == null || commands.isEmpty()) {
            return;
        }

        for (String command : commands) {
            String processedCommand = command
                    .replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString());

            if (processedCommand.startsWith("[CONSOLE]")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand.substring(9).trim());
            } else if (processedCommand.startsWith("[PLAYER]")) {
                player.performCommand(processedCommand.substring(8).trim());
            } else if (processedCommand.startsWith("[SERVER]")) {
                String serverName = processedCommand.substring(8).trim();
                sendPlayerToBungeeServer(player, serverName);
            } else {
                player.performCommand(processedCommand);
            }
        }
    }

    /**
     * Envia um jogador para um servidor BungeeCord
     */
    private void sendPlayerToBungeeServer(Player player, String serverName) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            out.writeUTF("Connect");
            out.writeUTF(serverName);

            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());

            plugin.getLogger().info("Enviando " + player.getName() + " para o servidor: " + serverName);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao enviar " + player.getName() + " para o servidor " + serverName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean npcExists(String id) {
        return npcLocations.containsKey(id);
    }

    public Set<String> getNPCIds() {
        return npcLocations.keySet();
    }

    public Location getNPCLocation(String id) {
        return npcLocations.get(id);
    }

    public List<String> getNPCCommands(String id) {
        return new ArrayList<>(npcCommands.getOrDefault(id, new ArrayList<>()));
    }

    public Integer getNPCEntityId(String id, Player player) {
        Map<UUID, Integer> entities = npcEntities.get(id);
        return entities != null ? entities.get(player.getUniqueId()) : null;
    }

    public String getNPCName(String id) {
        return npcNames.get(id);
    }

    public String getNPCType(String id) {
        return npcTypes.get(id);
    }

    public String getNPCSkin(String id) {
        return npcSkins.get(id);
    }

    public double getHologramHeight(String id) {
        return npcHologramHeights.getOrDefault(id, 2.3);
    }

    public boolean getLookAtPlayer(String id) {
        return npcLookAtPlayer.getOrDefault(id, false);
    }

    public boolean getFloatAnimation(String id) {
        return npcFloatAnimation.getOrDefault(id, false);
    }

    public boolean getRotateAnimation(String id) {
        return npcRotateAnimation.getOrDefault(id, false);
    }

    public double getRotateSpeed(String id) {
        return npcRotateSpeed.getOrDefault(id, 5.0);
    }

    public double getFloatAmplitude(String id) {
        return npcFloatAmplitude.getOrDefault(id, 0.3);
    }

    public boolean getParticleEnabled(String id) {
        return npcParticleEnabled.getOrDefault(id, false);
    }

    public String getParticleType(String id) {
        return npcParticleType.getOrDefault(id, "FLAME");
    }

    public String getParticleAnimation(String id) {
        return npcParticleAnimation.getOrDefault(id, "CIRCLE");
    }

    public double getParticleRadius(String id) {
        return npcParticleRadius.getOrDefault(id, 1.0);
    }

    public double getParticleHeight(String id) {
        return npcParticleHeight.getOrDefault(id, 2.0);
    }

    public int getParticleAmount(String id) {
        return npcParticleAmount.getOrDefault(id, 1);
    }

    public float getParticleSpeed(String id) {
        return npcParticleSpeed.getOrDefault(id, 0.1f);
    }

    public long getParticleInterval(String id) {
        return npcParticleInterval.getOrDefault(id, 2L);
    }

    public List<String> getNPCHologramLines(String id, String language) {
        Map<String, List<String>> hologramsByLang = npcHolograms.get(id);
        if (hologramsByLang == null) return new ArrayList<>();

        List<String> lines = hologramsByLang.get(language);
        if (lines == null) {
            String defaultLang = plugin.getConfig().getString("default-language", "pt_PT");
            lines = hologramsByLang.get(defaultLang);
        }

        return lines != null ? new ArrayList<>(lines) : new ArrayList<>();
    }

    private Property getSkinProperty(String skin) {
        // Retornar textura e assinatura da skin
        return null; // TODO: implementar com API ou cache
    }
}