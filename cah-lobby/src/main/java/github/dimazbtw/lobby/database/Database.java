package github.dimazbtw.lobby.database;

import github.dimazbtw.lobby.Main;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.*;

public class Database {

    private final Main plugin;
    private Connection connection;
    private final File databaseFile;

    public Database(Main plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "database.db");
        connect();
        createTables();
    }

    /**
     * Conecta ao banco de dados SQLite
     */
    private void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            plugin.getLogger().info("Conectado ao banco de dados SQLite!");

        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Erro ao conectar ao banco de dados!");
            e.printStackTrace();
        }
    }

    /**
     * Cria as tabelas necessárias
     */
    private void createTables() {
        String createPvPStatsTable = "CREATE TABLE IF NOT EXISTS pvp_stats (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "player_name VARCHAR(16) NOT NULL," +
                "kills INTEGER DEFAULT 0," +
                "deaths INTEGER DEFAULT 0," +
                "highest_streak INTEGER DEFAULT 0," +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPvPStatsTable);
            plugin.getLogger().info("Tabelas do banco de dados criadas/verificadas!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao criar tabelas!");
            e.printStackTrace();
        }
    }

    /**
     * Carrega as estatísticas de um jogador
     */
    public PvPStats loadStats(Player player) {
        String query = "SELECT * FROM pvp_stats WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, player.getUniqueId().toString());

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new PvPStats(
                        rs.getInt("kills"),
                        rs.getInt("deaths"),
                        rs.getInt("highest_streak")
                );
            } else {
                // Jogador novo, criar estatísticas zeradas
                createStats(player);
                return new PvPStats(0, 0, 0);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao carregar estatísticas de " + player.getName());
            e.printStackTrace();
            return new PvPStats(0, 0, 0);
        }
    }

    /**
     * Cria estatísticas para um jogador novo
     */
    private void createStats(Player player) {
        String insert = "INSERT INTO pvp_stats (uuid, player_name, kills, deaths, highest_streak) VALUES (?, ?, 0, 0, 0)";

        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getName());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao criar estatísticas para " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Salva as estatísticas de um jogador
     */
    public void saveStats(Player player, PvPStats stats) {
        String update = "UPDATE pvp_stats SET player_name = ?, kills = ?, deaths = ?, highest_streak = ?, last_updated = CURRENT_TIMESTAMP WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(update)) {
            stmt.setString(1, player.getName());
            stmt.setInt(2, stats.getKills());
            stmt.setInt(3, stats.getDeaths());
            stmt.setInt(4, stats.getHighestStreak());
            stmt.setString(5, player.getUniqueId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao salvar estatísticas de " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Adiciona uma kill ao jogador
     */
    public void addKill(Player player, int currentStreak) {
        PvPStats stats = loadStats(player);
        stats.addKill();

        // Atualizar highest streak se necessário
        if (currentStreak > stats.getHighestStreak()) {
            stats.setHighestStreak(currentStreak);
        }

        saveStats(player, stats);
    }

    /**
     * Adiciona uma morte ao jogador
     */
    public void addDeath(Player player) {
        PvPStats stats = loadStats(player);
        stats.addDeath();
        saveStats(player, stats);
    }

    /**
     * Obtém o ranking de kills (Top 10)
     */
    public List<RankingEntry> getTopKills(int limit) {
        String query = "SELECT player_name, kills, deaths FROM pvp_stats ORDER BY kills DESC LIMIT ?";
        List<RankingEntry> ranking = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            int position = 1;
            while (rs.next()) {
                ranking.add(new RankingEntry(
                        position++,
                        rs.getString("player_name"),
                        rs.getInt("kills"),
                        rs.getInt("deaths")
                ));
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao obter ranking de kills!");
            e.printStackTrace();
        }

        return ranking;
    }

    /**
     * Obtém o ranking de KDR (Top 10)
     */
    public List<RankingEntry> getTopKDR(int limit) {
        String query = "SELECT player_name, kills, deaths FROM pvp_stats WHERE deaths > 0 ORDER BY (CAST(kills AS REAL) / deaths) DESC LIMIT ?";
        List<RankingEntry> ranking = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            int position = 1;
            while (rs.next()) {
                ranking.add(new RankingEntry(
                        position++,
                        rs.getString("player_name"),
                        rs.getInt("kills"),
                        rs.getInt("deaths")
                ));
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao obter ranking de KDR!");
            e.printStackTrace();
        }

        return ranking;
    }

    /**
     * Obtém o ranking de killstreak (Top 10)
     */
    public List<RankingEntry> getTopStreak(int limit) {
        String query = "SELECT player_name, kills, deaths, highest_streak FROM pvp_stats ORDER BY highest_streak DESC LIMIT ?";
        List<RankingEntry> ranking = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            int position = 1;
            while (rs.next()) {
                RankingEntry entry = new RankingEntry(
                        position++,
                        rs.getString("player_name"),
                        rs.getInt("kills"),
                        rs.getInt("deaths")
                );
                entry.setStreak(rs.getInt("highest_streak"));
                ranking.add(entry);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao obter ranking de streak!");
            e.printStackTrace();
        }

        return ranking;
    }

    /**
     * Obtém a posição do jogador no ranking de kills
     */
    public int getPlayerKillsPosition(Player player) {
        String query = "SELECT COUNT(*) + 1 as position FROM pvp_stats WHERE kills > (SELECT kills FROM pvp_stats WHERE uuid = ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("position");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao obter posição de " + player.getName());
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Obtém a posição do jogador no ranking de KDR
     */
    public int getPlayerKDRPosition(Player player) {
        String query = "SELECT COUNT(*) + 1 as position FROM pvp_stats " +
                "WHERE deaths > 0 AND (CAST(kills AS REAL) / deaths) > " +
                "(SELECT CAST(kills AS REAL) / deaths FROM pvp_stats WHERE uuid = ? AND deaths > 0)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("position");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao obter posição KDR de " + player.getName());
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Fecha a conexão com o banco de dados
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Conexão com banco de dados fechada!");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao fechar conexão com banco de dados!");
            e.printStackTrace();
        }
    }

    /**
     * Reconecta ao banco de dados se a conexão foi perdida
     */
    public void reconnect() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao verificar conexão!");
            e.printStackTrace();
        }
    }
}