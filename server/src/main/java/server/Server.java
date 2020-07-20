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
        chatStory = new ChatStory();

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
                chatStory.disconnect();
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void broadcastMsg(String msg, ClientHandler sender) throws SQLException {
        String message = msg;
        if (!msg.startsWith(sender.getNick())) {
             message = String.format("%s: %s", sender.getNick(), msg);
             chatStory.messageToStory(sdf.format(new Date()), sender.getNick(), msg, null);
        }
        for (ClientHandler client : clients) {
                client.sendMsg(message);
            }

    }

    void privateMsg(String nickname, ClientHandler client, String msg) throws SQLException {
        if (nickname.equals("Сервер")) {
            client.sendMsg(nickname + ": "+ msg);
//            chatStory.messageToStory(sdf.format(new Date()), client.getNick(), msg, nickname);
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
                    chatStory.messageToStory(sdf.format(new Date()), nickname, msg, client.getNick());
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
        broadcastMsg(clientHandler.getNick() + " подключился к чату!", clientHandler);
        chatStory.messageToStory(sdf.format(new Date()), "Сервер", clientHandler.getNick()+" подключился к чату!", null);
        privateMsg("Сервер", clientHandler, "Добропожаловать в чат!\nДля смены ника направьте на сервер команду: /chgnick NewNickName\n" +
                "Для отправки приватного сообщения перед текстом сообщения введите: /w usernickname\nДля выхода из чата направьте команду: /end");
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler clientHandler) throws SQLException {
        broadcastMsg(clientHandler.getNick() + " вышел из чата", clientHandler);
        chatStory.messageToStory(sdf.format(new Date()), "Сервер", clientHandler.getNick() + " вышел из чата", null);
        clients.remove(clientHandler);
        broadcastClientsList();
    }
    public void changeNick(ClientHandler client, String newNick) throws SQLException {
        if (authService.changeNick(client.getNick(), newNick)) {
            System.out.println(client.getNick() + " " + newNick);
            broadcastMsg(client.getNick() + " сменил ник на " + newNick, client);
            chatStory.messageToStory(sdf.format(new Date()), "Сервер", client.getNick()+" сменил ник на " + newNick, null);
            client.setNick(newNick);
            broadcastClientsList();
        } else {privateMsg("Сервер", client, "данный никнейм уже занят");}
    }

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
        client.sendMsg("/loadStory " + chatStory.getChatStory(client.getNick()));
    }
}
