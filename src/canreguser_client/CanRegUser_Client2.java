package canreguser_client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CanRegUser_Client2 {

    static String URL;

    public static void main(String[] args) {

        /*
        String ip = args[0];
         String port = args[1];
         URL = "http://"+ip+":"+port+"/api/request";
        
         String fuenteId = args[2];
         */
        String ip = "localhost";
        String port = "8080";
        URL = "http://" + ip + ":" + port + "/api/request";

        String fuenteId = "inst1";

        // Crear un pool de hilos con un solo hilo
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        JsonObject metadata = new JsonObject();
        metadata.addProperty("type", "request");
        metadata.addProperty("fuenteId", fuenteId);
        //agregar aqui enviar el sobre digital (llave simetrica cifrada). que sirve para cifrar extre-extremo el objeto data 
        metadata.addProperty("envDig", "aqui debe ir el sobre digital que contiene la llave simetrica");

        JsonObject data = new JsonObject();
        //tambien la operación se especifica aqui
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

        //clientTask.
        // Programar la tarea para que se ejecute cada segundo
        //scheduler.scheduleAtFixedRate(clientTask, 0, 1, TimeUnit.SECONDS);
        // Apagar el scheduler después de una hora
        //scheduler.schedule(() -> scheduler.shutdown(), 1, TimeUnit.HOURS);
    }

    private static Runnable createClientTask(String data) {
        return () -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .version(Version.HTTP_2)
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
                //hacer aqui la segunda petición, por ejemplo logOut

                JsonObject metadataIn = gson.fromJson(jsonResponse.get("metadata"), JsonObject.class);
                JsonObject dataIn = gson.fromJson(jsonResponse.get("data"), JsonObject.class);

                int codigo = metadataIn.get("codigo").getAsInt();
                if (codigo == 200) {
                    //String LBCode = metadataIn.get("LBCode").getAsString();
                    String token = dataIn.get("token").getAsString();
                    String sessionId = dataIn.get("sessionId").getAsString();
                    TOTP totp = new TOTP(7, 6);
                    String OTP = totp.generarCodigo(token.getBytes());

                    JsonObject metadataOut = new JsonObject();
                    // para peticiones diferentes a login se envia el LBCode en metadata
                    //metadata.addProperty("LBCode", LBCode); //en este demo aún no he integrado el LB
                     
                    String fuenteId = "inst1";
                    metadataOut.addProperty("type", "request");
                    metadataOut.addProperty("fuenteId", fuenteId);
                    metadataOut.addProperty("LBCode", "LBCode");
                    //agregar aqui enviar el sobre digital (llave simetrica cifrada). que sirve para cifrar extre-extremo el objeto data 
                    metadataOut.addProperty("envDig", "aqui va la llave cifrada");

                    JsonObject dataOut = new JsonObject();
                    //tambien la operación se especifica aqui
                    //dataOut.addProperty("typeOfOperation", "savePatient");
                    dataOut.addProperty("typeOfOperation", "logOut");
                    dataOut.addProperty("sessionId", sessionId);
                    dataOut.addProperty("OTP", OTP);

                    JsonObject requestOut = new JsonObject();
                    requestOut.add("metadata", metadataOut);
                    requestOut.add("data", dataOut);

                    String requestOutStr = requestOut.toString();

                    //envia r logOut
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
