package github.dimazbtw.lobby.utils;

import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PacketAS {

    private final JavaPlugin plugin;
    private final Map<Integer, Location> armorStands = new HashMap<>();
    private final Map<Integer, EntityArmorStand> armorStandEntities = new HashMap<>();
    private final Map<Integer, BukkitTask> rotationTasks = new HashMap<>();
    private final Map<Integer, BukkitTask> floatTasks = new HashMap<>();
    private final Map<Integer, Set<UUID>> playerVisibility = new ConcurrentHashMap<>();

    public PacketAS(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    /**
     * Spawna uma cabeça flutuante para um jogador específico
     */
    public int spawnForPlayer(Player player, Location location, ItemStack head, boolean visible, double rotationSpeed, boolean floatAnimation, double floatAmplitude) {
        EntityArmorStand armorStand = new EntityArmorStand(((CraftWorld) location.getWorld()).getHandle());
        armorStand.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        armorStand.setInvisible(true);  // ArmorStand sempre invisível
        armorStand.setGravity(false);
        armorStand.setBasePlate(false);
        armorStand.setSmall(false);

        PacketPlayOutSpawnEntityLiving spawnPacket = new PacketPlayOutSpawnEntityLiving(armorStand);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(spawnPacket);

        addHelmetForPlayer(player, armorStand, head);

        playerVisibility.computeIfAbsent(armorStand.getId(), k -> ConcurrentHashMap.newKeySet()).add(player.getUniqueId());
        armorStands.put(armorStand.getId(), location);
        armorStandEntities.put(armorStand.getId(), armorStand);

        // Se ambas animações estão ativas, usar animação combinada
        if (rotationSpeed > 0 && floatAnimation) {
            BukkitTask task = rotateAndFloatArmorStand(armorStand, rotationSpeed, floatAmplitude);
            rotationTasks.put(armorStand.getId(), task);
        } else {
            // Iniciar animações separadamente se necessário
            if (rotationSpeed > 0) {
                BukkitTask task = rotateArmorStandForPlayers(armorStand, rotationSpeed);
                rotationTasks.put(armorStand.getId(), task);
            }

            if (floatAnimation) {
                BukkitTask task = floatArmorStandForPlayers(armorStand, floatAmplitude);
                floatTasks.put(armorStand.getId(), task);
            }
        }

        return armorStand.getId();
    }

    /**
     * Spawna uma cabeça flutuante para todos os jogadores online
     */
    public int spawnAll(Location location, ItemStack head, boolean visible, double rotationSpeed, boolean floatAnimation) {
        return spawnAll(location, head, visible, rotationSpeed, floatAnimation, 0.3);
    }

    /**
     * Spawna uma cabeça flutuante para todos os jogadores online com amplitude customizada
     */
    public int spawnAll(Location location, ItemStack head, boolean visible, double rotationSpeed, boolean floatAnimation, double floatAmplitude) {
        EntityArmorStand armorStand = new EntityArmorStand(((CraftWorld) location.getWorld()).getHandle());
        armorStand.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        armorStand.setInvisible(true);  // ArmorStand sempre invisível, só a cabeça visível
        armorStand.setGravity(false);
        armorStand.setBasePlate(false);
        armorStand.setSmall(false);

        PacketPlayOutSpawnEntityLiving spawnPacket = new PacketPlayOutSpawnEntityLiving(armorStand);

        for (Player player : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(spawnPacket);
            playerVisibility.computeIfAbsent(armorStand.getId(), k -> ConcurrentHashMap.newKeySet()).add(player.getUniqueId());
        }

        addHelmetAll(armorStand, head);
        armorStands.put(armorStand.getId(), location);
        armorStandEntities.put(armorStand.getId(), armorStand);

        // Se ambas animações estão ativas, usar animação combinada
        if (rotationSpeed > 0 && floatAnimation) {
            BukkitTask task = rotateAndFloatArmorStand(armorStand, rotationSpeed, floatAmplitude);
            rotationTasks.put(armorStand.getId(), task);
        } else {
            if (rotationSpeed > 0) {
                BukkitTask task = rotateArmorStandForPlayers(armorStand, rotationSpeed);
                rotationTasks.put(armorStand.getId(), task);
            }

            if (floatAnimation) {
                BukkitTask task = floatArmorStandForPlayers(armorStand, floatAmplitude);
                floatTasks.put(armorStand.getId(), task);
            }
        }

        return armorStand.getId();
    }

    /**
     * Remove um ArmorStand para todos os jogadores
     */
    public void removeArmorStand(int entityId) {
        // Cancelar animações
        BukkitTask rotationTask = rotationTasks.remove(entityId);
        if (rotationTask != null) {
            rotationTask.cancel();
        }

        BukkitTask floatTask = floatTasks.remove(entityId);
        if (floatTask != null) {
            floatTask.cancel();
        }

        PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(new int[]{entityId});

        for (Player player : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(destroyPacket);
        }

        armorStands.remove(entityId);
        armorStandEntities.remove(entityId);
        playerVisibility.remove(entityId);
    }

    /**
     * Remove um ArmorStand apenas para um jogador específico
     */
    public void removeArmorStandForPlayer(Player player, int entityId) {
        PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(new int[]{entityId});
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(destroyPacket);

        Set<UUID> visibility = playerVisibility.get(entityId);
        if (visibility != null) {
            visibility.remove(player.getUniqueId());

            // Se nenhum jogador vê mais, limpar completamente
            if (visibility.isEmpty()) {
                removeArmorStand(entityId);
            }
        }
    }

    /**
     * Teleporta um ArmorStand para todos os jogadores
     */
    public void teleportArmorStand(int entityId, Location newLocation) {
        PacketPlayOutEntityTeleport teleportPacket = new PacketPlayOutEntityTeleport(
                entityId,
                MathHelper.floor(newLocation.getX() * 32.0D),
                MathHelper.floor(newLocation.getY() * 32.0D),
                MathHelper.floor(newLocation.getZ() * 32.0D),
                (byte) ((int) (newLocation.getYaw() * 256.0F / 360.0F)),
                (byte) ((int) (newLocation.getPitch() * 256.0F / 360.0F)),
                false
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(teleportPacket);
        }

        armorStands.put(entityId, newLocation);
    }

    /**
     * Teleporta um ArmorStand apenas para um jogador específico
     */
    public void teleportArmorStandForPlayer(Player player, int entityId, Location newLocation) {
        PacketPlayOutEntityTeleport teleportPacket = new PacketPlayOutEntityTeleport(
                entityId,
                MathHelper.floor(newLocation.getX() * 32.0D),
                MathHelper.floor(newLocation.getY() * 32.0D),
                MathHelper.floor(newLocation.getZ() * 32.0D),
                (byte) ((int) (newLocation.getYaw() * 256.0F / 360.0F)),
                (byte) ((int) (newLocation.getPitch() * 256.0F / 360.0F)),
                false
        );

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(teleportPacket);
    }

    /**
     * Adiciona capacete (cabeça) para um jogador específico
     */
    private void addHelmetForPlayer(Player player, EntityArmorStand armorStand, ItemStack helmet) {
        if (helmet == null) return;

        net.minecraft.server.v1_8_R3.ItemStack nmsHelmet = CraftItemStack.asNMSCopy(helmet);
        PacketPlayOutEntityEquipment equipmentPacket = new PacketPlayOutEntityEquipment(armorStand.getId(), 4, nmsHelmet);

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(equipmentPacket);
    }

    /**
     * Adiciona capacete (cabeça) para todos os jogadores
     */
    private void addHelmetAll(EntityArmorStand armorStand, ItemStack helmet) {
        if (helmet == null) return;

        net.minecraft.server.v1_8_R3.ItemStack nmsHelmet = CraftItemStack.asNMSCopy(helmet);
        PacketPlayOutEntityEquipment equipmentPacket = new PacketPlayOutEntityEquipment(armorStand.getId(), 4, nmsHelmet);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) onlinePlayer).getHandle().playerConnection.sendPacket(equipmentPacket);
        }
    }

    /**
     * Rotaciona um ArmorStand continuamente (para todos os jogadores que veem)
     */
    private BukkitTask rotateArmorStandForPlayers(final EntityArmorStand armorStand, final double speed) {
        return new BukkitRunnable() {
            private float yaw = 0.0F;

            @Override
            public void run() {
                if (!armorStand.isAlive()) {
                    this.cancel();
                    return;
                }

                yaw += speed;
                if (yaw >= 360.0F) {
                    yaw -= 360.0F;
                }

                armorStand.yaw = yaw;

                PacketPlayOutEntityTeleport teleportPacket = new PacketPlayOutEntityTeleport(
                        armorStand.getId(),
                        MathHelper.floor(armorStand.locX * 32.0D),
                        MathHelper.floor(armorStand.locY * 32.0D),
                        MathHelper.floor(armorStand.locZ * 32.0D),
                        (byte) ((int) (yaw * 256.0F / 360.0F)),
                        (byte) ((int) (armorStand.pitch * 256.0F / 360.0F)),
                        false
                );

                // Enviar apenas para jogadores que veem este ArmorStand
                Set<UUID> viewers = playerVisibility.get(armorStand.getId());
                if (viewers != null) {
                    for (UUID viewerUUID : viewers) {
                        Player viewer = Bukkit.getPlayer(viewerUUID);
                        if (viewer != null && viewer.isOnline()) {
                            ((CraftPlayer) viewer).getHandle().playerConnection.sendPacket(teleportPacket);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Rotaciona E flutua um ArmorStand simultaneamente (animação combinada)
     */
    private BukkitTask rotateAndFloatArmorStand(final EntityArmorStand armorStand, final double speed, final double amplitude) {
        return new BukkitRunnable() {
            private float yaw = 0.0F;
            private double t = 0.0D;
            private final double initialY = armorStand.locY;

            @Override
            public void run() {
                if (!armorStand.isAlive()) {
                    this.cancel();
                    return;
                }

                // Atualizar rotação
                yaw += speed;
                if (yaw >= 360.0F) {
                    yaw -= 360.0F;
                }

                // Atualizar flutuação
                double yOffset = Math.sin(t) * amplitude;
                t += 0.15D;
                double newY = initialY + yOffset;

                armorStand.yaw = yaw;
                armorStand.setPosition(armorStand.locX, newY, armorStand.locZ);

                PacketPlayOutEntityTeleport teleportPacket = new PacketPlayOutEntityTeleport(
                        armorStand.getId(),
                        MathHelper.floor(armorStand.locX * 32.0D),
                        MathHelper.floor(newY * 32.0D),
                        MathHelper.floor(armorStand.locZ * 32.0D),
                        (byte) ((int) (yaw * 256.0F / 360.0F)),
                        (byte) ((int) (armorStand.pitch * 256.0F / 360.0F)),
                        false
                );

                // Enviar apenas para jogadores que veem este ArmorStand
                Set<UUID> viewers = playerVisibility.get(armorStand.getId());
                if (viewers != null) {
                    for (UUID viewerUUID : viewers) {
                        Player viewer = Bukkit.getPlayer(viewerUUID);
                        if (viewer != null && viewer.isOnline()) {
                            ((CraftPlayer) viewer).getHandle().playerConnection.sendPacket(teleportPacket);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Faz um ArmorStand flutuar suavemente (para todos os jogadores que veem)
     */
    private BukkitTask floatArmorStandForPlayers(final EntityArmorStand armorStand, final double amplitude) {
        return new BukkitRunnable() {
            private double t = 0.0D;
            private final double initialY = armorStand.locY;

            @Override
            public void run() {
                if (!armorStand.isAlive()) {
                    this.cancel();
                    return;
                }

                double yOffset = Math.sin(t) * amplitude;
                t += 0.15D;

                double newY = initialY + yOffset;
                armorStand.setPosition(armorStand.locX, newY, armorStand.locZ);

                // Usar teleport em vez de metadata para melhor sincronização
                PacketPlayOutEntityTeleport teleportPacket = new PacketPlayOutEntityTeleport(
                        armorStand.getId(),
                        MathHelper.floor(armorStand.locX * 32.0D),
                        MathHelper.floor(newY * 32.0D),
                        MathHelper.floor(armorStand.locZ * 32.0D),
                        (byte) ((int) (armorStand.yaw * 256.0F / 360.0F)),
                        (byte) ((int) (armorStand.pitch * 256.0F / 360.0F)),
                        false
                );

                // Enviar apenas para jogadores que veem este ArmorStand
                Set<UUID> viewers = playerVisibility.get(armorStand.getId());
                if (viewers != null) {
                    for (UUID viewerUUID : viewers) {
                        Player viewer = Bukkit.getPlayer(viewerUUID);
                        if (viewer != null && viewer.isOnline()) {
                            ((CraftPlayer) viewer).getHandle().playerConnection.sendPacket(teleportPacket);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Obtém a localização de um ArmorStand
     */
    public Location getArmorStandLocation(int entityId) {
        return armorStands.get(entityId);
    }

    /**
     * Verifica se um jogador pode ver um ArmorStand específico
     */
    public boolean canPlayerSee(Player player, int entityId) {
        Set<UUID> visibility = playerVisibility.get(entityId);
        return visibility != null && visibility.contains(player.getUniqueId());
    }

    /**
     * Cancela todas as animações
     */
    public void cancelAllAnimations() {
        for (BukkitTask task : rotationTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        rotationTasks.clear();

        for (BukkitTask task : floatTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        floatTasks.clear();
    }

    /**
     * Remove todos os ArmorStands
     */
    public void removeAll() {
        cancelAllAnimations();

        if (!armorStands.isEmpty()) {
            PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(
                    armorStands.keySet().stream().mapToInt(Integer::intValue).toArray()
            );

            for (Player player : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(destroyPacket);
            }
        }

        armorStands.clear();
        armorStandEntities.clear();
        playerVisibility.clear();
    }
}