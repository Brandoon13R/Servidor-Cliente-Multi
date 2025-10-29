package org.yourcompany.yourproject;

import java.util.Random;

public class JuegoGato {
    private UnCliente jugador1; // Retador
    private UnCliente jugador2; // Retado (quien aceptó)
    private UnCliente jugadorActual;
    private final String idJuego;
    private final char[][] tablero;
    private boolean juegoActivo;
    private char simboloJ1;
    private char simboloJ2;
    // El array 'jugadores' se elimina por redundancia, ya tenemos jugador1 y jugador2

    public JuegoGato(UnCliente J1, UnCliente J2, String idJuego) {
        this.jugador1 = J1;
        this.jugador2 = J2;
        this.idJuego = idJuego;
        this.tablero = new char[3][3];
        inicializarTablero();
        this.juegoActivo = true;

        // Decidir aleatoriamente quién empieza y qué símbolo usa
        Random random = new Random();
        boolean j1Empieza = random.nextBoolean(); 
        
        if (j1Empieza) {
            this.jugadorActual = this.jugador1;
            this.simboloJ1 = 'X';
            this.simboloJ2 = 'O';
        } else {
            this.jugadorActual = this.jugador2;
            this.simboloJ1 = 'O';
            this.simboloJ2 = 'X';
        }
    }

