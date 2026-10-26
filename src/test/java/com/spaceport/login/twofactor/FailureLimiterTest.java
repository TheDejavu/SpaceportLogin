package com.spaceport.login.twofactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FailureLimiterTest {

  @Test
  void locksOnTheFifthFailureForFiveMinutes() {
    long[] now = {0};
    FailureLimiter limiter = new FailureLimiter(5, 300_000L, () -> now[0]);
    for (int i = 0; i < 4; i++) {
      assertFalse(limiter.fail("Alice"));
      assertEquals(0L, limiter.lockedFor("Alice"));
    }
    assertTrue(limiter.fail("ALICE"));
    assertEquals(300_000L, limiter.lockedFor("alice"));
    now[0] = 299_999L;
    assertTrue(limiter.lockedFor("Alice") > 0);
    now[0] = 300_000L;
    assertEquals(0L, limiter.lockedFor("Alice"));
    assertFalse(limiter.fail("Alice"), "the count restarts after the lock");
  }

  @Test
  void successResetsTheCount() {
    FailureLimiter limiter = new FailureLimiter(5, 300_000L, () -> 0L);
    for (int i = 0; i < 4; i++) {
      limiter.fail("Alice");
    }
    limiter.success("Alice");
    for (int i = 0; i < 4; i++) {
      assertFalse(limiter.fail("Alice"));
    }
  }

  @Test
  void otherNamesAreUnaffected() {
    FailureLimiter limiter = new FailureLimiter(2, 1000L, () -> 0L);
    limiter.fail("Alice");
    assertTrue(limiter.fail("Alice"));
    assertEquals(0L, limiter.lockedFor("Bob"));
  }
}
