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
    private String nombre; // Puede ser de invitado o el nombre de usuario registrado

    // --> NUEVO: Atributos para el control de mensajes y autenticación
    private int messageCount = 0;
    private boolean isAuthenticated = false;
    private static final int MESSAGE_LIMIT = 3;

    public UnCliente(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.id = id;
        this.entrada = new DataInputStream(socket.getInputStream());
        this.salida = new DataOutputStream(socket.getOutputStream());
    }
    
    public String getId() { return id; }
    public String getNombre() { return nombre; }

    @Override
    public void run() {
        try {
            // El primer mensaje es el nombre de invitado
            this.nombre = entrada.readUTF();
            System.out.println("Cliente " + id + " se unió como invitado: " + this.nombre);
            
            // Le damos la bienvenida y le explicamos las reglas
            enviarMensaje("¡Bienvenido " + this.nombre + "! Eres un invitado. Tienes " + MESSAGE_LIMIT + " mensajes.");
            enviarMensaje("Usa /register <usuario> <contraseña> o /login <usuario> <contraseña> para chatear sin límites.");
            
            ServerMulti.broadcastMensaje("'" + this.nombre + "' (invitado) se ha unido al chat.", this.id);

            String mensajeRecibido;
            while (true) {
                mensajeRecibido = entrada.readUTF();
                
                // --> NUEVO: Procesamiento de comandos
                if (mensajeRecibido.startsWith("/")) {
                    handleCommand(mensajeRecibido);
                } else {
                    handleChatMessage(mensajeRecibido);
                }
            }
        } catch (IOException ex) {
            System.out.println("El cliente '" + this.nombre + "' (" + id + ") perdió la conexión.");
        } finally {
            ServerMulti.removerCliente(this.id);
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket para el cliente " + this.nombre);
            }
        }
    }

    // --> NUEVO: Método para gestionar comandos
    private void handleCommand(String commandMessage) {
        String[] parts = commandMessage.trim().split(" ");
        String command = parts[0];

        switch (command) {
            case "/register":
                if (parts.length == 3) {
                    if (ServerMulti.registerUser(parts[1], parts[2])) {
                        this.nombre = parts[1];
                        this.isAuthenticated = true;
                        enviarMensaje("✅ Registro exitoso. ¡Ahora eres '" + this.nombre + "'!");
                        ServerMulti.broadcastMensaje(this.nombre + " se ha unido al chat como usuario registrado.", null);
                    } else {
                        enviarMensaje("❌ Error: El nombre de usuario '" + parts[1] + "' ya está en uso.");
                    }
                } else {
                    enviarMensaje("❌ Uso incorrecto. Formato: /register <usuario> <contraseña>");
                }
                break;
            
            case "/login":
                if (parts.length == 3) {
                    if (ServerMulti.loginUser(parts[1], parts[2])) {
                        String oldName = this.nombre;
                        this.nombre = parts[1];
                        this.isAuthenticated = true;
                        enviarMensaje("✅ Inicio de sesión exitoso. ¡Bienvenido de nuevo, " + this.nombre + "!");
                        ServerMulti.broadcastMensaje("'" + oldName + "' ahora es '" + this.nombre + "'.", null);
                    } else {
                        enviarMensaje("❌ Error: Usuario o contraseña incorrectos.");
                    }
                } else {
                    enviarMensaje("❌ Uso incorrecto. Formato: /login <usuario> <contraseña>");
                }
                break;

            default:
                enviarMensaje("Comando desconocido: " + command);
                break;
        }
    }

    // --> NUEVO: Método para gestionar mensajes de chat normales
    private void handleChatMessage(String message) {
        if (isAuthenticated) {
            // Si está autenticado, puede enviar mensajes ilimitados
            String mensajeParaTodos = this.nombre + ": " + message;
            ServerMulti.broadcastMensaje(mensajeParaTodos, this.id);
        } else {
            // Si es un invitado, verificamos su contador de mensajes
            if (messageCount < MESSAGE_LIMIT) {
                messageCount++;
                String mensajeParaTodos = this.nombre + " (invitado): " + message;
                ServerMulti.broadcastMensaje(mensajeParaTodos, this.id);
                enviarMensaje("Te quedan " + (MESSAGE_LIMIT - messageCount) + " mensajes de invitado.");
            } else {
                enviarMensaje("🚫 Límite de mensajes alcanzado. Debes registrarte o iniciar sesión para continuar enviando mensajes.");
            }
        }
    }

    /**
     * Envía un mensaje a este cliente específico.
     */
    public void enviarMensaje(String mensaje) {
        try {
            salida.writeUTF(mensaje);
            salida.flush();
        } catch (IOException e) {
            // Si no se puede enviar, es probable que se haya desconectado
            ServerMulti.removerCliente(this.id);
        }
    }
}
