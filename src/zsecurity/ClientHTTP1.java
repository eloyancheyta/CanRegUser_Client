package zsecurity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class ClientHTTP1 {

    static String URL;

    public static void main(String[] args) throws Exception {
        String ip = "127.0.0.1";
        String port = "8080";
        String loginAPI = "login";
        String request = "request";
        
        port = "443";

        // Primera petición: login
        URL = "http://" + ip + ":" + port + "/api/" + loginAPI;

        JsonObject metadataRequestPayload = new JsonObject();
        metadataRequestPayload.addProperty("fuenteId", "fuente1");

        JsonObject metadataRequest = new JsonObject();
        metadataRequest.addProperty("hash", computeHash(metadataRequestPayload.toString()));
        metadataRequest.add("payload", metadataRequestPayload);

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("metadataDigEnv", "aqui va la llave publica de acuerdo de llaves para cifrar/descifrar metadata");
        requestJson.add("metadata", metadataRequest);

        JsonObject dataRequestPayload = new JsonObject();
        dataRequestPayload.addProperty("username", "morten");
        dataRequestPayload.addProperty("password", "ervik");

        JsonObject dataRequest = new JsonObject();
        dataRequest.addProperty("typeOfOperation", "login");
        dataRequest.addProperty("hash", computeHash(dataRequestPayload.toString()));
        dataRequest.add("payload", dataRequestPayload);

        requestJson.addProperty("dataDigEnv", "aqui va la llave publica de acuerdo de llaves para cifrar/descifrar data");
        requestJson.add("data", dataRequest);

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(new URI(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                .build();

        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Client received login response: " + loginResponse.body());

        JsonObject loginResponseJson = JsonParser.parseString(loginResponse.body()).getAsJsonObject();
        System.out.println("Login response JSON: " + loginResponseJson);

        // Extraer datos de la respuesta
        JsonObject metadataIn = loginResponseJson.getAsJsonObject("metadata");
        JsonObject dataIn = loginResponseJson.getAsJsonObject("data");

        int codigo = metadataIn.get("codigo").getAsInt();
        if (codigo == 200) {
            String sessionId = dataIn.getAsJsonObject("payload").get("sessionId").getAsString();
            String token = dataIn.getAsJsonObject("payload").get("token").getAsString();

            // Generar OTP para la sesión
            TOTP totp = new TOTP(7, 6); // Parámetros del TOTP
            String OTP = totp.generarCodigo(token.getBytes());

            // Segunda petición: logout
            URL = "http://" + ip + ":" + port + "/api/" + request;

            JsonObject logoutMetadataRequestPayload = new JsonObject();
            logoutMetadataRequestPayload.addProperty("fuenteId", "fuente1");

            JsonObject logoutMetadataRequest = new JsonObject();
            logoutMetadataRequest.addProperty("hash", computeHash(logoutMetadataRequestPayload.toString()));
            logoutMetadataRequest.add("payload", logoutMetadataRequestPayload);

            JsonObject logoutRequestJson = new JsonObject();
            logoutRequestJson.addProperty("metadataDigEnv", "aqui va la llave publica de acuerdo de llaves para cifrar/descifrar metadata");
            logoutRequestJson.add("metadata", logoutMetadataRequest);

             OTP = totp.generarCodigo(token.getBytes());
             
            JsonObject logoutDataRequestPayload = new JsonObject();
            logoutDataRequestPayload.addProperty("sessionId", sessionId);
            logoutDataRequestPayload.addProperty("OTP", OTP);

            JsonObject logoutDataRequest = new JsonObject();
            logoutDataRequest.addProperty("typeOfOperation", "logOut");
            logoutDataRequest.addProperty("hash", computeHash(logoutDataRequestPayload.toString()));
            logoutDataRequest.add("payload", logoutDataRequestPayload);
            logoutDataRequest.addProperty("sessionId", sessionId);
            logoutDataRequest.addProperty("OTP", OTP);

            logoutRequestJson.addProperty("dataDigEnv", "aqui va la llave publica de acuerdo de llaves para cifrar/descifrar data");
            logoutRequestJson.add("data", logoutDataRequest);
            
            

            HttpRequest logoutRequest = HttpRequest.newBuilder()
                    .uri(new URI(URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(logoutRequestJson.toString()))
                    .build();

            HttpResponse<String> logoutResponse = client.send(logoutRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Client received logout response: " + logoutResponse.body());
        } else {
            System.out.println("Login failed with code: " + codigo);
        }
    }

    // Método para calcular el hash (SHA-256) de un string
    private static String computeHash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
