package com.smartfarming.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection
 *
 * Manages JDBC connections to MySQL.
 * Uses a simple singleton pattern with lazy initialization.
 *
 * NOTE: For production, replace with a proper connection pool
 *       (e.g., Apache DBCP or HikariCP). For this academic
 *       project, we keep it simple and readable.
 */
public class DBConnection {

    // ─── Database Configuration ───────────────────────────────
    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    private static final String DATABASE = "smart_farming";
    private static final String USER     = "root";
    private static final String PASSWORD = "you@password";

    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
        + "?useSSL=false"
        + "&serverTimezone=UTC"
        + "&allowPublicKeyRetrieval=true"
        + "&useUnicode=true"
        + "&characterEncoding=UTF-8";

    // Singleton instance
    private static DBConnection instance;
    private Connection connection;

    // ─── Private constructor ───────────────────────────────────
    private DBConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Returns the singleton DBConnection.
     * If connection is closed or null, creates a new one.
     */
    public static DBConnection getInstance()
            throws ClassNotFoundException, SQLException {
        if (instance == null || instance.getConnection().isClosed()) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Returns the underlying java.sql.Connection.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Closes the connection (call on application shutdown).
     */
    public static void closeConnection() {
        if (instance != null) {
            try {
                if (!instance.connection.isClosed()) {
                    instance.connection.close();
                    System.out.println("[DBConnection] Connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("[DBConnection] Error closing: " + e.getMessage());
            }
        }
    }
}
