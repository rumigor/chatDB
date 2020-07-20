package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatStory {
    private Connection connection;
    private Statement stmt;
    private PreparedStatement addToStory;
    private PreparedStatement getStory;

    public ChatStory() throws SQLException, ClassNotFoundException {
        connect();
        prepareAllStatement();
    }


    private void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:chatstory.db");
        stmt = connection.createStatement();
    }

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
        addToStory = connection.prepareStatement("INSERT INTO story (time, nickname, message, private) VALUES (?, ?, ?, ?);");
        getStory = connection.prepareStatement("SELECT time, nickname, message, private FROM story");
    }

    public void messageToStory(String time, String name, String msg, String privateNick) throws SQLException {
        connection.setAutoCommit(false);
        addToStory.setString(1, time);
        addToStory.setString(2, name);
        addToStory.setString(3, msg);
        addToStory.setString(4, privateNick);
        addToStory.executeUpdate();
        connection.commit();
    }

    public String getChatStory (String nickname) throws SQLException {
        StringBuilder chatStory = new StringBuilder();
        connection.setAutoCommit(false);
        ResultSet story = getStory.executeQuery();
        while (story.next()) {
            if (story.getString(4) == null) {
                String msgToAll = String.format("%s %s: %s", story.getString(1), story.getString(2), story.getString(3));
                chatStory.append(msgToAll + "\n");
            }
            else if (story.getString(2).equals(nickname) || story.getString(4).equals(nickname)){
                String prvMsg = String.format("%s %s %s %s: %s", story.getString(1), story.getString(4), "лично для", story.getString(2), story.getString(3));
                chatStory.append(prvMsg + "\n");
            }
        }
        connection.commit();
        story.close();
        return chatStory.toString();
    }
}
