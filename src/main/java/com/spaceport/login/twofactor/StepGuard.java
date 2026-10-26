package com.spaceport.login.twofactor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Remembers the newest accepted time step per account so a code cannot be used twice. */
final class StepGuard {

  enum Outcome { ACCEPTED, WRONG, REPLAY }

  private final Map<String, Long> last = new HashMap<>();

  /** True and remembered when the step is newer than every accepted one; false for a replay. */
  synchronized boolean accept(String name, long step) {
    String key = name.toLowerCase(Locale.ROOT);
    Long seen = last.get(key);
    if (seen != null && step <= seen) {
      return false;
    }
    last.put(key, step);
    return true;
  }

  /** Judges a matched step (negative means the code did not match at all). */
  Outcome judge(String name, long step) {
    if (step < 0) {
      return Outcome.WRONG;
    }
    return accept(name, step) ? Outcome.ACCEPTED : Outcome.REPLAY;
  }
}
