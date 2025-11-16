package github.dimazbtw.lobby.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import github.dimazbtw.lobby.Main;
import github.dimazbtw.lobby.managers.NPCManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NPCInteractListener implements Listener {

    private final Main plugin;
    private final NPCManager npcManager;

    public NPCInteractListener(Main plugin) {
        this.plugin = plugin;
        this.npcManager = plugin.getNpcManager();

        // Registrar listener do ProtocolLib para ArmorStands (Heads)
        registerProtocolLibListener();
    }

    /**
     * Detecta clique com botão direito em NPCs (EntityPlayer)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        int entityId = event.getRightClicked().getEntityId();

        // Verificar se a entidade clicada é um NPC
        String npcId = findNPCByEntityId(player, entityId);
        if (npcId != null) {
            event.setCancelled(true);
            npcManager.executeCommands(npcId, player);
        }
    }

    /**
     * Registra listener do ProtocolLib para detectar cliques em ArmorStands (Heads)
     */
    private void registerProtocolLibListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Client.USE_ENTITY
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.USE_ENTITY) {
                    return;
                }

                Player player = event.getPlayer();

                // Obter entity ID do packet
                int entityId = event.getPacket().getIntegers().read(0);

                // Obter tipo de ação (INTERACT, ATTACK, INTERACT_AT)
                EnumWrappers.EntityUseAction action = event.getPacket().getEntityUseActions().read(0);

                // Verificar se é interação (clique direito)
                if (action == EnumWrappers.EntityUseAction.INTERACT ||
                        action == EnumWrappers.EntityUseAction.INTERACT_AT) {

                    // Verificar se a entidade clicada é um NPC/Head
                    String npcId = findNPCByEntityId(player, entityId);
                    if (npcId != null) {
                        // Cancelar o packet para evitar ações padrão
                        event.setCancelled(true);

                        // Executar comandos do NPC no próximo tick (evitar problemas de thread)
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            npcManager.executeCommands(npcId, player);
                        });
                    }
                }
            }
        });

        plugin.getLogger().info("ProtocolLib listener registrado para interações com NPCs!");
    }

    /**
     * Encontra o ID do NPC baseado no entity ID para um jogador específico
     */
    private String findNPCByEntityId(Player player, int entityId) {
        for (String npcId : npcManager.getNPCIds()) {
            Integer npcEntityId = npcManager.getNPCEntityId(npcId, player);
            if (npcEntityId != null && npcEntityId == entityId) {
                return npcId;
            }
        }
        return null;
    }
}