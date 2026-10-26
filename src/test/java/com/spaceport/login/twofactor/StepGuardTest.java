package com.spaceport.login.twofactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StepGuardTest {

  @Test
  void rejectsSameOrOlderStep() {
    StepGuard guard = new StepGuard();
    assertTrue(guard.accept("Alice", 100));
    assertFalse(guard.accept("alice", 100), "same step, name is case-insensitive");
    assertFalse(guard.accept("Alice", 99));
    assertTrue(guard.accept("Alice", 101));
    assertTrue(guard.accept("Bob", 100), "names are independent");
  }

  @Test
  void separatesWrongFromReplay() {
    StepGuard guard = new StepGuard();
    assertEquals(StepGuard.Outcome.WRONG, guard.judge("Al", -1));
    assertEquals(StepGuard.Outcome.ACCEPTED, guard.judge("Al", 7));
    assertEquals(StepGuard.Outcome.REPLAY, guard.judge("Al", 7));
    assertEquals(StepGuard.Outcome.WRONG, guard.judge("Al", -1));
  }
}
