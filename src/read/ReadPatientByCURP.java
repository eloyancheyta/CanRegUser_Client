package read;

/**
 *
 * @author eloya
 */
//public class RegisterPatientsWithSession 
import zsecurity.TOTP;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;

public class ReadPatientByCURP {

    private static final String LOGIN_API = "login";
    private static final String REQUEST_API = "request";

    /**
     * *******************
     */
    // d9d9de953aa505fda3aa3cca82eb45031abfc32ea6e6ad3b292a928c8570b409
    /**
     * *****************************
     */
    private static final String CANREG_ID = "b0901c14d76501054d960ed6942413eda901a5b399c4827288246674ba70e799";
//

    /**
     * *******************
     */
    // d9d9de953aa505fda3aa3cca82eb45031abfc32ea6e6ad3b292a928c8570b409
    /**
     * *****************************
     */
    static String URL;

    public static void main(String[] args) throws Exception {
        String ip = "127.0.0.1";
        String port = "8080";

        ip = "apix.tamps.cinvestav.mx";

        URL = "https://" + ip + "/canreg/api/";

        // Crear cliente HTTP
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                //.sslContext(sslContext)
                .build();

        // Iniciar sesión
        JsonObject loginResponse = login(URL + LOGIN_API, httpClient);

        JsonObject payload = loginResponse.getAsJsonObject("data").getAsJsonObject("payload");
        String sessionId = payload.get("sessionId").getAsString();
        String token = payload.get("token").getAsString();

        TOTP totp = new TOTP(7, 6);
        String OTP = totp.generarCodigo(token.getBytes());
        
        String CURP = "";
        CURP = "123456789";
        // patientJsonObj.addProperty("CURP", "123456789");    
        /*
        patientJsonObj.addProperty("FirstN", "Alejandro");
        patientJsonObj.addProperty("FamN", "Salazar");
        */

        readPatientByCURP(URL + REQUEST_API, httpClient, CURP, sessionId, totp.generarCodigo(token.getBytes()));
         
        OTP = totp.generarCodigo(token.getBytes());
        // Cerrar sesión
        logout(URL + REQUEST_API, httpClient, sessionId, OTP);
    }

    private static JsonObject login(String url, HttpClient httpClient) throws Exception {
        JsonObject metadataRequestPayload = new JsonObject();
        metadataRequestPayload.addProperty("fuenteId", CANREG_ID);

        JsonObject metadataRequest = new JsonObject();
        metadataRequest.addProperty("hash", computeHash(metadataRequestPayload.toString()));
        metadataRequest.add("payload", metadataRequestPayload);

        JsonObject dataRequestPayload = new JsonObject();
        dataRequestPayload.addProperty("username", "morten");
        dataRequestPayload.addProperty("password", "ervik");

        JsonObject dataRequest = new JsonObject();
        dataRequest.addProperty("typeOfOperation", "login");
        dataRequest.addProperty("hash", computeHash(dataRequestPayload.toString()));
        dataRequest.add("payload", dataRequestPayload);

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("metadataDigEnv", "llave_publica_metadata");
        requestJson.add("metadata", metadataRequest);
        requestJson.addProperty("dataDigEnv", "llave_publica_data");
        requestJson.add("data", dataRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Login response: " + response.body());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static void readPatientByCURP(String url, HttpClient httpClient, String  CURP, String sessionId, String OTP) throws Exception {
        JsonObject metadataPayload = new JsonObject();
        metadataPayload.addProperty("fuenteId", CANREG_ID);

        JsonObject metadata = new JsonObject();
        metadata.addProperty("hash", computeHash(metadataPayload.toString()));
        metadata.add("payload", metadataPayload);

        JsonObject dataPayload = new JsonObject();
        dataPayload.addProperty("sessionId", sessionId);
        dataPayload.addProperty("OTP", OTP);
        dataPayload.addProperty("CURP", CURP);
        //dataPayload.add("patient", patientData);

        JsonObject data = new JsonObject();
        // data.addProperty("typeOfOperation", "savePatient");
        data.addProperty("typeOfOperation", "readPatientByCURP");
        data.addProperty("hash", computeHash(dataPayload.toString()));
        data.addProperty("sessionId", sessionId);
        data.addProperty("OTP", OTP);
        data.add("payload", dataPayload);

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("metadataDigEnv", "llave_publica_metadata");
        requestJson.add("metadata", metadata);
        requestJson.addProperty("dataDigEnv", "llave_publica_data");
        requestJson.add("data", data);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("readSimilarPatients response: " + response.body());
    }

    private static void logout(String url, HttpClient httpClient, String sessionId, String OTP) throws Exception {
        JsonObject metadataRequestPayload = new JsonObject();
        metadataRequestPayload.addProperty("fuenteId", CANREG_ID);

        JsonObject metadataRequest = new JsonObject();
        metadataRequest.addProperty("hash", computeHash(metadataRequestPayload.toString()));
        metadataRequest.add("payload", metadataRequestPayload);

        JsonObject dataRequestPayload = new JsonObject();
        dataRequestPayload.addProperty("sessionId", sessionId);
        dataRequestPayload.addProperty("OTP", OTP);

        JsonObject dataRequest = new JsonObject();
        dataRequest.addProperty("typeOfOperation", "logOut");
        dataRequest.addProperty("hash", computeHash(dataRequestPayload.toString()));
        dataRequest.add("payload", dataRequestPayload);
        dataRequest.addProperty("sessionId", sessionId);
        dataRequest.addProperty("OTP", OTP);

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("metadataDigEnv", "llave_publica_metadata");
        requestJson.add("metadata", metadataRequest);
        requestJson.addProperty("dataDigEnv", "llave_publica_data");
        requestJson.add("data", dataRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Logout response: " + response.body());
    }

    private static String computeHash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
