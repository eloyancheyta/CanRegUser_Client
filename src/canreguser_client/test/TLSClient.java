/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package canreguser_client.test;

/**
 *
 * @author eloya
 */

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class TLSClient {
    public static void main(String[] args) {
        try {
            // Cargar el keystore del cliente
            KeyStore clientStore = KeyStore.getInstance("PKCS12");
            clientStore.load(TLSClient.class.getResourceAsStream("client-keystore.p12"), "password".toCharArray());

            // Inicializar KeyManagerFactory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(clientStore, "password".toCharArray());

            // Inicializar SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            // Crear SSLSocketFactory
            SSLSocketFactory factory = sslContext.getSocketFactory();

            // Crear socket TLS y conectarse al servidor
            SSLSocket socket = (SSLSocket) factory.createSocket("localhost", 8000);
            socket.startHandshake();

            // Enviar mensaje al servidor
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("Hello from Java TLS client!");

            // Leer respuesta del servidor
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Server says: " + in.readLine());

            // Cerrar conexiones
            in.close();
            out.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
