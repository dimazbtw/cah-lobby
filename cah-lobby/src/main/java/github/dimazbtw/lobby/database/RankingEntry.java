package github.dimazbtw.lobby.database;

import lombok.Data;

@Data
public class RankingEntry {

    private int position;
    private String playerName;
    private int kills;
    private int deaths;
    private int streak;

    public RankingEntry(int position, String playerName, int kills, int deaths) {
        this.position = position;
        this.playerName = playerName;
        this.kills = kills;
        this.deaths = deaths;
        this.streak = 0;
    }

    /**
     * Calcula o KDR
     */
    public double getKDR() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    /**
     * Retorna o KDR formatado
     */
    public String getFormattedKDR() {
        return String.format("%.2f", getKDR());
    }

    /**
     * Retorna a posição formatada com emoji
     */
    public String getFormattedPosition() {
        switch (position) {
            case 1:
                return "§6 #1";
            case 2:
                return "§7 #2";
            case 3:
                return "§c #3";
            default:
                return "§e#" + position;
        }
    }
}