package org.yourcompany.yourproject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClienteMulti {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 8080);
            System.out.println("Conectado al servidor en el puerto 8080.");

            // Pedimos el nombre al usuario
            Scanner scanner = new Scanner(System.in);
            System.out.print("Ingresa tu nombre de usuario: ");
            String nombreUsuario = scanner.nextLine();

            // Enviamos el nombre de usuario al servidor como primer mensaje
            DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
            salida.writeUTF(nombreUsuario);

            // Iniciar hilo para enviar mensajes
            paraEnviar paraEnviar = new paraEnviar(socket, scanner);
            Thread hiloMandar = new Thread(paraEnviar);
            hiloMandar.start();

            // Iniciar hilo para recibir mensajes
            paraRecibir paraRecibir = new paraRecibir(socket);
            Thread hiloRecibir = new Thread(paraRecibir);
            hiloRecibir.start();

        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor: " + e.getMessage());
        }
    }
}