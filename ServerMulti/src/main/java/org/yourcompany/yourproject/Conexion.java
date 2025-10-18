package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    private static final String URL = "jdbc:postgresql://localhost:5432/clienteservidor?ssl=false";
    private static final String USER = "postgres";
    private static final String PASSWORD = "19102003";
    
    /**
     * Establece y devuelve una conexión a la base de datos.
     * @return Un objeto Connection o null si falla.
     */
    public static Connection getConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("Error al conectar a la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }
}