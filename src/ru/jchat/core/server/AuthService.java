package ru.jchat.core.server;

import java.sql.*;

class AuthService {
    private Connection connection;
    private Statement stmt;

    void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        stmt = connection.createStatement();
    }

    String getNickByLoginAndPass(String login, String pass) {
        try {
            ResultSet rs = stmt.executeQuery("SELECT nick FROM users WHERE login = '" + login + "' AND password = '" + pass + "';");
            if (rs.next()) {
                return rs.getString("nick");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    String changeNick(String oldNick, String newNick) {

        try {
            ResultSet rs = stmt.executeQuery("SELECT nick FROM users WHERE nick = '" + newNick + "'");
            if (rs.next())return newNick + ": this nickname is already taken ";
            stmt.executeUpdate("UPDATE users SET nick ='" + newNick +"' WHERE nick = '" + oldNick + "'" );
            return "NickName is changed";
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    void disconnect() {
        try {
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
