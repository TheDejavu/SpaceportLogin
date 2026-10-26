package com.spaceport.login;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginHoldTest {

  @BeforeEach
  void clear() {
    LoginHold.clear();
  }

  @Test
  void notHeldByDefault() {
    assertFalse(LoginHold.isHeld(UUID.randomUUID()));
  }

  @Test
  void heldUntilReleased() {
    UUID id = UUID.randomUUID();
    LoginHold.hold(id);
    assertTrue(LoginHold.isHeld(id));
    LoginHold.release(id);
    assertFalse(LoginHold.isHeld(id));
  }

  @Test
  void clearFreesEveryone() {
    UUID id = UUID.randomUUID();
    LoginHold.hold(id);
    LoginHold.clear();
    assertFalse(LoginHold.isHeld(id));
  }
}
