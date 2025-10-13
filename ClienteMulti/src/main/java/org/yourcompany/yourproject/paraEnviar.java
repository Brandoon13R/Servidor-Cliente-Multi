package org.yourcompany.yourproject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class paraEnviar implements Runnable {

    private final DataOutputStream salida;
    private final Socket socket;
    private final Scanner tecladoScanner;

    public paraEnviar(Socket socket, Scanner scanner) throws IOException {
        this.socket = socket;
        this.salida = new DataOutputStream(socket.getOutputStream());
        this.tecladoScanner = scanner;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String mensaje = tecladoScanner.nextLine();
                salida.writeUTF(mensaje);
                salida.flush();

                if (mensaje.equalsIgnoreCase("/exit")) {
                    System.out.println("Cerrando conexión...");
                    break;
                }
            }
        } catch (IOException ex) {
            // Este error suele ocurrir si el servidor se cierra mientras el cliente está activo.
            System.err.println("La conexión con el servidor se ha perdido.");
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket: " + e.getMessage());
            }
        }
    }
}