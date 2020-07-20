package server;

import java.sql.*;
import java.util.List;

public class SQLAuthService implements  AuthService{
    private Connection connection;
    private Statement stmt;
    private PreparedStatement authValidation;
    private PreparedStatement registration;

    public SQLAuthService() throws SQLException, ClassNotFoundException {
        connect();
        prepareAllStatement();

    }

    private class UserData {
        String login;
        String password;
        String nickname;

        public UserData(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    private void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:userdata.db");
        System.out.println(connection.getCatalog());
        stmt = connection.createStatement();
    }

    @Override
    public void disconnect() {
        try {
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void prepareAllStatement() throws SQLException {
        authValidation = connection.prepareStatement("SELECT login, password, nickname FROM chatusers");
        registration = connection.prepareStatement("INSERT INTO chatusers (login, password, nickname) VALUES (?, ?, ?);");
    }



    @Override
    public String getNicknameByLoginAndPassword(String login, String password) throws SQLException {
//        ResultSet userData = stmt.executeQuery("SELECT login, password, nickname FROM chatusers");
        ResultSet userData = authValidation.executeQuery();
        while (userData.next()) {
            System.out.println(userData.getString("login") + " " + userData.getInt("password"));
            if (login.equals(userData.getString(1)) && password.equals(userData.getString(2))) {
                connection.commit();
                userData.close();
                return userData.getString(3);
            }
        }
        userData.close();
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) throws SQLException {
//        ResultSet userData = stmt.executeQuery("SELECT login, password, nickname FROM chatusers");
        ResultSet userData = authValidation.executeQuery();
        while (userData.next()) {
            System.out.println(userData.getString("login") + " " + userData.getInt("password"));
            if (login.equals(userData.getString(1)) || nickname.equals(userData.getString(3))) {
                userData.close();
                return false;
            }
        }
        registration.setString(1, login);
        registration.setString(2, password);
        registration.setString(3,nickname);
        connection.commit();
        userData.close();
        return true;
    }
}
