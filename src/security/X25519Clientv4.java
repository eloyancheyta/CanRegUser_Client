/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package security;

/**
 *
 * @author eloya
 */
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.bouncycastle.jcajce.spec.XDHParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class X25519Clientv4 {

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Cargar el KeyStore que contiene el certificado autofirmado
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(Files.newInputStream(Paths.get("truststore.jks")), "password".toCharArray());

        // Crear un TrustManager que confía en el certificado del servidor
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // Crear el contexto SSL y configurarlo con el TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, tmf.getTrustManagers(), null);

        // Generar un par de llaves X25519
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("XDH", "BC");
        keyPairGenerator.initialize(new org.bouncycastle.jcajce.spec.XDHParameterSpec(XDHParameterSpec.X25519));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Leer la llave pública del servidor desde un archivo PEM
        String serverPublicKeyPem = new String(Files.readAllBytes(Paths.get("server-public-key.pem")));
        String serverPublicKeyBase64 = serverPublicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", ""); // Eliminar espacios en blanco y saltos de línea

        byte[] serverPublicKeyBytes = Base64.getDecoder().decode(serverPublicKeyBase64);
        PublicKey serverPublicKey = KeyFactory.getInstance("X25519", "BC").generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));

        // Realizar el acuerdo de llaves usando la llave privada del cliente y la llave pública del servidor
        KeyAgreement keyAgreement = KeyAgreement.getInstance("XDH", "BC");
        keyAgreement.init(keyPair.getPrivate());
        keyAgreement.doPhase(serverPublicKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();

        // Derivar la llave AES de 128 bits
        byte[] aesKey = new byte[16];
        System.arraycopy(sharedSecret, 0, aesKey, 0, 16); // Tomar los primeros 128 bits

        byte[] iv = new byte[16];
        System.arraycopy(sharedSecret, 16, iv, 0, 16); // Tomar los siguientes 128 bits

        // Crear el contenido de metadata
        JsonObject metadataRequestPayload = new JsonObject();
        metadataRequestPayload.addProperty("fuenteId", "fuente1");

        JsonObject metadataRequest = new JsonObject();
        //metadataRequest.addProperty("typeOfOperation", "login");
        metadataRequest.addProperty("hash", computeHash(metadataRequestPayload.toString()));
        metadataRequest.add("payload", metadataRequestPayload);

        // Cifrar metadata usando AES-128-CBC
        //String iv = "1234567890123456";
        String encryptedMetadataRequest = encryptAES(metadataRequest.toString(), aesKey, iv);

        // Crear la estructura completa de la petición
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("metadataDigEnv", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        requestJson.addProperty("metadata", encryptedMetadataRequest);

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

        requestJson.addProperty("dataDigEnv", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));

        //aqui se debe cifrar data. Crear sobre digital o acuerdo de llave usando la llave ública del CRS-C (CanReg) receptor
        requestJson.add("data", dataRequest);

        // Enviar la petición al servidor
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .sslContext(sslContext)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://localhost:8080/api/login"))
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

        
        // Descifrar el contenido de metadata
        String encryptedMetadataResponse = responseJson.get("metadata").getAsString();
        String decryptedMetadataResponse = decryptAES(encryptedMetadataResponse, aesKey, iv);

        System.out.println("Decrypted Metadata: " + decryptedMetadataResponse);

        // Parsear metadata descifrado
        JsonObject metadataResponseJson = JsonParser.parseString(decryptedMetadataResponse).getAsJsonObject();
        String receivedHash = metadataResponseJson.get("hash").getAsString();
        JsonObject metadataResponsePayload = metadataResponseJson.getAsJsonObject("payload");

// Calcular el hash del payload recibido
        String computedHash = computeHash(metadataResponsePayload.toString());

// Verificar que el hash recibido coincide con el hash calculado
        if (!receivedHash.equals(computedHash)) {
            System.err.println("ERROR: Hash mismatch! Data integrity check failed.");
        } else {
            System.out.println("Hash verified successfully. Data integrity check passed.");
        }
        
        
        //rear un cliente hacia request
        //poner los metadatos y datos requeridos
         
    }

    // Método para cifrar datos usando AES-128-CBC
    private static String encryptAES(String data, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    // Método para descifrar datos usando AES-128-CBC
    private static String decryptAES(String encryptedData, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    // Método para calcular el hash (SHA-256) de un string
    private static String computeHash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
