package server;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public interface AuthService {
    String getNicknameByLoginAndPassword(String login, String password) throws SQLException;
    boolean registration(String login, String password, String nickname) throws SQLException;
    void disconnect();
    boolean changeNick(String oldNick, String newNick) throws SQLException;
    Connection getConnection();
}
