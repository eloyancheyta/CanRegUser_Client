package canreguser_client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CanRegUser_Client {

    static String URL;

    static String fuenteId = "fuente1";

    public static void main(String[] args) {

        /*
        String ip = args[0];
        String port = args[1];
        URL = "https://"+ip+":"+port+"/api/request";
        
        String fuenteId = args[2];
         */
        String ip = "localhost";
        String port = "8080";
        URL = "https://" + ip + ":" + port + "/api/request";

        // Crear un pool de hilos con un solo hilo
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        JsonObject metadata = new JsonObject();
        metadata.addProperty("type", "request");
        metadata.addProperty("fuenteId", fuenteId);
        // Agregar aquí enviar el sobre digital (llave simetrica cifrada) que sirve para cifrar extremo a extremo el objeto data 
        metadata.addProperty("envDig", "aqui debe ir el sobre digital que contiene la llave simetrica");

        JsonObject data = new JsonObject();
        // También la operación se especifica aquí
        //data.addProperty("typeOfOperation", "logins");
        data.addProperty("typeOfOperation", "login");
        data.addProperty("username", "morten");
        data.addProperty("password", "ervik");

        JsonObject request = new JsonObject();
        request.add("metadata", metadata);
        request.add("data", data);

        String requestStr = request.toString();

        System.out.println("requestStr: " + requestStr);

        // Crear una tarea para enviar peticiones de manera iterativa
        Runnable clientTask = createClientTask(requestStr);
        scheduler.scheduleAtFixedRate(clientTask, 0, 4, TimeUnit.SECONDS);
        scheduler.schedule(() -> scheduler.shutdown(), 3, TimeUnit.SECONDS);
    }

    private static Runnable createClientTask(String data) {
        return () -> {
            try {
                // Cargar el KeyStore que contiene el certificado autofirmado
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(Files.newInputStream(Paths.get("truststore.jks")), "password".toCharArray());

                // Crear un TrustManager que confía en el certificado del servidor
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                // Crear el contexto SSL y configurarlo con el TrustManager
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, tmf.getTrustManagers(), null);

                // Crear el cliente HTTP con el contexto SSL configurado
                HttpClient client = HttpClient.newBuilder()
                        .version(Version.HTTP_2)
                        .sslContext(sslContext)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(data))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Client received response: " + response.body());

                // Parsear el body de la respuesta a JSON
                Gson gson = new Gson();
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);

                // Imprimir el JSON parseado
                System.out.println("Parsed JSON: " + jsonResponse.toString());

                
                // Hacer aquí la segunda petición, por ejemplo logOut
                JsonObject metadataIn = gson.fromJson(jsonResponse.get("metadata"), JsonObject.class);
                JsonObject dataIn = gson.fromJson(jsonResponse.get("data"), JsonObject.class);

                int codigo = metadataIn.get("codigo").getAsInt();
                if (codigo == 200) {
                    String token = dataIn.get("token").getAsString();
                    String sessionId = dataIn.get("sessionId").getAsString();
                    TOTP totp = new TOTP(7, 6);
                    String OTP = totp.generarCodigo(token.getBytes());

                    JsonObject metadataOut = new JsonObject();
                    
                    metadataOut.addProperty("type", "request");
                    metadataOut.addProperty("fuenteId", fuenteId);
                    metadataOut.addProperty("LBCode", "LBCode");
                    metadataOut.addProperty("envDig", "aqui va la llave cifrada");

                    JsonObject dataOut = new JsonObject();
                    dataOut.addProperty("typeOfOperation", "logOut");
                    dataOut.addProperty("sessionId", sessionId);
                    dataOut.addProperty("OTP", OTP);
                    //dataOut.addProperty("OTP", "a");

                    JsonObject requestOut = new JsonObject();
                    requestOut.add("metadata", metadataOut);
                    requestOut.add("data", dataOut);

                    String requestOutStr = requestOut.toString();

                    // Enviar logOut
                    request = HttpRequest.newBuilder()
                            .uri(new URI(URL))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestOutStr))
                            .build();

                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("Client received response: " + response.body());
                    
                    request = HttpRequest.newBuilder()
                            .uri(new URI(URL))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestOutStr))
                            .build();

                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("Client received response: " + response.body());
                }
                 
            } catch (Exception e) {
                e.printStackTrace();
            }

        };

    }
}
