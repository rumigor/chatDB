package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class StoryController {

    @FXML
    public Button saveButton;
    @FXML
    public Button cancelButton;
    @FXML
    public TextArea msgArchive;

    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void saveToFile(ActionEvent actionEvent) throws IOException {
        String text = msgArchive.getText();
        BufferedWriter fw = new BufferedWriter(new FileWriter("chatstory.txt"));
        fw.write(text);
        fw.close();
    }

    public void clickCancelBtn(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            ((Stage) msgArchive.getScene().getWindow()).close();
        });
    }
}
