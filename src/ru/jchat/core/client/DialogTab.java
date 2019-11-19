package ru.jchat.core.client;

import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;

public class DialogTab extends Tab {
    TextArea textArea;

    public DialogTab(String text) {
        super(text);
        textArea = new TextArea();
        this.setClosable(text == "<General>" ? false : true);
        this.setContent(textArea);
    }

    public TextArea getTextArea() {
        return textArea;
    }
}
