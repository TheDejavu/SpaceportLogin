# SpaceportLogin

AuthMe companion plugin for servers behind a [Spaceport](https://github.com/TheDejavu/Velocity-Spaceport) proxy.

- **Premium players skip the password.** When the proxy marks a profile as Mojang-verified, the
  player is logged in (or registered) in AuthMe automatically.
- **Staff second step.** Players with `spaceport.staff` confirm each login with a TOTP code
  (`/2fa <code>`). Until they do, they can only run `/2fa`: other commands, chat, interaction and
  inventory use are blocked. Five wrong codes kick the player and lock the name for 5 minutes;
  a code can't be used twice.
- **Enrolment needs an admin.** The console or a player with `spaceport.2fa.admin` runs
  `/2fa allow <player>` on the login server; that player then has 10 minutes to run
  `/2fa setup` and confirm the first code. Staff without a secret stay held.
- **Tells the proxy.** Once the account is accepted, the plugin sends `spaceport:account` so the
  proxy lets the player leave the login server (Spaceport `login-gate`).

Needs Paper 1.21+, AuthMe and a MariaDB database (the plugin creates its own tables there).

## Build

```
./gradlew build
```

The jar is in `build/libs/`. Set the database details in `plugins/SpaceportLogin/config.yml`.

## API

- `com.spaceport.login.LoginHold.isHeld(Player)`: whether the player still owes a `/2fa` code.
- `com.spaceport.login.AccountAcceptedEvent`: fired when the account is accepted.
