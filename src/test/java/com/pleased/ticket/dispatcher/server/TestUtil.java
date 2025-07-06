package com.pleased.ticket.dispatcher.server;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pleased.ticket.dispatcher.server.config.SecurityConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class TestUtil {

    public static String generateValidJwt() throws JOSEException {
        // Create the JWT claims
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .claim("scope", "read write")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 3600 * 1000 * 5)) // 5 hours
                .build();

        // Create HMAC signer
        JWSSigner signer = new MACSigner(SecurityConfig.JWT_SECRET.getBytes());

        // Prepare JWS object
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims
        );

        // Compute the HMAC signature
        signedJWT.sign(signer);

        // Serialize to compact form
        return signedJWT.serialize();
    }

    public static String generateInvalidJwt() {
        String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());

        long now = System.currentTimeMillis() / 1000;
        String payload = base64UrlEncode(
                ("{\"sub\":\"test-user\",\"scope\":\"read write\",\"iat\":" + now +
                        ",\"exp\":" + (now + 3600) + "}").getBytes());

        String content = header + "." + payload;
        byte[] signature = hmacSha256(content.getBytes(), "wrong-secret".getBytes());
        String encodedSignature = base64UrlEncode(signature);

        return header + "." + payload + "." + encodedSignature;
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] hmacSha256(byte[] data, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HMAC-SHA256 signature", e);
        }
    }
}
