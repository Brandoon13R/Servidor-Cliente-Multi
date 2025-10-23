package org.yourcompany.yourproject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID; // Importar UUID

public class UnCliente implements Runnable {
    
    private final Socket socket;
    private final DataInputStream entrada;
    private final DataOutputStream salida;
    private final String id;
    
    private String nombre; 
    private String username; 
    private boolean isAuthenticated = false;

    // --- NUEVO CAMPO ---
    private String idJuegoActual = null; // ID del juego en el que está participando

    public UnCliente(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.id = id;
        this.entrada = new DataInputStream(socket.getInputStream());
        this.salida = new DataOutputStream(socket.getOutputStream());
    }
    
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getUsername() { return username; }

    // --- NUEVO GETTER/SETTER ---
    public String getIdJuegoActual() { return idJuegoActual; }
    public void setIdJuegoActual(String idJuegoActual) { this.idJuegoActual = idJuegoActual; }


    @Override
    public void run() {
        try {
            // --- Mensaje de bienvenida MODIFICADO ---
            enviarMensaje("--- BIENVENIDO AL SERVIDOR ---");
            enviarMensaje("Debes autenticarte para poder chatear.");
            enviarMensaje("Usa /login <usuario> <contraseña>");
            enviarMensaje("O /register <nombre> <usuario> <contraseña>");
            enviarMensaje("--- Comandos de Chat ---");
            enviarMensaje("/bloquear <usuario>   - Bloquea a un usuario.");
            enviarMensaje("/desbloquear <usuario> - Desbloquea a un usuario.");
            enviarMensaje("--- Comandos de Juego (Gato) ---");
            enviarMensaje("/gato <usuario>       - Reta a un jugador.");
            enviarMensaje("/aceptar <usuario>    - Acepta un reto.");
            enviarMensaje("/rechazar <usuario>   - Rechaza un reto.");
            enviarMensaje("/mover <fila> <col>   - (En partida) Coloca tu ficha (0-2).");
            enviarMensaje("/rendirse             - (En partida) Abandona la partida.");
            enviarMensaje("---------------------------------");
            
            String mensajeRecibido;
            while (true) {
                mensajeRecibido = entrada.readUTF();
                
                // --- LÓGICA DE ESTADO (JUGANDO O CHATEANDO) ---
                if (this.idJuegoActual != null) {
                    // El cliente está en una partida
                    handleGameCommand(mensajeRecibido);
                } else {
                    // El cliente está en el chat
                    if (mensajeRecibido.startsWith("/")) {
                        handleCommand(mensajeRecibido);
                    } else {
                        handleChatMessage(mensajeRecibido);
                    }
                }
                // --- FIN LÓGICA DE ESTADO ---
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

    // --- NUEVO MÉTODO ---
    /**
     * Maneja los comandos recibidos MIENTRAS el usuario está en una partida.
     */
    private void handleGameCommand(String commandMessage) {
        JuegoGato juego = ServerMulti.juegosActivos.get(this.idJuegoActual);

        // Seguridad: si el juego no existe por alguna razón, liberar al cliente
        if (juego == null || !juego.isJuegoActivo()) {
            this.idJuegoActual = null;
            enviarMensaje("Error: La partida en la que estabas ha finalizado o no existe.");
            return;
        }

        String[] parts = commandMessage.trim().split(" ");
        String command = parts[0];

        switch (command) {
            case "/mover":
                if (parts.length == 3) {
                    try {
                        int fila = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);
                        // La lógica de turnos y validación se delega al objeto JuegoGato
                        juego.hacerMovimiento(this, fila, col);
                    } catch (NumberFormatException e) {
                        enviarMensaje("Error: Fila y columna deben ser números (0, 1, o 2).");
                    }
                } else {
                    enviarMensaje("Uso incorrecto. Formato: /mover <fila> <col>");
                }
                break;
            
            case "/rendirse":
                enviarMensaje("Te has rendido.");
                UnCliente oponente = juego.getOponente(this);
                oponente.enviarMensaje("Tu oponente, " + this.getNombre() + ", se ha rendido.");
                oponente.enviarMensaje("¡Has ganado la partida!");
                // notificarVictoria se encarga de terminarJuego() y limpiar todo
                juego.notificarVictoria(oponente, false); // false = no por jugada, sino por rendición
                break;

            default:
                if (commandMessage.startsWith("/")) {
                    enviarMensaje("Comando no permitido durante el juego. Usa /mover <f> <c> o /rendirse.");
                } else {
                    enviarMensaje("No puedes enviar mensajes al chat general mientras estás en una partida.");
                }
                break;
        }
    }

    // Método para gestionar comandos (del chat)
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
            
            // --- NUEVOS CASOS DE JUEGO ---
            case "/gato":
                if (parts.length == 2) {
                    proponerPartida(parts[1]);
                } else {
                    enviarMensaje("Uso incorrecto. Formato: /gato <usuario>");
                }
                break;

            case "/aceptar":
                if (parts.length == 2) {
                    aceptarPartida(parts[1]);
                } else {
                    enviarMensaje("Uso incorrecto. Formato: /aceptar <usuario>");
                }
                break;

            case "/rechazar":
                if (parts.length == 2) {
                    rechazarPartida(parts[1]);
                } else {
                    enviarMensaje("Uso incorrecto. Formato: /rechazar <usuario>");
                }
                break;
            // --- FIN NUEVOS CASOS ---

            default:
                enviarMensaje("Comando desconocido: " + command);
                break;
        }
    }

