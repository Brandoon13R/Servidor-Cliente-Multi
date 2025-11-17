package org.yourcompany.yourproject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class UnCliente implements Runnable {
    private static final String COMANDO_PREFIX = "#"; 
    
    private final Socket socket;
    private final DataInputStream entrada;
    private final DataOutputStream salida;
    private final String id;
    
    private String nombre; 
    private String username; 
    private boolean isAuthenticated = false;

    private String idJuegoActual = null;

    public UnCliente(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.id = id;
        this.entrada = new DataInputStream(socket.getInputStream());
        this.salida = new DataOutputStream(socket.getOutputStream());
    }
    
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getUsername() { return username; }

    public String getIdJuegoActual() { return idJuegoActual; }
    public void setIdJuegoActual(String idJuegoActual) { this.idJuegoActual = idJuegoActual; }


    @Override
    public void run() {
        try {
            enviarMensaje("BIENVENIDO AL SERVIDOR");
            enviarMensaje("");
            enviarMensaje("Debes autenticarte para poder chatear.");
            enviarMensaje(COMANDO_PREFIX + "login <usuario> <contraseña>");
            enviarMensaje(COMANDO_PREFIX + "register <nombre> <usuario> <contraseña>");
            enviarMensaje("");
            
            String mensajeRecibido;
            while (true) {
                mensajeRecibido = entrada.readUTF();
                
                if (this.idJuegoActual != null) {
                    handleGameCommand(mensajeRecibido, COMANDO_PREFIX);
                } else {
                    if (mensajeRecibido.startsWith(COMANDO_PREFIX)) {
                        handleCommand(mensajeRecibido);
                    } else {
                        handleChatMessage(mensajeRecibido);
                    }
                }
            }
        } catch (IOException ex) {
            if (this.nombre != null) {
                System.out.println("El cliente '" + this.nombre + "' (" + id + ") perdió la conexión.");
            } else {
                System.out.println("Un cliente no autenticado (" + id + ") perdió la conexión.");
            }
        } finally {
            if (this.idJuegoActual != null) {
                JuegoGato juego = ServerMulti.juegosActivos.get(this.idJuegoActual);
                if (juego != null && juego.isJuegoActivo()) {
                    UnCliente oponente = juego.getOponente(this);
                    if (oponente != null) {
                        oponente.enviarMensaje("Tu oponente, " + this.getNombre() + ", se ha desconectado.");
                        oponente.enviarMensaje("¡Has ganado la partida!");
                        juego.notificarVictoria(oponente, false);
                    }
                }
            }

            ServerMulti.removerCliente(this.id);
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket para el cliente " + this.nombre);
            }
        }
    }

    public void menuPrincipal(){
        enviarMensaje("MENÚ PRINCIPAL");
        enviarMensaje("");

        enviarMensaje("Comandos de Chat Básicos:");
        enviarMensaje("  " + COMANDO_PREFIX + "bloquear <usuario>   - Bloquea a un usuario.");
        enviarMensaje("  " + COMANDO_PREFIX + "desbloquear <usuario> - Desbloquea a un usuario.");
        enviarMensaje("");

        enviarMensaje("Usa los siguientes comandos para ver más opciones:");
        enviarMensaje("  " + COMANDO_PREFIX + "comandos juego    - Muestra la ayuda del juego Gato.");
        enviarMensaje("  " + COMANDO_PREFIX + "comandos ranking  - Muestra la ayuda de clasificaciones.");
        enviarMensaje("");
    }

    private void handleGameCommand(String commandMessage, String prefix) {
        JuegoGato juego = ServerMulti.juegosActivos.get(this.idJuegoActual);

        if (juego == null || !juego.isJuegoActivo()) {
            this.idJuegoActual = null;
            enviarMensaje("Error: La partida en la que estabas ha finalizado o no existe.");
            return;
        }

        if (!commandMessage.startsWith(prefix)) {
            enviarMensaje("No puedes enviar mensajes al chat general mientras estás en una partida.");
            return;
        }

        String comandoLimpio = commandMessage.substring(prefix.length()).trim();
        String[] parts = comandoLimpio.split(" ");
        String command = parts[0]; 

        switch (command) {
            case "mover":
                if (parts.length == 3) {
                    try {
                        int fila = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);
                        juego.hacerMovimiento(this, fila, col);
                    } catch (NumberFormatException e) {
                        enviarMensaje("Error: Fila y columna deben ser números (0, 1, o 2).");
                    }
                } else {
                    enviarMensaje("Uso incorrecto. Comando: " + COMANDO_PREFIX + "mover <fila> <col>");
                }
                break;
            
            case "rendirse":
                enviarMensaje("Te has rendido.");
                UnCliente oponente = juego.getOponente(this);
                oponente.enviarMensaje("Tu oponente, " + this.getNombre() + ", se ha rendido.");
                oponente.enviarMensaje("¡Has ganado la partida!");
                juego.notificarVictoria(oponente, false);
                break;

            default:
                enviarMensaje("Comando no permitido durante el juego. Usa " + COMANDO_PREFIX + "mover <fila> <columna> o " + COMANDO_PREFIX + "rendirse.");
                break;
        }
    }

    private void handleCommand(String commandMessage) {
        String comandoLimpio = commandMessage.substring(COMANDO_PREFIX.length()).trim();
        String[] parts = comandoLimpio.split(" ");
        String command = parts[0]; 
        
        if (command.equals("login") || command.equals("register")) {
            
            if (isAuthenticated) {
                enviarMensaje("Ya has iniciado sesión con este usuario " + this.nombre);
                return;
            }
            
            if (command.equals("register")) {
                if (parts.length == 4) { 
                    if (ServerMulti.registerUser(parts[1], parts[2], parts[3])) {
                        this.nombre = parts[1];
                        this.username = parts[2];
                        this.isAuthenticated = true;
                        enviarMensaje("Registro exitoso. ¡Bienvenido '" + this.nombre + "'!");
                        ServerMulti.broadcastMensaje("'" + this.nombre + "' se ha unido al chat.", this);

                        menuPrincipal();
                    } else {
                        enviarMensaje("Error: El nombre de usuario '" + parts[2] + "' ya está en uso.");
                    }
                } else {
                    enviarMensaje("Uso incorrecto. Comando: " + COMANDO_PREFIX + "register <nombre> <usuario> <contraseña>");
                }
            } 
            
            if (command.equals("login")) {
                if (parts.length == 3) {
                    String usuarioLogin = parts[1];
                    String nombreLogueado = ServerMulti.loginUser(usuarioLogin, parts[2]);
                    
                    if (nombreLogueado != null) {
                        this.nombre = nombreLogueado;
                        this.username = usuarioLogin;
                        this.isAuthenticated = true;
                        enviarMensaje("Inicio de sesión exitoso. ¡Bienvenido de nuevo, " + this.nombre + "!");
                        ServerMulti.broadcastMensaje("'" + this.nombre + "' se ha unido al chat.", this);
                        
                        enviarMensaje("");
                        menuPrincipal(); 
                    } else {
                        enviarMensaje("Error: Usuario o contraseña incorrectos.");
                    }
                } else {
                    enviarMensaje("Uso incorrecto. Comando: " + COMANDO_PREFIX + "login <usuario> <contraseña>");
                }
            }
            return; 
        }
        
        if (!isAuthenticated) {
            enviarMensaje("Debes iniciar sesión para usar este comando.");
            return;
        }

        switch (command) {
            case "bloquear":
                if (parts.length == 2) {
                    String userToBloquear = parts[1];
                    String result = ServerMulti.blockUser(this.username, userToBloquear);
                    enviarMensaje(result);
                } else {
                    enviarMensaje("Uso incorrecto. Comando: " + COMANDO_PREFIX + "bloquear <usuario>");
                }
                break;

            case "desbloquear":
                if (parts.length == 2) {
                    String userToDesbloquear = parts[1];
                    String result = ServerMulti.unblockUser(this.username, userToDesbloquear);
                    enviarMensaje(result);
                } else {
                    enviarMensaje("Uso incorrecto. Comando: " + COMANDO_PREFIX + "desbloquear <usuario>");
                }
                break;
            
            case "gato":
                if (parts.length == 2) {
                    proponerPartida(parts[1]);
                } else {
                    enviarMensaje("Uso incorrecto. Comando: " + COMANDO_PREFIX + "gato <usuario>");
                }
                break;

            case "aceptar":
                if (parts.length == 2) {
                    aceptarPartida(parts[1]);
                } else {
                    enviarMensaje("Uso incorrecto. Comando: " + COMANDO_PREFIX + "aceptar <usuario>");
                }
                break;

            case "rechazar":
                if (parts.length == 2) {
                    rechazarPartida(parts[1]);
                } else {
                    enviarMensaje("Uso incorrecto. Comando: " + COMANDO_PREFIX + "rechazar <usuario>");
                }
                break;

            case "ranking":
                enviarMensaje("Calculando ranking general...");
                String ranking = ManejadorRankings.getRankingGeneralFormateado();
                enviarMensaje(ranking);
                break;

            case "vs":
                if (parts.length == 2) {
                    String oponente = parts[1];
                    String miUsuario = this.username;
                    
                    if (oponente.equalsIgnoreCase(miUsuario)) {
                        enviarMensaje("No puedes ver estadísticas contra ti mismo.");
                    } else {
                        enviarMensaje("Buscando estadísticas H2H vs " + oponente + "...");
                        String stats = ManejadorRankings.getEstadisticasH2HFormateado(miUsuario, oponente);
                        enviarMensaje(stats);
                    }
                } else {
                    enviarMensaje("Uso incorrecto. Comando: " + COMANDO_PREFIX + "vs <usuario>");
                }
                break;

            case "comandos":
                if (parts.length == 2) {
                    String subMenu = parts[1].toLowerCase(); 
                    
                    if (subMenu.equals("juego")) {
                        enviarMensaje("Comandos de Juego (Gato)");
                        enviarMensaje("  " + COMANDO_PREFIX + "gato <usuario>     - Reta a un jugador.");
                        enviarMensaje("  " + COMANDO_PREFIX + "aceptar <usuario>  - Acepta un reto.");
                        enviarMensaje("  " + COMANDO_PREFIX + "rechazar <usuario> - Rechaza un reto.");
                        enviarMensaje("  (Durante la partida, también puedes usar " + COMANDO_PREFIX + "mover y " + COMANDO_PREFIX + "rendirse)");

                    } else if (subMenu.equals("ranking")) {
                        enviarMensaje("Comandos de Ranking");
                        enviarMensaje("  " + COMANDO_PREFIX + "ranking        - Muestra el ranking general de Gato.");
                        enviarMensaje("  " + COMANDO_PREFIX + "vs <usuario>   - Muestra tus estadísticas contra otro jugador.");

                    } else {
                        enviarMensaje("Ayuda no encontrada. Opciones: " + COMANDO_PREFIX + "comandos juego, " + COMANDO_PREFIX + "comandos ranking");
                    }
                } else {
                    enviarMensaje("Escribe " + COMANDO_PREFIX + "comandos juego o " + COMANDO_PREFIX + "comandos ranking para ver la ayuda.");
                }
                break;

            default:
                enviarMensaje("Comando desconocido: " + command);
                break;
        }
    }

    private void proponerPartida(String targetUsername) {
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

        if (ServerMulti.invitacionesGato.containsKey(oponente.getUsername())) {
             enviarMensaje("Error: Ese jugador ya tiene una invitación pendiente.");
            return;
        }

        if (ServerMulti.invitacionesGato.containsKey(this.username)) {
             enviarMensaje("Error: Ya tienes una invitación pendiente de '" + ServerMulti.invitacionesGato.get(this.username) + "'.");
             enviarMensaje("Debes " + COMANDO_PREFIX + "aceptar o " + COMANDO_PREFIX + "rechazar esa invitación primero.");
            return;
        }

        ServerMulti.invitacionesGato.put(oponente.getUsername(), this.username);
        enviarMensaje("Invitación enviada a " + oponente.getNombre() + ". Esperando respuesta...");
        oponente.enviarMensaje("---------------------------------");
        oponente.enviarMensaje("¡NUEVO RETO! " + this.nombre + " (" + this.username + ") te ha retado a Gato.");
        oponente.enviarMensaje("Usa " + COMANDO_PREFIX + "aceptar " + this.username + " o " + COMANDO_PREFIX + "rechazar " + this.username);
        oponente.enviarMensaje("---------------------------------");
    }

    private void aceptarPartida(String retadorUsername) {
        String retadorQueInvito = ServerMulti.invitacionesGato.get(this.username);
        
        if (retadorQueInvito == null || !retadorQueInvito.equalsIgnoreCase(retadorUsername)) {
            enviarMensaje("Error: No tienes una invitación pendiente de '" + retadorUsername + "'.");
            return;
        }

        UnCliente retador = ServerMulti.getClientePorUsername(retadorUsername);
        
        if (retador == null) {
            enviarMensaje("Error: El retador '" + retadorUsername + "' se ha desconectado.");
            ServerMulti.invitacionesGato.remove(this.username);
            return;
        }

        if (this.idJuegoActual != null || retador.getIdJuegoActual() != null) {
            enviarMensaje("Error: Uno de los dos ya está en otra partida.");
            ServerMulti.invitacionesGato.remove(this.username);
            return;
        }

        ServerMulti.invitacionesGato.remove(this.username);
        
        String gameId = UUID.randomUUID().toString();
        JuegoGato nuevoJuego = new JuegoGato(retador, this, gameId); 
        
        ServerMulti.juegosActivos.put(gameId, nuevoJuego);
        retador.setIdJuegoActual(gameId);
        this.setIdJuegoActual(gameId);

        nuevoJuego.iniciarPartida();
    }

    private void rechazarPartida(String retadorUsername) {
        String retadorQueInvito = ServerMulti.invitacionesGato.get(this.username);

        if (retadorQueInvito == null || !retadorQueInvito.equalsIgnoreCase(retadorUsername)) {
            enviarMensaje("Error: No tienes una invitación pendiente de '" + retadorUsername + "'.");
            return;
        }

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
            enviarMensaje("Usa " + COMANDO_PREFIX + "login <usuario> <pass> o " + COMANDO_PREFIX + "register <nombre> <usuario> <pass>");
        }
    }

    public void enviarMensaje(String mensaje) {
        try {
            salida.writeUTF(mensaje);
            salida.flush();
        } catch (IOException e) {
        }
    }
}