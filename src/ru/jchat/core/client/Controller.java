package ru.jchat.core.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import ru.jchat.core.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

//CREATE TABLE users (
//    id       INTEGER PRIMARY KEY AUTOINCREMENT,
//            login    TEXT    UNIQUE,
//            password TEXT,
//            nick     TEXT    UNIQUE
//            );


public class Controller implements Initializable {

    public static final String GENERAL = "<General>";
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
    ListView<String> memberListView;
    @FXML
    TabPane tabPane;


    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    //    private DialogTab activeTab;
    private String myNick;
    private Message inMessage;
    private Message outMessage;
    private List<String> chatMembers;
    private ObservableList<String> observableMemberList = FXCollections.observableArrayList();
    private Gson gson = new GsonBuilder().create();

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
        } else {
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
            authPanel.setVisible(true);
            authPanel.setManaged(true);
            tabPane.setVisible(false);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            memberListView.setItems(observableMemberList);
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setAuthorized(false);
            tabPane.getTabs().add(new DialogTab(GENERAL));
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String s = in.readUTF();
                        inMessage = gson.fromJson(s, Message.class);
                        if (inMessage.getType() == Message.AUTHENTICATION_OK) {
                            setAuthorized(true);
                            myNick = inMessage.getText();
                            break;
                        }
                        if (inMessage.getType() == Message.AUTHENTICATION_DENY) showAlert(inMessage.getText());
                    }

                    while (true) {
                        String s = in.readUTF();
                        if (s.startsWith("/list ")) {
                            String[] arr = s.substring(6).split("\\s");
                            observableMemberList.setAll(arr);
                        } else {
                            ((DialogTab) tabPane.getSelectionModel().getSelectedItem()).getTextArea().appendText(s + "\n");
                        }
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
            outMessage = new Message(Message.AUTHENTICATION_REQUEST, loginField.getText() + " " + passField.getText(), new Date());
            out.writeUTF(gson.toJson(outMessage));
            loginField.clear();
            passField.clear();
        } catch (Exception e) {
            showAlert("Не удалось авторизоваться на сервере/n" + e.getMessage());
        }
    }

    public void sendMsg() {
        try {
            int type;
            String addressNick = tabPane.getSelectionModel().getSelectedItem().getText();
            if (addressNick == GENERAL) type = Message.BROADCAST_MESSAGE;
            else type = Message.PRIVATE_MESSAGE;
            outMessage = new Message(myNick, addressNick, new Date(), type);
            outMessage.setText(msgField.getText());
//            String msg = msgField.getText();
//            displayMsg(outMessage);
            out.writeUTF(gson.toJson(outMessage));
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            showAlert("Не удалось отправить сообщение");
        }
    }

    private void displayMsg(Message msg) {
        DialogTab dt=null;
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getText().equals(msg.getAddressNick())) {
                dt = (DialogTab) tab;
                break;
            }
        }
        if (dt == null){
            dt = new DialogTab(msg.getAddressNick());
            tabPane.getTabs().add(dt);
        }
        tabPane.getSelectionModel().select(dt);
        dt.getTextArea().setText(msg.getText());


//        msg.getAddressNick())startPrivateChat(msg.split("\\s")[1]);

    }

    public void startPrivateChat(String name) {
        DialogTab newTab = new DialogTab(name);
        newTab.setClosable(true);
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
        //TODO
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
