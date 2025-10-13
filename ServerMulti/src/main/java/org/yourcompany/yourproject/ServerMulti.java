package org.yourcompany.yourproject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMulti {
    
    // Almacena clientes usando su ID único como clave.
    static ConcurrentHashMap<String, UnCliente> clientes = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        try (ServerSocket servidorSocket = new ServerSocket(8080)) {
            System.out.println("Servidor iniciado en el puerto 8080. Esperando clientes...");
            
            while (true) {
                Socket socketCliente = servidorSocket.accept(); 
                System.out.println("Nuevo cliente conectado: " + socketCliente.getInetAddress());
                
                String clienteId = UUID.randomUUID().toString();
                
                // Pasamos el socket y el ID al constructor.
                UnCliente cliente = new UnCliente(socketCliente, clienteId); 
                clientes.put(clienteId, cliente);
                
                new Thread(cliente).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
    
    /**
     * Envía un mensaje a todos los clientes conectados, excepto al que lo envió.
     * @param mensaje El mensaje a difundir.
     * @param emisorId El ID del cliente que originó el mensaje.
     */
    public static void broadcastMensaje(String mensaje, String emisorId) {
        for (UnCliente cliente : clientes.values()) {
            // Comprobamos que no se le envíe el mensaje de vuelta al emisor.
            if (emisorId == null || !cliente.getId().equals(emisorId)) {
                cliente.enviarMensaje(mensaje);
            }
        }
    }
    
    /**
     * Elimina un cliente del mapa de clientes conectados.
     * @param clienteId El ID del cliente a remover.
     */
    public static void removerCliente(String clienteId) {
        UnCliente clienteRemovido = clientes.remove(clienteId);
        if (clienteRemovido != null) {
            System.out.println("Cliente " + clienteRemovido.getNombre() + " desconectado. Clientes restantes: " + clientes.size());
            broadcastMensaje("'" + clienteRemovido.getNombre() + "' ha abandonado el chat.", null);
        }
    }
}