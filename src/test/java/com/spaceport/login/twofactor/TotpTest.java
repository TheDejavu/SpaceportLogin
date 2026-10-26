package com.spaceport.login.twofactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class TotpTest {

  // Base32 test secret used by most TOTP examples, not a real account.
  private static final String SECRET = "JBSWY3DPEHPK3PXP";
  private static final long NOW = 1_700_000_000_000L;
  private static final long STEP = NOW / 30_000L;

  @Test
  void matchesRfc6238Vector() {
    // RFC 6238 SHA1 secret "12345678901234567890" at T=59s gives 94287082.
    assertEquals("287082", Totp.code("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", 1));
  }

  @Test
  void currentAndAdjacentStepsMatch() {
    assertEquals(STEP, Totp.matchStep(SECRET, Totp.code(SECRET, STEP), NOW));
    assertEquals(STEP - 1, Totp.matchStep(SECRET, Totp.code(SECRET, STEP - 1), NOW));
    assertEquals(STEP + 1, Totp.matchStep(SECRET, Totp.code(SECRET, STEP + 1), NOW));
  }

  @Test
  void farStepWrongAndMalformedCodesAreRejected() {
    assertEquals(-1, Totp.matchStep(SECRET, Totp.code(SECRET, STEP + 5), NOW));
    assertEquals(-1, Totp.matchStep(SECRET, "12345", NOW));
    assertEquals(-1, Totp.matchStep(SECRET, "abcdef", NOW));
    assertEquals(-1, Totp.matchStep(SECRET, null, NOW));
  }

  @Test
  void emptyOrInvalidSecretDoesNotThrow() {
    assertEquals(-1, Totp.matchStep("", "123456", NOW));
    assertEquals(-1, Totp.matchStep("!!!!", "123456", NOW));
    assertEquals(-1, Totp.matchStep(null, "123456", NOW));
    assertFalse(Totp.matches("", "123456"));
  }
}
