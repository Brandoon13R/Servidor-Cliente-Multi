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
    
    // --- CAMPOS MODIFICADOS ---
    private String nombre; 
    private String username; 
    private boolean isAuthenticated = false;

    public UnCliente(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.id = id;
        this.entrada = new DataInputStream(socket.getInputStream());
        this.salida = new DataOutputStream(socket.getOutputStream());
    }
    
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getUsername() { return username; }

    @Override
    public void run() {
        try {
            // --- Mensaje de bienvenida MODIFICADO ---
            enviarMensaje("--- BIENVENIDO AL SERVIDOR ---");
            enviarMensaje("Debes autenticarte para poder chatear.");
            enviarMensaje("Usa /login <usuario> <contraseña>");
            enviarMensaje("O /register <nombre> <usuario> <contraseña>");
            enviarMensaje("--- Comandos Adicionales ---");
            enviarMensaje("/bloquear <usuario>   - Bloquea a un usuario.");
            enviarMensaje("/desbloquear <usuario> - Desbloquea a un usuario.");
            enviarMensaje("---------------------------------");
            
            String mensajeRecibido;
            while (true) {
                mensajeRecibido = entrada.readUTF();
                
                if (mensajeRecibido.startsWith("/")) {
                    handleCommand(mensajeRecibido);
                } else {
                    handleChatMessage(mensajeRecibido);
                }
            }
        } catch (IOException ex) {
            if (this.nombre != null) {
                System.out.println("El cliente '" + this.nombre + "' (" + id + ") perdió la conexión.");
            } else {
                System.out.println("Un cliente no autenticado (" + id + ") perdió la conexión.");
            }
        } finally {
            ServerMulti.removerCliente(this.id);
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket para el cliente " + this.nombre);
            }
        }
    }

    // Método para gestionar comandos
    private void handleCommand(String commandMessage) {
        String[] parts = commandMessage.trim().split(" ");
        String command = parts[0];

        
        if (command.equals("/login") || command.equals("/register")) {
            
            if (isAuthenticated) {
                enviarMensaje("Ya has iniciado sesión como " + this.nombre);
                return;
            }
            
            if (command.equals("/register")) {
                if (parts.length == 4) { 
                    if (ServerMulti.registerUser(parts[1], parts[2], parts[3])) {
                        this.nombre = parts[1];
                        this.username = parts[2];
                        this.isAuthenticated = true;
                        enviarMensaje("Registro exitoso. ¡Ahora eres '" + this.nombre + "'!");
                        ServerMulti.broadcastMensaje("'" + this.nombre + "' se ha unido al chat.", this);
                    } else {
                        enviarMensaje("Error: El nombre de usuario '" + parts[2] + "' ya está en uso.");
                    }
                } else {
                    enviarMensaje("Uso incorrecto. Formato: /register <nombre> <usuario> <contraseña>");
                }
            } 
            
            if (command.equals("/login")) {
                if (parts.length == 3) {
                    String usuarioLogin = parts[1];
                    String nombreLogueado = ServerMulti.loginUser(usuarioLogin, parts[2]);
                    
                    if (nombreLogueado != null) {
                        this.nombre = nombreLogueado;
                        this.username = usuarioLogin;
                        this.isAuthenticated = true;
                        enviarMensaje("Inicio de sesión exitoso. ¡Bienvenido de nuevo, " + this.nombre + "!");
                        ServerMulti.broadcastMensaje("'" + this.nombre + "' se ha unido al chat.", this);
                    } else {
                        enviarMensaje("Error: Usuario o contraseña incorrectos.");
                    }
                } else {
                    enviarMensaje("Uso incorrecto. Formato: /login <usuario> <contraseña>");
                }
            }
            return; 
        }
        
        if (!isAuthenticated) {
            enviarMensaje("Debes iniciar sesión para usar este comando.");
            return;
        }

        switch (command) {
            case "/bloquear":
                if (parts.length == 2) {
                    String userToBloquear = parts[1];
                    String result = ServerMulti.blockUser(this.username, userToBloquear);
                    enviarMensaje(result);
                } else {
                    enviarMensaje("Uso incorrecto. Formato: /bloquear <usuario>");
                }
                break;

            case "/desbloquear":
                if (parts.length == 2) {
                    String userToDesbloquear = parts[1];
                    String result = ServerMulti.unblockUser(this.username, userToDesbloquear);
                    enviarMensaje(result);
                } else {
                    enviarMensaje("Uso incorrecto. Formato: /desbloquear <usuario>");
                }
                break;

            default:
                enviarMensaje("Comando desconocido: " + command);
                break;
        }
    }

    private void handleChatMessage(String message) {
        if (isAuthenticated) {
            String mensajeParaTodos = this.nombre + ": " + message;
            ServerMulti.broadcastMensaje(mensajeParaTodos, this);
        } else {
            enviarMensaje("Debes iniciar sesión o registrarte para enviar mensajes.");
            enviarMensaje("Usa /login <usuario> <pass> o /register <nombre> <usuario> <pass>");
        }
    }

    // Enviar mensaje (sin cambios)
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