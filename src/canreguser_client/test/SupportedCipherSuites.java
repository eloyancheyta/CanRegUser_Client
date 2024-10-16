/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package canreguser_client.test;

/**
 *
 * @author eloya
 */
import javax.net.ssl.SSLContext;

public class SupportedCipherSuites {
    public static void main(String[] args) {
        try {
            SSLContext context = SSLContext.getDefault();
            String[] supportedCipherSuites = context.getSupportedSSLParameters().getCipherSuites();

            System.out.println("Supported Cipher Suites:");
            for (String suite : supportedCipherSuites) {
                System.out.println(suite);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

