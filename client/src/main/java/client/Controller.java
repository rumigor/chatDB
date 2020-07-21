package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class Controller implements Initializable {
//    @FXML
//    public TextArea textArea;
    @FXML
    public TextField textField;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public HBox authPanel;
    @FXML
    public HBox msgPanel;
    @FXML
    public ListView<String > clientList;
    @FXML
    public ComboBox <String> smilesBox;
    @FXML
    public MenuBar menu;
    @FXML
    public TextFlow chatText;
    @FXML
    public ScrollPane sp;


    private final int PORT = 8189;
    private final String IP_ADDRESS = "localhost";
    private final String CHAT_TITLE_EMPTY = "Java-chat v.1.1";



    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private boolean authenticated;
    private String nick;
    private Stage stage;
    private Stage regStage;
    private Stage chatArchive;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    RegController regController;
    StoryController storyController;



    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);
        menu.setVisible(authenticated);
        menu.setManaged(authenticated);
        if (!authenticated) {
            nick = "";
        }
        setTitle(nick);
        Platform.runLater(() -> chatText.getChildren().clear());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textField.getScene().getWindow();
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    System.out.println("bye");
                    if (socket != null && !socket.isClosed()) {
                        try {
                            out.writeUTF("/end");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });
        setAuthenticated(false);
        regStage = createRegWindow();
        chatArchive = createChatArchiveWindow();
    }


    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/authok ")) {
                            nick = str.split("\\s")[1];
                            setAuthenticated(true);
                            break;
                        } else if (str.equals("Неверный логин / пароль") || str.equals("С этим логином уже авторизовались")) {
                            Platform.runLater(() -> {
                                Text text1 = new Text(str + "\n");
                                text1.setFill(Color.BLACK);
                                text1.setFont(Font.font("Helvetica", FontPosture.ITALIC, 12));
                                chatText.getChildren().addAll(text1);
                                sp.setVvalue( 1.0d );
                            });
                        }

                        if (str.startsWith("/regresult ")) {
                            String result = str.split("\\s")[1];
                            if (result.equals("ok")) {
                                regController.addMessage("Регистрация прошла успешно");
                            } else {
                                regController.addMessage("Регистрация не получилась, возможно логин или никнейм заняты");
                            }
                        }
                        if (str.equals("/end")) {
                            setAuthenticated(false);
                            out.close();
                            in.close();
                            Platform.runLater(() -> chatText.getChildren().clear());
                            break;
                        }
                    }

                    while (!socket.isClosed()) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                setAuthenticated(false);
                                Platform.runLater(() -> chatText.getChildren().clear());
                                break;
                            }

                            if (str.startsWith("/clientList")) {
                                String[] token = str.split("\\s");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    clientList.getItems().add("Список пользователей:");
                                    for (int i = 2; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });

                            }
                            if (str.startsWith("/chgnick")) {
                                String[] token = str.split("\\s", 2);
                                nick = token[2];
                                setTitle(nick);
                            }
                            if (str.startsWith("/loadStory")){
                                String[] token = str.split("\\s", 2);
                                storyController.msgArchive.appendText(token[1]); //загружаем историю в текстАрею окна Истории сообщений
                            }
                        }
                        else {
                            Platform.runLater(() -> {
                                String[] token = str.split("\\s", 2);
                                if (str.startsWith("Сервер")) {
                                    Text time = new Text(sdf.format(new Date())+ " ");
                                    Text textServ = new Text(token[0]+" ");
                                    textServ.setFill(Color.BLACK);
                                    textServ.setFont(Font.font("Helvetica", FontWeight.BOLD, 12));
                                    Text text1 = new Text(token[1] + "\n");
                                    text1.setFill(Color.BLACK);
                                    text1.setFont(Font.font("Helvetica", FontPosture.ITALIC, 12));
                                    chatText.getChildren().addAll(time, textServ, text1);
                                    sp.setVvalue( 1.0d );
                                }
                                else {
                                    if (token[0].endsWith(":")) {
                                        Text nickname = new Text();
                                        if (token[0].equals(nick+":")) {
                                            nickname = new Text("Я: ");
                                            nickname.setFill(Color.rgb(255,165,0));
                                        } else {
                                            nickname = new Text(token[0] + " ");
                                            nickname.setFill(Color.rgb(50,205,50));
                                        }
                                        nickname.setFont(Font.font("Helvetica", FontWeight.BOLD, 12));
                                        Text msg = new Text(token[1] + "\n");
                                        msg.setFill(Color.BLACK);
                                        msg.setFont(Font.font("Helvetica", FontWeight.NORMAL, 12));
                                        Text time = new Text(sdf.format(new Date())+ " ");
                                        chatText.getChildren().addAll(time, nickname, msg);
                                        sp.setVvalue( 1.0d );
                                    } else if (token[1].startsWith("приватно")) {
                                        token = str.split("\\s", 5);
                                        String nickText = String.format("%s %s %s %s ", token[0], token[1], token[2], token[3]);
                                        Text nickname = new Text(nickText);
                                        nickname.setFont(Font.font("Helvetica", FontWeight.BOLD, 12));
                                        nickname.setFill(Color.rgb(255, 99, 71));
                                        Text msg = new Text(token[4] + "\n");
                                        msg.setFont(Font.font("Helvetica", FontPosture.ITALIC, 12));
                                        Text time = new Text(sdf.format(new Date())+ " ");
                                        chatText.getChildren().addAll(time, nickname, msg);
                                        sp.setVvalue( 1.0d );
                                    } else {
                                        Text text1 = new Text(str + "\n");
                                        text1.setFill(Color.BLACK);
                                        text1.setFont(Font.font("Helvetica", FontPosture.ITALIC, 12));
                                        Text time = new Text(sdf.format(new Date()) + " ");
                                        chatText.getChildren().addAll(time, text1);
                                        sp.setVvalue( 1.0d );
                                    }
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendMsg(ActionEvent actionEvent) {
        try {
            out.writeUTF(textField.getText());
            if (textField.getText().startsWith("/chgnick")) {
                String [] msg = textField.getText().split("\\s");
                setTitle(msg[1]);
            }
            textField.requestFocus();
            textField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(String.format("/auth %s %s", loginField.getText().trim(), passwordField.getText().trim()));
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String nick) {
        Platform.runLater(() -> {
            stage.setTitle(CHAT_TITLE_EMPTY + " : " + nick);
        });
    }


    public void smilesAdd(ActionEvent actionEvent) {
        textField.appendText(smilesBox.getValue());
    }

    public void changeNick(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog("Nickname");

        dialog.setTitle("Chat");
        dialog.setHeaderText("Введите новый ник:");
        dialog.setContentText("Ник:");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(name -> {
            try {
                out.writeUTF("/chgnick " + name);
                nick = name;
                setTitle(nick);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        }

    public void offline(ActionEvent actionEvent) {
        try {
            out.writeUTF("/end");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copyText(ActionEvent actionEvent) {
        textField.copy();
    }

    public void pasteText(ActionEvent actionEvent) {
        textField.paste();
    }

    public void cutText(ActionEvent actionEvent) {
        textField.cut();
    }

    public void clickClientList(MouseEvent mouseEvent) {
        if (clientList.getSelectionModel().getSelectedItem().equals("Список пользователей:") || clientList.getSelectionModel().getSelectedItem().equals(nick)) {
            return;
        }
        String receiver = clientList.getSelectionModel().getSelectedItem();
        textField.setText(String.format("/w %s ", receiver));
    }

    public void showRegWindow(ActionEvent actionEvent) {
        regStage.show();
    }

    private Stage createRegWindow() {
        Stage stage = new Stage();
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();

            stage.setTitle("Chat reg window");
            stage.setScene(new Scene(root, 300, 150));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            regController = fxmlLoader.getController();
            regController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return stage;
    }
    public void tryToReg(String login, String password, String nickname) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(String.format("/reg %s %s %s", login, password, nickname));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage createChatArchiveWindow() { //создаем окно для истории сообщений
        Stage stage = new Stage();
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/story.fxml"));
            Parent root = fxmlLoader.load();

            stage.setTitle("История сообщений чата");
            stage.setScene(new Scene(root, 480, 360));
            stage.initModality(Modality.NONE);

            storyController = fxmlLoader.getController();
            storyController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return stage;
    }


    public void openStory(ActionEvent actionEvent) throws IOException { //открываем историю сообщений
        chatArchive.show();
        out.writeUTF("/loadStory");
    }
}
