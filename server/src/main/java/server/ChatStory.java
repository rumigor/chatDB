package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatStory {
    private Connection connection;
    private PreparedStatement addToStory;
    private PreparedStatement getStory;
    private PreparedStatement getNickName;
    private PreparedStatement getId;


    public ChatStory(Connection connection) throws SQLException, ClassNotFoundException {
        this.connection = connection;
        prepareAllStatement();
    }

    public void prepareAllStatement() throws SQLException {
        addToStory = connection.prepareStatement("INSERT INTO story (receiverID, senderID, time, message) VALUES (?, ?, ?, ?);");
        getStory = connection.prepareStatement("SELECT receiverID, senderID, time, message FROM story");
        getId = connection.prepareStatement("SELECT id FROM chatusers WHERE nickname = ?");
        getNickName = connection.prepareStatement("SELECT nickname FROM chatusers WHERE id = ?");
    }

    public void messageToStory(String receiverNick, String senderNick, String time, String msg) throws SQLException { //сохраняем сообщения в БД
        connection.setAutoCommit(false);
        int receiverID = getIdNumber(receiverNick);
        int senderID = getIdNumber(senderNick);
        addToStory.setInt(1, receiverID);
        addToStory.setInt(2, senderID);
        addToStory.setString(3, time);
        addToStory.setString(4, msg);
        addToStory.executeUpdate();
        connection.commit();
    }

    public String getChatStory (String nickname) throws SQLException { //вытаскиваем сообщения из БД
        StringBuilder chatStory = new StringBuilder();
        connection.setAutoCommit(false);
        ResultSet story = getStory.executeQuery();
        String receiverNick;
        String senderNick;
        while (story.next()) {
            if (story.getInt(1) == 0) {
                senderNick = getNick(story.getInt(2));
                String msgToAll = String.format("%s %s: %s", story.getString(3), senderNick, story.getString(4));
                chatStory.append(msgToAll).append("\n");
            }
            else {
                receiverNick = getNick(story.getInt(1));
                senderNick = getNick(story.getInt(2));
                if (receiverNick.equals(nickname) || senderNick.equals(nickname)) {
                    String prvMsg = String.format("%s %s %s %s: %s", story.getString(3), senderNick, "приватно для", receiverNick, story.getString(4));
                    chatStory.append(prvMsg).append("\n");
                }
            }
            }
        connection.commit();
        story.close();
        return chatStory.toString();
    }

    private String getNick(int id) throws SQLException {
        getNickName.setInt(1, id);
        ResultSet nickById = getNickName.executeQuery();
        String nickName = null;
        while (nickById.next()) {
            nickName = nickById.getString(1);
        }
        return nickName;
    }

    private int getIdNumber (String nickname) throws SQLException {
        getId.setString(1, nickname);
        ResultSet id = getId.executeQuery();
        int idNumber = 0;
        while (id.next()) {
            idNumber = id.getInt(1);
        }
        return idNumber;
    }
}
