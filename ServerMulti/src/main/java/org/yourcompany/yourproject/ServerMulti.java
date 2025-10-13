package org.yourcompany.yourproject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMulti {
    
    static ConcurrentHashMap<String, UnCliente> clientes = new ConcurrentHashMap<>();
    
    private static final ConcurrentHashMap<String, String> userDatabase = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        userDatabase.put("admin", "1234");

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
    
    public static synchronized boolean registerUser(String username, String password) {
        if (userDatabase.containsKey(username)) {
            return false; // El usuario ya existe
        }
        userDatabase.put(username, password);
        System.out.println("Nuevo usuario registrado: " + username);
        return true;
    }

    public static synchronized boolean loginUser(String username, String password) {
        return userDatabase.containsKey(username) && userDatabase.get(username).equals(password);
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
            System.out.println("Cliente " + clienteRemovido.getNombre() + " desconectado. Clientes restantes: " + clientes.size());
            // Solo anunciamos que se fue si ya tenía un nombre asignado
            if (clienteRemovido.getNombre() != null && !clienteRemovido.getNombre().isEmpty()) {
                broadcastMensaje("'" + clienteRemovido.getNombre() + "' ha abandonado el chat.", null);
            }
        }
    }
}