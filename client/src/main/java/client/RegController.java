package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegController {

    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public TextField nicknameField;

    public void tryToReg(ActionEvent actionEvent) {
        controller.tryToReg(loginField.getText().trim()
                , passwordField.getText().trim()
                , nicknameField.getText().trim());
    }

    public void clickCancelBtn(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            ((Stage) loginField.getScene().getWindow()).close();
        });
    }

    public void addMessage(String msg) {
        if (msg.equals("Регистрация прошла успешно")) {
            Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Регистрация в чате");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
            Platform.runLater(() -> {
                ((Stage) loginField.getScene().getWindow()).close();
            });
            });
        } else {
            Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Регистрация в чате");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
            });
            loginField.clear();
            passwordField.clear();
            nicknameField.clear();
        }
    }
}
