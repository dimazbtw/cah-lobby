package github.dimazbtw.lobby.database;

import lombok.Data;

@Data
public class X1Stats {

    private int wins;
    private int losses;

    public X1Stats(int wins, int losses) {
        this.wins = wins;
        this.losses = losses;
    }

    public void addWin() {
        this.wins++;
    }

    public void addLoss() {
        this.losses++;
    }

    public int getTotalMatches() {
        return wins + losses;
    }

    public double getWinRate() {
        if (getTotalMatches() == 0) return 0;
        return (double) wins / getTotalMatches() * 100;
    }

    public String getFormattedWinRate() {
        return String.format("%.1f%%", getWinRate());
    }

    public double getKDR() {
        if (losses == 0) return wins;
        return (double) wins / losses;
    }

    public String getFormattedKDR() {
        return String.format("%.2f", getKDR());
    }
}