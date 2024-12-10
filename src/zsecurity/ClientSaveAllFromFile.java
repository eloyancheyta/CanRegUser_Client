package zsecurity;

/**
 *
 * @author eloya
 */
import com.google.gson.Gson;
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
import java.util.HashMap;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class ClientSaveAllFromFile {

    static String URL;

    public static void main(String[] args) throws Exception {
        String ip = "127.0.0.1";
        String port = "8080";
        String API = "login";
        URL = "https://" + ip + ":" + port + "/api/" + API;

        // Cargar los datos de pacientes desde el archivo JSON
        String jsonFilePath = "patient_data.json";
        JsonArray patientDataArray = JsonParser.parseReader(new FileReader(Paths.get(jsonFilePath).toFile())).getAsJsonArray();

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

            // Iterar sobre cada paciente en el JSON y enviar las peticiones al servidor
            for (int i = 0; i < patientDataArray.size(); i++) {
                JsonObject patientData = patientDataArray.get(i).getAsJsonObject();
                //petición saveAll
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
                OTP = totp.generarCodigo(token.getBytes());
                dataRequestPayload.addProperty("OTP", OTP);

                // JsonObject patientData = getPatientData();
                // Agregar "datos del paciente" al payload
                dataRequestPayload.add("datos del paciente", patientData);

                /**/

 /*
        dataRequestPayload.addProperty("username", "nombre_de_usuario");
        dataRequestPayload.addProperty("password", "contraseña");
                 */
                //cifrar también data
                dataRequest = new JsonObject();
                dataRequest.addProperty("typeOfOperation", "saveAll");
                //dataRequest.addProperty("typeOfOperation", "logOut");// se requiere
                dataRequest.addProperty("hash", computeHash(dataRequestPayload.toString()));
                dataRequest.add("payload", dataRequestPayload);
                dataRequest.addProperty("sessionId", sessionId);
                dataRequest.addProperty("OTP", OTP);

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
            }
            ///petición get patients
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
            OTP = totp.generarCodigo(token.getBytes());
            dataRequestPayload.addProperty("OTP", OTP);
            /**/

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

            dataRequest.addProperty("CURP", "AEEE970320HCSNSL00");//20240001  0 1

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

            System.out.println("codigo: " + codigo);

            OTP = totp.generarCodigo(token.getBytes());

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
            dataRequest.addProperty("typeOfOperation", "logOut");//no se requiere
            dataRequest.addProperty("hash", computeHash(dataRequestPayload.toString()));
            dataRequest.add("payload", dataRequestPayload);
            dataRequest.addProperty("sessionId", sessionId);
            dataRequest.addProperty("OTP", OTP);

            requestJson.addProperty("dataDigEnv", "aqui va la llave publica de acuerdo de llaves para cifrar/descifrar data");

            //aqui se debe cifrar data. Crear sobre digital o acuerdo de llave usando la llave ública del CRS-C (CanReg) receptor
            requestJson.add("data", dataRequest);

            requestOutStr = requestJson.toString();

            // Enviar logOut
            request = HttpRequest.newBuilder()
                    .uri(new URI(URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestOutStr))
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Client received response: " + response.body());

            //20240001
        }

    }

    private static JsonObject getPatientData() {
        JsonObject patientData = new JsonObject();

        // Crear el objeto "paciente"
        JsonObject paciente = new JsonObject();
        paciente.addProperty("RegNo", "");
        paciente.addProperty("PerS", "");
        paciente.addProperty("FamN", "1 24112024 tree Ap1");
        paciente.addProperty("FamName", "1 24112024arbol Ap2");
        paciente.addProperty("FirstN", "1 24112024 name 1");
        paciente.addProperty("MidN", "1 24112024 name 2");
        paciente.addProperty("CURP", "1 24112024 curp");
        paciente.addProperty("NSS", "1 24112024 nss p");
        paciente.addProperty("Sex", "1");
        paciente.addProperty("EtnicGroup", "00");
        paciente.addProperty("BirthD", "19701121");
        paciente.addProperty("EstNac", "07");
        paciente.addProperty("ResPla", "038");
        paciente.addProperty("Calle", "street");
        paciente.addProperty("ObsoleteFlagPatientTable", "");
        paciente.addProperty("PatientRecordID", "");
        paciente.addProperty("PatientUpdatedBy", "");
        paciente.add("PatientUpdateDate", null); // null value
        paciente.addProperty("PatientRecordStatus", "");
        paciente.addProperty("PatientCheckStatus", "");
        paciente.addProperty("Num", "1111");
        paciente.addProperty("Cruz", "no cross");
        paciente.addProperty("Col", "col barrio la");
        paciente.addProperty("Loc", "0130001");
        paciente.addProperty("Mun", "013");
        paciente.addProperty("Est", "07");
        paciente.addProperty("CP", "97000");
        paciente.addProperty("DLC", "20240101");
        paciente.addProperty("Follow", "1");
        paciente.addProperty("vivo", "1");
        paciente.addProperty("Death", "3");
        paciente.addProperty("DeaDat", "20241105");

        // Crear el objeto "tumor"
        JsonObject tumor = new JsonObject();
        tumor.addProperty("RecS", "");
        tumor.addProperty("Chec", "");
        tumor.addProperty("MPcode", "");
        tumor.addProperty("MPSeq", "");
        tumor.addProperty("MPTot", "");
        tumor.add("UpDate", null); // null value
        tumor.addProperty("ObsoleteFlagTumourTable", "");
        tumor.addProperty("TumourID", "");
        tumor.addProperty("PatientIDTumourTable", "");
        tumor.addProperty("PatientRecordIDTumour", "");
        tumor.addProperty("TumourUpdatedBy", "");
        tumor.addProperty("TumourUnduplicationStatus", "");
        tumor.addProperty("InciD", "20191122");
        tumor.add("Edaddiag", null); // null value
        tumor.addProperty("Bas", "1");
        tumor.addProperty("Top", "021");
        tumor.addProperty("Mor", "8002");
        tumor.addProperty("Beh", "1");
        tumor.addProperty("histology", "1");
        tumor.addProperty("extention", "1");
        tumor.addProperty("laterality", "1");
        tumor.addProperty("Met", "1");
        tumor.addProperty("TTNM", "2");
        tumor.addProperty("Estadio", "1");
        tumor.addProperty("I10", "");
        tumor.addProperty("ICCC", "");
        tumor.addProperty("Registrador", "00");
        tumor.addProperty("DateReg", "20151119");

        // Crear el objeto "fuente"
        JsonObject fuente = new JsonObject();
        fuente.addProperty("TumourIDSourceTable", "");
        fuente.addProperty("SourceRecordID", "");
        fuente.addProperty("Fuente", "3111101");
        fuente.addProperty("NumAfilExp", "0124");
        fuente.addProperty("LabNo", "0124");
        fuente.addProperty("SouDate", "20181101");

        // Crear la lista de fuentes
        JsonArray fuentes = new JsonArray();
        fuentes.add(fuente);

        // Agregar el tumor y sus fuentes
        JsonObject tumorWithFuente = new JsonObject();
        tumorWithFuente.add("tumor", tumor);
        tumorWithFuente.add("fuentes", fuentes);

        // Crear la lista de tumores
        JsonArray tumores = new JsonArray();
        tumores.add(tumorWithFuente);

        // Agregar "paciente" y "tumores" al objeto "datos del paciente"
        patientData.add("paciente", paciente);
        patientData.add("tumores", tumores);

        return patientData;
    }

    // Método para calcular el hash (SHA-256) de un string
    private static String computeHash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
