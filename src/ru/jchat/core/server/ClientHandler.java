package ru.jchat.core.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public void setNick(String nick) {
        this.nick = nick;
    }

    private String nick;

    public String getNick() {
        return nick;
    }

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            server.getService().execute(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/auth ")) {
                            String[] data = msg.split("\\s");
                            String newNick = server.getAuthService().getNickByLoginAndPass(data[1], data[2]);
                            if (newNick != null) {
                                nick = newNick;
                                sendMsg("/authok");
                                server.subscribe(this);
                                break;
                            } else {
                                sendMsg("Неверный логин/пароль");
                            }
                        }
                    }
                    while (true) {
                        String msg = in.readUTF();
                        System.out.println(nick + ": " + msg);
                        if (msg.equals("/end")) {
                            server.broadcastMsg(nick + " покинул чат");
                            break;
                        } else if (msg.startsWith("/w ")) {
                            String targetNick = msg.split("\\s")[1];
                            String tempMsg = msg.substring(msg.indexOf(' ', 4));
                            server.privateMsg(targetNick, "private from " + nick + ":" + tempMsg);
                        } else if (msg.startsWith("/cn ")) {
                            if (msg.substring(4).contains(" ")) sendMsg("Nick cannot contain spaces");
                            else {
                                server.changeNick(this, msg.substring(4));
                            }
                        } else {
                            server.broadcastMsg(nick + ": " + msg);

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

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