    private void inicializarTablero() {
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                tablero[i][j] = ' '; // Usar espacio para vacío
            }
        }
    }
    
    public boolean isJuegoActivo() {
        return juegoActivo;
    }
    
    public String getIdJuego() {
        return idJuego;
    }

    /**
     * Devuelve el oponente de un jugador dado.
     */
    public UnCliente getOponente(UnCliente jugador) {
        if (jugador.getId().equals(jugador1.getId())) {
            return jugador2;
        } else if (jugador.getId().equals(jugador2.getId())) {
            return jugador1;
        }
        return null; // No debería pasar si el juego está bien configurado
    }

    /**
     * Envía el estado inicial de la partida a ambos jugadores.
     */
    public void iniciarPartida() {
        String msgInicio = "--- ¡PARTIDA INICIADA! ---";
        // Mensaje para Jugador 1 (Retador)
        String msgJ1 = "Juegas contra " + jugador2.getNombre() + ". Tú eres '" + simboloJ1 + "'.";
        // Mensaje para Jugador 2 (Retado)
        String msgJ2 = "Juegas contra " + jugador1.getNombre() + ". Tú eres '" + simboloJ2 + "'.";

        jugador1.enviarMensaje(msgInicio);
        jugador1.enviarMensaje(msgJ1);
        jugador2.enviarMensaje(msgInicio);
        jugador2.enviarMensaje(msgJ2);
        
        enviarTableroAMbos();
        jugadorActual.enviarMensaje("¡Es tu turno! Usa /mover <fila> <col> (ej: /mover 0 0)");
    }
    
    /**
     * Intenta realizar un movimiento en el tablero.
     * Es 'synchronized' para evitar problemas si ambos jugadores mandan /mover casi al mismo tiempo.
     */
    public synchronized void hacerMovimiento(UnCliente jugador, int fila, int col) {
        if (!juegoActivo) {
            jugador.enviarMensaje("El juego ya ha terminado.");
            return;
        }
        
        if (!jugador.getId().equals(jugadorActual.getId())) {
            jugador.enviarMensaje("No es tu turno.");
            return;
        }

        if (fila < 0 || fila > 2 || col < 0 || col > 2) {
            jugador.enviarMensaje("Movimiento inválido. Fila/Columna debe ser 0, 1, o 2.");
            return;
        }

        if (tablero[fila][col] != ' ') {
            jugador.enviarMensaje("Movimiento inválido. Esa casilla ya está ocupada.");
            return;
        }

        // Realizar movimiento
        char simbolo = (jugador.getId().equals(jugador1.getId())) ? simboloJ1 : simboloJ2;
        tablero[fila][col] = simbolo;

        // Enviar tablero actualizado
        enviarTableroAMbos();

        // Revisar estado del juego
        if (verificarVictoria(simbolo)) {
            notificarVictoria(jugador, true); // true = victoria por jugada
        } else if (verificarEmpate()) {
            notificarEmpate();
        } else {
            // Cambiar turno
            jugadorActual = getOponente(jugador);
            jugadorActual.enviarMensaje("¡Es tu turno!");
        }
    }

    private void enviarTableroAMbos() {
        String tableroStr = getTableroString();
        jugador1.enviarMensaje(tableroStr);
        jugador2.enviarMensaje(tableroStr);
    }

    /**
     * Genera una representación en texto del tablero.
     */
    private String getTableroString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n  Tablero (Gato):\n");
        sb.append("   0   1   2 \n");
        sb.append("0  ").append(tablero[0][0]).append(" | ").append(tablero[0][1]).append(" | ").append(tablero[0][2]).append(" \n");
        sb.append("  ---|---|---\n");
        sb.append("1  ").append(tablero[1][0]).append(" | ").append(tablero[1][1]).append(" | ").append(tablero[1][2]).append(" \n");
        sb.append("  ---|---|---\n");
        sb.append("2  ").append(tablero[2][0]).append(" | ").append(tablero[2][1]).append(" | ").append(tablero[2][2]).append(" \n");
        return sb.toString();
    }

    /**
     * Revisa si el último movimiento resultó en una victoria.
     */
    private boolean verificarVictoria(char simbolo) {
        // Filas
        for (int i = 0; i < 3; i++) {
            if (tablero[i][0] == simbolo && tablero[i][1] == simbolo && tablero[i][2] == simbolo) return true;
        }
        // Columnas
        for (int j = 0; j < 3; j++) {
            if (tablero[0][j] == simbolo && tablero[1][j] == simbolo && tablero[2][j] == simbolo) return true;
        }
        // Diagonales
        if (tablero[0][0] == simbolo && tablero[1][1] == simbolo && tablero[2][2] == simbolo) return true;
        if (tablero[0][2] == simbolo && tablero[1][1] == simbolo && tablero[2][0] == simbolo) return true;
        
        return false;
    }

    /**
     * Revisa si el tablero está lleno (empate).
     */
    private boolean verificarEmpate() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (tablero[i][j] == ' ') {
                    return false; // Todavía hay espacios
                }
            }
        }
        // Si no hay espacios y no hubo victoria (verificado en hacerMovimiento), es empate.
        return true; 
    }

    /**
     * Notifica a los jugadores del resultado de victoria y termina el juego.
     * @param ganador El jugador que ganó.
     * @param porJugada true si ganó por 3 en raya, false si ganó por rendición/desconexión.
     */
    public void notificarVictoria(UnCliente ganador, boolean porJugada) {
        UnCliente perdedor = getOponente(ganador);
        
        if (porJugada) {
            ganador.enviarMensaje("¡Felicidades, " + ganador.getNombre() + "! Has ganado la partida.");
            perdedor.enviarMensaje("Mejor suerte la próxima, " + perdedor.getNombre() + ". Has perdido.");
        }
        
        // --- ¡CORRECCIÓN AQUÍ! ---
        // Registra la victoria en la base de datos (usando getUsername() como acordamos)
        ManejadorRankings.registrarPartida(jugador1.getUsername(), jugador2.getUsername(), ganador.getUsername());
        
        terminarJuego();
    }

    /**
     * Notifica a los jugadores de un empate y termina el juego.
     */
    private void notificarEmpate() {
        String msg = "¡La partida es un empate! (Gato)";
        jugador1.enviarMensaje(msg);
        jugador2.enviarMensaje(msg);
        
        // --- ¡CORRECCIÓN AQUÍ! ---
        // Registra el empate en la base de datos (ganador es null)
        ManejadorRankings.registrarPartida(jugador1.getUsername(), jugador2.getUsername(), null);

        terminarJuego();
    }

    /**
     * Limpia el estado del juego y de los jugadores.
     */
    public void terminarJuego() {
        this.juegoActivo = false;
        
        String fin = "--- PARTIDA FINALIZADA --- \nHas vuelto al chat general.";
        
        // Liberar a los jugadores para que puedan chatear/jugar de nuevo
        if (jugador1 != null) {
            jugador1.setIdJuegoActual(null);
            jugador1.enviarMensaje(fin);
        }
        if (jugador2 != null) {
            jugador2.setIdJuegoActual(null);
            jugador2.enviarMensaje(fin);
        }
        
        // Quitar de la lista de juegos activos
        // (Asegúrate de que 'ServerMulti' y 'juegosActivos' son accesibles)
        if (ServerMulti.juegosActivos != null) {
            ServerMulti.juegosActivos.remove(this.idJuego);
        }
    }
}