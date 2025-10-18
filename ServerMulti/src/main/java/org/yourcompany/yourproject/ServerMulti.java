package org.yourcompany.yourproject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMulti {
    
    static ConcurrentHashMap<String, UnCliente> clientes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket servidorSocket = new ServerSocket(8080)) {
            System.out.println("Servidor iniciado en el puerto 8080. Esperando clientes...");
            
            while (true) {
                Socket socketCliente = servidorSocket.accept(); 
                System.out.println("Nuevo cliente conectado: " + socketCliente.getInetAddress());
                
                String clienteId = UUID.randomUUID().toString();
                
                UnCliente cliente = new UnCliente(socketCliente, clienteId); 
                clientes.put(clienteId, cliente);
                
                new Thread(cliente).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
    
    /**
     * Registra un nuevo usuario en la BD usando la tabla 'Usuarios'.
     * AHORA RECIBE 3 PARÁMETROS.
     */
    public static synchronized boolean registerUser(String nombre, String usuario, String contrasena) {
        String checkSql = "SELECT COUNT(*) FROM Usuarios WHERE Usuario = ?;";
        String insertSql = "INSERT INTO Usuarios (Nombre, Usuario, Contrasena) VALUES (?, ?, ?);";
        
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return false;

            // 1. Verificar si el 'Usuario' ya existe
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, usuario);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        System.out.println("Intento de registro fallido: Usuario '" + usuario + "' ya existe.");
                        return false; // El 'Usuario' ya existe
                    }
                }
            }
            
            // 2. Si no existe, insertarlo
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, nombre);
                insertStmt.setString(2, usuario);
                insertStmt.setString(3, contrasena); // ADVERTENCIA: Texto plano
                
                int rowsAffected = insertStmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("Nuevo usuario registrado en la BD: " + usuario);
                    return true;
                } else {
                    return false;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error de SQL al registrar usuario: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica las credenciales en la tabla 'Usuarios'.
     * AHORA DEVUELVE EL 'Nombre' (String) SI ES EXITOSO, O NULL SI FALLA.
     */
    public static synchronized String loginUser(String usuario, String contrasena) {
        String loginSql = "SELECT Nombre, Contrasena FROM Usuarios WHERE Usuario = ?;";
        
        try (Connection conn = Conexion.getConnection();
             PreparedStatement stmt = conn.prepareStatement(loginSql)) {
            
            if (conn == null) return null;

            stmt.setString(1, usuario);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Usuario encontrado, comparar la contraseña
                    String storedPassword = rs.getString("Contrasena");
                    
                    if (storedPassword.equals(contrasena)) { // ADVERTENCIA: Comparación en texto plano
                        System.out.println("Inicio de sesión exitoso para: " + usuario);
                        return rs.getString("Nombre"); // Devuelve el Nombre real
                    } else {
                        System.out.println("Inicio de sesión fallido (contraseña incorrecta) para: " + usuario);
                        return null; // Contraseña incorrecta
                    }
                } else {
                    System.out.println("Inicio de sesión fallido (usuario no encontrado) para: " + usuario);
                    return null; // Usuario no encontrado
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error de SQL al iniciar sesión: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public static void broadcastMensaje(String mensaje, String emisorId) {
        for (UnCliente cliente : clientes.values()) {
            if (emisorId == null || !cliente.getId().equals(emisorId)) {
                cliente.enviarMensaje(mensaje);
            }
        }
    }
    
    public static void removerCliente(String clienteId) {
        UnCliente clienteRemovido = clientes.remove(clienteId);
        if (clienteRemovido != null) {
            // Esta lógica ahora funciona perfecto: si el 'nombre' es null (porque nunca se logueó),
            // no anunciará nada.
            if (clienteRemovido.getNombre() != null && !clienteRemovido.getNombre().isEmpty()) {
                 System.out.println("Cliente " + clienteRemovido.getNombre() + " desconectado. Clientes restantes: " + clientes.size());
                broadcastMensaje("'" + clienteRemovido.getNombre() + "' ha abandonado el chat.", null);
            } else {
                System.out.println("Cliente no autenticado " + clienteId + " desconectado.");
            }
        }
    }
}