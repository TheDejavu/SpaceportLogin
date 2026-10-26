package com.spaceport.login.twofactor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/** Counts wrong codes per account name and locks the name after too many. */
final class FailureLimiter {

  private final int maxFailures;
  private final long lockMillis;
  private final LongSupplier clock;
  private final Map<String, Integer> failures = new HashMap<>();
  private final Map<String, Long> lockedUntil = new HashMap<>();

  FailureLimiter(int maxFailures, long lockMillis, LongSupplier clock) {
    this.maxFailures = maxFailures;
    this.lockMillis = lockMillis;
    this.clock = clock;
  }

  /** Milliseconds left on the lock, or 0 when the name may try. */
  synchronized long lockedFor(String name) {
    String key = key(name);
    Long until = lockedUntil.get(key);
    if (until == null) {
      return 0;
    }
    long left = until - clock.getAsLong();
    if (left <= 0) {
      lockedUntil.remove(key);
      failures.remove(key);
      return 0;
    }
    return left;
  }

  /** Records a wrong code. Returns true when this failure locked the name. */
  synchronized boolean fail(String name) {
    String key = key(name);
    if (lockedFor(name) > 0) {
      return true;
    }
    int count = failures.merge(key, 1, Integer::sum);
    if (count >= maxFailures) {
      lockedUntil.put(key, clock.getAsLong() + lockMillis);
      return true;
    }
    return false;
  }

  synchronized void success(String name) {
    String key = key(name);
    failures.remove(key);
    lockedUntil.remove(key);
  }

  private static String key(String name) {
    return name.toLowerCase(Locale.ROOT);
  }
}
