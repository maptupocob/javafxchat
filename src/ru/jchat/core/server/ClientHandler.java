package ru.jchat.core.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.jchat.core.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Message inMessage;
    private Message outMessage;
    private GsonBuilder builder;
    private Gson gson;


    public void setNick(String nick) {
        this.nick = nick;
    }

    private String nick;

    public String getNick() {
        return nick;
    }

    public ClientHandler(Server server, Socket socket) {
        try {
            builder = new GsonBuilder();
            gson = builder.create();
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            server.getService().execute(() -> {
                try {
                    while (true) {
                        String tempMsg = in.readUTF();
                        Message message = gson.fromJson(tempMsg, Message.class);
                        if (message.getType() == Message.AUTHENTICATION_REQUEST) {
                            String[] data = message.getText().split("\\s");
                            String newNick = server.getAuthService().getNickByLoginAndPass(data[0], data[1]);
                            if (newNick != null) {
                                nick = newNick;
//                                System.out.println(nick);
                                outMessage = new Message(Message.AUTHENTICATION_OK, nick, new Date());
                                sendMsg(outMessage);
                                server.subscribe(this);
                                break;
                            } else {
                                outMessage = new Message(Message.AUTHENTICATION_DENY, "Неверный логин/пароль", new Date());
                                sendMsg(outMessage);
                            }
                        }
                    }
                    while (true) {
                        String s = in.readUTF();
                        inMessage = gson.fromJson(s, Message.class);
                        System.out.println(inMessage.toString());
                        if (inMessage.getType() == Message.BYE_BYE_MESSAGE) {
                            outMessage=new Message(Message.BROADCAST_SERVICE_MESSAGE, nick + " покинул чат", new Date());
                            server.broadcastMsg(outMessage);
                            break;
                        } else if (inMessage.getType() == Message.PRIVATE_MESSAGE) {
                            server.privateMsg(inMessage);
//                        } else if (msg.startsWith("/cn ")) {
//                            if (msg.substring(4).contains(" ")) sendMsg("Nick cannot contain spaces");
//                            else {
//                                server.changeNick(this, msg.substring(4));
//                            }
                        } else if (inMessage.getType() == Message.BROADCAST_MESSAGE) {
                            server.broadcastMsg(inMessage);

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendMsg(Message msg) {
        try {
            out.writeUTF(gson.toJson(msg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
