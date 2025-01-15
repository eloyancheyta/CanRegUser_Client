/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package zHashGenerator;

/**
 *
 * @author eloya
 */
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashGenerator {

    /**
     * Genera un hash en formato hexadecimal a partir de un string de entrada.
     * 
     * @param input El string de entrada.
     * @param algorithm El algoritmo de hash (por ejemplo, "SHA-256", "SHA-1", "MD5").
     * @return El hash en formato hexadecimal.
     * @throws NoSuchAlgorithmException Si el algoritmo especificado no es válido.
     */
    public static String generateHash(String input, String algorithm) throws NoSuchAlgorithmException {
        // Crear una instancia del MessageDigest con el algoritmo especificado
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        
        // Convertir la entrada a un arreglo de bytes y generar el digesto
        byte[] hashBytes = digest.digest(input.getBytes());
        
        // Convertir los bytes del hash a formato hexadecimal
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
        try {
            String input = "nombre del estado";
            
            input += "apix.tamps.cinvestav.mx7d52dfa8f873870b9b8e5b9705d7c738e91db9d7897be37cac85f1bb39a855c9";
            String algorithm = "SHA-256";
            
            
            String hash = generateHash(input, algorithm);
            
            System.out.println("Entrada: " + input);
            System.out.println("Hash (" + algorithm + "): " + hash);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Algoritmo no válido: " + e.getMessage());
        }
    }
}
