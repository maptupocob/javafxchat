package ru.jchat.core;

import java.util.Date;

public class Message {
    private String senderNick;
    private String addressNick;
    private String text;
    private Date date;

    public Message(String senderNick, String addressNick, Date date) {
        this.senderNick = senderNick;
        this.addressNick = addressNick;
        this.date = date;
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
}
