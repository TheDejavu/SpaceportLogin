package com.spaceport.login;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.spaceport.login.storage.AccountStore;
import com.spaceport.login.twofactor.HoldGuard;
import com.spaceport.login.twofactor.StaffSecondFactor;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HexFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AuthMe companion for a Spaceport proxy: logs premium players in without a password, holds
 * staff for a TOTP second step, and tells the proxy when an account may leave the auth server.
 */
public final class SpaceportLogin extends JavaPlugin implements Listener {

  private static final String PROOF = "spaceport:premium";
  private static final String LOGGED_IN = "spaceport:account";

  private final SecureRandom random = new SecureRandom();
  private AccountStore store;
  private StaffSecondFactor secondFactor;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    store = new AccountStore(getConfig(), getLogger());
    secondFactor = new StaffSecondFactor(store, getLogger(), this::release);
    getServer().getMessenger().registerOutgoingPluginChannel(this, LOGGED_IN);
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getPluginManager().registerEvents(new HoldGuard(), this);
    getCommand("2fa").setExecutor(secondFactor);
    try {
      store.createTables();
    } catch (SQLException failure) {
      throw new IllegalStateException("account tables are not ready", failure);
    }
  }

  @Override
  public void onDisable() {
    // Keep the hold state so a reload cannot release staff; held players are removed instead.
    for (Player online : getServer().getOnlinePlayers()) {
      if (LoginHold.isHeld(online)) {
        online.kick(Component.text("Login plugin restarted. Join again."));
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    if (!proven(player)) {
      return;
    }
    getServer().getScheduler().runTaskLater(this, () -> {
      if (player.isOnline()) {
        admit(player);
      }
    }, 20L);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPassword(LoginEvent event) {
    Player player = event.getPlayer();
    if (secondFactor.holding(player)) {
      LoginHold.hold(player.getUniqueId());
    }
    if (proven(player)) {
      store.link(player.getName(), player.getUniqueId().toString());
    }
    getServer().getScheduler().runTaskLater(this, () -> {
      if (!player.isOnline()) {
        return;
      }
      if (secondFactor.holding(player)) {
        LoginHold.hold(player.getUniqueId());
        secondFactor.ask(player);
        return;
      }
      release(player);
    }, 10L);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    secondFactor.forget(event.getPlayer().getUniqueId());
    LoginHold.release(event.getPlayer().getUniqueId());
  }

  private void release(Player player) {
    LoginHold.release(player.getUniqueId());
    player.sendPluginMessage(this, LOGGED_IN, new byte[] {1});
    getServer().getPluginManager().callEvent(new AccountAcceptedEvent(player));
  }

  private void admit(Player player) {
    String linked = store.linkedUuid(player.getName());
    if (player.getUniqueId().toString().equals(linked)) {
      AuthMeApi.getInstance().forceLogin(player);
      return;
    }
    if (linked == null && !AuthMeApi.getInstance().isRegistered(player.getName())) {
      AuthMeApi.getInstance().forceRegister(player, newPassword());
      store.link(player.getName(), player.getUniqueId().toString());
      AuthMeApi.getInstance().forceLogin(player);
    }
  }

  private String newPassword() {
    byte[] bytes = new byte[15];
    random.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private static boolean proven(Player player) {
    for (ProfileProperty property : player.getPlayerProfile().getProperties()) {
      if (PROOF.equals(property.getName()) && "1".equals(property.getValue())) {
        return true;
      }
    }
    return false;
  }
}
