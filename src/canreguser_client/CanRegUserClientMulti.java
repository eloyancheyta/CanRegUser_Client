package canreguser_client;

import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CanRegUserClientMulti {

    public static void main(String[] args) {
        // Crear un pool de hilos con el número de hilos igual al número de clientes
        ExecutorService executorService = Executors.newFixedThreadPool(7);

        JsonObject metadata = new JsonObject();
        long timestamp = System.currentTimeMillis();
        metadata.addProperty("type", "request");
        metadata.addProperty("timestamp", timestamp);
        metadata.addProperty("institutionId", "inst1");

        // Crear tareas para enviar peticiones a las diferentes instancias
        JsonObject data = new JsonObject();
        data.addProperty("username", "morten");
        data.addProperty("password", "ervik");

        JsonObject request = new JsonObject();
        request.add("metadata", metadata);
        request.add("data", data);

        JsonObject metadata2 = new JsonObject();
        metadata2.addProperty("type", "request");
        metadata2.addProperty("timestamp", timestamp);
        metadata2.addProperty("institutionId", "inst2");

        JsonObject data2 = new JsonObject();
        data2.addProperty("username", "morten");
        data2.addProperty("password", "ervik");

        JsonObject request2 = new JsonObject();
        request2.add("metadata", metadata2);
        request2.add("data", data2);

        String r1Str = request.toString();
        String r2Str = request2.toString();

        // Crear más tareas concurrentes
        Runnable clientTask1 = createClientTask(r1Str);
        Runnable clientTask2 = createClientTask(r2Str);

        Runnable clientTask3 = createClientTask(r1Str);
        Runnable clientTask4 = createClientTask(r2Str);

        int m = 3600;
        int c = 1;
        while (c < m) {
            // Ejecutar las tareas en hilos separados
            executorService.submit(clientTask1);
            executorService.submit(clientTask2);
            executorService.submit(clientTask3);
            executorService.submit(clientTask4);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(CanRegUser_Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            c++;
        }

        // Apagar el executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
        }
    }

    private static Runnable createClientTask(String data) {
        return () -> {
            int retries = 0;
            final int maxRetries = 5;
            final int delay = 2000;

            while (retries < maxRetries) {
                try {
                    HttpClient client = HttpClient.newBuilder()
                            .version(Version.HTTP_2)
                            .build();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(new URI("http://localhost:8080/api/request"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(data))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("Client received response: " + response.body());
                    break;
                } catch (Exception e) {
                    System.err.println("Request failed: " + e.getMessage());
                    retries++;
                    if (retries < maxRetries) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        System.err.println("Max retries reached. Giving up.");
                    }
                }
            }
        };
    }
}
