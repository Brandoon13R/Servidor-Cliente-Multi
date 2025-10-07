package org.yourcompany.yourproject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class paraEnviar implements Runnable {

    private final DataOutputStream salida;
    private final BufferedReader tecladoReader;

    public paraEnviar(Socket s) throws IOException {
        this.salida = new DataOutputStream(s.getOutputStream());
        this.tecladoReader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {
        try {
            while (true) {
                String mensaje;
                System.out.print("Tú: ");
                mensaje = tecladoReader.readLine();
                if (mensaje == null || mensaje.equalsIgnoreCase("/exit")) {
                    System.out.println("Cerrando conexión...");
                    break;
                }

                salida.writeUTF(mensaje);
                salida.flush();
            }
        } catch (IOException ex) {
            System.err.println("Error al enviar mensaje. El servidor podría estar desconectado.");
        } finally {
            try {
                if (salida != null) salida.close();
                if (tecladoReader != null) tecladoReader.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar los streams: " + e.getMessage());
            }
            System.out.println("Hilo de envío terminado.");
        }
    }
}