    // --- NUEVOS MÉTODOS PRIVADOS (Lógica de invitación) ---

    private void proponerPartida(String targetUsername) {
        // 1. Validaciones
        if (targetUsername.equalsIgnoreCase(this.username)) {
            enviarMensaje("Error: No puedes retarte a ti mismo.");
            return;
        }
        
        UnCliente oponente = ServerMulti.getClientePorUsername(targetUsername);
        
        if (oponente == null) {
            enviarMensaje("Error: El usuario '" + targetUsername + "' no está conectado o no existe.");
            return;
        }

        if (oponente.getIdJuegoActual() != null) {
            enviarMensaje("Error: El usuario '" + oponente.getNombre() + "' ya está en una partida.");
            return;
        }

        // Regla: "no más de uno con alguien en particular" (interpretado como "invitación activa")
        // Esta regla previene que un usuario (targetUsername) reciba múltiples invitaciones
        if (ServerMulti.invitacionesGato.containsKey(oponente.getUsername())) {
             enviarMensaje("Error: Ese jugador ya tiene una invitación pendiente.");
            return;
        }
        // Esta regla previene que un usuario que YA recibió una invitación envíe otra
        if (ServerMulti.invitacionesGato.containsKey(this.username)) {
             enviarMensaje("Error: Ya tienes una invitación pendiente de '" + ServerMulti.invitacionesGato.get(this.username) + "'.");
             enviarMensaje("Debes /aceptar o /rechazar esa invitación primero.");
            return;
        }


        // 2. Enviar invitación
        // Mapeamos al invitado (target) con el retador (this)
        ServerMulti.invitacionesGato.put(oponente.getUsername(), this.username);
        enviarMensaje("Invitación enviada a " + oponente.getNombre() + ". Esperando respuesta...");
        oponente.enviarMensaje("---------------------------------");
        oponente.enviarMensaje("¡NUEVO RETO! " + this.nombre + " (" + this.username + ") te ha retado a Gato.");
        oponente.enviarMensaje("Usa /aceptar " + this.username + " o /rechazar " + this.username);
        oponente.enviarMensaje("---------------------------------");
    }

    private void aceptarPartida(String retadorUsername) {
        // 1. Validar invitación
        // Verificamos si 'this.username' (yo) tengo una invitación del 'retadorUsername'
        String retadorQueInvito = ServerMulti.invitacionesGato.get(this.username);
        
        if (retadorQueInvito == null || !retadorQueInvito.equalsIgnoreCase(retadorUsername)) {
            enviarMensaje("Error: No tienes una invitación pendiente de '" + retadorUsername + "'.");
            return;
        }

        UnCliente retador = ServerMulti.getClientePorUsername(retadorUsername);
        
        if (retador == null) {
            enviarMensaje("Error: El retador '" + retadorUsername + "' se ha desconectado.");
            ServerMulti.invitacionesGato.remove(this.username); // Limpiar invitación
            return;
        }

        // 2. Validar que sigan disponibles (por si el retador empezó otro juego)
        if (this.idJuegoActual != null || retador.getIdJuegoActual() != null) {
            enviarMensaje("Error: Uno de los dos ya está en otra partida.");
            ServerMulti.invitacionesGato.remove(this.username);
            return;
        }

        // 3. ¡Crear la partida!
        ServerMulti.invitacionesGato.remove(this.username); // Quitar invitación
        
        String gameId = UUID.randomUUID().toString();
        // El constructor de JuegoGato decide aleatoriamente quién empieza
        // J1 = retador, J2 = this (quien aceptó)
        JuegoGato nuevoJuego = new JuegoGato(retador, this, gameId); 
        
        ServerMulti.juegosActivos.put(gameId, nuevoJuego);
        retador.setIdJuegoActual(gameId);
        this.setIdJuegoActual(gameId);

        // 4. Notificar a los jugadores (JuegoGato se encarga)
        nuevoJuego.iniciarPartida();
    }

    private void rechazarPartida(String retadorUsername) {
        // Verificamos si la invitación existe
        String retadorQueInvito = ServerMulti.invitacionesGato.get(this.username);

        if (retadorQueInvito == null || !retadorQueInvito.equalsIgnoreCase(retadorUsername)) {
            enviarMensaje("Error: No tienes una invitación pendiente de '" + retadorUsername + "'.");
            return;
        }

        // Limpiar invitación
        ServerMulti.invitacionesGato.remove(this.username);
        enviarMensaje("Has rechazado la invitación de " + retadorUsername + ".");

        UnCliente retador = ServerMulti.getClientePorUsername(retadorUsername);
        if (retador != null) {
            retador.enviarMensaje(this.nombre + " (" + this.username + ") ha rechazado tu invitación.");
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