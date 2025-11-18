package github.dimazbtw.lobby.views;

import lombok.Data;
import java.util.List;

/**
 * Representa um item de menu
 */
@Data
public class MenuItem {
    private final int slot;
    private final String material;
    private final String name;
    private final List<String> lore;
    private final List<String> actions;

    public MenuItem(int slot, String material, String name, List<String> lore, List<String> actions) {
        this.slot = slot;
        this.material = material;
        this.name = name;
        this.lore = lore != null ? lore : new java.util.ArrayList<>();
        this.actions = actions != null ? actions : new java.util.ArrayList<>();
    }
}