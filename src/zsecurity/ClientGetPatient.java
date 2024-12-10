package zsecurity;

/**
 *
 * @author eloya
 */
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.HashMap;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class ClientGetPatient {

    static String URL;

    public static void main(String[] args) throws Exception {
        String ip = "127.0.0.1";
        String port = "8080";
        String API = "login";
        URL = "https://" + ip + ":" + port + "/api/" + API;

        // Cargar el KeyStore que contiene el certificado autofirmado
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(Files.newInputStream(Paths.get("truststore.jks")), "password".toCharArray());

        // Crear un TrustManager que confía en el certificado del servidor
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // Crear el contexto SSL y configurarlo con el TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, tmf.getTrustManagers(), null);

        // Crear el contenido de metadata
        JsonObject metadataRequestPayload = new JsonObject();
        metadataRequestPayload.addProperty("fuenteId", "fuente1");

        JsonObject metadataRequest = new JsonObject();
        //metadataRequest.addProperty("typeOfOperation", "login");
        metadataRequest.addProperty("hash", computeHash(metadataRequestPayload.toString()));
        metadataRequest.add("payload", metadataRequestPayload);

        // Crear la estructura completa de la petición
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("metadataDigEnv", "aqui va la llave publica de acuerdo de llaves para cifrar/descifrar metadata");
        requestJson.add("metadata", metadataRequest);

        JsonObject dataRequestPayload = new JsonObject();
        dataRequestPayload.addProperty("username", "morten");
        dataRequestPayload.addProperty("password", "ervik");

        /*
        dataRequestPayload.addProperty("username", "nombre_de_usuario");
        dataRequestPayload.addProperty("password", "contraseña");
         */
        //cifrar también data
        JsonObject dataRequest = new JsonObject();
        dataRequest.addProperty("typeOfOperation", "login");//no se requiere
        dataRequest.addProperty("hash", computeHash(dataRequestPayload.toString()));
        dataRequest.add("payload", dataRequestPayload);

        requestJson.addProperty("dataDigEnv", "aqui va la llave publica de acuerdo de llaves para cifrar/descifrar data");

        //aqui se debe cifrar data. Crear sobre digital o acuerdo de llave usando la llave ública del CRS-C (CanReg) receptor
        requestJson.add("data", dataRequest);

        // Enviar la petición al servidor
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .sslContext(sslContext)
                .build();

        String host = "localhost";

        host = "192.168.100.24";

        host = "127.0.0.1";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Client received response: " + response.body());

        //si es 200 ok procesar
        //descifrar respuesta metadata, luego data
        //verificar hash
        // Parsear la respuesta
        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        //imoprimir respuesta recibida
        System.out.println("responseJson: " + responseJson.toString());

        ///logout
        API = "request";
        URL = "https://" + ip + ":" + port + "/api/" + API;

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
            System.out.println("codigo: " + codigo);

            JsonObject payloadIn = dataIn.get("payload").getAsJsonObject();//"";
            String token = payloadIn.get("token").getAsString();//"";
            String sessionId = payloadIn.get("sessionId").getAsString();//"";
            TOTP totp = new TOTP(7, 6);
            String OTP = totp.generarCodigo(token.getBytes());

            //limpiar los campos y hacer nueva petición
            metadataRequestPayload = new JsonObject();
            metadataRequestPayload.addProperty("fuenteId", "fuente1");

            metadataRequest = new JsonObject();
            //metadataRequest.addProperty("typeOfOperation", "login");
            metadataRequest.addProperty("hash", computeHash(metadataRequestPayload.toString()));
            metadataRequest.add("payload", metadataRequestPayload);

            // Crear la estructura completa de la petición
            requestJson = new JsonObject();
            requestJson.addProperty("metadataDigEnv", "aqui va la llave publica de acuerdo de llaves para cifrar/descifrar metadata");
            requestJson.add("metadata", metadataRequest);

            dataRequestPayload = new JsonObject();
            //dataRequestPayload.addProperty("username", "morten");
            //dataRequestPayload.addProperty("password", "ervik");

            dataRequestPayload.addProperty("sessionId", sessionId);
            dataRequestPayload.addProperty("OTP", OTP);

            /*
        dataRequestPayload.addProperty("username", "nombre_de_usuario");
        dataRequestPayload.addProperty("password", "contraseña");
             */
            //cifrar también data
            dataRequest = new JsonObject();
            dataRequest.addProperty("typeOfOperation", "getPatient");
            //dataRequest.addProperty("typeOfOperation", "logOut");// se requiere
            dataRequest.addProperty("hash", computeHash(dataRequestPayload.toString()));
            dataRequest.add("payload", dataRequestPayload);
            dataRequest.addProperty("sessionId", sessionId);
            dataRequest.addProperty("OTP", OTP);

            dataRequest.addProperty("recordID", 1);//20240001  0 1

            requestJson.addProperty("dataDigEnv", "aqui va la llave publica de acuerdo de llaves para cifrar/descifrar data");

            //aqui se debe cifrar data. Crear sobre digital o acuerdo de llave usando la llave ública del CRS-C (CanReg) receptor
            requestJson.add("data", dataRequest);

            String requestOutStr = requestJson.toString();

            // Enviar logOut
            request = HttpRequest.newBuilder()
                    .uri(new URI(URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestOutStr))
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Client received response: " + response.body());

            jsonResponse = gson.fromJson(response.body(), JsonObject.class);

            // Imprimir el JSON parseado
            System.out.println("Parsed JSON: " + jsonResponse.toString());

            // Hacer aquí la segunda petición, por ejemplo logOut
            metadataIn = gson.fromJson(jsonResponse.get("metadata"), JsonObject.class);
            dataIn = gson.fromJson(jsonResponse.get("data"), JsonObject.class);
            
             payloadIn = dataIn.get("payload").getAsJsonObject();
            
            String patientStr = payloadIn.get("patient").getAsString();            
            System.out.println("patientStr: "+patientStr);
            
            JsonObject patientObj = gson.fromJson(patientStr, JsonObject.class);
            
            String tumorStr = payloadIn.get("tumor").getAsString();            
            System.out.println("tumorStr: "+tumorStr);
            
            String fuenteStr = payloadIn.get("fuente").getAsString();            
            System.out.println("fuenteStr: "+fuenteStr);

           
            //20240001
        }

    }

    // Método para calcular el hash (SHA-256) de un string
    private static String computeHash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
