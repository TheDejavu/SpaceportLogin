package com.spaceport.login.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;

/** The premium links and staff TOTP secrets, in the same MariaDB as AuthMe. */
public final class AccountStore {

  /** Returned by reads that failed, so callers can tell "none" from "could not check". */
  public static final String UNREADABLE = "";

  private final ConfigurationSection config;
  private final Logger logger;

  public AccountStore(ConfigurationSection config, Logger logger) {
    this.config = config;
    this.logger = logger;
  }

  /** Creates the tables if they are missing. */
  public void createTables() throws SQLException {
    try (Connection connection = open()) {
      connection.createStatement().execute(
          "CREATE TABLE IF NOT EXISTS spaceport_premium ("
              + "name VARCHAR(16) NOT NULL PRIMARY KEY, uuid CHAR(36) NOT NULL)");
      connection.createStatement().execute(
          "CREATE TABLE IF NOT EXISTS spaceport_totp ("
              + "name VARCHAR(16) NOT NULL PRIMARY KEY, secret VARCHAR(64) NOT NULL)");
    }
  }

  /** The UUID linked to a premium name, null when none, {@link #UNREADABLE} on error. */
  public String linkedUuid(String name) {
    return read("SELECT uuid FROM spaceport_premium WHERE name = ?", name, "premium link");
  }

  public void link(String name, String uuid) {
    write("INSERT INTO spaceport_premium (name, uuid) VALUES (?, ?) "
        + "ON DUPLICATE KEY UPDATE uuid = VALUES(uuid)", name, uuid, "premium link");
  }

  /** The TOTP secret of a name, null when none, {@link #UNREADABLE} on error. */
  public String totpSecret(String name) {
    return read("SELECT secret FROM spaceport_totp WHERE name = ?", name, "second step");
  }

  public void saveTotpSecret(String name, String secret) {
    write("INSERT INTO spaceport_totp (name, secret) VALUES (?, ?) "
        + "ON DUPLICATE KEY UPDATE secret = VALUES(secret)", name, secret, "second step");
  }

  private String read(String sql, String name, String what) {
    try (Connection connection = open();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, name);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? rows.getString(1) : null;
      }
    } catch (SQLException failure) {
      logger.warning("Could not read the " + what + " for " + name);
      return UNREADABLE;
    }
  }

  private void write(String sql, String name, String value, String what) {
    try (Connection connection = open();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, name);
      statement.setString(2, value);
      statement.executeUpdate();
    } catch (SQLException failure) {
      logger.warning("Could not store the " + what + " for " + name);
    }
  }

  private Connection open() throws SQLException {
    try {
      Class.forName("org.mariadb.jdbc.Driver");
    } catch (ClassNotFoundException missing) {
      throw new SQLException("mariadb driver is missing", missing);
    }
    String url = "jdbc:mariadb://" + config.getString("host") + ":"
        + config.getInt("port") + "/" + config.getString("database");
    return DriverManager.getConnection(url, config.getString("username"),
        config.getString("password"));
  }
}
