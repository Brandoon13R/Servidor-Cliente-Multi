package org.yourcompany.yourproject;

import java.util.Random;

public class JuegoGato {
    private UnCliente jugador1;
    private UnCliente jugador2;
    private UnCliente jugadorActual;
    private final String idJuego;
    private final char[][] tablero;
    private boolean juegoActivo;
    private final UnCliente[] jugadores;
    private char simboloJ1;
    private char simboloJ2;

    public JuegoGato(UnCliente J1, UnCliente J2, String idJuego) {
        this.jugador1 = J1;
        this.jugador2 = J2;
        this.idJuego = idJuego;
        this.tablero = new char[3][3];

        this.jugadores = new UnCliente[]{this.jugador1, this.jugador2};

        inicializarTablero();
        this.juegoActivo = true;

        Random random = new Random();
        boolean j1Empieza = random.nextBoolean(); // Randomly decide who starts
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
    
    public char[] getSimbolos() {
        return simbolos;
    }
    
    public UnCliente[] getJugadores() {
        return jugadores;
    }

    public String getIdJuego() {
        return idJuego;
    }
}