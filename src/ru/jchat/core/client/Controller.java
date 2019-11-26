package ru.jchat.core.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

        memberListView.setItems(observableMemberList);
        setAuthorized(false);
        tabPane.getTabs().add(new DialogTab(GENERAL));
    }

    private void startListeningSocket() {
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
                    inMessage = gson.fromJson(s, Message.class);
                    if (inMessage.getType() == Message.BROADCAST_SERVICE_MESSAGE) {
                        if (inMessage.getText().startsWith("/list ")) {
                            String[] arr = inMessage.getText().substring(6).split("\\s");
                            Platform.runLater(() -> observableMemberList.setAll(arr));
                        } else {
                            getDialogTab(GENERAL).getTextArea().appendText(inMessage.toString());
                        }
                    } else if ((inMessage.getType() == Message.PRIVATE_SERVICE_MESSAGE)) {
                        if (inMessage.getText().equals("Opened from another place"))
                            showAlert(inMessage.getText());
                        else {
                            getDialogTab(GENERAL).getTextArea().appendText(inMessage.toString());
                            System.out.println(inMessage);
                        }
                    } else {
                        System.out.println(s);
                        String tabName = inMessage.getAddressNick().equals(myNick) ? inMessage.getSenderNick() : inMessage.getAddressNick();
                        getDialogTab(tabName).getTextArea().appendText(inMessage.toString());
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
    }

    private DialogTab getDialogTab(String contactNick) {
        List<Tab> tabs = tabPane.getTabs();
        for (Tab tab : tabs) {
            if (tab.getText().equals(contactNick)) {
                System.out.println(tab.getText());
                return (DialogTab) tab;
            }
        }
        DialogTab newTab = new DialogTab(contactNick);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                tabPane.getTabs().add(newTab);
            }
        });
        return newTab;
    }

    public void sendAuthMsg() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            startListeningSocket();
            outMessage = new Message(Message.AUTHENTICATION_REQUEST, loginField.getText() + " " + passField.getText(), new Date());
            out.writeUTF(gson.toJson(outMessage));
            loginField.clear();
            passField.clear();

        } catch (Exception e) {
            showAlert("Не удалось авторизоваться на сервере\n" + e.getMessage());
        }
    }

    public void sendMsg() {
        if (msgField.getText().equals("")) {
            msgField.requestFocus();
            return;
        }
        try {
            int type;
            String addressNick = tabPane.getSelectionModel().getSelectedItem().getText();
            if (addressNick.equals(GENERAL)) type = Message.BROADCAST_MESSAGE;
            else type = Message.PRIVATE_MESSAGE;
            outMessage = new Message(myNick, addressNick, new Date(), type);
            outMessage.setText(msgField.getText());
            out.writeUTF(gson.toJson(outMessage));
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            showAlert("Не удалось отправить сообщение");
        }
    }

    public void changeNick() {
        Message msg = new Message(Message.PRIVATE_SERVICE_MESSAGE, "/cn " + msgField.getText(), new Date());
        msgField.clear();
        msgField.requestFocus();
        try {
            out.writeUTF(gson.toJson(msg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void contactClick(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (memberListView.getSelectionModel().getSelectedItem() == null) {
            return;
        }
        tabPane.getSelectionModel().select(getDialogTab(memberListView.getSelectionModel().getSelectedItem()));
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
