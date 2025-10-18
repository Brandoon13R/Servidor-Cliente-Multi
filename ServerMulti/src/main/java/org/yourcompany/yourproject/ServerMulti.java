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
    
    // Método de registro (sin cambios)
    public static synchronized boolean registerUser(String nombre, String usuario, String contrasena) {
        String checkSql = "SELECT COUNT(*) FROM Usuarios WHERE Usuario = ?;";
        String insertSql = "INSERT INTO Usuarios (Nombre, Usuario, Contrasena) VALUES (?, ?, ?);";
        
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, usuario);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return false; 
                    }
                }
            }
            
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, nombre);
                insertStmt.setString(2, usuario);
                insertStmt.setString(3, contrasena);
                int rowsAffected = insertStmt.executeUpdate();
                return rowsAffected > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error de SQL al registrar usuario: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Método de login (sin cambios)
    public static synchronized String loginUser(String usuario, String contrasena) {
        String loginSql = "SELECT Nombre, Contrasena FROM Usuarios WHERE Usuario = ?;";
        
        try (Connection conn = Conexion.getConnection();
             PreparedStatement stmt = conn.prepareStatement(loginSql)) {
            
            if (conn == null) return null;
            stmt.setString(1, usuario);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("Contrasena");
                    if (storedPassword.equals(contrasena)) {
                        return rs.getString("Nombre");
                    } else {
                        return null; 
                    }
                } else {
                    return null; 
                }
            }
        } catch (SQLException e) {
            System.err.println("Error de SQL al iniciar sesión: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verifica si un 'Usuario' (login) existe en la BD.
     */
    public static synchronized boolean doesUserExist(String username) {
        String checkSql = "SELECT COUNT(*) FROM Usuarios WHERE Usuario = ?;";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                // rs.next() es necesario para moverse a la primera fila de resultados
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si 'blocker_username' tiene bloqueado a 'blocked_username'.
     */
    public static synchronized boolean isBlocked(String blocker_username, String blocked_username) {
        String checkSql = "SELECT COUNT(*) FROM bloqueados WHERE blocker_username = ? AND blocked_username = ?;";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, blocker_username);
            checkStmt.setString(2, blocked_username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                // rs.next() es necesario
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Intenta bloquear un usuario. Devuelve un mensaje de estado.
     */
    public static synchronized String blockUser(String blocker_username, String blocked_username) {
        // Manejo de errores
        if (blocker_username.equalsIgnoreCase(blocked_username)) {
            return "Error: No puedes bloquearte a ti mismo.";
        }
        // Verificamos si el usuario a bloquear existe
        if (!doesUserExist(blocked_username)) {
            return "Error: El usuario '" + blocked_username + "' no existe.";
        }
        // Verificamos si ya está bloqueado
        if (isBlocked(blocker_username, blocked_username)) {
            return "Error: Ya tienes a '" + blocked_username + "' bloqueado.";
        }

        // Lógica de bloqueo
        String insertSql = "INSERT INTO bloqueados (blocker_username, blocked_username) VALUES (?, ?);";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            
            insertStmt.setString(1, blocker_username);
            insertStmt.setString(2, blocked_username);
            insertStmt.executeUpdate();
            System.out.println("Bloqueo exitoso: " + blocker_username + " bloqueó a " + blocked_username);
            return "Has bloqueado a '" + blocked_username + "'. No verás sus mensajes.";

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error de servidor al intentar bloquear.";
        }
    }

    /**
     * Intenta desbloquear un usuario. Devuelve un mensaje de estado.
     */
    public static synchronized String unblockUser(String blocker_username, String blocked_username) {
        // Manejo de errores
        if (!doesUserExist(blocked_username)) {
            return "Error: El usuario '" + blocked_username + "' no existe.";
        }
        if (!isBlocked(blocker_username, blocked_username)) {
            return "Error: No tenías a '" + blocked_username + "' bloqueado.";
        }

        // Lógica de desbloqueo
        String deleteSql = "DELETE FROM bloqueados WHERE blocker_username = ? AND blocked_username = ?;";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            
            deleteStmt.setString(1, blocker_username);
            deleteStmt.setString(2, blocked_username);
            deleteStmt.executeUpdate();
            System.out.println("Desbloqueo exitoso: " + blocker_username + " desbloqueó a " + blocked_username);
            return "Has desbloqueado a '" + blocked_username + "'.";

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error de servidor al intentar desbloquear.";
        }
    }
    /**
     * Envía un mensaje a todos los clientes, excepto al emisor y a quienes
     * hayan bloqueado al emisor.
     * @param formattedMessage Mensaje ya formateado (ej. "Nombre: Hola")
     * @param sender El objeto UnCliente del emisor (o null si es un mensaje del sistema)
     */
    public static void broadcastMensaje(String formattedMessage, UnCliente sender) {
        String senderUsername = (sender == null) ? null : sender.getUsername();

        for (UnCliente recipient : clientes.values()) {
            
            if (senderUsername != null) {
                // 1. No enviar el mensaje de vuelta al emisor
                if (recipient.getId().equals(sender.getId())) {
                    continue; // Saltar al siguiente cliente
                }
                
                // Si el destinatario (recipient) ha bloqueado al emisor (sender), no se lo enviamos.
                if (isBlocked(recipient.getUsername(), senderUsername)) {
                    System.out.println("Mensaje de " + senderUsername + " bloqueado para " + recipient.getUsername());
                    continue; // Saltar al siguiente cliente
                }
            }

            recipient.enviarMensaje(formattedMessage);
        }
    }
    
    public static void removerCliente(String clienteId) {
        UnCliente clienteRemovido = clientes.remove(clienteId);
        if (clienteRemovido != null) {
            if (clienteRemovido.getNombre() != null && !clienteRemovido.getNombre().isEmpty()) {
                 System.out.println("Cliente " + clienteRemovido.getNombre() + " desconectado. Clientes restantes: " + clientes.size());
                broadcastMensaje("'" + clienteRemovido.getNombre() + "' ha abandonado el chat.", null);
            } else {
                System.out.println("Cliente no autenticado " + clienteId + " desconectado.");
            }
        }
    }
}