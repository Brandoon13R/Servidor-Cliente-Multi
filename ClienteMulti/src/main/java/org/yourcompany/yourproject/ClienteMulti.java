/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package org.yourcompany.yourproject;

import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author brand
 */
public class ClienteMulti {

    public static void main(String[] args) throws IOException{
        Socket s = new Socket("localhost", 8080);
        paraEnviar ParaEnviar = new paraEnviar(s);
        Thread hiloMandar = new Thread(ParaEnviar);
        hiloMandar.start();

        paraRecibir paraRecibir = new paraRecibir(s);
        Thread hiloRecibir = new Thread(paraRecibir);
        hiloRecibir.start();
    }
}
