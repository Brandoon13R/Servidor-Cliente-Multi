package org.yourcompany.yourproject;

import java.util.Random;

public class JuegoGato {
    private UnCliente jugador1;
    private UnCliente jugador2;
    private UnCliente jugadorActual;
    private final String idJuego;
    private final char[][] tablero;
    private boolean juegoActivo;
    private char simboloJ1;
    private char simboloJ2;


    public JuegoGato(UnCliente J1, UnCliente J2, String idJuego) {
        this.jugador1 = J1;
        this.jugador2 = J2;
        this.idJuego = idJuego;
        this.tablero = new char[3][3];
        inicializarTablero();
        this.juegoActivo = true;

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
                tablero[i][j] = ' ';
            }
        }
    }
    
    public boolean isJuegoActivo() {
        return juegoActivo;
    }
    
    public String getIdJuego() {
        return idJuego;
    }

    public UnCliente getOponente(UnCliente jugador) {
        if (jugador.getId().equals(jugador1.getId())) {
            return jugador2;
        } else if (jugador.getId().equals(jugador2.getId())) {
            return jugador1;
        }
        return null;
    }

    public void iniciarPartida() {
        String msgInicio = "--- ¡PARTIDA INICIADA! ---";
        String msgJ1 = "Juegas contra " + jugador2.getNombre() + ". Tú eres '" + simboloJ1 + "'.";
        String msgJ2 = "Juegas contra " + jugador1.getNombre() + ". Tú eres '" + simboloJ2 + "'.";

        jugador1.enviarMensaje(msgInicio);
        jugador1.enviarMensaje(msgJ1);
        jugador2.enviarMensaje(msgInicio);
        jugador2.enviarMensaje(msgJ2);
        
        enviarTableroAMbos();
        jugadorActual.enviarMensaje("¡Es tu turno! Usa /mover <fila> <col> (ej: /mover 0 0)");
    }
    
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

        char simbolo = (jugador.getId().equals(jugador1.getId())) ? simboloJ1 : simboloJ2;
        tablero[fila][col] = simbolo;

        enviarTableroAMbos();

        if (verificarVictoria(simbolo)) {
            notificarVictoria(jugador, true);
        } else if (verificarEmpate()) {
            notificarEmpate();
        } else {
            jugadorActual = getOponente(jugador);
            jugadorActual.enviarMensaje("¡Es tu turno!");
        }
    }

    private void enviarTableroAMbos() {
        String tableroStr = getTableroString();
        jugador1.enviarMensaje(tableroStr);
        jugador2.enviarMensaje(tableroStr);
    }

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

    private boolean verificarVictoria(char simbolo) {
        for (int i = 0; i < 3; i++) {
            if (tablero[i][0] == simbolo && tablero[i][1] == simbolo && tablero[i][2] == simbolo) return true;
        }
        for (int j = 0; j < 3; j++) {
            if (tablero[0][j] == simbolo && tablero[1][j] == simbolo && tablero[2][j] == simbolo) return true;
        }
        if (tablero[0][0] == simbolo && tablero[1][1] == simbolo && tablero[2][2] == simbolo) return true;
        if (tablero[0][2] == simbolo && tablero[1][1] == simbolo && tablero[2][0] == simbolo) return true;
        
        return false;
    }

    private boolean verificarEmpate() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (tablero[i][j] == ' ') {
                    return false;
                }
            }
        }
        return true; 
    }

    /**
     * 
     * @param ganador
     * @param porJugada
     */
    public void notificarVictoria(UnCliente ganador, boolean porJugada) {
        UnCliente perdedor = getOponente(ganador);
        
        if (porJugada) {
            ganador.enviarMensaje("¡Felicidades, " + ganador.getNombre() + "! Has ganado la partida.");
            perdedor.enviarMensaje("Mejor suerte la próxima, " + perdedor.getNombre() + ". Has perdido.");
        }
        
        ManejadorRankings.registrarPartida(jugador1.getUsername(), jugador2.getUsername(), ganador.getUsername());
        
        terminarJuego();
    }

    private void notificarEmpate() {
        String msg = "¡La partida es un empate! (Gato)";
        jugador1.enviarMensaje(msg);
        jugador2.enviarMensaje(msg);
        
        ManejadorRankings.registrarPartida(jugador1.getUsername(), jugador2.getUsername(), null);

        terminarJuego();
    }

    public void terminarJuego() {
        this.juegoActivo = false;
        
        String fin = "--- PARTIDA FINALIZADA --- \nHas vuelto al chat general.";
        
        if (jugador1 != null) {
            jugador1.setIdJuegoActual(null);
            jugador1.enviarMensaje(fin);
        }
        if (jugador2 != null) {
            jugador2.setIdJuegoActual(null);
            jugador2.enviarMensaje(fin);
        }

        if (ServerMulti.juegosActivos != null) {
            ServerMulti.juegosActivos.remove(this.idJuego);
        }
    }
}