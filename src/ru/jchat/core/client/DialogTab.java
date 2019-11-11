package ru.jchat.core.client;

import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;

public class DialogTab extends Tab {
    TextArea textArea;

    public DialogTab(String text) {
        super(text);
        textArea = new TextArea();
        this.setClosable(true);
        this.setContent(textArea);
    }

    public void setDialogText(String text) {
        textArea.setText(text);
    }




}
