// src/main/java/org/example/EncryptionService.java
package com.application.Backend;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory; // Needed for PBKDF2
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;      // Needed for PBKDF2
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;         // Needed for PBKDF2
import java.util.Base64;

public class EncryptionService {

    // Constants for Symmetric Encryption and KDF
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String SYMMETRIC_ALGORITHM = "AES";
    private static final String SYMMETRIC_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;    // bytes
    private static final int GCM_TAG_LENGTH = 128;   // bits
    private static final int AES_KEY_LENGTH = 256;   // bits (deriving a 32-byte key)
    private static final int PBKDF2_ITERATIONS = 65536; // Standard iteration count
    private static final int SALT_LENGTH = 16;       // bytes

    private SecretKey roomSecretKey; // The single key for the room derived from password

    // Derive the room key from password and room name (used as salt context)
    // Returns true on success, false on failure
    public boolean deriveRoomKey(String roomName, String password) {
        try {
            // **Salt Generation Strategy:**
            // Ideally, salt should be unique per password *but* shareable.
            // Simple strategy for now: Use roomName bytes directly as salt material.
            // Weakness: Same room name -> same salt. A better approach would store/derive
            // a unique salt per room if persistence was involved.
            // Let's use first SALT_LENGTH bytes of UTF-8 encoded room name. Pad if needed.
            byte[] salt = new byte[SALT_LENGTH];
            byte[] roomNameBytes = roomName.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(roomNameBytes, 0, salt, 0, Math.min(roomNameBytes.length, SALT_LENGTH));
            System.out.println("[Crypto] Using salt derived from room name for PBKDF2.");

            // Create PBKDF2 key spec
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH);

            // Get instance of SecretKeyFactory for PBKDF2
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);

            // Generate the secret key (raw bytes)
            byte[] secretBytes = factory.generateSecret(spec).getEncoded();

            // Create the AES SecretKey object
            this.roomSecretKey = new SecretKeySpec(secretBytes, SYMMETRIC_ALGORITHM);
            System.out.println("[Crypto] Room key derived successfully using PBKDF2.");
            return true;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.err.println("[Crypto] CRITICAL ERROR: Failed to derive room key: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Encrypt using the single room key
    public String encrypt(String plaintext) throws Exception {
        if (this.roomSecretKey == null) throw new IllegalStateException("Room key not derived/initialized.");

        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = SecureRandom.getInstanceStrong();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(SYMMETRIC_TRANSFORMATION);
        GCMParameterSpec gcmParamSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, this.roomSecretKey, gcmParamSpec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] ivAndCiphertext = new byte[GCM_IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, ivAndCiphertext, 0, GCM_IV_LENGTH);
        System.arraycopy(ciphertext, 0, ivAndCiphertext, GCM_IV_LENGTH, ciphertext.length);

        return Base64.getEncoder().encodeToString(ivAndCiphertext);
    }

    // Decrypt using the single room key
    public String decrypt(String base64EncodedData) throws Exception {
        if (this.roomSecretKey == null) throw new IllegalStateException("Room key not derived/initialized.");

        byte[] ivAndCiphertext = Base64.getDecoder().decode(base64EncodedData);
        if (ivAndCiphertext.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data: too short.");
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(ivAndCiphertext, 0, iv, 0, GCM_IV_LENGTH);
        byte[] ciphertext = new byte[ivAndCiphertext.length - GCM_IV_LENGTH];
        System.arraycopy(ivAndCiphertext, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(SYMMETRIC_TRANSFORMATION);
        GCMParameterSpec gcmParamSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, this.roomSecretKey, gcmParamSpec);

        byte[] decryptedBytes = cipher.doFinal(ciphertext);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}