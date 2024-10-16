
package security;

/**
 *
 * @author eloya
 */
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.Base64;
import java.security.Security;
import org.bouncycastle.jcajce.spec.XDHParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class X25519Clientv3 {

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

        // Crear el contenido de metadata
        JsonObject metadataPayload = new JsonObject();
        metadataPayload.addProperty("fuenteId", "fuenteId1");

        JsonObject metadata = new JsonObject();
        metadata.addProperty("typeOfOperation", "login");
        metadata.addProperty("hash", computeHash(metadataPayload.toString()));
        metadata.add("payload", metadataPayload);

        // Cifrar metadata usando AES-128-CBC
        String iv = "1234567890123456";
        String encryptedMetadata = encryptAES(metadata.toString(), aesKey, iv);

        // Crear la estructura completa de la petición
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("metadataDigEnv", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        requestJson.addProperty("metadata", encryptedMetadata);

        JsonObject dataPayload = new JsonObject();
        dataPayload.addProperty("username", "nombre_de_usuario");
        dataPayload.addProperty("password", "contraseña");

        JsonObject data = new JsonObject();
        data.addProperty("typeOfOperation", "login");
        data.addProperty("hash", computeHash(dataPayload.toString()));
        data.add("payload", dataPayload);

        requestJson.addProperty("dataDigEnv", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        requestJson.add("data", data);

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
    }

    // Método para cifrar datos usando AES-128-CBC
    private static String encryptAES(String data, byte[] key, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    // Método para calcular el hash (SHA-256) de un string
    private static String computeHash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
