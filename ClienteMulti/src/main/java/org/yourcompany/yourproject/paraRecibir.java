package org.yourcompany.yourproject;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class paraRecibir implements Runnable {
    private final DataInputStream entrada;
    private final Socket socket;

    // Se corrige 's' por 'socket'
    public paraRecibir(Socket socket) throws IOException {
        this.socket = socket;
        this.entrada = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        while (true) {
            try {
                String mensaje = entrada.readUTF();
                System.out.println(mensaje);
            } catch (IOException ex) {
                System.out.println("Te has desconectado del servidor.");
                break; // Salimos del bucle si hay un error (desconexión)
            }
        }
    }
}
