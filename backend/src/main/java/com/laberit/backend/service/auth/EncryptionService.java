package com.laberit.backend.service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de la seguridad de las credenciales de proveedores.
 * Utiliza el algoritmo simétrico AES para asegurar que las API Keys
 * no se almacenen en texto plano en la base de datos SQL Server.
 */
@Service
public class EncryptionService {

    // La clave maestra debe tener 16, 24 o 32 caracteres para AES
    @Value("${app.security.secret-key}")
    private String encryptionSecret;

    private static final String ALGORITHM = "AES";

    /**
     * Genera la especificación de la llave a partir del secreto de configuración.
     * Se usa StandardCharsets.UTF_8 para evitar problemas de encoding entre SO.
     */
    private SecretKeySpec getKeyFromSecret() {
        byte[] keyBytes = encryptionSecret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Encripta una cadena (plaintext) y la devuelve en formato Base64.
     * El formato Base64 es necesario para guardar los bytes resultantes
     * como un String legible en la base de datos.
     */
    public String encrypt(String plaintext) {
        try {
            // Cipher es el motor criptográfico de Java
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKeyFromSecret());

            // Transformamos el texto en bytes, encriptamos y convertimos a Base64
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            // RuntimeException para no ensuciar la firma del método con throws
            throw new RuntimeException("Error crítico en el proceso de encriptación", e);
        }
    }

    /**
     * Toma una cadena en Base64, la revierte a bytes y la desencripta.
     * Retorna el texto original (API Key) para ser usado por el ApiClientService.
     */
    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKeyFromSecret());

            // Proceso inverso: Base64 -> Bytes encriptados -> Decrypt -> String
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al desencriptar: Verifique la integridad del secreto maestro", e);
        }
    }
}

