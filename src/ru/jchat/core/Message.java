package ru.jchat.core;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {
    private String senderNick;
    private String addressNick;
    private String text;
    private Date date;

    public int getType() {
        return type;
    }

    private int type;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
    public static final int BROADCAST_SERVICE_MESSAGE = 1;
    public static final int BROADCAST_INFORMATION_MESSAGE = 10;
    public static final int BROADCAST_MESSAGE = 2;
    public static final int PRIVATE_SERVICE_MESSAGE = 3;
    public static final int PRIVATE_MESSAGE = 4;
    public static final int AUTHENTICATION_REQUEST = 5;
    public static final int AUTHENTICATION_OK = 6;
    public static final int AUTHENTICATION_DENY = 7;
    public static final int BYE_BYE_MESSAGE = 8;


    public Message(String senderNick, String addressNick, Date date, int type) {
        this.senderNick = senderNick;
        this.addressNick = addressNick;
        this.date = date;
        this.type = type;
    }

    public Message(int type, String text, Date date) {
        this.text = text;
        this.date = date;
        this.type = type;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderNick() {
        return senderNick;
    }

    public String getAddressNick() {
        return addressNick;
    }

    public String getText() {
        return text;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        return sdf.format(date) + " " + (senderNick == null?"": senderNick) + "\n" + text+ "\n";
    }
}
