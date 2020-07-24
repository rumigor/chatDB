package client;

import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StorySaver {
    public Controller controller;
    public File file;
    public TextFlow chatText;
    private BufferedReader reader;
    private BufferedWriter writer;

    public StorySaver(Controller controller, File file, TextFlow chatText) throws IOException {
        this.controller = controller;
        this.file = file;
        this.chatText = chatText;
        this.reader = new BufferedReader(new FileReader(this.file));
        this.writer = new BufferedWriter(new FileWriter(this.file, true));
    }

    public BufferedWriter getWriter() {
        return writer;
    }

    public void readFile() throws IOException {
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            List<String> messages = new ArrayList<>();
            while (reader.ready()) {
                messages.add(reader.readLine());
            }
            int index = 0;
            if (messages.size() > 100) {index = messages.size() - 100;}
            Platform.runLater(() -> chatText.getChildren().add(new Text("----Предыдщие сессии чата----\n")));
            for (int i = index; i < messages.size() ; i++) {
                Text arhiveText = new Text(messages.get(i) + "\n");
                Platform.runLater(() -> chatText.getChildren().add(arhiveText));
            }
            Platform.runLater(() -> chatText.getChildren().add(new Text("----Текущая сессия чата----\n")));
            reader.close();
        }
    }

    public void writeFile(String msg) throws IOException {
        writer.write(msg);
    }
}
