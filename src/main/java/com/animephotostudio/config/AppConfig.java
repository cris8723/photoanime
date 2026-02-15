package com.animephotostudio.config;

public class AppConfig {
    public static final String APP_NAME = "AnimePhoto Studio";
    public static final String APP_VERSION = "1.0.0";
    // NOTE: replace this key in production. Kept here for MVP/demo only.
    // 16-byte secret (base64 "1234567890abcdef") — used for AES-GCM local license file.
    public static final String LICENSE_SECRET_BASE64 = "MTIzNDU2Nzg5MGFiY2RlZg==";
    public static final String LICENSE_FOLDER = System.getProperty("user.home") + java.io.File.separator + ".animephotostudio";
    public static final String LICENSE_FILE = LICENSE_FOLDER + java.io.File.separator + "license.dat";
}