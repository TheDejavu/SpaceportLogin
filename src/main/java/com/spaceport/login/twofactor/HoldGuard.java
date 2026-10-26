package com.spaceport.login.twofactor;

import com.spaceport.login.LoginHold;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/** Staff who still owe a /2fa code can only run /2fa: no other commands, chat or interaction. */
public final class HoldGuard implements Listener {

  /** Whether a command line may run while the player is held. */
  static boolean allowedWhileHeld(String message) {
    String label = message.toLowerCase(Locale.ROOT).split(" ", 2)[0];
    return label.equals("/2fa") || label.equals("/spaceportlogin:2fa");
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    if (LoginHold.isHeld(event.getPlayer()) && !allowedWhileHeld(event.getMessage())) {
      event.setCancelled(true);
      event.getPlayer().sendMessage(Component.text("Finish the second step first: /2fa <code>"));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(AsyncChatEvent event) {
    cancelIfHeld(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInteract(PlayerInteractEvent event) {
    cancelIfHeld(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInteractEntity(PlayerInteractEntityEvent event) {
    cancelIfHeld(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryClick(InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player player) {
      cancelIfHeld(player, event);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryOpen(InventoryOpenEvent event) {
    if (event.getPlayer() instanceof Player player) {
      cancelIfHeld(player, event);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onDrop(PlayerDropItemEvent event) {
    cancelIfHeld(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onBreak(BlockBreakEvent event) {
    cancelIfHeld(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlace(BlockPlaceEvent event) {
    cancelIfHeld(event.getPlayer(), event);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onAttack(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player) {
      cancelIfHeld(player, event);
    }
  }

  private static void cancelIfHeld(Player player, Cancellable event) {
    if (LoginHold.isHeld(player)) {
      event.setCancelled(true);
    }
  }
}
