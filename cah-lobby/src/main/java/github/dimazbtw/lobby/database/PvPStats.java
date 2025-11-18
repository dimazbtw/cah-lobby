package github.dimazbtw.lobby.database;

import lombok.Data;

@Data
public class PvPStats {

    private int kills;
    private int deaths;
    private int highestStreak;

    public PvPStats(int kills, int deaths, int highestStreak) {
        this.kills = kills;
        this.deaths = deaths;
        this.highestStreak = highestStreak;
    }

    /**
     * Adiciona uma kill
     */
    public void addKill() {
        this.kills++;
    }

    /**
     * Adiciona uma morte
     */
    public void addDeath() {
        this.deaths++;
    }

    /**
     * Calcula o KDR (Kill/Death Ratio)
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
     * Verifica se o jogador tem estatísticas
     */
    public boolean hasStats() {
        return kills > 0 || deaths > 0;
    }
}