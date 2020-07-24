package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class Server {
    protected List<ClientHandler> clients;
    private AuthService authService;
    private ChatStory chatStory;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public AuthService getAuthService() {
        return authService;
    }

    public Server() throws SQLException, ClassNotFoundException {
        clients = new Vector<>();
        authService = new SQLAuthService();
        chatStory = new ChatStory(authService.getConnection());

        ServerSocket server = null;
        Socket socket;

        final int PORT = 8189;

        try {
            server = new ServerSocket(PORT);
            System.out.println("Сервер запущен!");

            while (true) {
                socket = server.accept();
                System.out.println("Клиент подключился");
                System.out.println("socket.getRemoteSocketAddress(): "+socket.getRemoteSocketAddress());
                System.out.println("socket.getLocalSocketAddress() "+socket.getLocalSocketAddress());
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                authService.disconnect();
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void broadcastMsg(String msg, ClientHandler sender, boolean isServer) throws SQLException {
        String message = msg;
        String nick;
        if (isServer) {
            nick = "Сервер";
        }
        else  {
            nick = sender.getNick();
        }
        message = String.format("%s: %s", nick, msg);
        chatStory.messageToStory("null", nick, sdf.format(new Date()), msg); //копируем сообщение в историю
        for (ClientHandler client : clients) {
                client.sendMsg(message);
            }

    }

    void privateMsg(String nickname, ClientHandler client, String msg) throws SQLException {
        if (nickname.equals("Сервер")) {
            client.sendMsg(nickname + ": "+ msg);
            return;
        }
        else {
            if (nickname.equals(client.getNick())) {return;}
            boolean isNickNameValid = false;
            String message = String.format("%s %s %s: %s", client.getNick(), "приватно для", nickname, msg);
            for (ClientHandler anotherClient : clients) {
                if (nickname.equals(anotherClient.getNick())) {
                    anotherClient.sendMsg(message);
                    client.sendMsg(message);
//                    chatStory.messageToStory(nickname, client.getNick(), sdf.format(new Date()), msg); //копируем сообщение в историю
                    isNickNameValid = true;
                    break;
                }
            }
            if (!isNickNameValid) {
                client.sendMsg("В чате нет пользователя с таким ником!");
            }
        }

    }

    public void subscribe(ClientHandler clientHandler) throws SQLException {
        clients.add(clientHandler);
        broadcastMsg(clientHandler.getNick() + " подключился к чату!", clientHandler, true);
//        chatStory.messageToStory("null", "Сервер", sdf.format(new Date()), clientHandler.getNick()+" подключился к чату!"); //копируем сообщение в историю
        privateMsg("Сервер", clientHandler, "Добропожаловать в чат!\nДля смены ника направьте на сервер команду: /chgnick NewNickName\n" +
                "Для отправки приватного сообщения перед текстом сообщения введите: /w usernickname\nДля выхода из чата направьте команду: /end");
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler clientHandler) throws SQLException {
        broadcastMsg(clientHandler.getNick() + " вышел из чата", clientHandler, true);
        chatStory.messageToStory("null", "Сервер", sdf.format(new Date()), clientHandler.getNick() + " вышел из чата"); //копируем сообщение в историю
        clients.remove(clientHandler);
        broadcastClientsList();
    }
//    public void changeNick(ClientHandler client, String newNick) throws SQLException {
//        if (authService.changeNick(client.getNick(), newNick)) {
//            System.out.println(client.getNick() + " " + newNick);
//            broadcastMsg(client.getNick() + " сменил ник на " + newNick, client);
//            chatStory.messageToStory("null", "Сервер", sdf.format(new Date()), client.getNick()+" сменил ник на " + newNick); //копируем сообщение в историю
//            client.setNick(newNick);
//            broadcastClientsList();
//        } else {privateMsg("Сервер", client, "данный никнейм уже занят");}
//    }

    public void broadcastClientsList() {
        StringBuilder sb = new StringBuilder("/clients ");
        for (ClientHandler o : clients) {
            sb.append(o.getNick() + " ");
        }
        for (ClientHandler client : clients) {
            client.sendMsg("/clientList " + sb.toString());
        }
    }

    public boolean isLoginAuthorized(String login){
        for (ClientHandler c : clients) {
            if(c.getLogin().equals(login)){
                return true;
            }
        }
        return false;
    }


    public void loadStory(ClientHandler client) throws SQLException {
        client.sendMsg("/loadStory " + chatStory.getChatStory(client.getNick())); //пересылаем историю клиенту
    }
}
