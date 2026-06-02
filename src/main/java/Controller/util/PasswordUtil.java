package Controller.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {

    private PasswordUtil() {}

    // Genera el hash SHA-256 de un texto plano
    // Coincide con el algoritmo del admin inicial en el script SQL
    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    // Compara contraseña en texto plano contra el hash almacenado en BD
    public static boolean verificar(String passwordPlano, String hashAlmacenado) {
        return hash(passwordPlano).equals(hashAlmacenado);
    }
}