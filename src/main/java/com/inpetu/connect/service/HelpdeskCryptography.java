package com.inpetu.connect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class HelpdeskCryptography {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final byte[] TOKEN_DOMAIN = "inpetu:helpdesk:ticket-token:v1\0".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DUMMY_TOKEN_HASH = tokenDigestStatic(
        "helpdesk:nonexistent-ticket-token:v1".getBytes(StandardCharsets.UTF_8));
    private static final Pattern ENCODED_TOKEN = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final byte[] secret;
    private final byte[] rateLimitKey;
    private final boolean configured;

    public HelpdeskCryptography(@Value("${APP_RATE_LIMIT_SECRET:}") String secret) {
        this.secret = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        this.configured = this.secret.length >= 32;
        this.rateLimitKey = configured
            ? hmacStatic(this.secret, "inpetu:helpdesk:rate-limit-key:v1\0".getBytes(StandardCharsets.UTF_8))
            : new byte[0];
    }

    public String hmacHex(String domain, String value) {
        return HexFormat.of().formatHex(hmac(domain, value));
    }

    public byte[] hmac(String domain, String value) {
        if (!configured) {
            throw new HelpdeskException(HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMIT_UNAVAILABLE",
                "O limite de solicitações está temporariamente indisponível.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            byte[] key = domain.startsWith("inpetu:helpdesk:rate-limit:")
                ? rateLimitKey : secret;
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            mac.update(domain.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao proteger um identificador do Help Desk.", exception);
        }
    }

    public byte[] tokenHash(String token) {
        byte[] raw = (token == null ? "" : token).getBytes(StandardCharsets.UTF_8);
        byte[] tokenBytes = decodeToken(raw);
        return tokenDigest(tokenBytes);
    }

    public boolean matchesToken(String receivedToken, byte[] storedHash) {
        byte[] calculatedHash = tokenHash(receivedToken);
        byte[] expectedHash = storedHash == null ? DUMMY_TOKEN_HASH : storedHash;
        return MessageDigest.isEqual(calculatedHash, expectedHash) && storedHash != null;
    }

    public byte[] uuidBytes(UUID uuid) {
        return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits())
            .putLong(uuid.getLeastSignificantBits()).array();
    }

    public byte[] sha256(byte[] input) {
        return sha256Static(input);
    }

    public byte[] newTokenBytes() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    public String encodeToken(byte[] tokenBytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public byte[] tokenHashBytes(byte[] tokenBytes) {
        return tokenDigest(tokenBytes);
    }

    public String newPublicCode() {
        byte[] bytes = new byte[17];
        RANDOM.nextBytes(bytes);
        StringBuilder encoded = new StringBuilder(26);
        int buffer = 0;
        int bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5 && encoded.length() < 26) {
                bits -= 5;
                encoded.append(CODE_ALPHABET[(buffer >>> bits) & 31]);
            }
        }
        if (encoded.length() < 26) {
            encoded.append(CODE_ALPHABET[(buffer << (5 - bits)) & 31]);
        }
        return "HD-" + encoded.substring(0, 26);
    }

    private static byte[] sha256Static(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao calcular SHA-256.", exception);
        }
    }

    private byte[] tokenDigest(byte[] tokenBytes) {
        byte[] value = new byte[TOKEN_DOMAIN.length + tokenBytes.length];
        System.arraycopy(TOKEN_DOMAIN, 0, value, 0, TOKEN_DOMAIN.length);
        System.arraycopy(tokenBytes, 0, value, TOKEN_DOMAIN.length, tokenBytes.length);
        return sha256(value);
    }

    private byte[] decodeToken(byte[] raw) {
        if (raw.length == 43 && ENCODED_TOKEN.matcher(new String(raw, StandardCharsets.US_ASCII)).matches()) {
            try {
                byte[] decoded = Base64.getUrlDecoder().decode(raw);
                if (decoded.length == 32
                    && encodeToken(decoded).equals(new String(raw, StandardCharsets.US_ASCII))) {
                    return decoded;
                }
            } catch (IllegalArgumentException invalidEncoding) {
                return raw;
            }
        }
        return raw;
    }

    private static byte[] tokenDigestStatic(byte[] tokenBytes) {
        byte[] value = new byte[TOKEN_DOMAIN.length + tokenBytes.length];
        System.arraycopy(TOKEN_DOMAIN, 0, value, 0, TOKEN_DOMAIN.length);
        System.arraycopy(tokenBytes, 0, value, TOKEN_DOMAIN.length, tokenBytes.length);
        return sha256Static(value);
    }

    private static byte[] hmacStatic(byte[] key, byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao derivar a chave do Help Desk.", exception);
        }
    }
}
