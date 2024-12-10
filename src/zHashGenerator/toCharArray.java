/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package zHashGenerator;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

/**
 *
 * @author eloya
 */
public class toCharArray {

    public static void main(String[] args) {

        String p1;
        String p2;
        char[] p1C;
        char[] p2C;
                String rP1 = "";
        String rP2 = "";
        String recP1 = "";
        String recP2 = "";

        p1 = "password";
        p2 = "";

        p1C = p1.toCharArray();
        p2C = readPassword();

        rP1 = new String(p1C);
        p2 = new String(p2C);

         for (int i = 0; i < p1C.length; i++) {
            recP1 += p1C[i];
        }
        

        for (int i = 0; i < p2C.length; i++) {
            recP2 += p2C[i];
        }

        System.out.println("\n\nstring "
                + "\np1: " + p1
                + "\np2: " + p2
                + "\n\n toString "
                + "\nrP1: " + rP1
                + "\nrP2: " + p2
                + "\n\n reconstrucción en bucle "
                + "\nrecP1: " + recP1
                + "\nrecP2: " + recP2
        );
    }

    private static char[] readPassword() {

        // Crear un cuadro de diálogo para ingresar la contraseña
        JPasswordField passwordField = new JPasswordField();
        Object[] message = {
            "Por favor, ingresa la contraseña del almacén de llaves:",       passwordField  };

        int option = JOptionPane.showConfirmDialog(null, message, "Entrada de contraseña", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            // Obtener la contraseña ingresada como un arreglo de caracteres
            return passwordField.getPassword();

            // Limpiar el arreglo para mayor seguridad
            //java.util.Arrays.fill(password, '\0'); // Sobrescribir con ceros
        } else {
            System.out.println("Operación cancelada por el usuario.");
        }
        return null;
    }

}
