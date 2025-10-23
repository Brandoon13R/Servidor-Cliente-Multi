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
    static ConcurrentHashMap<String, String> invitacionesGato = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, JuegoGato> juegosActivos = new ConcurrentHashMap<>();


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

    public static synchronized boolean doesUserExist(String username) {
        // ... (Tu código existente)
        String checkSql = "SELECT COUNT(*) FROM Usuarios WHERE Usuario = ?;";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static synchronized boolean isBlocked(String blocker_username, String blocked_username) {
        String checkSql = "SELECT COUNT(*) FROM bloqueados WHERE blocker_username = ? AND blocked_username = ?;";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, blocker_username);
            checkStmt.setString(2, blocked_username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static synchronized String blockUser(String blocker_username, String blocked_username) {
        if (blocker_username.equalsIgnoreCase(blocked_username)) {
            return "Error: No puedes bloquearte a ti mismo.";
        }
        if (!doesUserExist(blocked_username)) {
            return "Error: El usuario '" + blocked_username + "' no existe.";
        }
        if (isBlocked(blocker_username, blocked_username)) {
            return "Error: Ya tienes a '" + blocked_username + "' bloqueado.";
        }

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

    public static synchronized String unblockUser(String blocker_username, String blocked_username) {
        if (!doesUserExist(blocked_username)) {
            return "Error: El usuario '" + blocked_username + "' no existe.";
        }
        if (!isBlocked(blocker_username, blocked_username)) {
            return "Error: No tenías a '" + blocked_username + "' bloqueado.";
        }

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
    
    public static void broadcastMensaje(String formattedMessage, UnCliente sender) {
        String senderUsername = (sender == null) ? null : sender.getUsername();

        for (UnCliente recipient : clientes.values()) {
            
            if (senderUsername != null) {
                if (recipient.getId().equals(sender.getId())) {
                    continue; 
                }
                if (isBlocked(recipient.getUsername(), senderUsername)) {
                    System.out.println("Mensaje de " + senderUsername + " bloqueado para " + recipient.getUsername());
                    continue;
                }
            }

            recipient.enviarMensaje(formattedMessage);
        }
    }

    /**
     * Busca un cliente conectado por su nombre de usuario (login).
     * @param username El nombre de usuario a buscar.
     * @return El objeto UnCliente si está conectado, o null si no.
     */
    public static UnCliente getClientePorUsername(String username) {
        if (username == null) return null;
        for (UnCliente c : clientes.values()) {
            if (c.getUsername() != null && c.getUsername().equalsIgnoreCase(username)) {
                return c;
            }
        }
        return null;
    }

    public static void removerCliente(String clienteId) {
        UnCliente clienteRemovido = clientes.remove(clienteId);
        
        if (clienteRemovido != null) {
            String idJuego = clienteRemovido.getIdJuegoActual();
            if (idJuego != null) {
                JuegoGato juego = juegosActivos.get(idJuego);
                if (juego != null && juego.isJuegoActivo()) {
                    UnCliente oponente = juego.getOponente(clienteRemovido);
                    if (oponente != null) {
                        oponente.enviarMensaje("--- PARTIDA FINALIZADA ---");
                        oponente.enviarMensaje("Tu oponente, " + clienteRemovido.getNombre() + ", se ha desconectado.");
                        oponente.enviarMensaje("¡Has ganado la partida por default!");
                        oponente.setIdJuegoActual(null);
                    }
                    juego.terminarJuego();
                    juegosActivos.remove(idJuego);
                }
            }

            if (clienteRemovido.getNombre() != null && !clienteRemovido.getNombre().isEmpty()) {
                System.out.println("Cliente " + clienteRemovido.getNombre() + " desconectado. Clientes restantes: " + clientes.size());
                broadcastMensaje("'" + clienteRemovido.getNombre() + "' ha abandonado el chat.", null);
            } else {
                System.out.println("Cliente no autenticado " + clienteId + " desconectado.");
            }
        }
    }
}