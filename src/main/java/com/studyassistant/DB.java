package com.studyassistant;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Connects to the MySQL/MariaDB server that WampServer runs. Change these if your WAMP setup differs. */
public class DB {
    static final String URL = "DB_URL";
    static final String USER = "DB_USER";
    static final String PASS = "DB_PASSWORD";   // WampServer default: empty password

    public static Connection get() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found", e);
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
