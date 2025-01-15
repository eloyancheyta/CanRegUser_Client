
package zHashGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
//import org.json.JSONObject;


/**
 *
 * @author eloya
 */
public class HashGeneratorJSON {

    /**
     * Genera un hash en formato hexadecimal a partir de un string de entrada.
     * 
     * @param input El string de entrada.
     * @param algorithm El algoritmo de hash (por ejemplo, "SHA-256", "SHA-1", "MD5").
     * @return El hash en formato hexadecimal.
     * @throws NoSuchAlgorithmException Si el algoritmo especificado no es válido.
     */
    public static String generateHash(String input, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hashBytes = digest.digest(input.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static void main(String[] args) {
        // Lista de estados de México
        String[] estados = {
            "Aguascalientes", "Baja California", "Baja California Sur", "Campeche",
            "Chiapas", "Chihuahua", "Ciudad de México", "Coahuila", "Colima", "Durango",
            "Guanajuato", "Guerrero", "Hidalgo", "Jalisco", "México", "Michoacán", "Morelos",
            "Nayarit", "Nuevo León", "Oaxaca", "Puebla", "Querétaro", "Quintana Roo",
            "San Luis Potosí", "Sinaloa", "Sonora", "Tabasco", "Tamaulipas", "Tlaxcala",
            "Veracruz", "Yucatán", "Zacatecas"
        };

        // Algoritmo de hash
        String algorithm = "SHA-256";

        // Prefijo para cada estado
        String prefix = "apix.tamps.cinvestav.mx";

        // Mapa para almacenar los resultados
        Map<String, String> hashMap = new HashMap<>();

        try {
            // Generar el hash para cada estado y almacenarlo en el mapa
            for (String estado : estados) {
                String input = estado + prefix;
                String hash = generateHash(input, algorithm);
                hashMap.put(estado, hash);
            }

            // Convertir el mapa a un objeto JSON
            //JSONObject jsonObject = new JSONObject(hashMap);

            // Escribir el objeto JSON a un archivo
            try (FileWriter file = new FileWriter("estados_hash.json")) {
       //         file.write(jsonObject.toString(4)); // Indentación de 4 espacios para legibilidad
                System.out.println("Archivo JSON generado con éxito: estados_hash.json");
            }

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Algoritmo no válido: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo JSON: " + e.getMessage());
        }
    }
}
