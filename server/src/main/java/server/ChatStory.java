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
        addToStory = connection.prepareStatement("INSERT INTO story (receiverID, senderID, time, message) VALUES (\n" +
                "(SELECT id FROM chatusers WHERE nickname=?),\n" +
                "(SELECT id FROM chatusers WHERE nickname=?),\n" +
                "?, ?)");
        getStory = connection.prepareStatement("SELECT (SELECT nickname FROM chatusers Where id = receiverID),\n + " +
                "(SELECT nickname FROM chatusers Where id = senderID), \n"  +
                "       time,\n" +
                "       message \n" +
                "FROM story \n" +
                "WHERE senderID = (SELECT id FROM chatusers WHERE nickname=?)\n" +
                "OR receiverID = (SELECT id FROM chatusers WHERE nickname=?)\n" +
                "OR receiverID = (SELECT id FROM chatusers WHERE nickname= 'null')");
    }

    public void messageToStory(String receiverNick, String senderNick, String time, String msg) throws SQLException { //сохраняем сообщения в БД
        connection.setAutoCommit(false);
        addToStory.setString(1, receiverNick);
        addToStory.setString(2, senderNick);
        addToStory.setString(3, time);
        addToStory.setString(4, msg);
        addToStory.executeUpdate();
        connection.commit();
    }

    public String getChatStory (String nickname) throws SQLException { //вытаскиваем сообщения из БД
        StringBuilder chatStory = new StringBuilder();
        connection.setAutoCommit(false);
        getStory.setString(1, nickname);
        getStory.setString(2, nickname);
        ResultSet story = getStory.executeQuery();

        while (story.next()) {
            String sender = story.getString(2);
            String receiver = story.getString(1);
            System.out.println(receiver);
            System.out.println(sender);
            String text = story.getString(4);
            String date = story.getString(3);
            //всем сообщение
            if (receiver.equals("null")) {
                String msgToAll = String.format("%s %s: %s", date, sender, text);
                chatStory.append(msgToAll).append("\n");
            } else {
                String prvMsg = String.format("%s %s %s %s: %s", date, sender, "приватно для", receiver, text);
                chatStory.append(prvMsg).append("\n");
            }
        }
        connection.commit();
        story.close();
        return chatStory.toString();
    }


}
