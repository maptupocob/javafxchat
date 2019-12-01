package ru.jchat.core.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.jchat.core.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;

class ClientHandler {
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Message inMessage;
    private Message outMessage;
    private Gson gson;
    private String nick;

    ClientHandler(Server server, Socket socket) {
        try {
            GsonBuilder builder = new GsonBuilder();
            gson = builder.create();
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            server.addNewThread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        String tempMsg = in.readUTF();
                        if (Thread.currentThread().isInterrupted()) break;
                        Message message = gson.fromJson(tempMsg, Message.class);
                        if (message.getType() == Message.AUTHENTICATION_REQUEST) {
                            String[] data = message.getText().split("\\s");
                            if (data.length < 2) {
                                outMessage = new Message(Message.AUTHENTICATION_DENY, "Недопустимые логин/пароль", new Date());
                                sendMsg(outMessage);
                            } else {
                                String newNick = server.getAuthService().getNickByLoginAndPass(data[0], data[1]);
                                if (newNick != null) {
                                    nick = newNick;
                                    server.closeExistingConnection(newNick);
                                    Thread.currentThread().setName(nick);
                                    outMessage = new Message(Message.AUTHENTICATION_OK, nick, new Date());
                                    sendMsg(outMessage);
                                    server.subscribe(this);
                                    break;
                                } else {
                                    outMessage = new Message(Message.AUTHENTICATION_DENY, "Неверный логин/пароль", new Date());
                                    server.getLog().warning(socket.getInetAddress() + " Неверный логин/пароль " + data[0]);
                                    sendMsg(outMessage);
                                }
                            }
                        }
                    }
                    while (!Thread.currentThread().isInterrupted()) {
                        String s = in.readUTF();
                        if (Thread.currentThread().isInterrupted()) break;
                        inMessage = gson.fromJson(s, Message.class);
                        System.out.println(inMessage.toString());
                        if (inMessage.getType() == Message.BYE_BYE_MESSAGE) {
                            outMessage = new Message(Message.BROADCAST_SERVICE_MESSAGE, nick + " покинул чат", new Date());
                            server.broadcastMsg(outMessage);
                            break;
                        } else if (inMessage.getType() == Message.PRIVATE_MESSAGE) {
                            server.privateMsg(inMessage);
                        } else if ((inMessage.getType() == Message.PRIVATE_SERVICE_MESSAGE) && (inMessage.getText().startsWith("/cn "))) {
                            if (inMessage.getText().substring(4).contains(" "))
                                sendMsg(new Message(Message.PRIVATE_SERVICE_MESSAGE, "Nick cannot contain spaces", new Date()));
                            else if (inMessage.getText().substring(4).equals(""))
                                sendMsg(new Message(Message.PRIVATE_SERVICE_MESSAGE, "Nick cannot be empty", new Date()));
                            else {
                                server.changeNick(this, inMessage.getText().substring(4));
                                Thread.currentThread().setName(this.getNick());
                            }
                        } else if (inMessage.getType() == Message.BROADCAST_MESSAGE) {
                            server.broadcastMsg(inMessage);

                        }
                    }
                } catch (IOException e) {
                    if ((e.getMessage() != null) && (e.getMessage().equals("Connection reset"))) {
                    } else {
//                        e.printStackTrace();
                    }
                    server.unsubscribe(this);
                } finally {
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                server.getLog().info(nick + " thread is closed");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void setNick(String nick) {
        this.nick = nick;
    }

    String getNick() {
        return nick;
    }

    Socket getSocket() {
        return socket;
    }

    void sendMsg(Message msg) {
        try {
            out.writeUTF(gson.toJson(msg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
