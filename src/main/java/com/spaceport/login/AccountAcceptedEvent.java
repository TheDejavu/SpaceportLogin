package com.spaceport.login;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/** The proxy has been told this account may leave the auth limbo. */
public final class AccountAcceptedEvent extends PlayerEvent {

  private static final HandlerList HANDLERS = new HandlerList();

  public AccountAcceptedEvent(Player player) {
    super(player);
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
