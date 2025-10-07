package org.yourcompany.yourproject;

import java.io.IOException;
import java.net.Socket;
import java.io.DataInputStream;

public class paraRecibir implements Runnable {
    final DataInputStream entrada;

    public paraRecibir(Socket s) throws IOException {
        this.entrada = new DataInputStream(s.getInputStream());
    }
    @Override
    public void run(){
        String mensaje;
        while(true){
            try {
                mensaje = entrada.readUTF();
                System.out.println(mensaje);
            } catch (final IOException ex) {
                System.out.println("Error al recibir mensaje");
            }
        }
    }
}
