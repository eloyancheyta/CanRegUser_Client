package canreguser_client;

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

public class CanRegUser_Client1 {
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
         URL = "http://"+ip+":"+port+"/api/request";
        
         String fuenteId = "inst1";
        
        
        
        // Crear un pool de hilos con un solo hilo
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        JsonObject metadata = new JsonObject();
        /*/
        para peticiones diferentes a login se envia el LBCode en metadata
        */
        metadata.addProperty("type", "request");
        metadata.addProperty("fuenteId", fuenteId);
        //agregar aqui enviar el sobre digital (llave simetrica cifrada). que sirve para cifrar extre-extremo el objeto data 

        JsonObject data = new JsonObject();
       
        //tambien la operación se especifica aqui
        data.addProperty("typeOfOperation", "login");
        data.addProperty("username", "morten");
        data.addProperty("password", "ervik");
         /*
        para peticiones diferentes a loggin
        se envia el sessionId y el OTP         
        */
        //data.addProperty("typeOfOperation", "savePatient");
        //data.addProperty("sessionId", "sessionId");
        //data.addProperty("OTP", "OTP");

        JsonObject request = new JsonObject();
        request.add("metadata", metadata);
        request.add("data", data);

        String requestStr = request.toString();

        // Crear una tarea para enviar peticiones de manera iterativa
        Runnable clientTask = createClientTask(requestStr);

        // Programar la tarea para que se ejecute cada segundo
        scheduler.scheduleAtFixedRate(clientTask, 0, 4, TimeUnit.SECONDS);
        //scheduler.scheduleAtFixedRate(clientTask, 0, 1, TimeUnit.SECONDS);

        // Apagar el scheduler después de una hora
        scheduler.schedule(() -> scheduler.shutdown(), 4, TimeUnit.SECONDS);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
}
