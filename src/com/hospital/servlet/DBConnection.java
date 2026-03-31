package com.hospital.servlet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

   

    private static Connection connection = null;
private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/hospital_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&autoReconnect=true");
private static final String USERNAME = System.getenv().getOrDefault("DB_USER", "root");
private static final String PASSWORD = System.getenv().getOrDefault("DB_PASS", "");

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            } catch (ClassNotFoundException e) {
                throw new SQLException("JDBC Driver not found: " + e.getMessage());
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }
}
