package com.spaceport.login.twofactor;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class Totp {

  private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  private static final SecureRandom RANDOM = new SecureRandom();

  private Totp() {
  }

  static String secret() {
    byte[] bytes = new byte[20];
    RANDOM.nextBytes(bytes);
    return encode(bytes);
  }

  static String uri(String name, String secret) {
    return "otpauth://totp/Spaceport:" + name + "?secret=" + secret
        + "&issuer=Spaceport&digits=6&period=30";
  }

  static boolean matches(String secret, String typed) {
    return matchStep(secret, typed, System.currentTimeMillis()) >= 0;
  }

  /**
   * Returns the time step the typed code belongs to (current step or one either side), or -1.
   * All three steps are always computed and compared in constant time.
   */
  static long matchStep(String secret, String typed, long nowMillis) {
    if (secret == null || typed == null || !typed.matches("\\d{6}")) {
      return -1;
    }
    try {
      byte[] key = decode(secret);
      if (key.length == 0) {
        return -1;
      }
      byte[] given = typed.getBytes(StandardCharsets.US_ASCII);
      long step = nowMillis / 30_000L;
      long matched = -1;
      for (long delta = -1; delta <= 1; delta++) {
        byte[] expected = code(key, step + delta).getBytes(StandardCharsets.US_ASCII);
        if (MessageDigest.isEqual(given, expected)) {
          matched = step + delta;
        }
      }
      return matched;
    } catch (IllegalArgumentException bad) {
      return -1;
    }
  }

  static String code(String secret, long step) {
    return code(decode(secret), step);
  }

  private static String code(byte[] key, long counter) {
    byte[] data = new byte[8];
    long value = counter;
    for (int i = 7; i >= 0; i--) {
      data[i] = (byte) (value & 0xff);
      value >>= 8;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      byte[] hash = mac.doFinal(data);
      int offset = hash[hash.length - 1] & 0x0f;
      int binary = ((hash[offset] & 0x7f) << 24)
          | ((hash[offset + 1] & 0xff) << 16)
          | ((hash[offset + 2] & 0xff) << 8)
          | (hash[offset + 3] & 0xff);
      return String.format("%06d", binary % 1_000_000);
    } catch (GeneralSecurityException failure) {
      return "";
    }
  }

  private static String encode(byte[] data) {
    StringBuilder out = new StringBuilder();
    int buffer = 0;
    int bits = 0;
    for (byte piece : data) {
      buffer = (buffer << 8) | (piece & 0xff);
      bits += 8;
      while (bits >= 5) {
        bits -= 5;
        out.append(ALPHABET.charAt((buffer >> bits) & 31));
      }
    }
    if (bits > 0) {
      out.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
    }
    return out.toString();
  }

  private static byte[] decode(String secret) {
    String cleaned = secret.replace(" ", "").replace("=", "").toUpperCase();
    int buffer = 0;
    int bits = 0;
    byte[] out = new byte[cleaned.length() * 5 / 8];
    int written = 0;
    for (int i = 0; i < cleaned.length(); i++) {
      int value = ALPHABET.indexOf(cleaned.charAt(i));
      if (value < 0) {
        throw new IllegalArgumentException("bad secret");
      }
      buffer = (buffer << 5) | value;
      bits += 5;
      if (bits >= 8) {
        bits -= 8;
        out[written++] = (byte) ((buffer >> bits) & 0xff);
      }
    }
    if (written != out.length) {
      byte[] trimmed = new byte[written];
      System.arraycopy(out, 0, trimmed, 0, written);
      return trimmed;
    }
    return out;
  }
}
