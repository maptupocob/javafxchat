package ru.jchat.core.server;

import ru.jchat.core.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Date;
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
    private Message msg;

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
        String result = authService.changeNick(clientHandler.getNick(), newNick);
        if (result != null) {
            msg = new Message(Message.PRIVATE_SERVICE_MESSAGE, result, new Date());
            clientHandler.sendMsg(msg);
            if (result == "NickName is changed") {
                String oldNick = clientHandler.getNick();
                clientHandler.setNick(newNick);
                msg = new Message(Message.BROADCAST_INFORMATION_MESSAGE, oldNick + " changed Nickname to " + newNick, new Date());
                broadcastMsg(msg);
            }
        } else {
            msg = new Message(Message.PRIVATE_SERVICE_MESSAGE, "Something went wrong on server", new Date());
            clientHandler.sendMsg(msg);
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public void privateMsg(Message message) {
        for (ClientHandler o : clients) {
            if ((o.getNick().equals(message.getAddressNick())) || (o.getNick().equals(message.getSenderNick()))) {
                o.sendMsg(message);
            }
        }
    }

    public void broadcastMsg(Message msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }

    public void sendMemberList(String nick) {
        StringBuilder builder = new StringBuilder();
        builder.append("/list ");
        for (ClientHandler cl : clients) {
            builder.append(cl.getNick());
            builder.append(" ");
        }
        msg = new Message(Message.BROADCAST_SERVICE_MESSAGE, builder.toString(), new Date());
        broadcastMsg(msg);
    }
}
