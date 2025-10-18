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

    private boolean isAuthenticated = false;

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
            enviarMensaje("--- BIENVENIDO AL SERVIDOR ---");
            enviarMensaje("Debes autenticarte para poder chatear.");
            enviarMensaje("Usa /login <usuario> <contraseña>");
            enviarMensaje("O /register <nombre> <usuario> <contraseña>");
            enviarMensaje("---------------------------------");
            
            String mensajeRecibido;
            while (true) {
                mensajeRecibido = entrada.readUTF();
                
                // Procesamos comandos (login/register)
                if (mensajeRecibido.startsWith("/")) {
                    handleCommand(mensajeRecibido);
                } else {
                    // O procesamos mensajes de chat (que serán bloqueados si no está logueado)
                    handleChatMessage(mensajeRecibido);
                }
            }
        } catch (IOException ex) {
            // Si el nombre es null, significa que nunca se logueó
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

    private void handleCommand(String commandMessage) {
        String[] parts = commandMessage.trim().split(" ");
        String command = parts[0];

        if (isAuthenticated && (command.equals("/login") || command.equals("/register"))) {
            enviarMensaje("Ya has iniciado sesión como " + this.nombre);
            return;
        }

        switch (command) {
            case "/register":
                if (parts.length == 4) { 
                    if (ServerMulti.registerUser(parts[1], parts[2], parts[3])) {
                        this.nombre = parts[1]; // Guardamos el Nombre real
                        this.isAuthenticated = true;
                        enviarMensaje("✅ Registro exitoso. ¡Ahora eres '" + this.nombre + "'!");
                        // AHORA SÍ ANUNCIAMOS QUE SE UNIÓ
                        ServerMulti.broadcastMensaje("'" + this.nombre + "' se ha unido al chat.", null);
                    } else {
                        enviarMensaje("Error: El nombre de usuario '" + parts[2] + "' ya está en uso.");
                    }
                } else {
                    enviarMensaje("Uso incorrecto. Formato: /register <nombre> <usuario> <contraseña>");
                }
                break;
            
            case "/login":
                if (parts.length == 3) {
                    // AHORA loginUser DEVUELVE EL 'Nombre' (String) O NULL
                    String nombreLogueado = ServerMulti.loginUser(parts[1], parts[2]);
                    
                    if (nombreLogueado != null) {
                        this.nombre = nombreLogueado; // Guardamos el Nombre real
                        this.isAuthenticated = true;
                        enviarMensaje("Inicio de sesión exitoso. ¡Bienvenido de nuevo, " + this.nombre + "!");
                        // AHORA SÍ ANUNCIAMOS QUE SE UNIÓ
                        ServerMulti.broadcastMensaje("'" + this.nombre + "' se ha unido al chat.", null);
                    } else {
                        enviarMensaje("Error: Usuario o contraseña incorrectos.");
                    }
                } else {
                    enviarMensaje("Uso incorrecto. Formato: /login <usuario> <contraseña>");
                }
                break;

            default:
                enviarMensaje("Comando desconocido: " + command);
                break;
        }
    }

    // Método para gestionar mensajes de chat normales
    private void handleChatMessage(String message) {
        if (isAuthenticated) {
            // Si está autenticado, envía el mensaje
            String mensajeParaTodos = this.nombre + ": " + message;
            ServerMulti.broadcastMensaje(mensajeParaTodos, this.id);
        } else {
            enviarMensaje("Debes iniciar sesión o registrarte para enviar mensajes.");
            enviarMensaje("Usa /login <usuario> <pass> o /register <nombre> <usuario> <pass>");
        }
    }

    public void enviarMensaje(String mensaje) {
        try {
            salida.writeUTF(mensaje);
            salida.flush();
        } catch (IOException e) {
            ServerMulti.removerCliente(this.id);
        }
    }
}