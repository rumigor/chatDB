package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

public class Server {
    protected List<ClientHandler> clients;
    private AuthService authService;
    private ChatStory chatStory;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private ExecutorService executorService;
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public AuthService getAuthService() {
        return authService;
    }

    public Server() throws SQLException, ClassNotFoundException, IOException {
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        Handler fileHandler = new FileHandler("server.log", true);
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(consoleHandler);
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);
        clients = new Vector<>();
        authService = new SQLAuthService();
        chatStory = new ChatStory(authService.getConnection());

        ServerSocket server = null;
        Socket socket;

        final int PORT = 8189;

        try {
            server = new ServerSocket(PORT);
            logger.log(Level.INFO, "Сервер запущен!");
            /* ограничиваем количество подключений к серверу, чтобы сервер не упал, если число подключений превысит критическую массу
            * либо просто хотим ограничить число участников чата*/
            executorService = Executors.newFixedThreadPool(5);

            while (true) {
                socket = server.accept();
                logger.log(Level.INFO, "Клиент подключился");
                logger.log(Level.INFO, "socket.getRemoteSocketAddress(): "+socket.getRemoteSocketAddress());
                logger.log(Level.INFO, "socket.getLocalSocketAddress() "+socket.getLocalSocketAddress());
                Socket finalSocket = socket;
                // --подключаем клиентов в разных потоках, чтобы снизить нагрузку на сервер
                executorService.execute(new ClientHandler(this, finalSocket));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                executorService.shutdown();
                authService.disconnect();
                server.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage());
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
