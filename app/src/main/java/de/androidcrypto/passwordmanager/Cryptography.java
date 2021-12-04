package de.androidcrypto.passwordmanager;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Base64;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Cryptography {

    // runs all cryptographic tasks

    // stores data in an encrypted file
    private static String mainKeyAlias; // for the masterKey
    private static String encryptedPreferencesFilename = "encryptedpreferences.dat";
    private static KeyGenParameterSpec keyGenParameterSpec;
    private static byte[] masterKey = new byte[0];

    // constants for database file import
    private static String fileImportHeaderLine = "### PWMANAGER IMPORT START V1 ###";
    private static String fileImportFooterLine = "### PWMANAGER IMPORT END V1 ###";
    // constants for masterkey import and export
    private static String masterkeyImportHeaderLine = "### PWMANAGER MASTERKEY START V1 ###";
    private static String masterkeyImportFooterLine = "### PWMANAGER MASTERKEY END V1 ###";

    private static String fileImportFieldDelimiter = "##";
    private static int fileImportIterations = 15000;

    int minimumPassphraseLength = 4; // todo check for password length

    public static boolean cryptographicInit(Context applicationContext) {
        // Although you can define your own key generation parameter specification, it's
        // recommended that you use the value specified here.
        keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
        try {
            mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return false;
        }
        // check for first start, then generate a fresh masterkey
        // for updating we have to delete the file, function does not overwrite the data
        File encryptedFilename = new File(applicationContext.getFilesDir(), encryptedPreferencesFilename);
        // create and store a new key only if file does not not exist
        if (!encryptedFilename.exists()) {
            EncryptedFile encryptedFile = null;
            try {
                encryptedFile = new EncryptedFile.Builder(
                        // store in internal app storage
                        new File(applicationContext.getFilesDir(), encryptedPreferencesFilename),
                        applicationContext,
                        mainKeyAlias,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build();
                masterKey = generateMasterKey32Bytes();
                OutputStream outputStream = null;
                outputStream = encryptedFile.openFileOutput();
                outputStream.write(masterKey);
                outputStream.flush();
                outputStream.close();
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            // load the existing masterKey
            EncryptedFile encryptedFile = null;
            ByteArrayOutputStream byteArrayOutputStream = null;
            try {
                encryptedFile = new EncryptedFile.Builder(
                        // store from internal app storage
                        new File(applicationContext.getFilesDir(), encryptedPreferencesFilename),
                        applicationContext,
                        mainKeyAlias,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build();
                InputStream inputStream = encryptedFile.openFileInput();
                byteArrayOutputStream = new ByteArrayOutputStream();
                int nextByte = 0;
                nextByte = inputStream.read();
                while (nextByte != -1) {
                    byteArrayOutputStream.write(nextByte);
                    nextByte = inputStream.read();
                }
                masterKey = byteArrayOutputStream.toByteArray();
            } catch (IOException | GeneralSecurityException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    public static byte[] generateMasterKey32Bytes() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        return key;
    }

    public static String encryptStringAesGcmToBase64 (String data) {
        if (masterKey.length < 32) return "";
        // generate random 12 byte gcm nonce
        SecureRandom secureRandom = new SecureRandom();
        byte[] nonce = new byte[12];
        secureRandom.nextBytes(nonce);
        SecretKeySpec secretKeySpec = new SecretKeySpec(masterKey, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
        Cipher cipher = null;
        byte[] ciphertext = new byte[0];
        try {
            cipher = Cipher.getInstance("AES/GCM/NOPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
            ciphertext = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            e.printStackTrace();
            return "";
        }
        return base64Encoding(nonce) + ":" + base64Encoding(ciphertext);
    }

    public static String decryptStringAesGcmFromBase64(String ciphertextBase64) {
        if (masterKey.length < 32) return "";
        if (!ciphertextBase64.contains(":")) return "";
        String[] parts = ciphertextBase64.split(":", 0);
        byte[] nonce = base64Decoding(parts[0]);
        byte[] ciphertextWithTag = base64Decoding(parts[1]);
        if ((nonce.length != 12) | (ciphertextWithTag.length < 17)) return "";
        SecretKeySpec secretKeySpec = new SecretKeySpec(masterKey, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NOPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            byte[] decryptedtext =  cipher.doFinal(ciphertextWithTag);
            return new String(decryptedtext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String decryptFileAesGcmFromBase64(char[] passphrase, String fileContent) {
        if (masterKey.length < 32) return "";
        // sanity check on header and footer of fileContent
        if (!fileContent.contains(fileImportHeaderLine) |
            !fileContent.contains(fileImportFooterLine)) {
            return"";
        }
        // get data
        String ciphertextBase64 = fileContent.replace(fileImportHeaderLine, "")
                .replace(fileImportFooterLine, "")
                .replaceAll("[\\r\\n]+", "");
        if (!ciphertextBase64.contains(":")) return "";
        String[] parts = ciphertextBase64.split(":", 0);
        byte[] salt = base64Decoding(parts[0]);
        byte[] nonce = base64Decoding(parts[1]);
        byte[] ciphertext = base64Decoding(parts[2]);
        byte[] gcmTag = base64Decoding(parts[3]);
        if ((salt.length != 32) | (nonce.length != 12) | (ciphertext.length < 1) | (gcmTag.length != 16)) return "";
        byte[] ciphertextWithTag = concatenateByteArrays(ciphertext, gcmTag);
        // key derivation
        SecretKeyFactory secretKeyFactory = null;
        byte[] key;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpec = new PBEKeySpec(passphrase, salt, fileImportIterations, 32 * 8);
            key = secretKeyFactory.generateSecret(keySpec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return "";
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NOPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            byte[] decryptedtext = cipher.doFinal(ciphertextWithTag);
            return new String(decryptedtext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String encryptMasterkeyAesGcmToBase64(char[] passphrase) {
        if (masterKey.length < 32) return "";
        // key derivation
        SecretKeyFactory secretKeyFactory = null;
        byte[] key;
        byte[] salt = generateSalt32Byte();
        byte[] nonce = generateRandomNonce();
        try {
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpec = new PBEKeySpec(passphrase, salt, fileImportIterations, 32 * 8);
            key = secretKeyFactory.generateSecret(keySpec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return "";
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NOPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
            byte[] ciphertextWithTag = cipher.doFinal(masterKey);
            byte[] ciphertext = new byte[(ciphertextWithTag.length-16)];
            byte[] gcmTag = new byte[16];
            System.arraycopy(ciphertextWithTag, 0, ciphertext, 0, (ciphertextWithTag.length - 16));
            System.arraycopy(ciphertextWithTag, (ciphertextWithTag.length-16), gcmTag, 0, 16);
            String saltBase64 = base64Encoding(salt);
            String nonceBase64 = base64Encoding(nonce);
            String ciphertextBase64 = base64Encoding(ciphertext);
            String gcmTagBase64 = base64Encoding(gcmTag);
            return masterkeyImportHeaderLine + "\n"
            + saltBase64 + ":" + nonceBase64 + ":" + ciphertextBase64 + ":" + gcmTagBase64
                    + "\n" + masterkeyImportFooterLine;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static byte[] generateSalt32Byte() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        return salt;
    }

    private static byte[] generateRandomNonce() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] nonce = new byte[12];
        secureRandom.nextBytes(nonce);
        return nonce;
    }


    public static byte[] concatenateByteArrays(byte[] a, byte[] b) {
        return ByteBuffer
                .allocate(a.length + b.length)
                .put(a).put(b)
                .array();
    }

    private static String base64Encoding(byte[] input) {
        return Base64.encodeToString(input, Base64.NO_WRAP);
    }

    private static byte[] base64Decoding(String input) {
        return Base64.decode(input, Base64.NO_WRAP);
    }
}
