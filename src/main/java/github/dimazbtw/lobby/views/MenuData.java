
package github.dimazbtw.lobby.views;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Dados de um menu para um idioma específico
 */
@Data
public class MenuData {
    private final String title;
    private final int size;
    private final int updateInterval;
    private final List<MenuItem> items;

    public MenuData(String title, int size, int updateInterval) {
        this.title = title;
        this.size = size;
        this.updateInterval = updateInterval;
        this.items = new ArrayList<>();
    }

    public void addItem(MenuItem item) {
        items.add(item);
    }
}