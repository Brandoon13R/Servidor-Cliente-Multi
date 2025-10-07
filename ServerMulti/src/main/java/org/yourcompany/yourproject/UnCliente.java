package org.yourcompany.yourproject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class UnCliente implements Runnable {
    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

    final DataInputStream entrada;

    UnCliente(Socket s) throws IOException{
        salida = new DataInputStream(s.getInputStream());
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        String mensaje;
        while(true){
            try {
                mensaje = entrada.readUTF();
                Mensaje mensajeProcesado = new Mensaje(mensaje);
                enviarMensaje(mensajeProcesado);
            } catch (IOException ex) {
                System.out.println("Error al recibir mensaje: " + ex.getMessage());
            }
        }
    }
}
