package com.spaceport.login.twofactor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HoldGuardTest {

  @Test
  void onlyTheSecondStepCommandRuns() {
    assertTrue(HoldGuard.allowedWhileHeld("/2fa 123456"));
    assertTrue(HoldGuard.allowedWhileHeld("/2FA setup"));
    assertTrue(HoldGuard.allowedWhileHeld("/2fa"));
    assertTrue(HoldGuard.allowedWhileHeld("/spaceportlogin:2fa 123456"));
    assertFalse(HoldGuard.allowedWhileHeld("/2faa 123456"));
    assertFalse(HoldGuard.allowedWhileHeld("/op Alice"));
    assertFalse(HoldGuard.allowedWhileHeld("/gamemode creative"));
    assertFalse(HoldGuard.allowedWhileHeld("/minecraft:op Alice"));
  }
}
