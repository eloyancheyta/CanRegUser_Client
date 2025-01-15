package create;

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class saveAll {

    private static final String LOGIN_API = "login";
    private static final String REQUEST_API = "request";
    private static final String REGISTER_API = "savePatient";
    private static final String CANREG_ID = "329e3d72bdbf889057de42bcd02ce17ad4ef2456a4f61d1a54fda862b11f914d";
    
    //CANREG_ID = "7d52dfa8f873870b9b8e5b9705d7c738e91db9d7897be37cac85f1bb39a855c9";

    static String URL;

    public static void main(String[] args) throws Exception {
        String ip = "127.0.0.1";
        String port = "8080";

        ip = "apix.tamps.cinvestav.mx";

        //URL = "https://" + ip + "/canreg/api/"+ loginAPI ;
        URL = "https://" + ip + ":" + port + "/canreg/api/";
        URL = "https://" + ip + "/canreg/api/";
        /*
        // Configurar contexto SSL
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(Files.newInputStream(Paths.get("truststore.jks")), "password".toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, tmf.getTrustManagers(), null);
         */
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

        // Leer archivo JSON con pacientes
       JsonArray patientsData = JsonParser.parseReader(new FileReader(Paths.get("patient_data.json").toFile())).getAsJsonArray();
 //JsonArray patientsData = JsonParser.parseReader(new FileReader(Paths.get("patient_data_updated.json").toFile())).getAsJsonArray();

        // Registrar pacientes
        //for (int i = 0; i < patientsData.size(); i++) {
       for (int i = 0; i < /*patientsData.size()*/ 1; i++) {
            JsonObject patientData = patientsData.get(i).getAsJsonObject();
            registerPatient(URL + REQUEST_API, httpClient, patientData, sessionId, totp.generarCodigo(token.getBytes()));
        }

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

    private static void registerPatient(String url, HttpClient httpClient, JsonObject patientData, String sessionId, String OTP) throws Exception {
        JsonObject metadataPayload = new JsonObject();
        metadataPayload.addProperty("fuenteId", CANREG_ID);

        JsonObject metadata = new JsonObject();
        metadata.addProperty("hash", computeHash(metadataPayload.toString()));
        metadata.add("payload", metadataPayload);

        JsonObject dataPayload = new JsonObject();
        dataPayload.addProperty("sessionId", sessionId);
        dataPayload.addProperty("OTP", OTP);
        dataPayload.add("datosPaciente", patientData);
        //dataPayload.add("patient", patientData);

        JsonObject data = new JsonObject();
        // data.addProperty("typeOfOperation", "savePatient");
        data.addProperty("typeOfOperation", "saveAll");
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
        System.out.println("Patient registration response: " + response.body());
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
