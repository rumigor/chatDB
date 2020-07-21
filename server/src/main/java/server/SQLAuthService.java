package server;

import java.sql.*;


public class SQLAuthService implements  AuthService{
    protected Connection connection;
    protected Statement stmt;
    private PreparedStatement authValidation;
    private PreparedStatement registration;
    private PreparedStatement changeNickName;

    public SQLAuthService() throws SQLException, ClassNotFoundException {
        connect();
        prepareAllStatement();
    }
    @Override
    public Connection getConnection() {
        return connection;
    }

    private void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:userdata.db");
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
        changeNickName = connection.prepareStatement("UPDATE chatusers SET nickname = ? WHERE nickname = ?");
    }



    @Override
    public String getNicknameByLoginAndPassword(String login, String password) throws SQLException {
        connection.setAutoCommit(false);
        ResultSet userData =  authValidation.executeQuery();
        while (userData.next()) {
            if (login.equals(userData.getString(1)) && password.equals(userData.getString(2))) {
                connection.commit();
                String nickname = userData.getString(3);
                userData.close();
                return nickname;
            }
        }
        userData.close();
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) throws SQLException {
        connection.setAutoCommit(false);
        ResultSet userData = authValidation.executeQuery();
        while (userData.next()) {
            if (login.equals(userData.getString(1)) || nickname.equals(userData.getString(3))) {
                userData.close();
                return false;
            }
        }
        registration.setString(1,login);
        registration.setString(2,password);
        registration.setString(3,nickname);
        registration.executeUpdate();
        connection.commit();
        userData.close();
        return true;
    }

    @Override
    public boolean changeNick(String oldNick, String newNick) throws SQLException {
        connection.setAutoCommit(false);
        ResultSet userData = authValidation.executeQuery();
        while (userData.next()) {
            if (newNick.equals(userData.getString(3))) {
                userData.close();
                return false;
            }
        }
        changeNickName.setString(1, newNick);
        changeNickName.setString(2, oldNick);
        changeNickName.executeUpdate();
        connection.commit();
        userData.close();
        return true;
    }
}
