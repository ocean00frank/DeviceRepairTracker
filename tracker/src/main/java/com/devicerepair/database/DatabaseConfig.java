package com.devicerepair.database;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConfig {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "5432";
    private static final String DEFAULT_DB = "DeviceRepair_db";

    private DatabaseConfig() {
    }

    public static String getDbName() {
        return getEnv("DB_NAME", DEFAULT_DB);
    }

    public static String getDbHost() {
        return getEnv("DB_HOST", DEFAULT_HOST);
    }

    public static String getDbPort() {
        return getEnv("DB_PORT", DEFAULT_PORT);
    }

    public static String getDbUser() {
        return getEnv("DB_USER", "postgres");
    }

    public static String getDbPassword() {
        return getEnv("DB_PASSWORD", "");
    }

    public static Connection getConnection() throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%s/%s",
                getDbHost(), getDbPort(), getDbName());

        return DriverManager.getConnection(url, getDbUser(), getDbPassword());
    }

    public static void initializeDatabase() {
        Path schemaPath = Path.of("database", "schema.sql");
        if (!Files.exists(schemaPath)) {
            return;
        }

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            String sql = Files.readString(schemaPath);
            statement.execute(sql);
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Unable to initialize database schema: " + e.getMessage(), e);
        }
    }

    private static String getEnv(String key, String defaultValue) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String value = dotenv.get(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(key);
        }
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
