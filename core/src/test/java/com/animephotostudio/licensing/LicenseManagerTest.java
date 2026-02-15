package com.animephotostudio.licensing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class LicenseManagerTest {
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("aps-license-test");
        System.setProperty("animephotostudio.license.path", tempDir.resolve("license.dat").toString());
        // ensure clean state
        LicenseManager.getInstance().clearLicense();
    }

    @AfterEach
    void tearDown() throws Exception {
        LicenseManager.getInstance().clearLicense();
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ignored) {}
        System.clearProperty("animephotostudio.license.path");
    }

    @Test
    void activateCreatesLicenseFileAndSetsPro() {
        LicenseManager lm = LicenseManager.getInstance();
        assertFalse(lm.isPro());
        assertFalse(lm.hasLicenseFile());

        boolean ok = lm.activate("PRO-DEMO-0001");
        assertTrue(ok);
        assertTrue(lm.isPro());
        assertTrue(lm.hasLicenseFile());
        assertEquals("PRO", lm.getLicenseType());
        assertTrue(lm.getIssuedTimestamp().isPresent());
    }

    @Test
    void invalidKeyDoesNotActivate() {
        LicenseManager lm = LicenseManager.getInstance();
        assertFalse(lm.activate("INVALID-KEY"));
        assertFalse(lm.isPro());
        assertFalse(lm.hasLicenseFile());
    }

    @Test
    void clearLicenseRemovesFileAndResetsState() {
        LicenseManager lm = LicenseManager.getInstance();
        assertTrue(lm.activate("PRO-DEMO-0001"));
        assertTrue(lm.hasLicenseFile());
        lm.clearLicense();
        assertFalse(lm.hasLicenseFile());
        assertFalse(lm.isPro());
    }

    @Test
    void reloadKeepsStateFromFile() {
        LicenseManager lm = LicenseManager.getInstance();
        assertTrue(lm.activate("PRO-DEMO-0001"));
        // force reload (reads file again)
        lm.reload();
        assertTrue(lm.isPro());
        assertEquals("PRO", lm.getLicenseType());
    }
}