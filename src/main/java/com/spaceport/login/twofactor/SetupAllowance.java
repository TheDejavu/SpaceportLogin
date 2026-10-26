package com.spaceport.login.twofactor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/** Names an admin has allowed to run /2fa setup, each for a limited time and one enrolment. */
final class SetupAllowance {

  private final long validMillis;
  private final LongSupplier clock;
  private final Map<String, Long> until = new HashMap<>();

  SetupAllowance(long validMillis, LongSupplier clock) {
    this.validMillis = validMillis;
    this.clock = clock;
  }

  synchronized void allow(String name) {
    until.put(key(name), clock.getAsLong() + validMillis);
  }

  synchronized boolean allowed(String name) {
    Long end = until.get(key(name));
    if (end == null || clock.getAsLong() >= end) {
      until.remove(key(name));
      return false;
    }
    return true;
  }

  /** Spends the allowance once the new secret is saved. */
  synchronized void used(String name) {
    until.remove(key(name));
  }

  private static String key(String name) {
    return name.toLowerCase(Locale.ROOT);
  }
}
