package com.animephotostudio.licensing;

import com.animephotostudio.config.AppConfig;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Simple offline license manager (MVP).
 * - Local encrypted file (AES-GCM)
 * - Integrity via GCM
 * - Prepared for adding RSA-signed server tokens later
 */
public class LicenseManager {
    private static final LicenseManager INSTANCE = new LicenseManager();
    private final Path licensePath;
    private volatile boolean pro = false;
    private volatile long issued = -1L;

    private LicenseManager() {
        String override = System.getProperty("animephotostudio.license.path");
        if (override != null && !override.isEmpty()) {
            licensePath = Paths.get(override);
        } else {
            licensePath = Paths.get(AppConfig.LICENSE_FILE);
        }
        load();
    }

    public static LicenseManager getInstance() { return INSTANCE; }

    public boolean isPro() { return pro; }

    /** Activate with a simple demo key (replace with real server flow later) */
    public synchronized boolean activate(String key) {
        try {
            if (key == null) return false;
            if (key.trim().equalsIgnoreCase("PRO-DEMO-0001")) {
                writeLicense("PRO");
                pro = true;
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void load() {
        try {
            if (!Files.exists(licensePath)) { pro = false; return; }
            byte[] data = Files.readAllBytes(licensePath);
            String b64 = new String(data, StandardCharsets.UTF_8);
            byte[] decoded = Base64.getDecoder().decode(b64);
            byte[] iv = new byte[12];
            System.arraycopy(decoded, 0, iv, 0, iv.length);
            byte[] cipherText = new byte[decoded.length - iv.length];
            System.arraycopy(decoded, iv.length, cipherText, 0, cipherText.length);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(AppConfig.LICENSE_SECRET_BASE64), "AES");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            c.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] plain = c.doFinal(cipherText);
            String content = new String(plain, StandardCharsets.UTF_8);
            // simple content format: type=PRO
            if (content.contains("type=PRO")) {
                pro = true;
            } else {
                pro = false;
            }
        } catch (Exception e) {
            pro = false; // treat as free if anything fails
        }
    }

    private void writeLicense(String type) throws Exception {
        Files.createDirectories(licensePath.getParent());
        long now = System.currentTimeMillis();
        String payload = "type=" + type + ";issued=" + now;
        byte[] plain = payload.getBytes(StandardCharsets.UTF_8);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(AppConfig.LICENSE_SECRET_BASE64), "AES");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        c.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] cipherText = c.doFinal(plain);
        byte[] out = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
        String b64 = Base64.getEncoder().encodeToString(out);
        Files.write(licensePath, b64.getBytes(StandardCharsets.UTF_8));
        // update runtime state
        issued = now;
        pro = "PRO".equalsIgnoreCase(type);
    }

    public String getLicenseType() { return pro ? "PRO" : "FREE"; }

    public java.util.OptionalLong getIssuedTimestamp() { return issued > 0 ? java.util.OptionalLong.of(issued) : java.util.OptionalLong.empty(); }

    public java.nio.file.Path getLicensePath() { return licensePath; }

    public boolean hasLicenseFile() { return Files.exists(licensePath); }

    public synchronized void clearLicense() {
        try { Files.deleteIfExists(licensePath); } catch (Exception ignored) {}
        pro = false;
        issued = -1L;
    }

    public synchronized void reload() { load(); }

    // placeholder for future RSA-signed token verification
    public boolean verifySignedLicense(byte[] signedToken) {
        // TODO: implement RSA signature verification and parse token
        return false;
    }
}