/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package security;

/**
 *
 * @author eloy
 */
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.KeyAgreement;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Security;
import org.bouncycastle.jcajce.spec.XDHParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class X25519Clientv2 {

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

        // Convertir la llave pública del cliente a Base64
        String clientPublicKeyBase64 = java.util.Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // Crear el cuerpo de la solicitud con la llave pública del cliente
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("clientPublicKey", clientPublicKeyBase64);

        // Enviar la llave pública del cliente al servidor
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

        // Leer la llave pública del servidor desde un archivo PEM (ya conocido por el cliente)
        String serverPublicKeyPem = new String(Files.readAllBytes(Paths.get("server-public-key.pem")));
        String serverPublicKeyBase64 = serverPublicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", ""); // Eliminar espacios en blanco y saltos de línea

        byte[] serverPublicKeyBytes = java.util.Base64.getDecoder().decode(serverPublicKeyBase64);
        PublicKey serverPublicKey = KeyFactory.getInstance("X25519", "BC").generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));

        // Realizar el acuerdo de llaves usando la llave privada del cliente y la llave pública del servidor
        KeyAgreement keyAgreement = KeyAgreement.getInstance("XDH", "BC");
        keyAgreement.init(keyPair.getPrivate());
        keyAgreement.doPhase(serverPublicKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();

        // Convertir el sharedSecret a una cadena hexadecimal
        String sharedSecretHex = bytesToHex(sharedSecret);
        System.out.println("Shared Secret (Client): " + sharedSecretHex);
    }

    // Método para convertir un arreglo de bytes a una cadena hexadecimal
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
