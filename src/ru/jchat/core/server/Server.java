package ru.jchat.core.server;

import ru.jchat.core.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

class Server {

    private static final String SERVER_NAME = "serverName";
    private ArrayList<Thread> threads;
    private Vector<ClientHandler> clients;
    private AuthService authService = null;
    private Message msg;
    private final Logger log = Logger.getLogger("");
    private SimpleDateFormat sdf;

    AuthService getAuthService() {
        return authService;
    }

    Logger getLog() {
        return log;
    }

    Server() {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            setLogging();
            clients = new Vector<>();
            threads = new ArrayList<>();
            authService = new AuthService();
            authService.connect();
            log.info("Server started... Waiting clients...");
            while (true) {
                Socket socket = serverSocket.accept();
                log.info("Client connected " + socket.getInetAddress() + " " + socket.getPort() + " " + socket.getLocalPort());
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            log.severe(e.getMessage());
        } catch (SQLException | ClassNotFoundException e) {
            log.severe("Не удалось запустить сервис авторизации");
        } finally {
            if (authService != null) authService.disconnect();
        }
    }

    private void setLogging() throws IOException {
        sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
        log.addHandler(new FileHandler("logs/log.txt", true));
        log.getHandlers()[1].setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return sdf.format(new Date(record.getMillis())) + " ThreadID: " + record.getThreadID() + "\n" + record.getLevel() + ": " + record.getMessage() + "\n";
            }
        });
    }

    void changeNick(ClientHandler clientHandler, String newNick) {
        String result = authService.changeNick(clientHandler.getNick(), newNick);
        if (result != null) {
            msg = new Message(Message.PRIVATE_SERVICE_MESSAGE, result, new Date());
            clientHandler.sendMsg(msg);
            if (result.equals("NickName is changed")) {
                String oldNick = clientHandler.getNick();
                clientHandler.setNick(newNick);
                msg = new Message(Message.BROADCAST_INFORMATION_MESSAGE, oldNick + " changed Nickname to " + newNick, new Date());
                log.info(oldNick + " changed Nickname to " + newNick);
                broadcastMsg(msg);
                sendMemberList();
            }
        } else {
            msg = new Message(Message.PRIVATE_SERVICE_MESSAGE, "Something went wrong on server", new Date());
            clientHandler.sendMsg(msg);
        }
    }

    synchronized void subscribe(ClientHandler clientHandler) {
        if (clients.add(clientHandler)) {
            log.info("Client " + clientHandler.getNick() + " on " + clientHandler.getSocket().getInetAddress() + " came in");
            sendMemberList();
            msg = new Message(Message.BROADCAST_INFORMATION_MESSAGE, clientHandler.getNick() + " came in chat!", new Date());
            broadcastMsg(msg);
        }
    }

    synchronized void unsubscribe(ClientHandler clientHandler) {
        if (clients.remove(clientHandler)) {
            log.info("Client " + clientHandler.getNick() + " on " + clientHandler.getSocket().getInetAddress() + " came out");
            sendMemberList();
            msg = new Message(Message.BROADCAST_INFORMATION_MESSAGE, clientHandler.getNick() + " came out chat!", new Date());
            broadcastMsg(msg);
        }
    }

    synchronized void privateMsg(Message message) {
        for (ClientHandler o : clients) {
            if ((o.getNick().equals(message.getAddressNick())) || (o.getNick().equals(message.getSenderNick()))) {
                o.sendMsg(message);
            }
        }
    }

    synchronized void broadcastMsg(Message msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }

    private synchronized void sendMemberList() {
        StringBuilder builder = new StringBuilder();
        builder.append("/list ");
        for (ClientHandler cl : clients) {
            builder.append(cl.getNick());
            builder.append(" ");
        }
        msg = new Message(Message.BROADCAST_SERVICE_MESSAGE, builder.toString(), new Date());
        broadcastMsg(msg);
    }

    synchronized void closeExistingConnection(String nick) {
        Message message = new Message(SERVER_NAME, nick, new Date(), Message.PRIVATE_SERVICE_MESSAGE);
        message.setText("Opened from another place");
        privateMsg(message);
        for (Thread thread : threads) {
            if (thread.getName().equals(nick)) {
                if (!thread.isAlive()) continue;
                while (!thread.isInterrupted()) {
                    thread.interrupt();
                    threads.remove(thread);
                    for (ClientHandler o : clients) {
                        if (o.getNick().equals(nick)) {
                            unsubscribe(o);
                            break;
                        }
                    }
                }
                log.info("Thread of " + nick + " closed because was opened from another place");
                return;
            }
        }
    }

    void addNewThread(Runnable run) {
        Thread thread = new Thread(run);
        thread.start();
        threads.add(thread);
    }
}
