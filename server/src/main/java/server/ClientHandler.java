package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;

public class ClientHandler {
    Server server;
    Socket socket = null;
    DataInputStream in;
    DataOutputStream out;

    private String nick;
    private String login;
    private boolean isSubscribed;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    socket.setSoTimeout(120000); //проверяем активность клиента
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/auth ")) {
                            String[] token = str.split("\\s");
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server
                                    .getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {

                                if (!server.isLoginAuthorized(login)) {
                                    sendMsg("/authok " + newNick);
                                    nick = newNick;
                                    server.subscribe(this);
                                    socket.setSoTimeout(0); //если вошли, сбрасываем счетчик, иначе через 120 сек может выкинуть из чата
                                    isSubscribed = true;
                                    System.out.printf("Клиент %s подключился \n", nick);
                                    break;
                                } else {
                                    sendMsg("С этим логином уже авторизовались");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }

                        if (str.startsWith("/reg ")) {
                            String[] token = str.split("\\s");
                            if (token.length < 4) {
                                continue;
                            }
                            boolean b = server.getAuthService()
                                    .registration(token[1],token[2],token[3]);
                            if(b){
                                sendMsg("/regresult ok");
                            }else{
                                sendMsg("/regresult failed");
                            }
                        }

                    }
                    //цикл работы
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                out.writeUTF("/end");
                                break;
                            }

                            if (str.startsWith("/w ")) {
                                String[] token = str.split("\\s", 3);
                                if (token.length < 3) {
                                    continue;
                                }

                                server.privateMsg( token[1], this, token[2]);
                            }
                            if (str.startsWith("/chgnick ")) {
                                String[] token = str.split("\\s", 2);
                                if (token.length < 2) {
                                    continue;
                                }
                                server.changeNick(this, token[1]);
                            }
                            if (str.startsWith("/loadStory")) {
                                server.loadStory(this);
                            }
                        }
                        else {
                            server.broadcastMsg(str, this);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Клиент не активен более 120 секунд");
                    try {
                        server.privateMsg("Сервер", this, "Соедиение с сервером прервано из-за неактивности клиента");
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    sendMsg("/end");
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Клиент отключился");
                    if (isSubscribed) {
                        try {
                            server.unsubscribe(this);
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    }
                    try {
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
///

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    void sendMsg(String str) {
        try {
            out.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getLogin() {
        return login;
    }
}
