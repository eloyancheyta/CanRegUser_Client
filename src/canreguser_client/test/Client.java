/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package canreguser_client.test;

/**
 *
 * @author eloya
 */
 //TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256
// Client.java
import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        String hostname = "192.168.100.4";
        int port = 3000;

        try (Socket socket = new Socket(hostname, port)) {

            // Enviar datos al servidor
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println("Hola desde el cliente");

            // Recibir respuesta del servidor
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String response = reader.readLine();
            System.out.println("Respuesta del servidor: " + response);

        } catch (UnknownHostException ex) {
            System.out.println("Servidor no encontrado: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Error de I/O: " + ex.getMessage());
        }
    }
}
