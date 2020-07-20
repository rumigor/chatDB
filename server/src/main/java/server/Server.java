package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

public class Server {
    protected List<ClientHandler> clients;
    private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

    public Server() throws SQLException, ClassNotFoundException {
        clients = new Vector<>();
        authService = new SQLAuthService();

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
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void broadcastMsg(String msg, ClientHandler sender){
        String message = msg;
        if (!msg.startsWith(sender.getNick())) {
             message = String.format("%s: %s", sender.getNick(), msg);
        }
        for (ClientHandler client : clients) {
                client.sendMsg(message);
            }
    }

    void privateMsg(String nickname, ClientHandler client, String msg){
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
                    isNickNameValid = true;
                    break;
                }
            }
            if (!isNickNameValid) {
                client.sendMsg("В чате нет пользователя с таким ником!");
            }
        }

    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastMsg(clientHandler.getNick() + " подключился к чату!", clientHandler);
        privateMsg("Сервер", clientHandler, "Добропожаловать в чат!\nДля смены ника направьте на сервер команду: /chgnick NewNickName\n" +
                "Для отправки приватного сообщения перед текстом сообщения введите: /w usernickname\nДля выхода из чата направьте команду: /end");
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler clientHandler){
        broadcastMsg(clientHandler.getNick() + " вышел из чата", clientHandler);
        clients.remove(clientHandler);
        broadcastClientsList();
    }
    public void changeNick(ClientHandler client, String newNick) throws SQLException {
        if (authService.changeNick(client.getNick(), newNick)) {
            System.out.println(client.getNick() + " " + newNick);
            broadcastMsg(client.getNick() + " сменил ник на " + newNick, client);
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


}
