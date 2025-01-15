
package zHashGenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author eloya
 */
public class HashGeneratorGson {

    /**
     * Genera un hash en formato hexadecimal a partir de un string de entrada.
     *
     * @param input     El string de entrada.
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
            "Guanajuato", "Guerrero", "Hidalgo", "Jalisco", "Estado de México", "Michoacán", "Morelos",
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

            // Crear un objeto Gson con formato bonito
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            // Convertir el mapa a JSON
            String json = gson.toJson(hashMap);

            // Escribir el JSON en un archivo
            try (FileWriter file = new FileWriter("estados_CanRegID.json")) {
                file.write(json);
                System.out.println("Archivo JSON generado con éxito: estados_CanRegID.json");
            }

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Algoritmo no válido: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo JSON: " + e.getMessage());
        }
    }
}

