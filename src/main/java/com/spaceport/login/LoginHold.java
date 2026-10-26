package com.spaceport.login;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/**
 * Players that this server is holding until they finish /2fa. The state is per JVM and only
 * covers players whose hold started here, so a backend that never held a player never reports
 * one as held. Other plugins read it through {@link #isHeld(Player)}.
 */
public final class LoginHold {

  private static final Set<UUID> HELD = ConcurrentHashMap.newKeySet();

  private LoginHold() {
  }

  public static boolean isHeld(Player player) {
    return isHeld(player.getUniqueId());
  }

  static boolean isHeld(UUID id) {
    return HELD.contains(id);
  }

  static void hold(UUID id) {
    HELD.add(id);
  }

  static void release(UUID id) {
    HELD.remove(id);
  }

  static void clear() {
    HELD.clear();
  }
}
