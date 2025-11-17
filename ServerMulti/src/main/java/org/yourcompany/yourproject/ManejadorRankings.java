package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ManejadorRankings {    
    /**
     * @param j1_usr Usuario del Jugador 1
     * @param j2_usr Usuario del Jugador 2
     * @param ganador_usr Usuario del Ganador (ENVIAR null si fue empate)
     */
    public static synchronized void registrarPartida(String j1_usr, String j2_usr, String ganador_usr) {
        String sql = "INSERT INTO Partidas (jugador1_usr, jugador2_usr, ganador_usr) VALUES (?, ?, ?)";
        
        try (Connection conn = Conexion.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, j1_usr);
            pstmt.setString(2, j2_usr);
            
            if (ganador_usr != null) {
                pstmt.setString(3, ganador_usr);
            } else {
                pstmt.setNull(3, java.sql.Types.VARCHAR);
            }
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error al registrar la partida: " + e.getMessage());
        }
    }

    /**
     * @return
     */
    public static String getRankingGeneralFormateado() {
        String sql = """
            SELECT usuario, SUM(puntos) AS puntos_totales
            FROM (
                -- Victorias (2 puntos)
                SELECT ganador_usr AS usuario, 2 AS puntos FROM Partidas WHERE ganador_usr IS NOT NULL
                UNION ALL
                -- Empates (1 punto para J1)
                SELECT jugador1_usr AS usuario, 1 AS puntos FROM Partidas WHERE ganador_usr IS NULL
                UNION ALL
                -- Empates (1 punto para J2)
                SELECT jugador2_usr AS usuario, 1 AS puntos FROM Partidas WHERE ganador_usr IS NULL
            ) AS resultados_combinados
            WHERE usuario IS NOT NULL
            GROUP BY usuario
            ORDER BY puntos_totales DESC;
            """;
        
        StringBuilder ranking = new StringBuilder("--- 🏆 RANKING GENERAL (GATO) 🏆 ---\n");
        ranking.append("Pos. | Jugador         | Puntos\n");
        ranking.append("------------------------------------\n");
        
        try (Connection conn = Conexion.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            int pos = 1;
            while (rs.next()) {
                String usuario = rs.getString("usuario");
                int puntos = rs.getInt("puntos_totales");
                
                ranking.append(String.format("%-4s | %-15s | %s pts\n", pos + ".", usuario, puntos));
                pos++;
            }
            
            if (pos == 1) {
                ranking.append("Aún no hay partidas registradas.\n");
            }

        } catch (SQLException e) {
            System.err.println("Error al calcular ranking general: " + e.getMessage());
            return "Error al obtener el ranking.";
        }
        
        return ranking.toString();
    }
    
    /**
     * @return
     */
    public static String getEstadisticasH2HFormateado(String usuario1, String usuario2) {
        String sqlTotal = "SELECT COUNT(*) FROM Partidas WHERE (jugador1_usr = ? AND jugador2_usr = ?) OR (jugador1_usr = ? AND jugador2_usr = ?)";
        String sqlWinsU1 = "SELECT COUNT(*) FROM Partidas WHERE ganador_usr = ? AND ((jugador1_usr = ? AND jugador2_usr = ?) OR (jugador1_usr = ? AND jugador2_usr = ?))";
        String sqlTies = "SELECT COUNT(*) FROM Partidas WHERE ganador_usr IS NULL AND ((jugador1_usr = ? AND jugador2_usr = ?) OR (jugador1_usr = ? AND jugador2_usr = ?))";

        int totalPartidas = 0;
        int winsU1 = 0;
        int empates = 0;
        int winsU2 = 0;

        try (Connection conn = Conexion.getConnection()) {
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlTotal)) {
                pstmt.setString(1, usuario1); pstmt.setString(2, usuario2);
                pstmt.setString(3, usuario2); pstmt.setString(4, usuario1);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) totalPartidas = rs.getInt(1);
            }
            
            if (totalPartidas == 0) {
                return String.format("Aún no hay partidas registradas entre %s y %s.", usuario1, usuario2);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlWinsU1)) {
                pstmt.setString(1, usuario1); // Ganador
                pstmt.setString(2, usuario1); pstmt.setString(3, usuario2);
                pstmt.setString(4, usuario2); pstmt.setString(5, usuario1);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) winsU1 = rs.getInt(1);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlTies)) {
                pstmt.setString(1, usuario1); pstmt.setString(2, usuario2);
                pstmt.setString(3, usuario2); pstmt.setString(4, usuario1);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) empates = rs.getInt(1);
            }
            
            winsU2 = totalPartidas - winsU1 - empates;
            
            double porc_u1 = (double) winsU1 / totalPartidas * 100.0;
            double porc_u2 = (double) winsU2 / totalPartidas * 100.0;
            double porc_tie = (double) empates / totalPartidas * 100.0;

            StringBuilder h2h = new StringBuilder(String.format("--- 📊 H2H: %s vs %s 📊 ---\n", usuario1, usuario2));
            h2h.append(String.format("Total Partidas: %d\n", totalPartidas));
            h2h.append("------------------------------------\n");
            h2h.append(String.format("Victorias %s: %d (%.1f%%)\n", usuario1, winsU1, porc_u1));
            h2h.append(String.format("Victorias %s: %d (%.1f%%)\n", usuario2, winsU2, porc_u2));
            h2h.append(String.format("Empates: %d (%.1f%%)\n", empates, porc_tie));
            
            return h2h.toString();
            
        } catch (SQLException e) {
            System.err.println("Error al calcular H2H: " + e.getMessage());
            return "Error al obtener estadísticas H2H.";
        }
    }
}