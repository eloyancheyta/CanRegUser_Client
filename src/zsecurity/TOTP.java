package zsecurity;

//import canreguser_client.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author eloy_
 */
public class TOTP {

    private int ventanaTemporal;
    private int digitosCodigo;
    private int timeW;

    public TOTP(int ventanaTemporal, int digitosCodigo) {
        this.ventanaTemporal = ventanaTemporal;
        this.digitosCodigo = digitosCodigo;
    }

    public TOTP(int ventanaTemporal, int digitosCodigo, int timeW) {
        this.ventanaTemporal = ventanaTemporal;
        this.digitosCodigo = digitosCodigo;
        this.timeW = timeW;
    }


    public String generarCodigo(byte[] secreto) {
        long tiempoActual = System.currentTimeMillis() / 1000L / getVentanaTemporal();
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secreto, "HmacSHA1"));
            byte[] hmac = mac.doFinal(String.valueOf(tiempoActual).getBytes());

            int offset = hmac[hmac.length - 1] & 0xF;
            int binCode = ((hmac[offset] & 0x7F) << 24)
                    | ((hmac[offset + 1] & 0xFF) << 16)
                    | ((hmac[offset + 2] & 0xFF) << 8)
                    | (hmac[offset + 3] & 0xFF);

            int codigo = binCode % (int) Math.pow(10, getDigitosCodigo());
            return String.format("%0" + getDigitosCodigo() + "d", codigo);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(TOTP.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public boolean validarCodigo(byte[] secreto, String codigo) {
        long tiempoActual = System.currentTimeMillis() / 1000L / getVentanaTemporal();
        //el for lo hice para ampliar la ventana temporal, pero creo que eso ya estaba asegurado en el algoritomo
        /*for (int i = -ventanaTemporal; i <= ventanaTemporal; i++) {
            String codigoGenerado = generarCodigo(secreto, tiempoActual + i);
            String codigoGenerado = generarCodigo(secreto, tiempoActual);
            if (codigoGenerado != null && codigo.equals(codigoGenerado)) {
                return true;
            }*/
        String codigoGenerado = generarCodigo(secreto, tiempoActual);
        if (codigoGenerado != null && codigo.equals(codigoGenerado)) {
            return true;
        }
        return false;
    }

    private String generarCodigo(byte[] secreto, long tiempo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secreto, "HmacSHA1"));
            byte[] hmac = mac.doFinal(String.valueOf(tiempo).getBytes());

            int offset = hmac[hmac.length - 1] & 0xF;
            int binCode = ((hmac[offset] & 0x7F) << 24)
                    | ((hmac[offset + 1] & 0xFF) << 16)
                    | ((hmac[offset + 2] & 0xFF) << 8)
                    | (hmac[offset + 3] & 0xFF);

            int codigo = binCode % (int) Math.pow(10, getDigitosCodigo());
            return String.format("%0" + getDigitosCodigo() + "d", codigo);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(TOTP.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * @return the ventanaTemporal
     */
    public int getVentanaTemporal() {
        return ventanaTemporal;
    }

    /**
     * @param ventanaTemporal the ventanaTemporal to set
     */
    public void setVentanaTemporal(int ventanaTemporal) {
        this.ventanaTemporal = ventanaTemporal;
    }

    /**
     * @return the digitosCodigo
     */
    public int getDigitosCodigo() {
        return digitosCodigo;
    }

    /**
     * @param digitosCodigo the digitosCodigo to set
     */
    public void setDigitosCodigo(int digitosCodigo) {
        this.digitosCodigo = digitosCodigo;
    }

    /**
     * @return the timeW
     */
    public int getTimeW() {
        return timeW;
    }

    /**
     * @param timeW the timeW to set
     */
    public void setTimeW(int timeW) {
        this.timeW = timeW;
    }

}
