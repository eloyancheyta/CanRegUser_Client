package read;

/**
 *
 * @author eloya
 */
import zsecurity.TOTP;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class ReadPatientByCURP_0 {

    static String URL;

    public static void main(String[] args) throws Exception {
        //d9d9de953aa505fda3aa3cca82eb45031abfc32ea6e6ad3b292a928c8570b409
        //7d52dfa8f873870b9b8e5b9705d7c738e91db9d7897be37cac85f1bb39a855c9  //pruebas con el Doctor
        
        
        //b0901c14d76501054d960ed6942413eda901a5b399c4827288246674ba70e799  solo mía
        
        String CanRegID = "b0901c14d76501054d960ed6942413eda901a5b399c4827288246674ba70e799";
        String ip = "127.0.0.1";
        String port = "443";
        String loginAPI = "login";
        String requestAPI = "request";

        ip = "apix.tamps.cinvestav.mx";

        URL = "https://" + ip + "/canreg/api/" + loginAPI;

        // Crear el contenido de metadata
        JsonObject metadataRequestPayload = new JsonObject();
        metadataRequestPayload.addProperty("fuenteId", CanRegID);

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
                .build();

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
        URL = "https://" + ip + ":" + port + "/canreg/api/" + requestAPI;

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
            metadataRequestPayload.addProperty("fuenteId", CanRegID);

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
            dataRequest.addProperty("typeOfOperation", "getPatientByCURP");
            //dataRequest.addProperty("typeOfOperation", "logOut");// se requiere
            dataRequest.addProperty("hash", computeHash(dataRequestPayload.toString()));
            dataRequest.add("payload", dataRequestPayload);
            dataRequest.addProperty("sessionId", sessionId);
            dataRequest.addProperty("OTP", OTP);

            //
            //AEEE970320HCSNSL00
            dataRequest.addProperty("CURP", "123456789");//20240001  0 1

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

            JsonArray patients = payloadIn.get("patients").getAsJsonArray();
            
            OTP = totp.generarCodigo(token.getBytes());

            // Segunda petición: logout
            URL = "https://" + ip + "/canreg/api/" + requestAPI;

            JsonObject logoutMetadataRequestPayload = new JsonObject();
            logoutMetadataRequestPayload.addProperty("fuenteId", CanRegID);

            JsonObject logoutMetadataRequest = new JsonObject();
            logoutMetadataRequest.addProperty("hash", computeHash(logoutMetadataRequestPayload.toString()));
            logoutMetadataRequest.add("payload", logoutMetadataRequestPayload);

            JsonObject logoutRequestJson = new JsonObject();
            logoutRequestJson.addProperty("metadataDigEnv", "aqui va la llave publica de acuerdo de llaves para cifrar/descifrar metadata");
            logoutRequestJson.add("metadata", logoutMetadataRequest);

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
        }

    }

    // Método para calcular el hash (SHA-256) de un string
    private static String computeHash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
