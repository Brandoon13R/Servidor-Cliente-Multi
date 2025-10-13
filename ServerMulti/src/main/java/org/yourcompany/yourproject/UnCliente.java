package org.yourcompany.yourproject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class UnCliente implements Runnable {
    
    private final Socket socket;
    private final DataInputStream entrada;
    private final DataOutputStream salida;
    private final String id;
    private String nombre;

    // Constructor corregido: acepta socket y un ID único
    public UnCliente(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.id = id;
        this.entrada = new DataInputStream(socket.getInputStream());
        this.salida = new DataOutputStream(socket.getOutputStream());
    }
    
    // Getters para acceder a propiedades privadas
    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public void run() {
        try {
            // 1. El primer mensaje que se recibe es el nombre del usuario
            this.nombre = entrada.readUTF();
            System.out.println("El cliente " + id + " se identificó como: " + this.nombre);
            
            // 2. Anunciamos la llegada del nuevo usuario a todos los demás
            ServerMulti.broadcastMensaje("'" + this.nombre + "' se ha unido al chat.", this.id);

            // 3. Entramos en el bucle para escuchar mensajes del chat
            String mensajeRecibido;
            while (true) {
                mensajeRecibido = entrada.readUTF();
                
                // Si el cliente envía /exit, se desconecta
                if (mensajeRecibido.equalsIgnoreCase("/exit")) {
                    break; 
                }
                
                System.out.println(this.nombre + " (" + id + "): " + mensajeRecibido);
                String mensajeParaTodos = this.nombre + ": " + mensajeRecibido;
                
                // Usamos el ID (String) en lugar del objeto (this)
                ServerMulti.broadcastMensaje(mensajeParaTodos, this.id);
            }
        } catch (IOException ex) {
            // Esto suele pasar si el cliente cierra la ventana de la consola
            System.out.println("El cliente '" + this.nombre + "' (" + id + ") perdió la conexión.");
        } finally {
            // Al salir del bucle (por /exit o por error), removemos al cliente
            ServerMulti.removerCliente(this.id);
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket para el cliente " + this.nombre);
            }
        }
    }

    /**
     * Envía un mensaje a este cliente específico.
     * @param mensaje El mensaje a enviar.
     */
    public void enviarMensaje(String mensaje) {
        try {
            salida.writeUTF(mensaje);
            salida.flush();
        } catch (IOException e) {
            System.err.println("Error al enviar mensaje a " + this.nombre + ". Eliminando cliente.");
            // Si no se puede enviar un mensaje, es probable que esté desconectado.
            ServerMulti.removerCliente(this.id);
        }
    }
}
