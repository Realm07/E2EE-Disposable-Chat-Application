// src/main/java/org/example/EncryptionService.java
package com.application.Backend;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory; // Needed for PBKDF2
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;      // Needed for PBKDF2
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;         // Needed for PBKDF2
import java.util.Base64;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey; // Already present
import javax.crypto.spec.SecretKeySpec;

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
    public boolean isRoomKeySet() {
        return this.roomSecretKey != null;
    }
    // Derive the room key from password and room name (used as salt context)
    // Returns true on success, false on failure
    public boolean deriveRoomKey(String roomName, String password) {
        try {
            // **Salt Generation Strategy: **
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
    /**
     * Encrypts data bytes using the current roomSecretKey.
     * Used for encrypting the one-time file key.
     * @param data The plaintext bytes.
     * @return Base64 encoded encrypted string.
     * @throws Exception If room key is not set or encryption fails.
     */
    public String encryptDataWithRoomKey(byte[] data) throws Exception {
        if (!isRoomKeySet()) {
            throw new IllegalStateException("Room key not derived/initialized. Cannot encrypt data.");
        }
        // Using the same AES/GCM parameters as message encryption
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = SecureRandom.getInstanceStrong(); // Or just new SecureRandom()
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(SYMMETRIC_TRANSFORMATION);
        GCMParameterSpec gcmParamSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, this.roomSecretKey, gcmParamSpec);

        byte[] ciphertext = cipher.doFinal(data);

        byte[] ivAndCiphertext = new byte[GCM_IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, ivAndCiphertext, 0, GCM_IV_LENGTH);
        System.arraycopy(ciphertext, 0, ivAndCiphertext, GCM_IV_LENGTH, ciphertext.length);

        return Base64.getEncoder().encodeToString(ivAndCiphertext);
    }

    /**
     * Decrypts Base64 encoded data using the current roomSecretKey.
     * Used for decrypting the one-time file key.
     * @param base64EncodedData The Base64 encoded encrypted string.
     * @return The decrypted plaintext bytes.
     * @throws Exception If room key is not set or decryption fails.
     */
    public byte[] decryptDataWithRoomKey(String base64EncodedData) throws Exception {
        if (!isRoomKeySet()) {
            throw new IllegalStateException("Room key not derived/initialized. Cannot decrypt data.");
        }
        byte[] ivAndCiphertext = Base64.getDecoder().decode(base64EncodedData);
        if (ivAndCiphertext.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data for key decryption: too short.");
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(ivAndCiphertext, 0, iv, 0, GCM_IV_LENGTH);
        byte[] ciphertext = new byte[ivAndCiphertext.length - GCM_IV_LENGTH];
        System.arraycopy(ivAndCiphertext, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(SYMMETRIC_TRANSFORMATION);
        GCMParameterSpec gcmParamSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, this.roomSecretKey, gcmParamSpec);

        return cipher.doFinal(ciphertext);
    }


    /**
     * Encrypts a file stream using a provided symmetric key (e.g., a one-time key).
     * Writes the encrypted output to another file.
     *
     * @param inputFile The original file to encrypt.
     * @param outputFile The file to write encrypted content to.
     * @param keyBytes The raw bytes of the symmetric key to use for encryption.
     * @throws Exception On any error during file I/O or encryption.
     */
    public void encryptFileWithGivenKey(File inputFile, File outputFile, byte[] keyBytes) throws Exception {
        SecretKey secretKey = new SecretKeySpec(keyBytes, SYMMETRIC_ALGORITHM); // "AES"

        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = SecureRandom.getInstanceStrong();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(SYMMETRIC_TRANSFORMATION); // "AES/GCM/NoPadding"
        GCMParameterSpec gcmParamSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParamSpec);

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            // Write IV to the beginning of the output file
            fos.write(iv);

            // Use CipherOutputStream to encrypt and write
            try (CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }
            } // CipherOutputStream will call doFinal on close
        }
        System.out.println("[EncryptionService] File encrypted successfully with one-time key: " + outputFile.getName());
    }

    /**
     * Decrypts a file stream using a provided symmetric key (e.g., a one-time key).
     * Writes the decrypted output to another file.
     * Assumes the IV is prepended to the encrypted file.
     *
     * @param inputFile The encrypted file.
     * @param outputFile The file to write decrypted content to.
     * @param keyBytes The raw bytes of the symmetric key used for decryption.
     * @throws Exception On any error during file I/O or decryption.
     */
    public void decryptFileWithGivenKey(File inputFile, File outputFile, byte[] keyBytes) throws Exception {
        SecretKey secretKey = new SecretKeySpec(keyBytes, SYMMETRIC_ALGORITHM);

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            // Read IV from the beginning of the input file
            byte[] iv = new byte[GCM_IV_LENGTH];
            int ivBytesRead = fis.read(iv);
            if (ivBytesRead < GCM_IV_LENGTH) {
                throw new IOException("Encrypted file is too short to contain IV.");
            }

            Cipher cipher = Cipher.getInstance(SYMMETRIC_TRANSFORMATION);
            GCMParameterSpec gcmParamSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParamSpec);

            // Use CipherInputStream to decrypt and read
            try (CipherInputStream cis = new CipherInputStream(fis, cipher)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
        }
        System.out.println("[EncryptionService] File decrypted successfully with one-time key: " + outputFile.getName());
    }
}