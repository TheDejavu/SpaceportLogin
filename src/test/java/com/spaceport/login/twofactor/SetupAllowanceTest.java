package com.spaceport.login.twofactor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SetupAllowanceTest {

  @Test
  void nobodyIsAllowedByDefault() {
    assertFalse(new SetupAllowance(1000L, () -> 0L).allowed("Alice"));
  }

  @Test
  void allowanceExpires() {
    long[] now = {0};
    SetupAllowance allowance = new SetupAllowance(1000L, () -> now[0]);
    allowance.allow("Alice");
    assertTrue(allowance.allowed("alice"), "names are case-insensitive");
    now[0] = 999L;
    assertTrue(allowance.allowed("Alice"));
    now[0] = 1000L;
    assertFalse(allowance.allowed("Alice"));
  }

  @Test
  void allowanceIsSpentOnce() {
    SetupAllowance allowance = new SetupAllowance(1000L, () -> 0L);
    allowance.allow("Alice");
    allowance.used("Alice");
    assertFalse(allowance.allowed("Alice"));
  }

  @Test
  void otherNamesAreNotAllowed() {
    SetupAllowance allowance = new SetupAllowance(1000L, () -> 0L);
    allowance.allow("Alice");
    assertFalse(allowance.allowed("Bob"));
  }
}
