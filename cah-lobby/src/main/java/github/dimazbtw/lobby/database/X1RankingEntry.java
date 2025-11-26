package github.dimazbtw.lobby.database;

import lombok.Data;

@Data
public class X1RankingEntry {

    private int position;
    private String playerName;
    private int wins;
    private int losses;

    public X1RankingEntry(int position, String playerName, int wins, int losses) {
        this.position = position;
        this.playerName = playerName;
        this.wins = wins;
        this.losses = losses;
    }

    public int getTotalMatches() {
        return wins + losses;
    }

    public double getKDR() {
        if (losses == 0) return wins;
        return (double) wins / losses;
    }

    public String getFormattedKDR() {
        return String.format("%.2f", getKDR());
    }

    public double getWinRate() {
        if (getTotalMatches() == 0) return 0;
        return (double) wins / getTotalMatches() * 100;
    }

    public String getFormattedWinRate() {
        return String.format("%.1f%%", getWinRate());
    }

    public String getFormattedPosition() {
        switch (position) {
            case 1: return "§6§l#1";
            case 2: return "§7§l#2";
            case 3: return "§c§l#3";
            default: return "§e#" + position;
        }
    }
}