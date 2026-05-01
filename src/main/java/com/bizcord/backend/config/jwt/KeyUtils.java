package com.bizcord.backend.config.jwt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class KeyUtils {

    private KeyUtils() {}

    public static PrivateKey loadPrivateKey(Path pemPath) throws GeneralSecurityException, IOException {
        byte[] decoded = Base64.getDecoder().decode(readPemBody(pemPath, "PRIVATE KEY"));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    public static PublicKey loadPublicKey(Path pemPath) throws GeneralSecurityException, IOException {
        byte[] decoded = Base64.getDecoder().decode(readPemBody(pemPath, "PUBLIC KEY"));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private static String readPemBody(Path pemPath, String label) throws IOException {
        if (!Files.isReadable(pemPath)) {
            throw new IllegalStateException("Key file not readable: " + pemPath.toAbsolutePath()
                    + " — generate with: openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out private_key.pem");
        }
        return Files.readString(pemPath)
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s+", "");
    }
}
