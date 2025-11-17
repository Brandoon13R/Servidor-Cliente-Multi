package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexion {

    private static final String URL = "jdbc:sqlite:clienteservidor.db";

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL);
            return conn;
        } catch (SQLException e) {
            System.err.println("Error conectando a SQLite: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void inicializarTablas() {
        
        String sqlUsuarios = """
            CREATE TABLE IF NOT EXISTS Usuarios (
                Nombre TEXT,
                Usuario TEXT PRIMARY KEY,
                Contrasena TEXT
            );
        """;

        String sqlBloqueados = """
            CREATE TABLE IF NOT EXISTS bloqueados (
                blocker_username TEXT,
                blocked_username TEXT,
                PRIMARY KEY (blocker_username, blocked_username),
                FOREIGN KEY (blocker_username) REFERENCES Usuarios(Usuario) ON DELETE CASCADE,
                FOREIGN KEY (blocked_username) REFERENCES Usuarios(Usuario) ON DELETE CASCADE
            );
        """;

        String sqlPartidas = """
            CREATE TABLE IF NOT EXISTS Partidas (
                partida_id INTEGER PRIMARY KEY AUTOINCREMENT,
                jugador1_usr TEXT,
                jugador2_usr TEXT,
                ganador_usr TEXT,
                FOREIGN KEY (jugador1_usr) REFERENCES Usuarios(Usuario),
                FOREIGN KEY (jugador2_usr) REFERENCES Usuarios(Usuario),
                FOREIGN KEY (ganador_usr) REFERENCES Usuarios(Usuario)
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            if (conn != null) {
                stmt.execute(sqlUsuarios);
                stmt.execute(sqlBloqueados);
                stmt.execute(sqlPartidas);
                System.out.println(">> Base de datos 'clienteservidor.db' y tablas verificadas correctamente.");
            }
        } catch (SQLException e) {
            System.err.println(">> Error inicializando tablas: " + e.getMessage());
        }
    }
}