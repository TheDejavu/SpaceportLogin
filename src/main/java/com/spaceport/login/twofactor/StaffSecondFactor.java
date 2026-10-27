package com.spaceport.login.twofactor;

import com.spaceport.login.LoginHold;
import com.spaceport.login.storage.AccountStore;
import fr.xephi.authme.api.v3.AuthMeApi;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** The /2fa command: staff confirm a login with a TOTP code before they are released. */
public final class StaffSecondFactor implements CommandExecutor {

  static final String PERMISSION = "spaceport.staff";
  static final String ADMIN = "spaceport.2fa.admin";

  private final AccountStore store;
  private final Logger logger;
  private final Consumer<Player> release;
  private final Map<UUID, String> pending = new ConcurrentHashMap<>();
  private final Set<UUID> confirmed = ConcurrentHashMap.newKeySet();
  private final FailureLimiter limiter = new FailureLimiter(5, 5 * 60_000L, System::currentTimeMillis);
  private final StepGuard steps = new StepGuard();
  private final SetupAllowance allowance = new SetupAllowance(10 * 60_000L, System::currentTimeMillis);

  public StaffSecondFactor(AccountStore store, Logger logger, Consumer<Player> release) {
    this.store = store;
    this.logger = logger;
    this.release = release;
  }

  /** Whether this staff player still owes a code. */
  public boolean holding(Player player) {
    return player.hasPermission(PERMISSION) && !confirmed.contains(player.getUniqueId());
  }

  /** Tells a held player what to do next. */
  public void ask(Player player) {
    if (stored(player.getName()) == null) {
      player.sendMessage(Component.text("This staff account has no second step yet. Ask an admin to run"
          + " /2fa allow " + player.getName() + ", then run /2fa setup."));
      return;
    }
    player.sendMessage(Component.text("Enter the code from your authenticator with /2fa <code>."));
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 2 && args[0].equalsIgnoreCase("allow")) {
      allow(sender, args[1]);
      return true;
    }
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("A player runs this."));
      return true;
    }
    if (!AuthMeApi.getInstance().isAuthenticated(player)) {
      player.sendMessage(Component.text("Log in first."));
      return true;
    }
    if (!player.hasPermission(PERMISSION)) {
      player.sendMessage(Component.text("This account does not use a second step."));
      return true;
    }
    if (args.length == 1 && args[0].equalsIgnoreCase("setup")) {
      begin(player);
      return true;
    }
    if (args.length != 1) {
      player.sendMessage(Component.text("Usage: /2fa <code>"));
      return true;
    }
    confirm(player, args[0]);
    return true;
  }

  /** Drops what is known about a player who left. */
  public void forget(UUID id) {
    pending.remove(id);
    confirmed.remove(id);
  }

  private void allow(CommandSender sender, String name) {
    // Only a logged-in admin past their own second step can vouch for anyone.
    if (sender instanceof Player player && (!player.hasPermission(ADMIN)
        || !AuthMeApi.getInstance().isAuthenticated(player) || holding(player) || LoginHold.isHeld(player))) {
      sender.sendMessage(Component.text("You cannot allow a second step setup."));
      return;
    }
    allowance.allow(name);
    logger.info(sender.getName() + " allowed " + name + " to set up a second step");
    sender.sendMessage(Component.text(name + " may run /2fa setup on this server in the next 10 minutes."));
  }

  private void begin(Player player) {
    if (stored(player.getName()) != null) {
      player.sendMessage(Component.text("This account already has a second step. Enter the current code."));
      return;
    }
    if (!allowance.allowed(player.getName())) {
      player.sendMessage(Component.text("Setting up a second step needs an admin: ask for /2fa allow "
          + player.getName() + "."));
      return;
    }
    String secret = Totp.secret();
    pending.put(player.getUniqueId(), secret);
    player.sendMessage(Component.text("Add this secret to an authenticator, then run /2fa <code>."));
    player.sendMessage(Component.text(secret));
    player.sendMessage(Component.text(Totp.uri(player.getName(), secret)));
  }

  private void confirm(Player player, String code) {
    String name = player.getName();
    long locked = limiter.lockedFor(name);
    if (locked > 0) {
      player.sendMessage(Component.text(
          "Too many wrong codes. Try again in " + ((locked + 59_999) / 60_000) + " minutes."));
      return;
    }
    String fresh = pending.get(player.getUniqueId());
    String secret = fresh != null ? fresh : stored(name);
    if (AccountStore.UNREADABLE.equals(secret)) {
      player.sendMessage(Component.text("The second step could not be checked. Try again soon."));
      return;
    }
    long step = secret == null ? -1 : Totp.matchStep(secret, code, System.currentTimeMillis());
    StepGuard.Outcome outcome = steps.judge(name, step);
    if (outcome == StepGuard.Outcome.REPLAY) {
      player.sendMessage(Component.text("That code was already used. Wait for the next one."));
      return;
    }
    if (outcome == StepGuard.Outcome.WRONG) {
      player.sendMessage(Component.text("That code was not accepted."));
      if (limiter.fail(name)) {
        logger.warning("Locked the second step for " + name + " after wrong codes");
        player.kick(Component.text("Too many wrong codes. Try again in a few minutes."));
      }
      return;
    }
    limiter.success(name);
    if (fresh != null) {
      store.saveTotpSecret(name, fresh);
      pending.remove(player.getUniqueId());
      allowance.used(name);
    }
    confirmed.add(player.getUniqueId());
    release.accept(player);
    player.sendMessage(Component.text("Second step accepted."));
  }

  private String stored(String name) {
    return store.totpSecret(name);
  }
}
