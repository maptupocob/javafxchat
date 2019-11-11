package ru.jchat.core.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

//CREATE TABLE users (
//    id       INTEGER PRIMARY KEY AUTOINCREMENT,
//            login    TEXT    UNIQUE,
//            password TEXT,
//            nick     TEXT    UNIQUE
//            );


public class Controller implements Initializable {
    @FXML
    TextArea textArea;
    @FXML
    TextField msgField;
    @FXML
    HBox authPanel;
    @FXML
    HBox msgPanel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passField;
    @FXML
    VBox participants;
    @FXML
    Tab generalTabChat;
    @FXML
    TabPane tabPane;


    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    final String SERVER_IP = "localhost";
    final int SERVER_PORT = 8189;

    private boolean authorized;

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
        if (authorized) {
            msgPanel.setVisible(true);
            msgPanel.setManaged(true);
            authPanel.setVisible(false);
            authPanel.setManaged(false);
            tabPane.setVisible(true);
//            generalTabChat.setDisable(false);
        } else {
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
            authPanel.setVisible(true);
            authPanel.setManaged(true);
            tabPane.setVisible(false);
//            generalTabChat.setDisable(true);
        }
    }

    public void startPrivateChat(String name){
        DialogTab newTab = new DialogTab(name);
        newTab.setClosable(true);
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
        //TODO
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setAuthorized(false);
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String s = in.readUTF();
                        if (s.equals("/authok")) {
                            setAuthorized(true);
                            break;
                        }
                        if (s.equals("Неверный логин/пароль")) showAlert("Неверный логин/пароль");
//                        textArea.appendText(s + "\n");
                    }
                    while (true) {
                        String s = in.readUTF();
                        textArea.appendText(s + "\n");
                    }
                } catch (IOException e) {
                    showAlert("Нет соединения с сервером");
                } finally {
                    setAuthorized(false);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        showAlert("Не удалось корректно завершить соединение");
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            showAlert("Невозможно подключиться к серверу");
        }
    }

    public void sendAuthMsg() {
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passField.getText());
            loginField.clear();
            passField.clear();
        } catch (Exception e) {
            showAlert("Не удалось авторизоваться на сервере");
        }
    }

    public void sendMsg() {
        try {
            String msg = msgField.getText();
            if (msg.startsWith("/w ")) startPrivateChat(msg.split("\\s")[1]);
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            showAlert("Не удалось отправить сообщение");
        }
    }

    public void showAlert(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Возникли проблемы");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

}
