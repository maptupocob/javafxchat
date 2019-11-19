package ru.jchat.core.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public ExecutorService getService() {
        return service;
    }

    private ExecutorService service;
    private Vector<ClientHandler> clients;
    private AuthService authService = null;

    public AuthService getAuthService() {
        return authService;
    }

    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            clients = new Vector<>();
            service = Executors.newCachedThreadPool();
            authService = new AuthService();
            authService.connect();
            System.out.println("Server started... Waiting clients...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected " + socket.getInetAddress() + " " + socket.getPort() + " " + socket.getLocalPort());
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Не удалось запустить сервис авторизации");
        } finally {
            authService.disconnect();
        }
    }

    public void changeNick(ClientHandler clientHandler, String newNick) {
        String msg = authService.changeNick(clientHandler.getNick(), newNick);
        if (msg != null) {
            clientHandler.sendMsg(msg);
            if (msg == "NickName is changed") {
                String oldNick = clientHandler.getNick();
                clientHandler.setNick(newNick);
                broadcastMsg(oldNick + " changed Nickname to " + newNick);
                oldNick = null;
            }
        } else clientHandler.sendMsg("Something went wrong on server");
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public void privateMsg(String nick, String msg) {
        for (ClientHandler o : clients) {
            if (o.getNick().equals(nick)) {
                o.sendMsg(msg);
            }
        }
    }

    public void broadcastMsg(String msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }

    public void sendMemberList(String nick) {
        StringBuilder builder = new StringBuilder();
        builder.append("/list ");
        for (ClientHandler cl: clients) {
            builder.append(cl.getNick());
            builder.append(" ");
        }
        broadcastMsg(builder.toString());
    }
}
