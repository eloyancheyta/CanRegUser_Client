/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ztest;

/**
 *
 * @author eloya
 */
public class test {

    public static void main(String[] args) {
        String str = "123"; // String que deseas convertir
        
        int s = 123;
        
        String SS = "123";
        try {
            
           // Integer intObj = (Integer) s;
           
           
            
            Integer intObj = Integer.valueOf(SS.toString());
            // Integer intObj = Integer.valueOf(s);
            
         //   Integer intObj = Integer.valueOf(str); // Devuelve un objeto Integer
            System.out.println("El número es: " + intObj);
            
            
            String cadena;
            
            cadena = intObj.toString();
            
             System.out.println("La cadena del número es: " + cadena);
            
        } catch (NumberFormatException e) {
            System.out.println("El string no es un número válido.");
        }

    }
    
  

}
